package org.dhc.blockchain;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.dhc.network.MajorRunnerLock;
import org.dhc.persistence.BlockStore;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ThreadExecutor;

public class Trimmer {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final Trimmer instance = new Trimmer();
	
	private volatile boolean running;
	private ReentrantLock lock = MajorRunnerLock.getInstance().getLock();
	
	public static Trimmer getInstance() {
		return instance;
	}
	
	private Trimmer() {
		
	}
	
	public void runAsync() {
		ThreadExecutor.getInstance().execute(new DhcRunnable("Trimmer") {
			public void doRun() {
				runImmediately();
			}
		});
	}
	
	public void runImmediately() {

		if (!lock.tryLock()) {
			return;
		}
		
		if(running) {
			return;
		}
		
		running = true;

		try {

			long start = System.currentTimeMillis();

			Blockchain blockchain = Blockchain.getInstance();

			blockchain.pretrim();
			
			long minCompeting = BlockStore.getInstance().getMinCompeting();
			long checkpoint = Math.max(0, minCompeting - 1);

			logger.info("START checkpoint {}, minCompeting {}", checkpoint, minCompeting);

			if (minCompeting <= 0) {
				return;
			}
			Iterator<Block> iterator = blockchain.getLastBlocks().iterator();
			if (!iterator.hasNext()) {
				return;
			}
			Block block = iterator.next();
			for (int i = 0; i < 10; i++) {
				block = blockchain.getByHash(block.getPreviousHash());
				if (block == null) {
					return;
				}
			}
			while (block != null && block.getIndex() > checkpoint) {
				List<Block> blocks = blockchain.getBlocks(block.getIndex());
				for (Block b : blocks) {
					if (!b.getBlockHash().equals(block.getBlockHash())) {
						blockchain.removeBranch(b.getBlockHash());
					}
				}
				block = blockchain.getByHash(block.getPreviousHash());
			}
			
			logger.info("Trimmer.runImmediately() took {} ms.", System.currentTimeMillis() - start);
			
		} finally {
			running = false;
			lock.unlock();
			
		}
	}

}
