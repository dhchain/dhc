package org.dhc.lite;

import java.util.Set;

import org.dhc.blockchain.Transaction;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class FindFaucetTransactionsAsyncReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress from;
	private DhcAddress to;
	private Set<Transaction> transactions;

	public FindFaucetTransactionsAsyncReply(DhcAddress from, DhcAddress to, Set<Transaction> transactions) {
		this.from = from;
		this.to = to;
		this.transactions = transactions;
		
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);

		if(DhcAddress.getMyDhcAddress().equals(from)) {
			FindFaucetTransactionsEvent event = new FindFaucetTransactionsEvent(transactions, getCorrelationId());
			Listeners.getInstance().sendEvent(event);
		}
		Network network = Network.getInstance();
		network.sendToAddress(from, this);// it might be useful in case several instances are running under the same DHC address
		logger.trace("sent to {} message {}", from, this);
		
	}
	
	@Override
	public String toString() {
		String str = String.format("GetSenderPublicKeyAsyncReply %s-%s-%s", from, to, getCorrelationId());
		return str;
	}

}
