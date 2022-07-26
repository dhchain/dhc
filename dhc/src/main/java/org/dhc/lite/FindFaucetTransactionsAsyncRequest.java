package org.dhc.lite;

import java.util.Set;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Transaction;
import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;

public class FindFaucetTransactionsAsyncRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress from;
	private DhcAddress to;
	private String ip;

	public FindFaucetTransactionsAsyncRequest(DhcAddress from, DhcAddress to, String ip) {
		this.from = from;
		this.to = to;
		this.ip = ip;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);
		Network network = Network.getInstance();
		
		Blockchain blockchain = Blockchain.getInstance();
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(to, blockchain.getPower()) && !ChainSync.getInstance().isRunning()) {
			Set<Transaction> transactions = blockchain.getFindFaucetTransactions(to, ip);
			FindFaucetTransactionsAsyncReply message  = new FindFaucetTransactionsAsyncReply(from, to, transactions);
			message.setCorrelationId(getCorrelationId());
			network.sendToAddress(from, message);
			logger.trace("sent to {} message {}", from, message);
			return;
		}
		
		network.sendToAddress(to, this);
	}
	
	@Override
	public String toString() {
		String str = String.format("FindFaucetTransactionsAsyncRequest %s-%s-%s-%s", from, to, ip, getCorrelationId());
		return str;
	}

}
