package org.dhc.blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcLogger;

public class Keywords {

	private static final DhcLogger logger = DhcLogger.getLogger();
	public static final int KEYWORD_MAX_LENGTH = 64;
	public static final int MAX_NUMBER_OF_KEYWORDS = 10;
	
	private Map<String, String> map = new HashMap<String, String>();
	private String hash;
	private String transactionId;
	private String blockHash;
	
	public Keywords clone() {
		Keywords clone = new Keywords();
		clone.putAll(getMap());
		clone.hash = hash;
		clone.transactionId = transactionId;
		clone.blockHash = blockHash;
		return clone;
	}
	
	private String calculateHash() {
		List<String> keys = new ArrayList<String>(map.keySet());
		List<String> values = new ArrayList<String>(map.values());
		String hash = CryptoUtil.getHashBase58Encoded(CryptoUtil.getMerkleTreeRoot(keys) + CryptoUtil.getMerkleTreeRoot(values));
		return hash;
	}

	private Map<String, String> getMap() {
		return map;
	}

	public String getHash() {
		if(hash == null) {
			hash = calculateHash();
		}
		return hash;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public boolean isValid() {
		if(getMap().isEmpty()) {
			logger.debug("map is empty");
			return false;
		}
		for(String key: getMap().keySet()) {
			if(key.length() > KEYWORD_MAX_LENGTH) {
				logger.debug("key is too long");
				return false;
			}
			if(getMap().get(key).length() > KEYWORD_MAX_LENGTH) {
				logger.debug("value is too long");
				return false;
			}
		}
		if(!calculateHash().equals(getHash())) {
			logger.debug("calculateHash()={}, hash={}", calculateHash(), hash);
			return false;
		}
		
		return true;
	}
	
	public void put(String key, String value) {
		if(key == null) {
			throw new RuntimeException("key cannot be null");
		}
		if(key.length() > KEYWORD_MAX_LENGTH) {
			throw new RuntimeException("key cannot be longer than " + KEYWORD_MAX_LENGTH + " characters");
		}
		if(value == null) {
			throw new RuntimeException("value cannot be null");
		}
		if(value.length() > KEYWORD_MAX_LENGTH) {
			throw new RuntimeException("value cannot be longer than " + KEYWORD_MAX_LENGTH + " characters");
		}
		getMap().put(key, value);
		
	}
	
	public void putAll(Map<String, String> map) {
		for(String key: map.keySet()) {
			put(key, map.get(key));
		}
	}
	
	public boolean isEmpty() {
		return getMap().isEmpty();
	}

	public String get(String key) {
		return getMap().get(key);
	}
	
	public Set<String> keySet() {
		return getMap().keySet();
	}

	@Override
	public String toString() {
		return getMap().toString();
	}

	public String getBlockHash() {
		return blockHash;
	}

	public void setBlockHash(String blockHash) {
		this.blockHash = blockHash;
	}
	
}
