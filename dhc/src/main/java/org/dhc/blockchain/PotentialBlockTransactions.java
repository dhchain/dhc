package org.dhc.blockchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dhc.network.Network;
import org.dhc.util.Expiring;
import org.dhc.util.DhcLogger;

public class PotentialBlockTransactions implements Expiring {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private String blockhash;
	private List<Transaction> list = Collections.synchronizedList(new ArrayList<>());
	
	@Override
	public void expire() {
		if(!list.isEmpty()) {
			logger.info("PotentialBlockTransactions.expire blockhash={} list.size()={}", blockhash, list.size());
		}
		
		addTransactions();
	}
	
	public void addTransactions() {
		Blockchain blockchain = Blockchain.getInstance();
		if (!blockchain.contains(blockhash)) {
			return;
		}
		String bucketKey = blockchain.getBucketKey(blockhash);
		if(bucketKey == null) {
			return;
		}
		while(!list.isEmpty()) {
			Transaction transaction = list.remove(0);
			try {
				addTransaction(transaction, bucketKey);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	private void addTransaction(Transaction transaction, String bucketKey) {
		Blockchain blockchain = Blockchain.getInstance();
		if(!transaction.getReceiver().isMyKey(bucketKey)) {
			return;
		}
		try {
			if(blockchain.addTransaction(transaction)) {
				logger.trace("blockBucketKey={} Received and saved transaction {}", bucketKey, transaction);
				Network.getInstance().sendToAddress(transaction.getReceiver(), new SendCShardTxMessage(transaction));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void setBlockhash(String blockhash) {
		this.blockhash = blockhash;
	}

	public void add(Transaction transaction) {
		synchronized(list) {
			if(list.contains(transaction)) {
				return;
			}
			list.add(transaction);
		}
	}

	public List<Transaction> getList() {
		return list;
	}

}
