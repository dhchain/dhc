package org.dhc.network;

import org.dhc.util.DhcLogger;
import org.dhc.util.Message;

public class ClosePeerIfNotInUseMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	public ClosePeerIfNotInUseMessage() {
		
	}

	@Override
	public void process(Peer peer) {
		Network network = Network.getInstance();
		if(!network.getAllPeers().contains(peer)) {
			logger.info("Closing not in use peer {}", peer);
			peer.close("Closing because peer is not in use");
		}

	}

}
