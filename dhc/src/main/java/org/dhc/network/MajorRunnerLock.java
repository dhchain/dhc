package org.dhc.network;

import java.util.concurrent.locks.ReentrantLock;

public class MajorRunnerLock {
	
	private static final MajorRunnerLock instance = new MajorRunnerLock();
	
	private ReentrantLock lock = new ReentrantLock();
	
	public static MajorRunnerLock getInstance() {
		return instance;
	}
	
	private MajorRunnerLock() {
		
	}

	public ReentrantLock getLock() {
		return lock;
	}

}
