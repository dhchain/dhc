package org.dhc.lite;

import java.util.Set;

import org.dhc.blockchain.Transaction;
import org.dhc.network.Peer;
import org.dhc.util.Message;

public class GetTransactionsForAppReply extends Message {
	
	private Set<Transaction> transactions;

	public GetTransactionsForAppReply(Set<Transaction> transactions) {
		this.transactions = transactions;
	}

	@Override
	public void process(Peer peer) {


	}

	public Set<Transaction> getTransactions() {
		return transactions;
	}

}
