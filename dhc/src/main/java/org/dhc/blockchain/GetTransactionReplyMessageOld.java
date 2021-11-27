package org.dhc.blockchain;

import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.DhcLogger;

public class GetTransactionReplyMessageOld extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Transaction transaction;

	public GetTransactionReplyMessageOld(Transaction transaction) {
		this.transaction = transaction;
		logger.trace("Sent transaction {}", transaction);
	}

	@Override
	public void process(Peer peer) {
		logger.info("Received transaction {}", transaction);
		Registry.getInstance().getGetTransaction().put(transaction);//put will also process the transaction
	}

	@Override
	public String toString() {
		String str = String.format("GetTransactionReplyMessageOld %s", transaction.getTransactionId());
		return str;
	}

}
