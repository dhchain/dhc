package org.dhc.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.dhc.util.Callback;
import org.dhc.util.Constants;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ExpiringMap;
import org.dhc.util.GsonUtil;
import org.dhc.util.Message;
import org.dhc.util.StringUtil;
import org.dhc.util.TAddress;
import org.dhc.util.ThreadExecutor;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class Peer {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final GsonUtil gsonUtil = GsonUtil.getInstance();
	private static final Map<InetSocketAddress, Peer> peers = new ConcurrentHashMap<InetSocketAddress, Peer>();
	private static InetSocketAddress myself;

	private TAddress tAddress;// it is set in process(Message message), don't set it here.
	private String networkIdentifier;// process(Message message), don't set it here.
	private int power;// process(Message message), don't set it here.
	private Boolean inUse;
	private int possiblePower;
	private PeerType type;
	private InetSocketAddress inetSocketAddress;
	private transient long lastSeen;// process(Message message), don't set it here.
	private transient long timeAdded;
	private transient Socket socket;
	

	private transient volatile boolean receiverStarted = false;
	private transient JsonWriter writer;
    private transient JsonReader reader;
    private transient Map<String, PeerLock> locks = new ConcurrentHashMap<String, PeerLock>();
    private transient ExpiringMap<String, Callback> callbacks =  new ExpiringMap<String, Callback>();
    private transient ScheduledFuture<?> trimPeerFuture;
    
    public static Peer getInstance(InetSocketAddress inetSocketAddress) {

    	synchronized (peers) {
    		Peer instance = peers.get(inetSocketAddress);
        	if(instance != null) {
        		return instance;
        	}
        	
        	instance = new Peer();
    		instance.setInetSocketAddress(inetSocketAddress);
    		instance.timeAdded = System.currentTimeMillis();
    		instance.peersPut();
        	return instance;
    	}

		
    }

    public static Peer getInstance(Socket socket) {
    	InetSocketAddress inetSocketAddress = new InetSocketAddress( socket.getInetAddress(), socket.getPort());
    	Peer instance = getInstance(inetSocketAddress);
    	instance.socket = socket;
    	return instance;
    }
    
    public static void closeAllPeers() {
    	synchronized (peers) {
	    	for(Peer peer: peers.values()) {
	    		peer.close("Close peer because of closeAllPeers()");
	    	}
    	}
    }
    
    private Peer() {

    }
    
    private boolean peersPut() {
    	synchronized (peers) {
    		Peer localVarPeer = peers.get(inetSocketAddress);
    		if(localVarPeer == this) {
    			logger.trace("peersPut peers already contains me inetSocketAddress = {} Peer@{}", inetSocketAddress, super.hashCode());
    			return true;
    		}
    		if(localVarPeer == null) {
    			peers.put(inetSocketAddress, this);
    			logger.trace("peersPut inetSocketAddress = {} Peer@{}", inetSocketAddress, super.hashCode());
    			return true;
    		}
    		logger.trace("peersPut already contains another peer Peer@{} for inetSocketAddress = {}. This is Peer@{}", localVarPeer.getSuperHashCode(), inetSocketAddress, super.hashCode());
    		return false;
		}
    }
    
    public int getSuperHashCode() {
    	return super.hashCode();
    }
    
    private boolean peersRemove() {
    	boolean result;
    	synchronized (peers) {
    		Peer localVarPeer = peers.get(inetSocketAddress);
    		if(this == localVarPeer) {
    			peers.remove(inetSocketAddress);
    			logger.trace("peersRemove inetSocketAddress = {} Peer@{}", inetSocketAddress, super.hashCode());
    			result = true;
    		}
    		result = false;
		}
    	if(result) {
    		Network.getInstance().removePeer(this);
    	}
    	return result;
    }
    
    private void scheduleTrim() {
    	
    	trimPeerFuture = ThreadExecutor.getInstance().schedule(new DhcRunnable("Trim peer " + toString()) {
			
			@Override
			public void doRun() {
				trimPeer();
				
			}
		}, Constants.MINUTE * 3);
	}

	private void trimPeer() {
		//logger.info("trimPeer() peer: {}", this);
		if(isClosed()) {
			close("Close peer because it is already closed");
			return;
		}
		
		if(!peers.values().contains(this)) {
			close("Peers does not contain this peer");
			return;
		}
		if(getTAddress() == null) {
			close("TAdress is null");
			return;
		}
		
		if(isThin()) {
			long ago = System.currentTimeMillis() - getLastSeen();
			if(ago > Constants.HOUR) {
				close("Thin peer was seen over one hour ago");
				return;
			}
			if(ago > Constants.MINUTE * 10) {
				send(new KeepAliveMessage());
			}
			scheduleTrim();
			return;
		}
		
		Network network = Network.getInstance();
		if(!network.getAllPeers().contains(this) && !getInUse()) {
			close("Network buckets do not contain this peer and it is not in use");
			return;
		}
		if(!network.getAllPeers().contains(this)) {
			send(new ClosePeerIfNotInUseMessage());
		}
		scheduleTrim();
	}

	public boolean isClosed() {
    	return socket == null || socket.isClosed();
    }
    
	public boolean connectSocket() throws Exception {
		
		if(inetSocketAddress.equals(myself)) {
			close("Cannot connect to yourself " + myself);
			return false;
		}
		
		synchronized(this) {
			
			logger.trace("Locked connectSocket() for Peer@{}", super.hashCode());
			
			if(!peersPut()) {
				logger.trace("Another peer is already in peers for inetSocketAddress = {}", inetSocketAddress);
				return false;
			}

			try {
				Socket localSocket = socket;
				if (localSocket != null && !localSocket.isClosed()) {
					return false;
				}
				localSocket = new Socket();
				long start = System.currentTimeMillis();
				try {
					logger.trace("Try to connect to inetSocketAddress = {}, Peer@{}", inetSocketAddress, super.hashCode());
					localSocket.connect(inetSocketAddress, (int) Constants.SECOND * 5);
					
				} catch (IOException e) {
					logger.trace("Failed in {} ms to connect to socket inetSocketAddress = {}, Peer@{}", System.currentTimeMillis() - start, inetSocketAddress, super.hashCode());
					Bootstrap.getInstance().getCandidatePeers().remove(this);
					close("Failed to connect");
					return false;
				}
				logger.trace("socket before {}", socket);
				socket = localSocket;
				resetStreams();
				logger.trace("socket after  {}", socket);
				logger.trace("It took {} ms to be connected to socket {}, Peer@{}", System.currentTimeMillis() - start, socketToString(), super.hashCode());
				
			} finally {
				logger.trace("Unlocked connectSocket() for Peer@{}", super.hashCode());
			}
			
		}
		
		startReceiver();
		return true;
	}

	public String getNetworkIdentifier() {
		return networkIdentifier;
	}

	public void setNetworkIdentifier(String networkIdentifier) {
		this.networkIdentifier = networkIdentifier;
	}

	public long getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(long lastSeen) {
		this.lastSeen = lastSeen;
	}

	public PeerType getType() {
		return type;
	}

	public void setType(PeerType type) {
		this.type = type;
	}

	public boolean startReceiver() {
		//logger.info("startReceiver() receiverStarted={} peer: {}", receiverStarted, this);
		if (receiverStarted) {
			return true;
		}

		synchronized(this) {
			if (receiverStarted) {
				return true;
			}
			receiverStarted = true;
		}
		
		if(isClosed()) {
			close("Close peer because it is already closed");
			return false;
		}

		try {
			
			getReader();
			
			ThreadExecutor.getInstance().execute(new DhcRunnable("Peer receiver " + socketToString()) {
				public void doRun() {
					receive();
				}
			});
			scheduleTrim();
			
		} catch (Exception e) {
			logger.trace(e.getMessage());
			close(e.getMessage());
			return false;
		}

		return true;
	}
	
	private void receive() {
		try {
			peersPut();
			while(true) {
				Message message = gsonUtil.read(reader);
				if(message != null) {
					if(message.isThin()) {
						setType(PeerType.THIN);
					}
					ReceiverPool.getInstance().process(this, message);
				}
				if(!peers.containsKey(inetSocketAddress) && !socket.isClosed()) {
					logger.trace("***************************************************");
					logger.trace("peers do not contain {}, socket.isClosed()={}", inetSocketAddress, socket.isClosed());
					peersPut();
				}
			}
		} catch (NullPointerException e) {
			logger.trace(e.getMessage(), e);
			String str = String.format("%s %s", e.getMessage(), e);
			close(str);
		} catch (Exception e) {
			//logger.trace(, e);
			String str = String.format("%s %s", e.getMessage(), e);
			close(str);
		} finally {
			resetStreams();
		}
	}
	
	public void process(Message message) {
		try {
			setLastSeen(System.currentTimeMillis());
			setNetworkIdentifier(message.getNetworkIdentifier());
			setPower(message.getPower());
			setPossiblePower(message.getPossiblePower());
			setInUse(message.isInUse());
			setTAddress(message.getTAddress());
			message.process(this);
			putResponse(message);
			callback(message);
		} catch (DisconnectException e) {
			//logger.trace(e.getMessage(), e);
			String str = String.format("%s %s", e.getMessage(), e);
			close(str);
		} catch (NullPointerException e) {
			logger.trace(e.getMessage(), e);
			String str = String.format("%s %s", e.getMessage(), e);
			close(str);
		} catch (Exception e) {
			logger.info(e.getMessage(), e);
			String str = String.format("%s %s", e.getMessage(), e);
			close(str);
		}
	}

	
	private void callback(Message message) {
		String callbackId = message.getCallbackId();
		if(callbackId == null) {
			return;
		}
		Callback callback = callbacks.remove(callbackId);
		if(callback == null) {
			return;
		}
		callback.callBack(message);
	}

	public void close(String reason) {
		
		try {
			synchronized(this) {
				peersRemove();
				if(socket != null) {
					if(!socket.isClosed()) {
						socket.close();
						logger.trace("Closed socket {} Peer@{} reason {}", socketToString(), super.hashCode(), reason);
						if(reason != null && reason.startsWith("java.net.SocketException") && this.type.equals(PeerType.TO)) {
							ThreadExecutor.getInstance().execute(new DhcRunnable("Restart peer") {
								public void doRun() {
									
									Peer restartPeer = Peer.getInstance(getInetSocketAddress());
									restartPeer.setType(PeerType.TO);
									if(Bootstrap.getInstance().connect(restartPeer)) {
										logger.info("Restarted peer for socket {}, previous was closed because of {}", restartPeer.getSocket(), reason);
									}
								}
							});
						}
					}
				}
			}
			
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		List<PeerLock> values = new ArrayList<PeerLock>(locks.values());
		for(PeerLock lock: values) {
			lock.cancel();
		}
		callbacks.clear();
		if(trimPeerFuture != null) {
			trimPeerFuture.cancel(false);
		}
	}
	
	private void putResponse(Message message) {
		String correlationId = message.getCorrelationId();
		PeerLock lock = locks.get(correlationId);
		if(lock == null) {
			return;
		}
		lock.setResponse(message);
	}
	
	public Message sendSync(Message message, long timeout) {
		long start = System.currentTimeMillis();
		String correlationId = message.getCorrelationId();
		try {
			PeerLock lock = new PeerLock(timeout, message, this);
			locks.put(correlationId, lock);

			send(message);
			if(socket == null || socket.isClosed()) {
				return null;
			}
			return lock.getResponse();
		} finally {
			locks.remove(correlationId);
			logger.trace("sendSync took {} ms for message {}", System.currentTimeMillis() - start, message);
		}
	}
	
	public void sendWithCallback(Message message, Callback callBack, long timeout) {
		SenderPool.getInstance().sendWithCallback(this, message, callBack, timeout);
	}
	
	public void sendWithCallback(Message message, Callback callBack) {
		sendWithCallback(message, callBack, Constants.MINUTE);;
	}
	
	public void doSendWithCallback(Message message, Callback callBack, long timeout) {
		String callbackId = CryptoUtil.getRandomString(16);
		callbacks.put(callbackId, callBack, timeout);
		message.setCallbackId(callbackId);
		doSend(message);
	}

	public String socketToString() {
		if(socket == null || socket.getInetAddress() == null) {
			return inetSocketAddress.toString();
		}
		String result = "/" + socket.getInetAddress().getHostAddress() + ":" + 
				socket.getPort() + ", localPort=" + 
				socket.getLocalPort();
		result  = StringUtil.padRight(result, 40);
		return result;
	}
	
	public synchronized JsonWriter getWriter() throws Exception {
		if(writer != null) {
			return writer;
		}
		writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)));
		writer.setLenient(true);
		return writer;
	}


	public synchronized JsonReader getReader() throws Exception {
		if(reader != null) {
			return reader;
		}
		reader = new JsonReader(new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)));
		reader.setLenient(true);
		return reader;
	}
	
	@Override
	public int hashCode() {
		return getInetSocketAddress().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Peer)) {
			return false;
		}
		Peer other = (Peer) obj;
		return getInetSocketAddress().equals(other.getInetSocketAddress());
	}
	
	public void send(Message message) {
		message.setNetworkIdentifier(Message.NETWORK_IDENTIFIER);
		message.setTAddress(TAddress.getMyTAddress());
		SenderPool.getInstance().send(this, message);
	}

	public void doSend(Message message) {
		try {
			message.setPower(Network.getInstance().getPower());//inform node on the other side about my power
			
			if(!Network.getInstance().getAllPeers().contains(this)) {
				message.setInUse(false);
			} else {
				message.setInUse(true);
			}
			if(message instanceof ConnectMessage || message instanceof NavigateMessage) {
				message.setInUse(true);
			}
			
			message.setPossiblePower(Network.getInstance().getPossiblePower());
			getWriter();
			gsonUtil.write(message, writer);
			message.successfullySent(this);
			removeExcessPeer();
		} catch (NullPointerException e) {
			logger.trace(e.getMessage(), e);
			String str = String.format("%s %s", e.getMessage(), e);
			close(str);
			message.failedToSend(this, e);
		} catch (SocketException e) {
			// logger.trace(e.getMessage(), e);
			close(e.toString());
			message.failedToSend(this, e);
		} catch (Exception e) {
			// logger.trace(e.getMessage(), e);
			String str = String.format("%s %s", e.getMessage(), e);
			close(str);
			message.failedToSend(this, e);
		}
	}
	
	private void removeExcessPeer() {
		if(getExcessPeers().contains(this)) {
			logger.info("Removing excess peer {}", this);
			close("Close peer because it is in excess");
		}
	}
	
	
	private List<Peer> getExcessPeers() {
		List<Peer> list = getPeers();
		list.removeAll(Network.getInstance().getAllPeers());
		Collections.sort(list, new TimeAddedPeerComparator());
		if(Constants.MAX_CONN > list.size()) {
			return new ArrayList<Peer>();
		}
		return list.subList(Constants.MAX_CONN, list.size());
	}

	public InetSocketAddress getInetSocketAddress() {
		return inetSocketAddress;
	}

	public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
		this.inetSocketAddress = inetSocketAddress;
	}
	
	public String toString() {
		return socketToString() + " \t" + StringUtil.padRight("" + networkIdentifier, 45) + " \t" + StringUtil.padRight("" + tAddress, 7) + " \tclosed=" + isClosed() + " \tinUse=" + getInUse() + 
				" \t\tlastSeen=" + new Date(getLastSeen()) + " \ttimeAdded=" + new Date(getTimeAdded()) + " \t" + getType() + " \t";
	}
	
	public void connect() {
		send(new ConnectMessage());
	}
	
	public static List<Peer> getPeersByNetworkIdentifier(String networkIdentifier) {
		if(networkIdentifier == null) {
			return null; //calls to this method expect not null identifiers, returning empty array would cause adding peer with null identifier which will create more problems if peer inetaddress is invalid
		}
		List<Peer> result = new ArrayList<Peer>();
		
		for(Peer peer: getPeers()) {
			if(networkIdentifier.equals(peer.getNetworkIdentifier())) {
				result.add(peer);
			}
		}
		logger.trace("networkIdentifier={} peers={}", networkIdentifier, result);
		return result;
	}

	public static int getTotalPeerCount() {
		return peers.size();
	}

	public static List<Peer> getPeers() {
		List<Peer> result = new ArrayList<Peer>();
		for(Peer peer: new ArrayList<>(peers.values())) {
			if(!peer.isThin()) {
				result.add(peer);
			}
		}
		return result;
	}
	
	public static List<Peer> getThinPeers() {
		List<Peer> result = new ArrayList<Peer>();
		for(Peer peer: new ArrayList<>(peers.values())) {
			if(peer.isThin() && !peer.isClosed()) {
				result.add(peer);
			}
		}
		return result;
	}
	
	public boolean isThin() {
		return PeerType.THIN.equals(getType());
	}

	public static List<Peer> getClosestKPeers(TAddress tAddress) {
		List<Peer> peers = getAllToPeers();
		//logger.trace("Will sort {} peers: {}", peers.size(), peers);
		Collections.sort(peers, new Comparator<Peer>() {
			@Override
			public int compare(Peer p1, Peer p2) {
				return tAddress.compareDistance(p1.getTAddress(), p2.getTAddress());
			}
		});
		return peers.subList(0, Math.min(Constants.k * 3, peers.size()));
	}

	public static List<Peer> getAllToPeers() {
		List<Peer> result = new ArrayList<Peer>();
		for(Peer peer: new ArrayList<>(peers.values())) {
			if(PeerType.TO.equals(peer.getType()) && peer.getTAddress() != null) {
				result.add(peer);
			}
		}
		return result;
	}
	
	public long getTimeAdded() {
		return timeAdded;
	}

	public void setTimeAdded(long timeAdded) {
		this.timeAdded = timeAdded;
	}

	public int getPower() {
		return power;
	}

	public void setPower(int power) {
		this.power = power;
	}

	public boolean isMyBucketPeer() {
		return Network.getInstance().getMyBucketPeers().contains(this);
	}

	public int getPossiblePower() {
		return possiblePower;
	}

	public void setPossiblePower(int possiblePower) {
		this.possiblePower = possiblePower;
	}
	
	public String getKey() {
		return getTAddress().getBinary(getPower());
	}

	/**
	 * @return the inUse
	 */
	public Boolean getInUse() {
		return Boolean.TRUE.equals(inUse);
	}

	/**
	 * @param inUse the inUse to set
	 */
	public void setInUse(Boolean inUse) {
		this.inUse = inUse;
	}
	
	public Socket getSocket() {
		return socket;
	}

	public TAddress getTAddress() {
		return tAddress;
	}

	public void setTAddress(TAddress tAddress) {
		if(tAddress == null) {
			this.tAddress = null;
			return;
		}
		if(this.tAddress == null) {
			this.tAddress = tAddress;
			Network.getInstance().addPeer(this);
			return;
		}
		if(!this.tAddress.equals(tAddress)) {
			RuntimeException e = new RuntimeException("TAddresses are not equal");
			logger.info("this.tAddress={}, tAddress={}, peer {}", this.tAddress, tAddress, this);
			logger.info(e.getMessage(), e);
			throw e;
		}
	}

	public static void setMyself(InetSocketAddress myself) {
		Peer.myself = myself;
	}
	
	public void resetStreams() {
		receiverStarted = false;
		writer = null;
	    reader = null;
	    inUse = false;
	}

}
