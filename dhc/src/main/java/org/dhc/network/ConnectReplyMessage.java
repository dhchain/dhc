package org.dhc.network;

import java.util.List;

import org.dhc.util.Message;
import org.dhc.util.DhcLogger;

public class ConnectReplyMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	public ConnectReplyMessage() {
		logger.trace("ConnectReplyMessage init");
	}

	@Override
	public void process(Peer peer) {
		if(Network.getInstance().getNetworkIdentifier().equals(peer.getNetworkIdentifier())) {
			logger.info("ConnectReplyMessage - Cannot connect to yourself");
			throw new DisconnectException("ConnectReplyMessage - Cannot connect to yourself");
		}
		List<Peer> list = Peer.getPeersByNetworkIdentifier(peer.getNetworkIdentifier());
		list.remove(peer);
		if(!list.isEmpty()) {
			logger.trace("ConnectReplyMessage - Already connected to this peer");
			throw new DisconnectException("ConnectReplyMessage - Already connected to this peer");
		}
		logger.trace("ConnectReplyMessage - SUCCESS");
	}

}
