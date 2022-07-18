package org.dhc.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.BucketHashes;
import org.dhc.persistence.BlockStore;
import org.dhc.persistence.BucketHashStore;
import org.dhc.util.BoundedMap;
import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.GsonUtil;
import org.dhc.util.TAddress;
import org.dhc.util.ThreadExecutor;

import com.google.gson.Gson;

public class ChainRest {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final ChainRest instance = new ChainRest();
	
	private final BoundedMap<String, Block> map = new BoundedMap<>(10);
	private boolean failed;
	private volatile boolean running;
	private final ReentrantLock lock = MajorRunnerLock.getInstance().getLock();

	public static ChainRest getInstance() {
		return instance;
	}
	
	public void executeAsync() {
		ThreadExecutor.getInstance().execute(new DhcRunnable("ChainRest") {
			public void doRun() {
				execute();
			}
		});
	}
	
	public void execute() {
		if (!lock.tryLock()) {
			return;
		}
		
		if(running) {
			return;
		}
		
		running = true;

		try {

			Network network = Network.getInstance();
			long start = System.currentTimeMillis();
			logger.info("START ChainRestorer networkPower={}, blockchainPower={}", network.getPower(), Blockchain.getInstance().getPower());
			int attemptNumber = 0;
			if(network.getPower() < Blockchain.getInstance().getPower()) {
				PeerSync.getInstance().executeAndWait();
			}
			

			while (true) {
				logger.info("ChainRestorer attempt {}", attemptNumber++);
				if (attemptNumber > 10) {
					logger.info("ChainRestorer exiting, too many attempts");
					PeerSync.getInstance().executeAndWait();
					break;
				}
				failed = false;
				doExecute();
				int networkPower = network.getPower();
				int maxPower = Blockchain.getInstance().getPower();
				if (networkPower < maxPower) {
					Bootstrap.getInstance().navigate(Network.getInstance().getAllPeers(), TAddress.getMyTAddress());
					logger.trace("networkPower {} < maxPower {}", networkPower, maxPower);
					continue;
				}
				if (failed == false) {
					break;
				}
			}
			Map<Long, Long> map = BlockStore.getInstance().getPowerReport();
			logger.info("Report for blocks power: {}, networkPower={}, blockchainPower={}", map, network.getPower(),
					Blockchain.getInstance().getPower());
			logger.info("END ChainRestorer. Took {} ms. Number of attempts {}", System.currentTimeMillis() - start, attemptNumber);

		} finally {
			running = false;
			lock.unlock();
			
		}
		
		Bootstrap.getInstance().navigate(Network.getInstance().getAllPeers(), TAddress.getMyTAddress());

	}

	private void doExecute() {
		logger.info("ChainRestorer doExecute()");
		List<Block> blocks = null;
		do {
			
			List<Block> loopBlocks = BlockStore.getInstance().restore();
			if(loopBlocks.equals(blocks)) {
				ThreadExecutor.sleep(Constants.SECOND);
			}
			blocks = loopBlocks;
			for(Block block: blocks) {
				processBlock(block);
			}
			logger.info("networkPower={}, blockchainPower={}", Network.getInstance().getPower(), Blockchain.getInstance().getPower());
		} while (!blocks.isEmpty());
	}
	
	private void processBlock(Block block) {
		try {
			if(!Blockchain.getInstance().contains(block.getBlockHash())) {
				logger.trace("Block with hash {} was deleted, skip", block.getBlockHash());
			} else {
				logger.trace("Will call getBucketHashes by {}", block.getBlockHash());
				BucketHashes bucketHashes = BucketHashStore.getInstance().getBucketHashes(block.getBlockHash());
				block.setBucketHashes(bucketHashes);
				doBlock(block);
			}
		} catch (Throwable e) {
			logger.error("Error in ChainRestorer processing block {}", block);
			logger.error(e.getMessage(), e);
			failed = true;
		}
	}
	
	private void doBlock(Block block) throws Exception {
		logger.trace("doBlock() block {}", block);
		if(!Network.getInstance().isConnected()) {
			logger.info("Network is not connected");
			PeerSync.getInstance().executeAndWait();
			logger.info("Total # peers {}", Peer.getTotalPeerCount());
			return;
		}
		if(block.getPower() <= Network.getInstance().getPower()) {
			return;
		}
		
		String key = block.getBucketKey();
		String otherKey = new BucketKey(key).getOtherBucketKey().getKey();
		Block otherBlock;
		
		while(true) {
			otherBlock = getBlock(otherKey, block.getBlockHash());
			if(otherBlock != null) {
				break;
			}
			if("".equals(otherKey)) {
				break;
			}
			otherKey = new BucketKey(otherKey).getParentKey().getKey();
		}

		if(otherBlock != null) {
			Block combinedBlock = otherBlock.getPower() < block.getPower()? otherBlock: block.combine(otherBlock);
			if(!combinedBlock.isValid()) {
				logger.info("combined block is not valid: {}", combinedBlock);
				logger.info("block: {}", block);
				logger.info("otherBlock: {}", otherBlock);
				failed = true;
				return;
			}
			if(!combinedBlock.isBranchValid()) {
				Gson gson = GsonUtil.getInstance().getGson();
				logger.info("combined block branch is not valid: {}\n{}", combinedBlock, gson.toJson(combinedBlock));
				logger.info("block: {}\n{}", block, gson.toJson(block));
				logger.info("otherBlock: {}\n{}", otherBlock, gson.toJson(otherBlock));
				failed = true;
				Blockchain.getInstance().removeBranch(block.getBlockHash());
				return;
			}
			if (!combinedBlock.hasOutputsForAllInputs()) {
				logger.info("combined block hasOutputsForAllInputs=false: {}", combinedBlock);
				logger.info("block: {}", block);
				logger.info("otherBlock: {}", otherBlock);
				failed = true;
				return;
			}
			Blockchain.getInstance().replace(combinedBlock);
			logger.info("{} Replaced block     {}", block.getBucketKey(), block);
			logger.info("{}  with combinedBlock {}\n", combinedBlock.getBucketKey(), combinedBlock);
			doBlock(combinedBlock);
		} else {
			logger.info("***********************************************************************");
			logger.info("Could not retrieve other block myKey={} otherKey={} for block {}", key, otherKey, block);
			logger.info("networkPower()={}, blockchainPower={}", Network.getInstance().getPower(), Blockchain.getInstance().getPower());
			
			List<Block> competingBlocks = BlockStore.getInstance().getByPreviousHash(block.getPreviousHash());
			if(competingBlocks.size() > 1) {
				logger.info("Block might be a competing block and was deleted");
				logger.info("Competing blocks: ");
				for(Block competingBlock: competingBlocks) {
					logger.info("{}", competingBlock);
				}
				Blockchain.getInstance().removeBranch(block.getBlockHash());
			}
			failed = true;
		}
		
	}

	public Block getBlock(String key, String blockHash) {
		List<Peer> peers = Network.getInstance().getPeersWithKey(key);
		Map<String, Block> blocks = new HashMap<>();
		for (Peer peer : peers) {
			Block block = map.get(key + "-" + blockHash);
			if(block != null) {
				return block;
			}
			GetBlocksReplyMessage reply = (GetBlocksReplyMessage) peer.sendSync(new GetBlocksMessage(key, blockHash), Constants.SECOND * 20);
			if(reply == null) {
				continue;
			}
			block = reply.getBlock();
			if(block == null) {
				continue;
			}

			if(!block.isValidHashForTransactions()) {
				logger.info("getBlock returned invalid block {}", block);
				logger.info("getBlock returned invalid block from peer {}", peer);
				continue;
			}
			if (block.getPower() <= key.length()) {
				map.put(key + "-" + blockHash, block);
				return block;
			}

			putFoundBlock(block, blocks);
		}
		
		Block block = blocks.get(key);
		return block;
	}

	private void putFoundBlock(Block block, Map<String, Block> blocks) {
		String key = block.getBucketKey();
		blocks.put(key, block);
		if("".equals(key)) {
			return;
		}
		BucketKey bucketKey = new BucketKey(key);
		String otherKey = bucketKey.getOtherBucketKey().getKey();
		Block otherblock = blocks.get(otherKey);
		if(otherblock == null) {
			return;
		}
		Block combinedBlock = block.combine(otherblock);
		putFoundBlock(combinedBlock, blocks);
	}

	public boolean isRunning() {
		return running;
	}

}
