package org.dhc.lite.post;

import java.util.Set;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Keywords;
import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionMemoryPool;
import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.persistence.KeywordStore;
import org.dhc.util.Applications;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;

public class GetKeywordsAsyncRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myDhcAddress;
	private String rateeName;
	private String transactionId;

	public GetKeywordsAsyncRequest(DhcAddress myDhcAddress, String rateeName, String transactionId) {
		this.myDhcAddress = myDhcAddress;
		this.rateeName = rateeName;
		this.transactionId = transactionId;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);
		Network network = Network.getInstance();
		DhcAddress dhcAddress = CryptoUtil.getDhcAddressFromString(rateeName);

		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(dhcAddress, Blockchain.getInstance().getPower()) && !ChainSync.getInstance().isRunning()) {
			Keywords keywords = KeywordStore.getInstance().getKeywords(transactionId);
			if(keywords == null) {
				keywords = getPendingKeywords();
			}
			GetKeywordsAsyncReply message  = new GetKeywordsAsyncReply(myDhcAddress, keywords);
			message.setCorrelationId(getCorrelationId());
			network.sendToAddress(myDhcAddress, message);
			logger.trace("sent to {} message {}", myDhcAddress, message);
			return;
		}
		
		network.sendToAddress(dhcAddress, this);
		
	}
	
	private Keywords getPendingKeywords() {
		Keywords keywords = null;
		Set<Transaction> pendingTransactions = TransactionMemoryPool.getInstance().getTransactions();
		DhcAddress dhcAddress = CryptoUtil.getDhcAddressFromString(rateeName);
		for(Transaction transaction: pendingTransactions) {
			if(!Applications.RATING.equals(transaction.getApp())) {
				continue;
			}
			
			if(transaction.getTransactionId().equals(transactionId) && transaction.getReceiver().equals(dhcAddress)) {
				keywords  = transaction.getKeywords();
			}
		}
		return keywords;
	}
	
	@Override
	public String toString() {
		String str = String.format("GetKeywordsAsyncRequest %s-%s-%s", myDhcAddress, rateeName, getCorrelationId());
		return str;
	}

}
