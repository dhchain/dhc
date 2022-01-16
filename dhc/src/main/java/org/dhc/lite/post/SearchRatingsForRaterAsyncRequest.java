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
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.persistence.TransactionStore;
import org.dhc.util.Applications;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;

public class SearchRatingsForRaterAsyncRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myAddress;
	private DhcAddress dhcAddress;
	private String rater;

	public SearchRatingsForRaterAsyncRequest(DhcAddress myAddress, DhcAddress dhcAddress, String rater) {
		this.myAddress = myAddress;
		this.dhcAddress = dhcAddress;
		this.rater = rater;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);
		Network network = Network.getInstance();

		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(dhcAddress, Blockchain.getInstance().getPower())) {
			List<Rating> list = getPendingRatings();
			list.addAll(TransactionStore.getInstance().getRatings(rater));
			SearchRatingsForRaterAsyncReply message  = new SearchRatingsForRaterAsyncReply(myAddress, dhcAddress, list);
			message.setCorrelationId(getCorrelationId());
			network.sendToAddress(myAddress, message);
			logger.trace("sent to {} message {}", myAddress, message);
			return;
		}
		
		network.sendToAddress(dhcAddress, this);
		
	}
	
	private List<Rating> getPendingRatings() {
		Map<String, Rating> map = new HashMap<String, Rating>();
		Set<Transaction> pendingTransactions = TransactionMemoryPool.getInstance().getTransactions();
		
		for(Transaction transaction: pendingTransactions) {
			if(!Applications.RATING.equals(transaction.getApp())) {
				continue;
			}
			
			if(!transaction.getSenderDhcAddress().getAddress().equals(rater)) {
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
			
			Rating rating = new Rating();
			
			rating.setRater(rater);
			rating.setComment(transaction.getExpiringData().getData());
			rating.setRate(rate);
			rating.setRatee(keywords.get("ratee"));
			rating.setTimeStamp(System.currentTimeMillis());
			
			String transactionId = keywords.get("transactionId") ;
			rating.setTransactionId(transactionId);
			map.put(transactionId, rating);
		}
		
		return new ArrayList<Rating>(map.values());
	}
	
	@Override
	public String toString() {
		String str = String.format("SearchRatingsForRaterAsyncRequest %s-%s-%s-%s", myAddress, dhcAddress, getCorrelationId());
		return str;
	}

}
