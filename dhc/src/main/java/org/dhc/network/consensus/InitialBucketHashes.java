package org.dhc.network.consensus;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.dhc.blockchain.Blockchain;
import org.dhc.util.DhcLogger;
import org.dhc.util.Registry;

public class InitialBucketHashes {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	// Multi key map to store BucketHash by index, previousBlockHash, key, hash
	private Map<Long, Map<String, Map<String, Map<String, BucketHash>>>> bucketHashes =  new LinkedHashMap<>();
	
	public BucketHash waitForBucketHash(BucketHash bucketHashParameter, long blockchainIndex, long bits) {
		BucketHash bucketHash = bucketHashParameter;
		BucketHash result = findExisting(bucketHash, blockchainIndex);
		if(result != null && result.isMined()) {
			return result;
		}
		synchronized(this) {
			BucketHash alreadyWatingBucketHash = get(bucketHash, blockchainIndex);
			if(alreadyWatingBucketHash != null) {
				bucketHash = alreadyWatingBucketHash;
			} else {
				put(bucketHash, blockchainIndex);
			}
		}

		long start = System.currentTimeMillis();

		bucketHash.mine(blockchainIndex, bits);

		if(!bucketHash.isMined() && blockchainIndex == Blockchain.getInstance().getIndex()) {
			logger.trace("{} {} {} waitForBucketHash () done   buckethash={} {}ms", blockchainIndex, bucketHash.isMined(), bucketHash.getRealHashCode(), bucketHash.getKeyHash(), System.currentTimeMillis() - start);
		}
		
		if(bucketHash.isMined()) {
			bucketHashParameter.setTimestamp(bucketHash.getTimestamp());
			bucketHashParameter.setNonce(bucketHash.getNonce());
		}

		result = findExisting(bucketHash, blockchainIndex);
		if(result != null && bucketHash.isMined() && bucketHash.getHash().equals(result.getHash()) && !result.isMined()) {
			result.setTimestamp(bucketHash.getTimestamp());
			result.setNonce(bucketHash.getNonce());
		}
		
		if(result != null && !result.isMined() && "".equals(result.getBinaryStringKey())) {
			logger.info("{} {} {} waitForBucketHash() result={}", blockchainIndex, result.isMined(), result.getRealHashCode(), result.getKeyHash());
			logger.info("", new RuntimeException());
		}
		
		return result;
	}
	
	/**
	 * Searches BucketHashes in memory by index, previous block hash, bucket key. Returns first found or null if none found
	 * @param bucketHash
	 * @param blockchainIndex
	 * @return BucketHash
	 */
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

		logger.trace("{} Found {} existing buckethashes", blockchainIndex, existingBucketHashes.size());
		logger.trace("{} {} Will use existing buckethash {}", blockchainIndex, result.isMined(), result.toStringFull());

		return result;
	}
	
	public void notifyForBucketHash(BucketHash bucketHash, long blockchainIndex) {
		
		if(!bucketHash.isMined()) {
			String str = String.format("notify skip bucketHash.getKeyHash()=%s blockchainIndex=%s bucketHash.getRealHashCode()=%s bucketHash.isMined=%s",
					bucketHash.getKeyHash(), blockchainIndex, bucketHash.getRealHashCode(), bucketHash.isMined());
			logger.trace(str, new RuntimeException());
			return;
		}
		
		Set<BucketHash> hashes = new HashSet<>();
		synchronized(this) {
			Map<String, BucketHash> map = getByKey(bucketHash, blockchainIndex);
			if(map == null) {
				return;
			}
			hashes.addAll(map.values());
		}
		
		for(BucketHash foundBucketHash: hashes) {
			if(foundBucketHash.isMined()) {
				continue;
			}
			
			if(foundBucketHash.getHash().equals(bucketHash.getHash())) {
				foundBucketHash.setTimestamp(bucketHash.getTimestamp());
				foundBucketHash.setNonce(bucketHash.getNonce());
			}
			
			foundBucketHash.stopMining();
			
		}

	}
	
	public synchronized void notifyForBucketHashFromRecover(BucketHash bucketHash, long blockchainIndex) { // notify without checking if mined
		Map<String, BucketHash> map = getByKey(bucketHash, blockchainIndex);
		if(map == null) {
			return;
		}
		for(BucketHash foundBucketHash: map.values()) {
			foundBucketHash.stopMining();
		}
	}
	
	private synchronized BucketHash get(BucketHash bucketHash, long blockchainIndex) {
		Map<String, BucketHash> mapByKey = getByKey(bucketHash, blockchainIndex);
		if(mapByKey == null) {
			return null;
		}
		BucketHash result = mapByKey.get(bucketHash.getHash());
		return result;
	}
	
	private synchronized Map<String, BucketHash> getByKey(BucketHash bucketHash, long blockchainIndex) {
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
		return mapByKey;
	}
	
	private synchronized void put(BucketHash bucketHash, long blockchainIndex) {
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

	
	public synchronized void clear() {
		bucketHashes.clear();
	}

	

}
