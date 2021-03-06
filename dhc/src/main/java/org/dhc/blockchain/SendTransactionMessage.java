package org.dhc.blockchain;

import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Applications;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;
import org.dhc.util.Registry;

public class SendTransactionMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Transaction transaction;

	public SendTransactionMessage(Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("START transaction {}", transaction);
		if(ChainSync.getInstance().isRunning()) {
			logger.trace("END chainsync is running transaction {}", transaction);
			return;
		}
		
		if(alreadySent(toString())) {
			logger.trace("END already sent transaction {}", transaction);
			return;
		}
		
		if(!transaction.isTransactionDataValid()) {
			logger.trace("END transaction data is not valid transaction {}", transaction);
			return;
		}
		
		if(Applications.MESSAGING.equals(transaction.getApp())) {
			Network.getInstance().sendToAddress(transaction.getReceiver(), new SendSMTransactionMessage(transaction));
		}
		
		Network.getInstance().sendToAllMyPeers(new SendTransactionMessage(transaction));
		
		logger.trace("SendTransactionMessage transaction.getBlockHash()={} {}", transaction.getBlockHash(), transaction);
		Registry.getInstance().getPendingTransactions().processTransaction(transaction);
	}
	
	@Override
	public String toString() {
		String str = String.format("SendTransactionMessage-%s", transaction.getTransactionId());
		return str;
	}
	
	@Override
	public void failedToSend(Peer peer, Exception e) {
		logger.trace("Failed to send  {}", this);
		logger.trace("Failed to send to peer {}", peer);
	}

}
