package org.dhc.blockchain;

import java.util.Iterator;
import java.util.List;

import org.dhc.persistence.BlockStore;
import org.dhc.util.Constants;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class Trimmer {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final Trimmer instance = new Trimmer();
	
	public static Trimmer getInstance() {
		return instance;
	}
	
	private Trimmer() {

	}
	
	public void start() {
		ThreadExecutor.getInstance().schedule(new DhcRunnable("Trimmer") {
			
			@Override
			public void doRun() {
				trim();
				
			}
		}, Constants.MINUTE * 1);
	}
	
	private void trim() {
		
		ThreadExecutor.getInstance().schedule(new DhcRunnable("Trimmer") {
			
			@Override
			public void doRun() {
				trim();
				
			}
		}, Constants.MINUTE * 10);
		
		try {
			runImmediately();
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public void runImmediately() {
		long start = System.currentTimeMillis();
		
		Blockchain blockchain = Blockchain.getInstance();
		
		blockchain.pretrim();

		long checkpoint = Math.max(0, BlockStore.getInstance().getMinCompeting() - 1);
		
		logger.trace("START checkpoint {}", checkpoint);

		if(checkpoint == 0) {
			return;
		}
		Iterator<Block> iterator = blockchain.getLastBlocks().iterator();
		if(!iterator.hasNext()) {
			return;
		}
		Block block = iterator.next();
		for(int i=0; i< 10; i++) {
			block = blockchain.getByHash(block.getPreviousHash());
			if(block == null) {
				return;
			}
		}
		while(block != null && block.getIndex() > checkpoint) {
			List<Block> blocks = blockchain.getBlocks(block.getIndex());
			for(Block b: blocks) {
				if(!b.getBlockHash().equals(block.getBlockHash())) {
					blockchain.removeBranch(b.getBlockHash());
				}
			}
			block = blockchain.getByHash(block.getPreviousHash());
		}
		logger.info("Trimmer.runImmediately() took {} ms.", System.currentTimeMillis() - start);
	}

}
