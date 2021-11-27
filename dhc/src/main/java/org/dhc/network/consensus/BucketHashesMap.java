package org.dhc.network.consensus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.dhc.blockchain.BucketHashes;
import org.dhc.network.BucketKey;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class BucketHashesMap {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Map<String, BucketHashes> bucketHashesMap = new HashMap<>();
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private DhcAddress dhcAddress;

	public BucketHashesMap(DhcAddress dhcAddress) {
		this.dhcAddress = dhcAddress;
	}

	public void clear() {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			bucketHashesMap.clear();
		} finally {
			writeLock.unlock();
		}
	}

	public void put(BucketHash bucketHash) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			String key = bucketHash.getBinaryStringKey();
			if(!key.equals(dhcAddress.getBinary(key.length())) && !key.equals(new BucketKey(dhcAddress.getBinary(key.length())).getOtherBucketKey().getKey())) {
				logger.info("key {} is wrong for dhcAddress {}", key, dhcAddress.getBinary(key.length()));
				return;
			}
			BucketHashes hashes = bucketHashesMap.get(bucketHash.getPreviousBlockHash());
			if (hashes == null) {
				hashes = new BucketHashes();
				bucketHashesMap.put(bucketHash.getPreviousBlockHash(), hashes);
			}
			hashes.put(bucketHash);
		} finally {
			writeLock.unlock();
		}
	}
	
	public void set(BucketHash bucketHash) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			String key = bucketHash.getBinaryStringKey();
			if(!key.equals(dhcAddress.getBinary(key.length())) && !key.equals(new BucketKey(dhcAddress.getBinary(key.length())).getOtherBucketKey().getKey())) {
				logger.info("key {} is wrong for dhcAddress {}", key, dhcAddress.getBinary(key.length()));
				return;
			}
			BucketHashes hashes = bucketHashesMap.get(bucketHash.getPreviousBlockHash());
			if (hashes == null) {
				hashes = new BucketHashes();
				bucketHashesMap.put(bucketHash.getPreviousBlockHash(), hashes);
			}
			hashes.set(bucketHash);
		} finally {
			writeLock.unlock();
		}
	}

	public Map<String, BucketHash> getBySecondKey(String key) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			Map<String, BucketHash> map = new HashMap<>();
			for (String blockHash : bucketHashesMap.keySet()) {
				BucketHashes bucketHashes = bucketHashesMap.get(blockHash);
				BucketHash bucketHash = bucketHashes.getBucketHash(key);
				if (bucketHash != null) {
					map.put(blockHash, bucketHash);
				}
			}
			return map;
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public String toString() {
		return bucketHashesMap.toString();
	}

	public BucketHash get(String blockHash, String key) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			BucketHashes bucketHashes = bucketHashesMap.get(blockHash);
			if (bucketHashes == null) {
				return null;
			}
			return bucketHashes.getBucketHash(key);
		} finally {
			readLock.unlock();
		}
	}

	public int longestSize() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			int result = 0;
			for (BucketHashes hashes : bucketHashesMap.values()) {
				result = result < hashes.size() ? hashes.size() : result;
			}
			return result;
		} finally {
			readLock.unlock();
		}
	}

	public void replace(BucketHashes bucketHashes) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			bucketHashesMap.put(bucketHashes.getPreviousBlockHash(), bucketHashes.clone());
		} finally {
			writeLock.unlock();
		}
	}
	
	public void replace(BucketHash bucketHash) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			String key = bucketHash.getBinaryStringKey();
			if(!key.equals(dhcAddress.getBinary(key.length())) && !key.equals(new BucketKey(dhcAddress.getBinary(key.length())).getOtherBucketKey().getKey())) {
				logger.info("key {} is wrong for dhcAddress {}", key, dhcAddress.getBinary(key.length()));
				return;
			}
			BucketHashes hashes = bucketHashesMap.get(bucketHash.getPreviousBlockHash());
			if (hashes == null) {
				hashes = new BucketHashes();
				bucketHashesMap.put(bucketHash.getPreviousBlockHash(), hashes);
			}
			hashes.replace(bucketHash);
		} finally {
			writeLock.unlock();
		}
		
	}

}
