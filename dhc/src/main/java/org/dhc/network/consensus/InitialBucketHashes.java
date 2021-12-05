package org.dhc.network.consensus;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.dhc.util.Constants;
import org.dhc.util.Registry;
import org.dhc.util.DhcLogger;

public class InitialBucketHashes {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final long WAIT_TIME = Constants.SECOND * 10;
	
	// Multi key map to store BucketHash by index, previousBlockHash, key, hash
	private Map<Long, Map<String, Map<String, Map<String, BucketHash>>>> bucketHashes =  new LinkedHashMap<>();
	
	public BucketHash waitForBucketHash(BucketHash bucketHash, long blockchainIndex) {
		BucketHash result = findExisting(bucketHash, blockchainIndex);
		if(result != null) {
			return result;
		}
		put(bucketHash, blockchainIndex);
		synchronized (bucketHash) {
			try {
				long waitTime = ThreadLocalRandom.current().nextLong(1, WAIT_TIME);//Have to start with one because if 0 then the lock will wait forever, if not notified.
				logger.info("wait   buckethash={} blockchainIndex={} hashcode={}", bucketHash.getKeyHash(), blockchainIndex, bucketHash.getRealHashCode());
				long start = System.currentTimeMillis();
				bucketHash.wait(waitTime);
				logger.info("done   buckethash={} blockchainIndex={} hashcode={} {}ms", bucketHash.getKeyHash(), blockchainIndex, bucketHash.getRealHashCode(), System.currentTimeMillis() - start);
			} catch (InterruptedException e) {
				logger.info(e.getMessage(), e);
			}
		}
		result = findExisting(bucketHash, blockchainIndex);
		return result;
	}
	
	private BucketHash findExisting(BucketHash bucketHash, long blockchainIndex) {
		boolean hasNullTransactions = bucketHash.hasNullTransactions();
		BucketHash result = null;
		Collection<BucketHash> existingBucketHashes = Registry.getInstance().getBucketConsensuses().getByIndexByPreviousBlockHashByKey(bucketHash, blockchainIndex);
		if (existingBucketHashes == null || existingBucketHashes.isEmpty()) {
			return null;
		}
		
		for(Iterator<BucketHash> iterator = existingBucketHashes.iterator(); iterator.hasNext();) {
			BucketHash b = iterator.next();
			if (!hasNullTransactions) {
				if (!b.hasNullTransactions()) {
					result = b;
					break;
				}
			} else {
				result = b;
				break;
			}
		}

		if (result == null) {
			return null;
		}

		logger.trace("Found {} existing buckethashes", existingBucketHashes.size());
		logger.trace("Will use existing buckethash {} {}", blockchainIndex, result.toStringFull());

		return result;
	}
	
	public void notifyForBucketHash(BucketHash bucketHash, long blockchainIndex) {
		BucketHash foundBucketHash = get(bucketHash, blockchainIndex);
		if(foundBucketHash == null) {
			return;
		}
		synchronized (foundBucketHash) {
			foundBucketHash.notify();
			logger.info("notify buckethash={} blockchainIndex={} hashcode={}", foundBucketHash.getKeyHash(), blockchainIndex, foundBucketHash.getRealHashCode());
		}
	}
	
	private BucketHash get(BucketHash bucketHash, long blockchainIndex) {
		Map<String, Map<String, Map<String, BucketHash>>> mapByIndex = bucketHashes.get(blockchainIndex);
		if(mapByIndex == null) {
			return null;
		}
		Map<String, Map<String, BucketHash>> mapByPreviousBlockHash = mapByIndex.get(bucketHash.getPreviousBlockHash());
		if(mapByPreviousBlockHash == null) {
			return null;
		}
		Map<String, BucketHash> mapByKey = mapByPreviousBlockHash.get(bucketHash.getBinaryStringKey());
		if(mapByKey == null) {
			return null;
		}
		BucketHash result = mapByKey.get(bucketHash.getHash());
		return result;
	}
	
	private void put(BucketHash bucketHash, long blockchainIndex) {
		Map<String, Map<String, Map<String, BucketHash>>> mapByIndex = bucketHashes.get(blockchainIndex);
		if(mapByIndex == null) {
			mapByIndex = new HashMap<>();
			bucketHashes.put(blockchainIndex, mapByIndex);
		}
		Map<String, Map<String, BucketHash>> mapByPreviousBlockHash = mapByIndex.get(bucketHash.getPreviousBlockHash());
		if(mapByPreviousBlockHash == null) {
			mapByPreviousBlockHash = new HashMap<>();
			mapByIndex.put(bucketHash.getPreviousBlockHash(), mapByPreviousBlockHash);
		}
		Map<String, BucketHash> mapByKey = mapByPreviousBlockHash.get(bucketHash.getBinaryStringKey());
		if(mapByKey == null) {
			mapByKey = new HashMap<>();
			mapByPreviousBlockHash.put(bucketHash.getBinaryStringKey(), mapByKey);
		}

		mapByKey.put(bucketHash.getHash(), bucketHash);
	}

	
	public void clear() {
		bucketHashes.clear();
	}

}
