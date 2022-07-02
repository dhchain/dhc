package org.dhc.network;

import java.util.concurrent.locks.ReentrantLock;

import org.dhc.util.DhcLogger;

public class MajorRunnerLock {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final MajorRunnerLock instance = new MajorRunnerLock();
	
	private final ReentrantLock lock = new ReentrantLock();
	
	public static MajorRunnerLock getInstance() {
		return instance;
	}
	
	private MajorRunnerLock() {
		
	}

	public ReentrantLock getLock() {
		return lock;
	}
	
	public void ifLockedThenWait() throws InterruptedException {
		if(lock.tryLock()) {
			lock.unlock();
			logger.trace("not locked, returning");
			return;
		}
		logger.trace("START waiting for lock");
		lock.lock();
		try {
			logger.trace("END waiting for lock");
		} finally {
			lock.unlock();
		}
	}

}
