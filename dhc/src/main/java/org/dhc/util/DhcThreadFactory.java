package org.dhc.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class DhcThreadFactory implements ThreadFactory {
	
	private String poolName;
	private AtomicLong threadCounter = new AtomicLong();

	public DhcThreadFactory(String poolName) {
		super();
		this.poolName = poolName;
	}

	@Override
	public Thread newThread(Runnable runnable) {
		return new Thread(runnable, poolName + "-thread-" + threadCounter.getAndIncrement());
	}

}
