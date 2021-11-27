package org.dhc.lite;

import java.util.Set;

import org.dhc.blockchain.TransactionOutput;
import org.dhc.network.Peer;
import org.dhc.util.Message;

public class GetTransactionOutputsReply extends Message {
	
	private Set<TransactionOutput> transactionOutputs;

	public GetTransactionOutputsReply(Set<TransactionOutput> transactionOutputs) {
		this.transactionOutputs = transactionOutputs;
	}

	@Override
	public void process(Peer peer) {

	}

	public Set<TransactionOutput> getTransactionOutputs() {
		return transactionOutputs;
	}

}
