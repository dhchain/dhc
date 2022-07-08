package org.dhc.lite.post;

import java.util.Set;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Keywords;
import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionMemoryPool;
import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.persistence.TransactionStore;
import org.dhc.util.Applications;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;

public class SearchRateeAsyncRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private DhcAddress myAddress;
	private DhcAddress dhcAddress;
	private String transactionId;


	public SearchRateeAsyncRequest(DhcAddress myAddress, DhcAddress dhcAddress, String transactionId) {
		this.myAddress = myAddress;
		this.dhcAddress = dhcAddress;
		this.transactionId = transactionId;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);
		Network network = Network.getInstance();

		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(dhcAddress, Blockchain.getInstance().getPower()) && !ChainSync.getInstance().isRunning()) {
			Ratee ratee = TransactionStore.getInstance().getRatee(transactionId);
			if(ratee == null) {
				ratee = getPendingRatee();
			}
			SearchRateeAsyncReply message  = new SearchRateeAsyncReply(myAddress, dhcAddress, ratee);
			message.setCorrelationId(getCorrelationId());
			network.sendToAddress(myAddress, message);
			logger.trace("sent to {} message {}", myAddress, message);
			return;
		}
		
		network.sendToAddress(dhcAddress, this);
		
	}

	
	private Ratee getPendingRatee() {
		Set<Transaction> pendingTransactions = TransactionMemoryPool.getInstance().getTransactions();
		for(Transaction transaction: pendingTransactions) {
			if(!Applications.RATING.equals(transaction.getApp())) {
				continue;
			}
			
			Keywords keywords  = transaction.getKeywords();
			if(keywords == null) {
				continue;
			}
			String accountName = keywords.get("create");
			if(accountName == null) {
				continue;
			}
			
			if(!dhcAddress.getAddress().equals(accountName)) {
				continue;
			}
			
			if(transaction.getTransactionId().equals(transactionId)) {
				Ratee ratee = new Ratee();
				ratee.setName(dhcAddress.getAddress());
				ratee.setDescription(transaction.getExpiringData().getData());
				ratee.setTimeStamp(System.currentTimeMillis());
				ratee.setTransactionId(transactionId);
				ratee.setTotalRating(0);
				ratee.setCreatorDhcAddress(transaction.getSenderDhcAddress().getAddress());
				return ratee;
			}
		}
		return null;
		
	}
	
	@Override
	public String toString() {
		String str = String.format("SearchRateeAsyncRequest %s-%s-%s-%s", myAddress, dhcAddress, transactionId, getCorrelationId());
		return str;
	}


}
