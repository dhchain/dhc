package org.dhc.gui.promote;

import org.dhc.blockchain.Transaction;
import org.dhc.util.Event;

public class JoinTransactionEvent implements Event {
	
	private Transaction transaction;
	
	public JoinTransactionEvent(Transaction transaction) {
		this.transaction = transaction;
	}

	public Transaction getTransaction() {
		return transaction;
	}

}
