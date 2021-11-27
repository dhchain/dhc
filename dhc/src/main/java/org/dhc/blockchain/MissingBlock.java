package org.dhc.blockchain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.dhc.network.ChainRest;
import org.dhc.network.ChainSync;
import org.dhc.util.Constants;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class MissingBlock {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final Set<String> blocks = Collections.synchronizedSet(new HashSet<>());

	private String blockHash;
	private String key;
	private long index;
	
	public MissingBlock(String blockHash, long index) {
		if(Registry.getInstance().getBannedBlockhashes().contains(blockHash)) {
			return;
		}
		synchronized(blocks) {
			if(blocks.contains(blockHash)) {
				return;
			}
			blocks.add(blockHash);
		}
		this.blockHash = blockHash;
		this.key = DhcAddress.getMyDhcAddress().getBinary(Blockchain.getInstance().getPower());
		this.index = index;
		long lastIndex = Math.max(Blockchain.getInstance().getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
		if(index < lastIndex) {
			return;
		}
		schedule();
	}

	private void schedule() {
		
		String str = String.format("Missing block index=%s, hash=%s", index, blockHash);
		ThreadExecutor.getInstance().schedule(new DhcRunnable(str) {
			
			@Override
			public void doRun() {
				process();
			}
		}, Constants.MINUTE * 3);
		
	}
	
	private void process() {
		blocks.remove(blockHash);
		Blockchain blockchain = Blockchain.getInstance();
		long lastIndex = Math.max(Blockchain.getInstance().getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
		if(blockchain.contains(blockHash) || index < lastIndex) {
			return;
		}
		
		logger.info("MissingBlock.process() trying to get block from my peers for index={}, key={}, hash={}", index, key, blockHash);
		Block block = ChainRest.getInstance().getBlock(key, blockHash);
		if(block != null && block.isMine()) {
			logger.info("MissingBlock.process() got block from my peers {}", block);
			if(blockchain.add(block)) {
				return;
			}
			logger.info("MissingBlock.process() could not add block {}", block);
		}
		
		logger.info("*************************************************************************");
		logger.info("MissingBlock.process() index={}, key={}, hash={}", index, key, blockHash);
		new MissingBlockMessage(blockHash, key, index, DhcAddress.getMyDhcAddress()).send();
	}

}
