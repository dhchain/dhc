package org.dhc.blockchain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;

public class Compactor {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private long prunedIndex = 0;

	private Set<Transaction> getPrunings() {
		Set<Transaction> result = new HashSet<>();
		List<Block> lastBlocks = Blockchain.getInstance().getLastBlocks();
		if(lastBlocks.size() != 1) {
			return result;
		}
		Block block = lastBlocks.get(0);
		Set<String> recipients = Blockchain.getInstance().getPruningRecipients();
		for(String recipient: recipients) {
			Transaction pruning = Transaction.createPruning(recipient, block);
			result.add(pruning);
		}
		
		return result;
	}
	
	public void addPrunings() {
		Set<Transaction> prunings = getPrunings();
		TransactionMemoryPool.getInstance().add(prunings);
	}
	
	public void pruneBlockchain() {
		execute();
	}
	
	private void execute() {
		logger.trace("Compactor.Execute() START");
		long time = System.currentTimeMillis();
		Blockchain blockchain = Blockchain.getInstance();
		if(blockchain == null) {
			return;
		}
		long index = blockchain.getIndex() - Constants.MAX_NUMBER_OF_BLOCKS;
		long unprunedIndex = getPrunedIndex() + 1;
		logger.trace("Min unprunedIndex={}", unprunedIndex);
		if (unprunedIndex != -1 && unprunedIndex < index) {
			for (long i = unprunedIndex; i < index; i++) {
				pruneBlockchain(i);
			}
		}
		logger.trace("pruneBlockchain END, took {} ms", System.currentTimeMillis() - time);
	}
	
	private void pruneBlockchain(long index) {
		Blockchain blockchain = Blockchain.getInstance();
		List<Block> blocks = blockchain.getBlocks(index);
		for(Block block: blocks) {
			if(block.isPruned()) {
				continue;
			}
			block.prune();
			try {
				blockchain.replace(block);
				setPrunedIndex(index);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public long getPrunedIndex() {
		if(prunedIndex == 0) {
			prunedIndex = Blockchain.getInstance().getMinUnprunedIndex() - 1; 
		}
		return prunedIndex;
	}

	public void setPrunedIndex(long prunedIndex) {
		this.prunedIndex = prunedIndex;
	}

}
