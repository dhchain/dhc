package org.dhc.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

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
			throw new DisconnectException("ConnectMessage - Cannot connect to yourself");
		}
		List<Peer> list = Peer.getPeersByNetworkIdentifier(peer.getNetworkIdentifier());
		list.remove(peer);
		if(!list.isEmpty()) {
			logger.trace("ConnectMessage - Already connected to this peer");
			throw new DisconnectException("ConnectMessage - Already connected to this peer");
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
		logger.trace("Successfully connected to candidate {}", peer.getInetSocketAddress());
	}

	@Override
	public void failedToSend(Peer peer, Exception e) {
		Bootstrap.getInstance().getCandidatePeers().remove(peer);
		logger.trace("Failed to connect to candidate {}", peer.getInetSocketAddress());
		logger.trace(e.getMessage(), e);
	}

}
