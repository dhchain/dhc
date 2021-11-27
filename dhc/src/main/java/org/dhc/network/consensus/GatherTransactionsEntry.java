package org.dhc.network.consensus;

import java.util.HashMap;
import java.util.Map;

import org.dhc.util.Callback;

public class GatherTransactionsEntry {
	
	private BucketHash bucketHash;
	private long blockchainIndex;
	private Callback callback;
	private Map<String, BucketHash> map = new HashMap<String, BucketHash>();
	
	public GatherTransactionsEntry(BucketHash bucketHash, long blockchainIndex, Callback callback) {
		this.bucketHash = bucketHash;
		this.blockchainIndex = blockchainIndex;
		this.callback = callback;
	}
	
	public String getKey() {
		String key = bucketHash.getKeyHash() + "-" + blockchainIndex;
		return key;
	}
	
	public static String constructKey(BucketHash bucketHash, long blockchainIndex) {
		String key = bucketHash.getKeyHash() + "-" + blockchainIndex;
		return key;
	}

	public BucketHash getBucketHash() {
		return bucketHash;
	}

	public void setBucketHash(BucketHash bucketHash) {
		this.bucketHash = bucketHash;
	}

	public long getBlockchainIndex() {
		return blockchainIndex;
	}

	public void setBlockchainIndex(long blockchainIndex) {
		this.blockchainIndex = blockchainIndex;
	}

	public Callback getCallback() {
		return callback;
	}

	public void setCallback(Callback callback) {
		this.callback = callback;
	}

	public Map<String, BucketHash> getMap() {
		return map;
	}
	

}
