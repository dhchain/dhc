package org.dhc.blockchain;

import java.util.concurrent.locks.Lock;

import org.dhc.network.MajorRunnerLock;
import org.dhc.network.Network;
import org.dhc.network.PeerSync;
import org.dhc.network.consensus.BlockchainIndexStaleException;
import org.dhc.network.consensus.Consensus;
import org.dhc.network.consensus.ResetMiningException;
import org.dhc.util.BlockEvent;
import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Listeners;
import org.dhc.util.SharedLock;
import org.dhc.util.ThreadExecutor;

public class Miner {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final Consensus consensus = Consensus.getInstance();
	
	private final SharedLock readWriteLock = SharedLock.getInstance();
	
	private boolean stop = true;

	public void start() {
		if(!Network.getInstance().isRunning()) {
			logger.info("Network is not running. Please start network first.");
			return;
		}
		if(stop == false) {
			return;
		}
		logger.info("Miner START");
		stop = false;
		ThreadExecutor.getInstance().execute(new DhcRunnable("Miner") {
			public void doRun() {
				while (true) {
					if(stop) {
						break;
					}
					try {
						MajorRunnerLock.getInstance().ifLockedThenWait();
						mine();
					} catch (BlockchainIndexStaleException e) {
						logger.trace("\n");
						logger.trace("{}\n", e.getMessage());
					} catch (ResetMiningException e) {
						logger.info("\n");
						logger.info("{}\n", e.getMessage());
					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
					}
				}
				logger.info("Miner END");
			}
		});
	}

	public void mine() {
		Block block = consensus.getMiningBlock();
		if(stop) {
			return;
		}
		block = block.clone();
		
		Blockchain blockchain = Blockchain.getInstance();
		Network network = Network.getInstance();
		
		if (blockchain.getIndex() >= block.getIndex()) {
			logger.info("Blockchain index is already greater or the same as mined block. Ignoring mined block.");
			return;
		}
		
/*		if(block.getPower() > network.getPower()) {
			logger.info("Network power is smaller than mined block's power. Ignoring mined block.");
			return;
		}*/
		
		if(network.getMyBucketPeers().isEmpty()) {
			logger.info("My bucket is empty");
			PeerSync.getInstance().executeAndWait();
			return;
		}
		
		logger.info("Mined block {}", block);
		Listeners.getInstance().sendEvent(new BlockEvent(block, "Mined"));
		
		block.computeFullMerklePath();
		if (blockchain.add(block)) {
			Network.getInstance().sendToSomePeers(new SendBlockMessage(block));
			new SendCShardTxMessage().send(block);
		}
	}

	public void stop() {
		this.stop = true;
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			consensus.notifyMiningLock();
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

}
