package org.dhc.lite;

import org.dhc.blockchain.Transaction;
import org.dhc.network.Peer;
import org.dhc.util.Message;

public class NotifyTransactionMessage extends Message {

	private Transaction transaction;

	public NotifyTransactionMessage(Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public void process(Peer peer) {


	}

	public Transaction getTransaction() {
		return transaction;
	}

}
