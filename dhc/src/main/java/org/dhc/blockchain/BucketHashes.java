package org.dhc.blockchain;

import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;

import org.dhc.network.Network;
import org.dhc.network.consensus.BucketHash;
import org.dhc.network.consensus.SendBucketHashMessage;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BucketHashes {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private Map<String, BucketHash> bucketHashes = new LinkedHashMap<String, BucketHash>();
	
	public BucketHashes() {
		
	}
	
	public BucketHashes clone() {
		BucketHashes clone = new BucketHashes();
		for(String key: bucketHashes.keySet()) {
			BucketHash bucketHash = bucketHashes.get(key);
			BucketHash bucketHashClone = bucketHash.clone();
			clone.bucketHashes.put(key, bucketHashClone);
		}
		return clone;
	}
	
	public Set<Transaction> getTransactions() {
		return getLastBucketHash().getTransactions();
	}
	
	public Set<Transaction> getReceivedTx() {
		return getLastBucketHash().getReceivedTx();
	}
	
	public Set<Transaction> getAllTransactions() {
		return getLastBucketHash().getAllTransactions();
	}
	
	public BucketHash getLastBucketHash() {
		LinkedList<BucketHash> list = new LinkedList<>(bucketHashes.values());
		BucketHash hash = list.getLast();
		return hash;
	}
	
	public BucketHash getFirstBucketHash() {
		LinkedList<BucketHash> list = new LinkedList<>(bucketHashes.values());
		BucketHash hash = list.getFirst();
		return hash;
	}
	
	public boolean isValid() {
		List<BucketHash> list = new ArrayList<>(bucketHashes.values());
		for(BucketHash hash: list) {
			if(!hash.isValid()) {
				return false;
			}
		}
		int difference = list.size() <= 2? 1: 2;
		BucketHash lastHash = getLastBucketHash();
		if(lastHash.getBinaryStringKey().length() != list.size() - difference) {
			logger.info("hash.getBinaryStringKey().length() {} != list.size() {}", lastHash.getBinaryStringKey().length(), list.size());
			displayHashes();
			return false;
		}
		
		if(lastHash != null && !lastHash.isHashForTransactionsValid()) {
			logger.info("Hash {} for {} transactions is not valid", lastHash.getHash(), 
					lastHash.getNumberOfTransactions());
			return false;
		}
		
		if(!isPathValid()) {
			logger.info("Path is not valid");
			return false;
		}

		return true;
	}
	
	public boolean isFeeValid() {
		List<BucketHash> list = new ArrayList<>(bucketHashes.values());
		Coin fee = Coin.ZERO;
		BucketHash first = list.remove(0);
		Transaction coinbase = getCoinbase();
		if(coinbase !=null && !coinbase.getValue().equals(first.getFee())) {
			logger.info("coinbase.getValue()={}, first.getFee()={}", coinbase.getValue(), first.getFee());
			return false;
		}
		if(list.isEmpty()) {
			fee = first.getFee();
		} else {
			Collections.reverse(list);
			for(BucketHash b: list) {
				fee = fee.add(b.getFee());
				//logger.info("fee={}, key='{}'", fee, b.getBinaryStringKey());
			}
		}
		if(fee.equals(first.getFee())) {
			return true;
		}
		logger.info("fee={}, first.getFee()={}", fee, first.getFee());
		return false;
	}
	
	public void cleanTransactions() {
		List<BucketHash> list = new ArrayList<>(bucketHashes.values());
		list.remove(list.size() - 1);
		for(BucketHash b: list) {
			b.setTransactionsToNull();
		}
	}

	private boolean isPathValid() {
		if(bucketHashes.size() <= 1) {
			return true;
		}
		BucketHash hash = getLastBucketHash();
		while(true) {
			BucketHash other = bucketHashes.get(hash.getKey().getOtherBucketKey().getKey());
			if(other == null) {
				logger.info("Other bucket hash is null");
				return false;
			}
			hash = new BucketHash(hash, other);
			if("".equals(hash.getBinaryStringKey())) {
				BucketHash check = bucketHashes.get(hash.getBinaryStringKey());
				if(check == null) {
					logger.info("Check bucket hash is null");
					return false;
				}
				if(!hash.getHash().equals(check.getHash())) {
					logger.info("!hash.getHash().equals(check.getHash())");
					logger.info("hash {}", hash.toStringFull());
					logger.info("check {}", check.toStringFull());
					logger.info("other {}", other.toStringFull());
					displayHashes();
					return false;
				}
				break;
			}
		}
		return true;
	}

	public int getNumberOfTransactions() {
		return getLastBucketHash().getNumberOfTransactions();
	}

	public String getConsensus() {
		if(bucketHashes.get("") == null) {
			return null;
		}
		return bucketHashes.get("").getHash();
	}
	
	@Override
	public String toString() {
		return "" + bucketHashes + ", # transactions: " + getNumberOfTransactions() + ", averagePower=" + getAveragePower();
	}

	public void put(BucketHash bucketHash) {
		
		if("".equals(bucketHash.getBinaryStringKey()) && !bucketHash.isMined()) {
			logger.info("put() Not mined bucketHash   : {}", bucketHash.toStringFull());
			logger.info("", new RuntimeException());
		}
		
		String key = bucketHash.getBinaryStringKey();
		String hash = bucketHash.getHash();
		BucketHash original = bucketHashes.get(key);
		if(original != null && !original.getHash().equals(hash)) {
			logger.info("Attempt to replace hash '{}' with '{}' for key '{}'", bucketHashes.get(key).getHash(), hash, key);
			logger.info("original hash: {}", original.toStringFull());
			logger.info("bucketHash   : {}", bucketHash.toStringFull());
			logger.info("", new RuntimeException());
			return;
		}
		if(!areChildrenValid(bucketHash)) {
			return;
		}
		if(original == null) {
			logger.trace("{} BucketHashes.put() {}", bucketHash.isMined(), bucketHash.toStringFull());
		}
		bucketHashes.put(key, bucketHash);

    }
	
	//used by database call to retrieve it
	public void set(BucketHash bucketHash) {
		String key = bucketHash.getBinaryStringKey();
		bucketHashes.put(key, bucketHash);
	}

	public BucketHash replace(BucketHash bucketHash) {
		
		if("".equals(bucketHash.getBinaryStringKey()) && !bucketHash.isMined()) {
			String str = String.format("%s replace() Not mined bucketHash %s", bucketHash.isMined(), bucketHash.toStringFull());
			logger.info(str, new RuntimeException());
		}
		
		BucketHash original = bucketHashes.get(bucketHash.getBinaryStringKey());
		if(original != null && original.isMined() && !bucketHash.isMined() && bucketHash.getHash().equals(original.getHash())) {
			logger.trace("Not replacing because original is mined but bucketHash is not");
			logger.trace("{} original   = {}", original.isMined(), original.toStringFull());
			logger.trace("{} bucketHash = {}", bucketHash.isMined(), bucketHash.toStringFull());
			return original;
		}
		if(!areChildrenValid(bucketHash)) {
			return original;
		}
		validateParent(bucketHash);
		bucketHashes.put(bucketHash.getBinaryStringKey(), bucketHash);
		if(original == null) {
			logger.trace("{} BucketHashes.put() {}", bucketHash.isMined(), bucketHash.toStringFull());
		} else {
			
			logger.trace("{} BucketHashes.replace() {}", original.isMined(), original.toStringFull());
			logger.trace("{} with                   {}", bucketHash.isMined(), bucketHash.toStringFull());
		}
		return bucketHash;
	}
	
	private void validateParent(BucketHash bucketHash) {
		if("".equals(bucketHash.getBinaryStringKey())) {
			return;
		}
		BucketHash parent = getBucketHash(bucketHash.getKey().getParentKey().getKey());
		if(parent == null) {
			return;
		}
		if(!parent.hasChild(bucketHash)) {
			logger.trace("parent does not have this child, not necessary an error if later parent is replaced also");
			logger.trace("child  {}", bucketHash.toStringFull());
			logger.trace("parent {}", parent.toStringFull());
		}
	}

	private boolean areChildrenValid(BucketHash bucketHash) {
		BucketHash left = getBucketHash(bucketHash.getKey().getLeftKey().getKey());
		BucketHash right = getBucketHash(bucketHash.getKey().getRightKey().getKey());
		if(left != null && right != null) {
			BucketHash test = new BucketHash(left, right);
			if(!test.getHash().equals(bucketHash.getHash())) {
				logger.info("left {}", left.toStringFull());
				logger.info("right {}", right.toStringFull());
				logger.info("test {}", test.toStringFull());
				logger.info("Wrong children for bucketHash {}", bucketHash.toStringFull());
				return false;
			}
		}
		return true;
	}

	public BucketHash getBucketHash(String key) {
		return bucketHashes.get(key);
	}

	public int size() {
		return bucketHashes.size();
	}
	
	public boolean isEmpty() {
		return bucketHashes.isEmpty();
	}
	
	public List<BucketHash> getBucketHashes() {
		return new ArrayList<>(bucketHashes.values());
	}
	
	public void displayHashes() {
		List<BucketHash> list = getBucketHashes();
		for(BucketHash hash: list) {
			logger.info("{}", hash.toStringFull());
		}
	}
	
	public void traceHashes() {
		List<BucketHash> list = getBucketHashes();
		for(BucketHash hash: list) {
			logger.trace("{}", hash.toStringFull());
		}
	}

	public int getPower() {
		LinkedList<String> list = new LinkedList<>(bucketHashes.keySet());
		String key = list.getLast();
		return key.length();
	}
	
	public long getShard() {
		LinkedList<String> list = new LinkedList<>(bucketHashes.keySet());
		String key = list.getLast();
		if("".equals(key)) {
			return 0;
		}
		BigInteger bigInteger = new BigInteger(key, 2);
		return bigInteger.longValue();
	}
	
	public String getShardOutOf() {
		return Long.toString(getShard()) + "/" + new BigInteger("2").pow(getPower()).longValue();
	}
	
	public boolean isMine() {
		return getLastBucketHash().isMyHash();
	}

	public boolean isHis(DhcAddress dhcAddress) {
		return getLastBucketHash().isHis(dhcAddress);
	}

	public void trim(int power) {
		if(getPower() <= power) {
			return;
		}
		
		LinkedList<String> list = new LinkedList<>(bucketHashes.keySet());
		String last = list.removeLast();
		BucketHash hash = getBucketHash(last);
		while(!list.isEmpty()) {
			String beforeLast = list.removeLast();
			hash =  new BucketHash(getBucketHash(beforeLast), hash);
			if(hash.getBinaryStringKey().length() == power) {
				break;
			}
		}
		
		LinkedHashMap<String, BucketHash> newBucketHashes = new LinkedHashMap<String, BucketHash>();
		for(String key: list) {
			newBucketHashes.put(key, getBucketHash(key));
		}
		newBucketHashes.put(hash.getBinaryStringKey(), hash);
		bucketHashes = newBucketHashes;
	}
	
	// TODO review and remove. This method used to be called from processReadyBucketHashesCallback but produces non mined bucket hashes
	public void addMyTransactions(Set<Transaction> transactions, long blockchainIndex) {
		BucketHash last = getLastBucketHash().clone();
		last.setTransactions(transactions);
		last.setLeftRight(null, null);
		
		bucketHashes.put(last.getBinaryStringKey(), last);
		if("".equals(last.getBinaryStringKey())) {
			return;
		}
		BucketHash next = last;
		BucketHash parent;
		while(true) {
			BucketHash nextOther = getBucketHash(next.getKey().getOtherBucketKey().getKey());
			parent = new BucketHash(next, nextOther);
			if(next.getAllTransactions() != null || nextOther.getAllTransactions() != null) {
				Set<Transaction> result = new HashSet<>();
				if(next.getAllTransactions() != null) {
					result.addAll(next.getAllTransactions());
				}
				if(nextOther.getAllTransactions() != null) {
					result.addAll(nextOther.getAllTransactions());
				}
				BucketHash clone = parent.clone();
				clone.setAllTransactions(result);
				if(clone.isHashForTransactionsValid() && !parent.isHashForTransactionsValid()) {
					parent = clone;
					logger.trace("Added transactions to parent {}", parent.toStringFull());
				}
			}

			if(parent.isMined()) {
				Network.getInstance().sendToKey(parent.getBinaryStringKey(), new SendBucketHashMessage(parent, blockchainIndex));
			} else {
				logger.trace("{} {} recover() Not mined parent.getKeyHash()={} parent.isMined={}", blockchainIndex, parent.getRealHashCode(), 
						parent.getKeyHash(), parent.isMined());
			}
			
			
			
			if("".equals(parent.getBinaryStringKey())) {
				break;
			}
			next = parent;
		}
		bucketHashes.put(parent.getBinaryStringKey(), parent);
		if(!isValid()) {
			displayHashes();
			throw new RuntimeException();
		}
	}

	public int getAveragePower() {
		return getFirstBucketHash().getAveragePower().intValue();
	}
	
	public String getPreviousBlockHash() {
		return getFirstBucketHash().getPreviousBlockHash();
	}

	public List<SimpleEntry<String, String>> getMerklePath() {
		List<SimpleEntry<String, String>> result = new LinkedList<>();
		for(String key: bucketHashes.keySet()) {
			result.add(new SimpleEntry<String, String>(key, bucketHashes.get(key).getHash()));
		}
		if(result.size() > 1) {
			result.remove(result.size() - 1);
		}
		return result;
	}

	public void addTransaction(Transaction transaction) {
		getLastBucketHash().addTransaction(transaction);
	}
	
	public Transaction getCoinbase() {
		return getLastBucketHash().getCoinbase();
	}

	public void removeCoinbase() {
		getLastBucketHash().removeCoinbase();
		
	}

	public Set<Transaction> getReceivedTx(String bucketKey) {
		return getLastBucketHash().getReceivedTx(bucketKey);
	}

}
