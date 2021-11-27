package org.dhc.blockchain;

import org.dhc.network.ChainSync;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class SendTransactionMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Transaction transaction;

	public SendTransactionMessage(Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("START");
		if(ChainSync.getInstance().isRunning()) {
			return;
		}
		
		if(alreadySent(toString())) {
			return;
		}

		if(!DhcAddress.getMyDhcAddress().isFromTheSameShard(transaction.getSenderDhcAddress(), Blockchain.getInstance().getPower())) {
			//returning because this is not a cross shard transaction message
			return;
		}
		
		logger.trace("SendTransactionMessage transaction.getBlockHash()={} {}", transaction.getBlockHash(), transaction);
		Registry.getInstance().getPendingTransactions().processTransaction(transaction);
	}
	
	@Override
	public String toString() {
		String str = String.format("SendTransactionMessage-%s", transaction.getTransactionId());
		return str;
	}

}
