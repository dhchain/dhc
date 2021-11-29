package org.dhc.network;

import java.util.List;

import org.dhc.util.Message;
import org.dhc.util.DhcLogger;

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
		message.setCorrelationId(getCorrelationId());
		message.setCallbackId(getCallbackId());
		peer.send(message);
		Bootstrap.getInstance().addToCandidates(peer, serverPort);
		logger.trace("ConnectMessage - SUCCESS");
	}

}
