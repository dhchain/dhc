package org.dhc.network;

import org.dhc.util.Constants;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class PeerSync {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final PeerSync instance = new PeerSync();
	
	public static PeerSync getInstance() {
		return instance;
	}

	private volatile boolean running;
	
	private PeerSync() {
		
	}
	
	public void start() {
		schedule();
	}
	
	private void schedule() {
		ThreadExecutor.getInstance().schedule(new DhcRunnable("PeerSynchronizer") {
			
			@Override
			public void doRun() {
				schedule();
				executeNow();
				
			}
		}, Constants.HOUR * 24);

	}
	
	public void executeAndWait() {
		synchronized (this) {
			if (running) {
				try {
					wait();
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
				}
				return;
			}
		}
		executeNow();
	}
	
	public void executeNow() {
		if (running) {
			return;
		}
		synchronized (this) {
			if (running) {
				return;
			}
			running = true;
		}
		logger.info("PeerSynchronizer START");
		try {
			Network network = Network.getInstance();
			Bootstrap.getInstance().bootstrap();
			int myPeersCount = network.getMyBucketPeers().size();
			while(true) {
				network.reloadBuckets();
				PeersFinder.getInstance().getPeers();
				int count = network.getMyBucketPeers().size();
				if(myPeersCount == count) {
					break;
				}
				myPeersCount = count;
			}
			
		} finally {
			synchronized (this) {
				running = false;
				logger.info("PeerSynchronizer END");
				notifyAll();
			}
		}
	}

}
