package org.dhc.blockchain;

import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.util.BoundedMap;
import org.dhc.util.Constants;
import org.dhc.util.Registry;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class GetTransaction {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private final BoundedMap<String, Transaction> transactions =  new BoundedMap<>(Constants.MAX_NUMBER_OF_BLOCKS);
	private final BoundedMap<String, Integer> counts =  new BoundedMap<>(Constants.MAX_NUMBER_OF_BLOCKS);
	
	private String getKey(Transaction transaction) {
		return getKey(transaction.getBlockHash(), transaction.getTransactionId());
	}
	
	private String getKey(String blockhash, String transactionId) {
		String str = String.format("%s-%s", blockhash, transactionId);
		return str;
	}
	
	public void put(Transaction transaction) {
		transactions.put(getKey(transaction), transaction);
		process(transaction);
	}
	
	public Transaction get(String blockhash, String transactionId) {
		return transactions.get(getKey(blockhash, transactionId));
	}
	
	public void process(Transaction transaction) {


		if(!transaction.isValid()) {
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
		Blockchain blockchain = Blockchain.getInstance();
		
		try {
			if(blockchain.addTransaction(transaction)) {
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
			logger.trace("GetTransaction block is null");
			return;
		}

		Block existingBlock = Blockchain.getInstance().getByHash(block.getBlockHash());
		if (existingBlock == null) {
			Blockchain blockchain = Blockchain.getInstance();
			
			if (blockchain.add(block)) {
				logger.trace("GetTransaction add block {}", block);
				long lastIndex = Math.max(blockchain.getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
				if(lastIndex == block.getIndex()) {
					Network.getInstance().sendToKey(block.getBucketKey(), new SendMyBlockMessage(block));
					new SendCShardTxMessage().send(block);
				}
			}
			return;
		}
		if (existingBlock.getPower() <= block.getPower()) {
			logger.trace("GetTransaction existingBlock.getPower()={}, block.getPower()={}", existingBlock.getPower(), block.getPower());
			return;
		}

		try {
			logger.info("GetTransaction replace block {}", block);
			Blockchain.getInstance().replace(block);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void addCount(String blockhash, String transactionId) {
		String key = getKey(blockhash, transactionId);
		Integer count = counts.get(key);
		if(count == null) {
			count = 0;
		}
		counts.put(key, count + 1);
	}
	
	public int getCount(String blockhash, String transactionId) {
		String key = getKey(blockhash, transactionId);
		Integer count = counts.get(key);
		if(count == null) {
			count = 0;
		}
		return count;
	}
	
	public void send(TransactionInput input) {
		
		String blockhash = input.getOutputBlockHash();
		String transactionId = input.getOutputTransactionId();
		DhcAddress from = DhcAddress.getMyDhcAddress();
		DhcAddress to = input.getSender();

		int count = getCount(blockhash, transactionId);
		if(count > 5) {
			new GetTransactionMessage(blockhash, transactionId, from, to).send();
		}
		new GetTransactionMessageOld(blockhash, transactionId).send();
	}


}
