package org.dhc.blockchain;

import org.dhc.network.Network;
import org.dhc.network.consensus.BucketHash;
import org.dhc.network.consensus.GatherTransactions;
import org.dhc.util.BoundedMap;
import org.dhc.util.Callback;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class RecoveringBlocks {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final RecoveringBlocks instance = new RecoveringBlocks();
	
	private BoundedMap<String, Block> blocks = new BoundedMap<>(10);
	
	public static RecoveringBlocks getInstance() {
		return instance;
	}
	
	private RecoveringBlocks() {
		
	}
	
	public synchronized void remove(String blockhash) {
		blocks.remove(blockhash);
		logger.trace("recovering blocks remove {}", blockhash);
	}
	
	public boolean containsKey(String blockhash) {
		return blocks.containsKey(blockhash);
	}
	
	public void run(Block block) {
		String str = String.format("Recovering block %s-%s-%s", block.getBucketKey(), block.getIndex(), block.getBlockHash().substring(0, 7));
		ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
			public void doRun() {
				process(block);
			}
		});
	}
	
	private void process(Block block) {
		synchronized (this) {
			if (block.getConsensus() == null || Blockchain.getInstance().contains(block.getBlockHash())) {
				return;
			}

			if (containsKey(block.getBlockHash())) {
				return;
			}
			blocks.put(block.getBlockHash(), block);
			logger.trace("recovering blocks put {}", block.getBlockHash());

		}

		Registry.getInstance().getBucketConsensuses().recover(block);
	}

	public void set(String blockHash, BucketHashes bucketHashes) {
		logger.trace("RecoveringBlocks.set() blockHash={}, bucketHashes={}", blockHash, bucketHashes);
		Block block;
		synchronized (this) {
			block = blocks.get(blockHash);
			if (block == null) {
				return;
			}
		}
		logger.info("Got from my peers {} for block={} {}\n", bucketHashes, block.getIndex(), block.getBlockHash());
		block.setBucketHashes(bucketHashes);

		Registry.getInstance().getBucketConsensuses().trim(block);
		
		BucketHash lastHash = block.getBucketHashes().getLastBucketHash();
		
		GatherTransactions.getInstance().run(lastHash, block.getIndex(), new Callback() {
			
			@Override
			public void expire() {
				logger.info("Expire for block {}", block);
				remove(block.getBlockHash());
			}
			
			@Override
			public void callBack(Object object) {
				logger.info("GatherTransactions callBack will try to add block {}", block);
				if (Blockchain.getInstance().add(block)) {
					Network.getInstance().sendToKey(block.getBucketKey(), new SendMyBlockMessage(block));
					new SendCShardTxMessage().send(block);
				}
				remove(block.getBlockHash());
			}
		});
	}


}
