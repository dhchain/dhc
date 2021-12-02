package org.dhc.blockchain;

import java.util.HashMap;
import java.util.Map;

import org.dhc.network.BucketKey;
import org.dhc.util.Constants;
import org.dhc.util.ExpiringMap;
import org.dhc.util.DhcLogger;

public class MissingBlocks {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final MissingBlocks instance = new MissingBlocks();
	
	public static MissingBlocks getInstance() {
		return instance;
	}
	
	private MissingBlocks() {

	}
	
	private final ExpiringMap<String, Map<String, Block>> missingBlocks =  new ExpiringMap<String, Map<String, Block>>(Constants.MINUTE * 10);
	private final ExpiringMap<String, Map<String, Block>> foundBlocks =  new ExpiringMap<String, Map<String, Block>>(Constants.MINUTE * 10);
	
	public synchronized void putMissingBlocks(String blockhash, String key, Block block) {
		Map<String, Block> map = missingBlocks.get(blockhash);
		if(map == null) {
			map = new HashMap<>();
			missingBlocks.put(blockhash, map);
		}
		map.put(key, block);
	}
	
	public synchronized void removeMissingBlocks(String blockhash, String key) {
		Map<String, Block> map = missingBlocks.get(blockhash);
		if(map == null) {
			return;
		}
		map.remove(key);
	}
	
	public synchronized Block getMissingBlocks(String blockhash, String key) {
		Map<String, Block> map = missingBlocks.get(blockhash);
		if(map == null) {
			return null;
		}
		return map.get(key);
	}
	
	public synchronized void putFoundBlocks(String blockhash, String key, Block block) {
		logger.info("MissingBlocks.putFoundBlocks() {} {} {} {}", block.getIndex(), key, blockhash, block);
		Map<String, Block> map = foundBlocks.get(blockhash);
		if(map == null) {
			map = new HashMap<>();
			foundBlocks.put(blockhash, map);
		}
		map.put(key, block);
		BucketKey bucketKey = new BucketKey(block.getBucketKey());
		Block otherBlock = null;
		Block combinedBlock = null;
		for(Block b: map.values()) {
			if(b.getBucketKey().equals(bucketKey.getOtherBucketKey().getKey())) {
				otherBlock = b;
			}
			if(b.getBucketKey().equals(bucketKey.getParentKey().getKey())) {
				combinedBlock = b;
			}
		}
		if(combinedBlock != null) {
			return;
		}
		if(otherBlock == null) {
			return;
		}
		combinedBlock = block.combine(otherBlock);
		putFoundBlocks(blockhash, combinedBlock.getBucketKey(), combinedBlock);
	}
	
	public synchronized Block getFoundBlocks(String blockhash, String key) {
		Map<String, Block> map = foundBlocks.get(blockhash);
		if(map == null) {
			return null;
		}
		return map.get(key);
	}

}
