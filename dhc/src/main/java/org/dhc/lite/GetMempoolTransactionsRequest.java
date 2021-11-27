package org.dhc.lite;

import java.util.Set;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionMemoryPool;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.Message;

public class GetMempoolTransactionsRequest extends Message {

	private DhcAddress myDhcAddress;

	public GetMempoolTransactionsRequest(DhcAddress myDhcAddress) {
		this.myDhcAddress = myDhcAddress;
	}

	@Override
	public void process(Peer peer) {
		Set<Transaction> transactions = null;
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(myDhcAddress, Blockchain.getInstance().getPower())) {
			transactions = TransactionMemoryPool.getInstance().getCopyOfTransactions();
		}
		Message message  = new GetMempoolTransactionsReply(transactions);
		message.setCorrelationId(getCorrelationId());
		peer.send(message);
	}

	public DhcAddress getMyDhcAddress() {
		return myDhcAddress;
	}

}
