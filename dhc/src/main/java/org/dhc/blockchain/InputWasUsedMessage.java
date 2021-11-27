package org.dhc.blockchain;

import java.util.AbstractMap.SimpleEntry;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.persistence.BlockStore;
import org.dhc.util.Message;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

import java.util.List;
import java.util.Set;

public class InputWasUsedMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Transaction transaction;
	private TransactionInput input;
	private Transaction transactionWitness;
	private TransactionInput inputWitness;

	public InputWasUsedMessage(Transaction transaction, TransactionInput input, Transaction transactionWitness, TransactionInput inputWitness) {
		logger.info("InputWasUsedMessage() start blockindex={}, was used in block {}", transaction.getBlockIndex(), transactionWitness.getBlockIndex());
		this.transaction = transaction;
		this.input = input;
		this.transactionWitness = transactionWitness;
		this.inputWitness = inputWitness;
		TransactionMemoryPool.getInstance().remove(transaction);
		logger.info("transaction        {}", transaction);
		logger.info("input              {}", input);
		logger.info("transactionWitness {}", transactionWitness);
		logger.info("inputWitness       {}", inputWitness);
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.info("InputWasUsedMessage() start blockindex={}, was used in block {}", transaction.getBlockIndex(), transactionWitness.getBlockIndex());
		
		if(transaction.getBlockHash().equals(transactionWitness.getBlockHash())) {
			return;
		}

		if(!validate(transaction, input)) {
			return;
		}
		
		if(!validate(transactionWitness, inputWitness)) {
			return;
		}
		
		Block block = BlockStore.getInstance().getByBlockhash(transaction.getBlockHash());
		if(block == null) {
			return;
		}
		
		Block previousBlock = BlockStore.getInstance().getByBlockhash(block.getPreviousHash());
		if(previousBlock == null) {
			return;
		}
		
		logger.info("transaction        {}", transaction);
		logger.info("input              {}", input);
		logger.info("transactionWitness {}", transactionWitness);
		logger.info("inputWitness       {}", inputWitness);
		
		Set<String> blockhashes = Blockchain.getInstance().getBranchBlockhashes(previousBlock);
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		long branchIndex = minCompeting == 0 || minCompeting > previousBlock.getIndex() ? previousBlock.getIndex() : minCompeting;

		logger.info("minCompeting       {}", minCompeting);
		logger.info("branchIndex        {}", branchIndex);
		
		
		if(inputWitness.getInputBlockIndex() < branchIndex || blockhashes.contains(inputWitness.getInputBlockHash())) {
			TransactionMemoryPool.getInstance().remove(transaction);
			Blockchain.getInstance().removeBranch(transaction.getBlockHash());
			Network.getInstance().sendToAllPeers(this);
			addWitnessTransaction();
		}

	}
	
	private void addWitnessTransaction() {
		int power = Blockchain.getInstance().getPower();
		if(transactionWitness.getSenderDhcAddress().isFromTheSameShard(DhcAddress.getMyDhcAddress(), power)
				|| transactionWitness.getReceiver().isFromTheSameShard(DhcAddress.getMyDhcAddress(), power)) {
			try {
				Blockchain.getInstance().addTransaction(transactionWitness);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		
	}

	private boolean validate(Transaction transaction, TransactionInput input) {
		
		Blockchain blockchain = Blockchain.getInstance();
		
		if(!transaction.isMerklePathValid()) {
			return false;
		}
		if(!transaction.containsInput(input)) {
			return false;
		}
		
		if(!input.getInputTransactionId().equals(transaction.getTransactionId())) {
			return false;
		}
		
		if(!input.getInputBlockHash().equals(transaction.getBlockHash())) {
			return false;
		}
		
		Block block = blockchain.getByHash(transaction.getBlockHash());
		if(block == null) {
			return false;
		}
		
		List<SimpleEntry<String, String>> merklePath = transaction.getMerklePath();
		SimpleEntry<String, String> first = merklePath.get(0);
		if(!first.getValue().equals(block.getConsensus())) {
			logger.info("not the same: first.getValue()={}, block.getBucketHashes().getConsensus()={}", first.getValue(), block.getConsensus());
			return false;
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		String str = String.format("InputWasUsedMessage %s-%s", transaction.getBlockHash(), transaction.getTransactionId());
		return str;
	}

}
