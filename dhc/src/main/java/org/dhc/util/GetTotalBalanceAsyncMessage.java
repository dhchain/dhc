package org.dhc.util;

import org.dhc.network.Peer;

public class GetTotalBalanceAsyncMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	public GetTotalBalanceAsyncMessage() {

	}

	@Override
	public void process(Peer peer) {
		logger.info("GetTotalBalanceMessage START");
		Registry.getInstance().getTotalBalance().process();
		logger.info("GetTotalBalanceMessage END");
	}



}
