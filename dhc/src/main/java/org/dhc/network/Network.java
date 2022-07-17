package org.dhc.network;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.TAddress;

public class Network {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private static Network instance = new Network();
	
	private Buckets buckets = new Buckets();;
	private String networkIdentifier;
	private int port;
	private Server server  = new Server();
	
	public static Network getInstance() {
		return instance;
	}
	
	private Network() {
		init();
	}

	private void init() {
		networkIdentifier = Message.NETWORK_IDENTIFIER;
	}

	public String getNetworkIdentifier() {
		return networkIdentifier;
	}

	public void start() {
		if(buckets == null) {
			buckets = new Buckets();
		}
		if(port == 0) {
			logger.info("Started Distributed Hash Chain");
		} else {
			server.setPort(port);
			server.start();
		}
		PeerSync.getInstance().executeAndWait();
	}
	
	public void stop() {
		Registry.getInstance().getMiner().stop();
		server.stop();
		buckets = null; // to prevent running reloadBuckets()
		Peer.closeAllPeers();
	}
	
	public void reloadBuckets() {
		if(buckets == null) {
			return;
		}
		buckets.reload();
	}
	
	public int getPower() {
		Buckets buckets = this.buckets;
		if(buckets == null) {
			return 0;
		}
		return buckets.getPower();
	}
	
	public int getPossiblePower() {
		if(buckets == null) {
			return 0;
		}
		return buckets.getPossiblePower();
	}
	
	public void sendToAllPeers(Message message) {
		for(Peer p: getAllPeers()) {
			p.send(message);
		}
	}
	
	public void sendToSomePeers(Message message) {
		List<Peer> peers = getAllPeers();
		send(peers, message);
	}
	
	public void sendToAllPeers(Message message, Peer peer) {
		List<Peer> peers = getAllPeers();
		peers.remove(peer);
		for(Peer p: getAllPeers()) {
			p.send(message);
		}
	}
	
	public void sendToSomePeers(Message message, Peer peer) {
		List<Peer> peers = getAllPeers();
		peers.remove(peer);
		send(peers, message);
	}
	
	public void sendToKey(String key, Message message) {
		send(getPeersForKey(key), message);
	}
	
	public void sendToKey(String key, Message message, Peer peer) {
		List<Peer> peers = getPeersForKey(key);
		peers.remove(peer);
		send(peers, message);
	}
	
	/**
	 * @param key
	 * @return list of peers including non bucket peers that start with key
	 */
	public List<Peer> getPeersWithKey(String key) {
		List<Peer> result = new ArrayList<Peer>();
		for(Peer p: new ArrayList<Peer>(Peer.getPeers())) {
			if(p.getTAddress() != null && p.getTAddress().isMyKey(key)) {
				result.add(p);
			}
		}
		return result;
	}
	
	public List<Peer> getAllPeers() {
		if(buckets == null) {
			return new ArrayList<Peer>();
		}
		return buckets.getAllPeers();
	}
	
	public List<Peer> getPeersForKey(String key) {
		List<Peer> result = new ArrayList<Peer>();
		for(Peer p: getAllPeers()) {
			if(p.getTAddress().isMyKey(key)) {
				result.add(p);
			}
		}
		return result;
	}

	public List<Peer> getMyBucketPeers() {
		if(buckets == null) {
			return new ArrayList<Peer>();
		}
		return buckets.getMyBucketPeers();
	}
	
	public void sendToAllPeersInBucket(int index, Message message) {
		if(buckets == null) {
			return;
		}
		send(buckets.getAllPeersInBucket(index), message);
	}
	
	public void sendToAllMyPeers(Message message) {
		if(buckets == null) {
			return;
		}
		send(buckets.getMyBucketPeers(), message);
	}
	
	public void sendToAllMyPeers(Message message, Peer peer) {
		if(buckets == null) {
			return;
		}
		List<Peer> list = buckets.getMyBucketPeers();
		list.remove(peer);
		send(list, message);
	}
	
	public String getBucketKey(DhcAddress dhcAddress) {
		if(buckets == null || buckets.isEmpty()) {
			return "";
		}
		return buckets.getBucketKey(dhcAddress);
	}
	
	public String getBucketKey(TAddress tAddress) {
		if(buckets == null || buckets.isEmpty()) {
			return "";
		}
		return buckets.getBucketKey(tAddress);
	}

	public String getBucketKey(int index) {
		return buckets.getBucketKey(index);
	}
	
	public String getBucketKey() {
		return buckets.getBucketKey(getPower());
	}
	
	public void printBuckets() {
		buckets.printBuckets();
	}
	
	public List<Peer> getMyBucketToPeers() {
		List<Peer> result = new ArrayList<Peer>();
		for(Peer peer: getMyBucketPeers()) {
			if(PeerType.TO.equals(peer.getType())) {
				result.add(peer);
			}
		}
		return result;
	}

	public List<String> getBucketKeys() {
		return buckets.getBucketKeys();
	}

	public void send(List<Peer> peers, Message message) {
		Collections.shuffle(peers);
		peers = peers.subList(0, Math.min(Constants.maxNumberOfPeersToSend, peers.size()));
		for (Peer p : peers) {
			p.send(message);
		}
	}

	public void sendToAddress(DhcAddress address, Message message) {
		String bucketKey = getBucketKey(address);
		sendToKey(bucketKey, message);
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean isRunning() {
		return buckets != null;
	}

	public List<Peer> getThinPeers() {
		return Peer.getThinPeers();
	}

	public boolean isConnected() {
		return !getMyBucketPeers().isEmpty();
	}

	public Peer getPeerByPort(InetSocketAddress inetSocketAddress) {
		List<Peer> peers = new ArrayList<Peer>(Peer.getPeers());
		for(Peer peer: peers) {
			if(peer.getSocket() != null && peer.getSocket().getLocalPort() == inetSocketAddress.getPort() && peer.getSocket().getLocalAddress().equals(inetSocketAddress.getAddress())) {
				return peer;
			}
		}
		return null;
	}

	public void removePeer(Peer peer) {
		if(buckets == null) {
			return;
		}
		buckets.removePeer(peer);
	}

	public void addPeer(Peer peer) {
		if(buckets == null) {
			return;
		}
		buckets.addPeer(peer);
	}

	public boolean shouldAddPeer(TAddress tAddress) {
		if(tAddress == null) {
			return true;
		}
		return buckets.shouldAddPeer(tAddress);
	}

}
