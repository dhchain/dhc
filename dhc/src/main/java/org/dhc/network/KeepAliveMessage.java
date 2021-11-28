package org.dhc.network;

import org.dhc.util.DhcLogger;
import org.dhc.util.Message;

public class KeepAliveMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	public KeepAliveMessage() {

	}

	@Override
	public void process(Peer peer) {
		logger.trace("KeepAliveMessage from {}", peer);
	}

}
