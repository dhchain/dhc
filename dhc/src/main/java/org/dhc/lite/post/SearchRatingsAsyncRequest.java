package org.dhc.lite.post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class SearchRatingsAsyncRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress from;
	private DhcAddress to;
	private String account;
	private String transactionId;
	


	public SearchRatingsAsyncRequest(DhcAddress from, DhcAddress to, String account, String transactionId) {
		this.from = from;
		this.to = to;
		this.account = account;
		this.transactionId = transactionId;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);
		Network network = Network.getInstance();

		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(to, Blockchain.getInstance().getPower()) && !ChainSync.getInstance().isRunning()) {
			List<Rating> list = getPendingRatings();
			list.addAll(TransactionStore.getInstance().getRatings(account, transactionId));
			
			SearchRatingsAsyncReply message  = new SearchRatingsAsyncReply(from, to, list);
			message.setCorrelationId(getCorrelationId());
			network.sendToAddress(from, message);
			logger.trace("sent to {} message {}", from, message);
			return;
		}
		
		network.sendToAddress(to, this);
		
	}
	
	private List<Rating> getPendingRatings() {
		Map<String, Rating> map = new HashMap<String, Rating>();
		Set<Transaction> pendingTransactions = TransactionMemoryPool.getInstance().getTransactions();
		
		for(Transaction transaction: pendingTransactions) {
			if(!Applications.RATING.equals(transaction.getApp())) {
				continue;
			}
			
			if(!transaction.getReceiver().equals(to)) {
				continue;
			}
			
			Keywords keywords  = transaction.getKeywords();
			if(keywords == null) {
				continue;
			}
			
			String rate = keywords.get("rating");
			if(rate == null) {
				continue;
			}
			
			String ratee = keywords.get("ratee");
			if(ratee == null || !ratee.equals(account)) {
				continue;
			}
			
			
			
			Rating rating = new Rating();
			
			rating.setRater(transaction.getSenderDhcAddress().getAddress());
			rating.setComment(transaction.getExpiringData().getData());
			rating.setRate(rate);
			rating.setRatee(account);
			rating.setTimeStamp(System.currentTimeMillis());
			
			String transactionId = keywords.get("transactionId");
			rating.setTransactionId(transactionId);
			map.put(transactionId, rating);
		}
		
		return new ArrayList<Rating>(map.values());
	}
	
	@Override
	public String toString() {
		String str = String.format("SearchRatingsAsyncRequest %s-%s-%s", from, to, getCorrelationId());
		return str;
	}

}
