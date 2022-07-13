package org.dhc.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;

public class ConnectMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private int serverPort;
	
	public ConnectMessage() {
		logger.trace("ConnectMessage init");
		serverPort = Network.getInstance().getPort();
	}

	@Override
	public void process(Peer peer) {
		if(Network.getInstance().getNetworkIdentifier().equals(peer.getNetworkIdentifier())) {
			//logger.trace("ConnectMessage - Cannot connect to yourself");
			logger.trace("*********************************************************");
			logger.trace("ConnectMessage - Cannot connect to yourself {}", peer.getInetSocketAddress());
			Peer oppositePeer = Network.getInstance().getPeerByPort(peer.getInetSocketAddress());
			
			if(oppositePeer != null) {
				logger.trace("ConnectMessage - Cannot connect to yourself oppositePeer = {}", oppositePeer.getInetSocketAddress());
				Peer.setMyself(oppositePeer.getInetSocketAddress());
			}
			
			throw new DisconnectException("ConnectMessage - Cannot connect to yourself");
		}
		List<Peer> list = Peer.getPeersByNetworkIdentifier(peer.getNetworkIdentifier());
		list.remove(peer);
		if(!list.isEmpty()) {
			logger.trace("ConnectMessage - Already connected to this peer {}", peer);
			for(Peer p: list) {
				logger.trace("ConnectMessage - reconnection, closing peer {}", peer);
				p.close("Reconnection request from peer " + peer);
			}
			//throw new DisconnectException("ConnectMessage - Already connected to this peer");
		}
		if(Math.abs(System.currentTimeMillis() - getTimestamp()) > Constants.MINUTE * 10) {
			String str = String.format("ConnectMessage - The difference in time is greater than 10 minutes, disconnecting from peer %s", peer);
			logger.info(str);
			throw new DisconnectException(str);
		}
		Message message = new ConnectReplyMessage();
		peer.send(message);
		ThreadExecutor.getInstance().execute(new DhcRunnable("connectCandidate") {
			public void doRun() {
				connectCandidate(peer);
			}
		});
		logger.trace("ConnectMessage - SUCCESS");
	}
	
	private void connectCandidate(Peer peer) {
		if(serverPort == 0) {
			return;
		}
		String ip = peer.getSocket().getInetAddress().getHostAddress();
		InetSocketAddress inetSocketAddress;
		try {
			inetSocketAddress = new InetSocketAddress(InetAddress.getByName(ip), serverPort);
			Peer candidate = Peer.getInstance(inetSocketAddress);
			candidate.setType(PeerType.TO);
			
			if(Peer.getAllToPeers().contains(candidate) || Bootstrap.getInstance().getCandidatePeers().contains(candidate)) {
				if(candidate.isClosed()) {
					//logger.info("closed candidate peer {}", candidate);
					//logger.info("closed inetSocketAddress {}", inetSocketAddress);
					candidate.close("Close peer because it is closed candidate");
				}
				return;
			}
			
			
			logger.trace("Will try to connect to candidate {}", candidate.getInetSocketAddress());
			Bootstrap.getInstance().connect(candidate);
		} catch (UnknownHostException e) {
			
		}
	}

	@Override
	public void successfullySent(Peer peer) {
		Bootstrap.getInstance().getCandidatePeers().add(peer);
		logger.trace("Successfully connected to peer {}", peer.getInetSocketAddress());
	}

	@Override
	public void failedToSend(Peer peer, Exception e) {
		//this method will not be triggered by connectCandidate() but can be triggered from Bootstrap.connectPeers()
		Bootstrap.getInstance().getCandidatePeers().remove(peer);
		logger.info("Failed to connect to peer {}", peer.getInetSocketAddress());
		logger.trace("peer.getSocket()={}", peer.getSocket());
		if(!(e instanceof SocketException) && !(e instanceof NullPointerException)) {
			logger.trace(e.getMessage(), e);
		}
		
	}

}
