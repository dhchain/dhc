package org.dhc.lite;

import java.util.Set;

import org.dhc.blockchain.Transaction;
import org.dhc.util.Event;

public class FindFaucetTransactionsEvent implements Event {
	
	private Set<Transaction> transactions;
	private String correlationId;

	public FindFaucetTransactionsEvent(Set<Transaction> transactions, String correlationId) {
		this.transactions = transactions;
		this.correlationId = correlationId;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public Set<Transaction> getTransactions() {
		return transactions;
	}

}
