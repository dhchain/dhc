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
import org.dhc.util.StringUtil;

public class SearchRateesAsyncRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myAddress;
	private DhcAddress dhcAddress;
	private String account;
	private Set<String> words;

	public SearchRateesAsyncRequest(DhcAddress myAddress, DhcAddress dhcAddress, String account, Set<String> words) {
		this.myAddress = myAddress;
		this.dhcAddress = dhcAddress;
		this.account = account;
		this.words = words;

	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);
		Network network = Network.getInstance();

		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(dhcAddress, Blockchain.getInstance().getPower())) {
			List<Ratee> ratees = getPendingPosts();
			ratees.addAll(TransactionStore.getInstance().getRatees(account, words));
			SearchRateesAsyncReply message  = new SearchRateesAsyncReply(myAddress, dhcAddress, ratees);
			message.setCorrelationId(getCorrelationId());
			network.sendToAddress(myAddress, message);
			logger.trace("sent to {} message {}", myAddress, message);
			return;
		}
		
		network.sendToAddress(dhcAddress, this);
		
	}
	
	private List<Ratee> getPendingPosts() {
		Map<String, Ratee> map = new HashMap<String, Ratee>();
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
			
			if(!StringUtil.isEmpty(account)  && !account.equals(accountName)) {
				continue;
			}
			
			if(words != null && !words.isEmpty() && !keywords.keySet().containsAll(words)) {
				continue;
			}
			
			String transactionId = keywords.get("transactionId");
			if(transactionId == null) {
				transactionId = transaction.getTransactionId();
			}
			
			Ratee ratee = new Ratee();
			ratee.setName(accountName);
			ratee.setDescription(transaction.getExpiringData().getData());
			ratee.setTimeStamp(System.currentTimeMillis());
			ratee.setTransactionId(transactionId);
			ratee.setTotalRating(0);
			ratee.setCreatorDhcAddress(transaction.getSenderDhcAddress().getAddress());
			map.put(transactionId, ratee);
		}
		
		return new ArrayList<Ratee>(map.values());
	}
	
	@Override
	public String toString() {
		String str = String.format("SearchRateesAsyncRequest %s-%s-%s", myAddress, dhcAddress, getCorrelationId());
		return str;
	}

}
