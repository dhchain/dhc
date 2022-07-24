package org.dhc.network;

import java.util.List;

import org.dhc.util.Message;
import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;

public class ConnectReplyMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	public ConnectReplyMessage() {
		logger.trace("ConnectReplyMessage init");
	}

	@Override
	public void process(Peer peer) {
		logger.trace("ConnectReplyMessage - START");
		if(Network.getInstance().getNetworkIdentifier().equals(peer.getNetworkIdentifier())) {
			logger.info("ConnectReplyMessage - Cannot connect to yourself");
			logger.info("*********************************************************");
			logger.info("ConnectMessage - Cannot connect to yourself {}", peer.getInetSocketAddress());
			throw new DisconnectException("ConnectReplyMessage - Cannot connect to yourself");
		}
		List<Peer> list = Peer.getPeersByNetworkIdentifier(peer.getNetworkIdentifier());
		list.remove(peer);
		if(!list.isEmpty()) {
			logger.trace("ConnectReplyMessage - Already connected to this peer");
			throw new DisconnectException("ConnectReplyMessage - Already connected to this peer");
		}
		if(Math.abs(System.currentTimeMillis() - getTimestamp()) > Constants.MINUTE * 10) {
			String str = String.format("ConnectReplyMessage - The difference in time is greater than 10 minutes, disconnecting from peer %s", peer);
			logger.info(str);
			throw new DisconnectException(str);
		}
		logger.trace("ConnectReplyMessage - SUCCESS");
	}

}
