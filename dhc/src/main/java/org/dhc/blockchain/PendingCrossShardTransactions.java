package org.dhc.blockchain;

import org.dhc.network.Network;
import org.dhc.util.Constants;
import org.dhc.util.ExpiringMap;
import org.dhc.util.Registry;
import org.dhc.util.DhcLogger;

public class PendingCrossShardTransactions {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private final ExpiringMap<String, PendingBlockCrossShardTransactions> pendingTransactions =  new ExpiringMap<>(Constants.MINUTE * 10);

	private synchronized void put(Transaction transaction) {
		PendingBlockCrossShardTransactions pendingBlockTransactions = pendingTransactions.get(transaction.getBlockHash());
		if(pendingBlockTransactions == null) {
			pendingBlockTransactions = new PendingBlockCrossShardTransactions();
			pendingBlockTransactions.setBlockhash(transaction.getBlockHash());
			pendingTransactions.put(transaction.getBlockHash(), pendingBlockTransactions);
		}
		pendingBlockTransactions.add(transaction);
	}

	public void addCrossShardTransactions(String blockhash) {
		PendingBlockCrossShardTransactions pendingBlockTransactions;
		synchronized(this) {
			pendingBlockTransactions = pendingTransactions.get(blockhash);
			Blockchain blockchain = Blockchain.getInstance();
			if(pendingBlockTransactions == null || blockchain == null || !blockchain.contains(blockhash)) {
				return;
			}
			pendingTransactions.remove(blockhash);
		}
		
		pendingBlockTransactions.addTransactions();
	}
	
	private void addTransaction(Transaction transaction) {
		try {
			Blockchain blockchain = Blockchain.getInstance();
			String bucketKey = blockchain.getBucketKey(transaction.getBlockHash());
			if(!transaction.getReceiver().isMyKey(bucketKey)) {
				Registry.getInstance().getPotentialTransactions().put(transaction);
				return;
			}
			if(blockchain.addTransaction(transaction)) {
				long index = blockchain.getIndex();
				logger.trace("{}-{} blockBucketKey={} Received and saved crossshard transaction {}", Network.getInstance().getBucketKey(), index, bucketKey, transaction);
				if(transaction.getBlockIndex() + 5 < index) {
					logger.info("***********************************************************************");
					logger.info("{}-{} blockBucketKey={} Received and saved resent crossshard transaction {}", Network.getInstance().getBucketKey(), index, bucketKey, transaction);
					logger.info("***********************************************************************");
				}
				Network.getInstance().sendToAddress(transaction.getReceiver(), new SendCShardTxMessage(transaction));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void process(Transaction transaction) {
		if (Blockchain.getInstance().contains(transaction.getBlockHash())) {
			addTransaction(transaction);
		} else {
			put(transaction);
		}
	}

}
