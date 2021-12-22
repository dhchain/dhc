package org.dhc.blockchain;

import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.util.Constants;
import org.dhc.util.ExpiringMap;
import org.dhc.util.Registry;
import org.dhc.util.DhcLogger;

public class MissingOutputsForTransaction {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private final ExpiringMap<TransactionKey, Transaction> map =  new ExpiringMap<>(Constants.MINUTE * 5);
	
	public void put(TransactionKey key, Transaction transaction) {
		logger.trace("MissingOutputsForTransaction.put() key         {}", key);
		logger.trace("MissingOutputsForTransaction.put() transaction {}", transaction);
		map.put(key, transaction);
	}

	private Transaction get(TransactionKey transactionKey) {
		return map.remove(transactionKey);
	}

	public void process(String transactionId, String blockHash) {
		
		Transaction transaction  = get(new TransactionKey(transactionId, blockHash));
		if(transaction == null) {
			return;
		}
		logger.trace("MissingOutputsForTransaction.process() transactionId={}, blockHash={}", transactionId, blockHash);
		if(transaction.getBlockHash() == null) {
			logger.trace("MissingOutputsForTransaction add to pool transaction {}", transaction);
			TransactionMemoryPool.getInstance().add(transaction);
			return;
		}
		
		if(!transaction.isValid(null)) {
			return;
		}

		if(!transaction.inputsOutputsValid()) {
			return;
		}

		if (!Blockchain.getInstance().contains(transaction.getBlockHash())) {
			return;
		}
		
		addTransaction(transaction);
	}
	
	private void addTransaction(Transaction transaction) {
		try {
			if(Blockchain.getInstance().addTransaction(transaction)) {
				logger.info("Received and saved transaction {}", transaction);
				logger.trace("Transaction         MerklePath {}", transaction.getMerklePath());
				addBlock(transaction);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private void addBlock(Transaction transaction) {
		Block block = Registry.getInstance().getMissingOutputs().get(new TransactionKey(transaction.getTransactionId(), transaction.getBlockHash()));
		if(block == null) {
			logger.trace("MissingOutputsForTransaction block is null for transaction {}", transaction);
			return;
		}

		Block existingBlock = Blockchain.getInstance().getByHash(block.getBlockHash());
		if (existingBlock == null) {
			Blockchain blockchain = Blockchain.getInstance();
			
			if (blockchain.add(block)) {
				logger.trace("MissingOutputsForTransaction add block {} for transaction {}", block, transaction);
				long lastIndex = Math.max(blockchain.getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
				if(lastIndex == block.getIndex()) {
					Network.getInstance().sendToKey(block.getBucketKey(), new SendMyBlockMessage(block));
					new SendCShardTxMessage().send(block);
				}
			}
			return;
		}
		if (existingBlock.getPower() <= block.getPower()) {
			logger.trace("MissingOutputsForTransaction existingBlock.getPower()={}, block.getPower()={}", existingBlock.getPower(), block.getPower());
			return;
		}

		try {
			logger.info("MissingOutputsForTransaction replace block {} for transaction {}", block, transaction);
			Blockchain.getInstance().replace(block);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
