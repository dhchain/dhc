package org.dhc.blockchain;

import java.util.AbstractMap.SimpleEntry;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.persistence.BlockStore;
import org.dhc.util.Message;
import org.dhc.util.DhcLogger;

import java.util.List;
import java.util.Set;

public class InputNotFromBranchMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Transaction transaction;

	public InputNotFromBranchMessage(Transaction transaction) {
		this.transaction = transaction;
		log();
	}
	
	public void log() {
		logger.info("transaction {}", transaction);
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}

		if(!transaction.isMerklePathValid()) {
			return;
		}

		Block block = BlockStore.getInstance().getByBlockhash(transaction.getBlockHash());
		if(block == null) {
			return;
		}
		
		List<SimpleEntry<String, String>> merklePath = transaction.getMerklePath();
		SimpleEntry<String, String> first = merklePath.get(0);
		if(!first.getValue().equals(block.getConsensus())) {
			logger.info("not the same: first.getValue()={}, block.getConsensus()={}", first.getValue(), block.getConsensus());
			return;
		}

		Block previousBlock = BlockStore.getInstance().getByBlockhash(block.getPreviousHash());
		if(previousBlock == null) {
			return;
		}
		
		Set<String> blockhashes = Blockchain.getInstance().getBranchBlockhashes(previousBlock);
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		long branchIndex = minCompeting == 0 || minCompeting > previousBlock.getIndex() ? previousBlock.getIndex() : minCompeting;
		
		log();
		logger.info("minCompeting       {}", minCompeting);
		logger.info("branchIndex        {}", branchIndex);
		
		for (TransactionInput input : transaction.getInputs()) {
			if (input.getOutputBlockIndex() >= branchIndex && !blockhashes.contains(input.getOutputBlockHash())) {
				Blockchain.getInstance().removeBranch(transaction.getBlockHash());
				Network.getInstance().sendToAllPeers(this);
				break;
			}
		}

	}
	
	@Override
	public String toString() {
		String str = String.format("InputNotFromBranchMessage %s-%s", transaction.getBlockHash(), transaction.getTransactionId());
		return str;
	}

}
