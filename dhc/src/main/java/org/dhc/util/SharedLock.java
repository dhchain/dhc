package org.dhc.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SharedLock {
	
	private static SharedLock instance = new SharedLock();
	
	public static SharedLock getInstance() {
		return instance;
	}
	
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	
	private SharedLock() {
		
	}

	public Lock readLock() {
		return readWriteLock.readLock();
	}

	public Lock writeLock() {
		return readWriteLock.writeLock();
	}


}
