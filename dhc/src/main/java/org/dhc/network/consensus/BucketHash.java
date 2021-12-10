package org.dhc.network.consensus;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Transaction;
import org.dhc.network.BucketKey;
import org.dhc.network.Network;
import org.dhc.util.Coin;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Difficulty;

public class BucketHash {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final BigDecimal TWO = new BigDecimal(2);
	
	private BucketKey key;
	private String hash;
	private BucketHash left;
	private BucketHash right;
	private Set<Transaction> transactions;
	private BigDecimal averagePower = BigDecimal.ZERO;
	private String previousBlockHash;
	private Coin fee = Coin.ZERO;
	private long timestamp = System.currentTimeMillis();
	private int nonce;
	private transient volatile boolean stop;
	private long bits;
	
	public BucketHash() {
		
	}
	
	public synchronized BucketHash clone() {
		BucketHash clone = new BucketHash();
		clone.key = key;
		clone.hash = hash;
		
		if(left != null && right != null) {
			clone.setLeftRight(left.clone(), right.clone());
		}
		clone.transactions = cloneTransactions(transactions);
		clone.averagePower = averagePower;
		clone.previousBlockHash = previousBlockHash;
		clone.setFee(getFee());
		clone.timestamp = timestamp;
		clone.nonce = nonce;
		clone.bits = bits;

		return clone;
	}
	
	public BucketHash cloneWithoutTransactions() {
		BucketHash clone = new BucketHash();
		clone.key = key;
		clone.hash = hash;
		if(left != null && right != null) {
			clone.setLeftRight(left.clone(), right.clone());
		}
		clone.averagePower = averagePower;
		clone.previousBlockHash = previousBlockHash;
		clone.setFee(getFee());
		clone.timestamp = timestamp;
		clone.nonce = nonce;
		clone.bits = bits;

		return clone;
	}
	
	private Set<Transaction> cloneTransactions(Set<Transaction> transactions) {
		Set<Transaction> set = transactions;
		if(set == null) {
			return null;
		}
		set = new HashSet<>(set);
		Set<Transaction> result = new HashSet<>();
		for(Transaction transaction: transactions) {
			result.add(transaction.clone());
		}
		
		return result;
		
	}
	
	public BucketHash(String key, Set<Transaction> transactions, String previousBlockHash) {
		this.key = new BucketKey(key);
		this.previousBlockHash = previousBlockHash;
		if(Network.getInstance().getPower() == getPower()) {
			setAveragePower(new BigDecimal(Network.getInstance().getPossiblePower()));
		}
		setTransactions(transactions);
	}
	
	public BucketHash(String key, String hash, String previousBlockHash) {
		this.key = new BucketKey(key);
		this.hash = hash;
		this.previousBlockHash = previousBlockHash;
		if(Network.getInstance().getPower() == getPower()) {
			setAveragePower(new BigDecimal(Network.getInstance().getPossiblePower()));
		}
	}
	
	public BucketHash(BucketHash child, BucketHash otherChild) {
		if(!child.previousBlockHash.equals(otherChild.previousBlockHash)) {
			logger.info("child {}, child.previousBlockHash {}", child.toStringFull(), child.previousBlockHash);
			logger.info("otherChild {}, otherChild.previousBlockHash {}", otherChild.toStringFull(), otherChild.previousBlockHash);
			throw new RuntimeException("Child and otherChild have different previous block hashes");
		}
		previousBlockHash = child.previousBlockHash;
	    addChild(child);

	    if(!child.getKey().getOtherBucketKey().getKey().equals(otherChild.getBinaryStringKey())) {
	    	logger.error("child {}", child.toStringFull());
	    	logger.error("otherChild {}", otherChild.toStringFull());
	    	throw new RuntimeException("otherChild has wrong key");
	    }

	    addChild(otherChild);
	    computeFromChildren();
	    if(hasOnlyOneChild()) {
			logger.info("bucketHash {}", toStringFull());
			logger.info("", new RuntimeException());
		}
	}
	
	

	public String getBinaryStringKey() {
		return key.getKey();
	}
	public void setBinaryStringKey(String key) {
		this.key = new BucketKey();
		this.key.setKey(key);
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public BucketHash getLeft() {
		return left;
	}
	private void setLeft(BucketHash left) {
		if(left != null) {
			assert key.getLeftKey().equals(left.key) : "wrong left key";
		}
		
		this.left = left;
	}
	public BucketHash getRight() {
		return right;
	}
	private void setRight(BucketHash right) {
		if(right != null) {
			assert key.getRightKey().equals(right.key) : "wrong right key";
		}
		
		this.right = right;
		isValid();
	}
	
	public synchronized void setLeftRight(BucketHash left, BucketHash right) {
		if((left == null && right != null) || (left != null && right == null)) {
			logger.trace("Only one child is not null, will not set children");
			logger.trace("this  buckethash {}", this.toStringFull());
			logger.trace("left  buckethash {}", left == null? "null": left.toStringFull());
			logger.trace("right buckethash {}", right == null? "null": right.toStringFull());
			return;
		}
		setLeft(left);
		setRight(right);
	}
	
	public void addChild(BucketHash child) {
		if(key == null) {
			key = child.key.getParentKey();
		}
		if(key.getLeftKey().equals(child.key)) {
			setLeft(child);
		} else {
			setRight(child);
		}
	}
	
	public void computeFromChildren() {
		if(key == null) {
			key = left.key.getParentKey();
		}
		
		String parentHash;
		if("".equals(left.getHash())) {
			parentHash = right.getHash();
		} else if("".equals(right.getHash())) {
			parentHash = left.getHash();
		} else {
			parentHash = CryptoUtil.getHashBase58Encoded((left.getHash() + right.getHash()));
		}
		
		assert hash == null || hash.equals(parentHash): "Existing hash is not equal computed from children hash";
		hash = parentHash;
		averagePower = left.averagePower.add(right.averagePower).divide(TWO);
		setFee(left.
				getFee().
				add(right.
						getFee()));

		
	}
	public BucketKey getKey() {
		return key;
	}
	public void setKey(BucketKey key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return hash;
	}
	
	public String toStringFull() {
		Set<Transaction> set = getTransactions();
		return getKeyHash() 
				+ ", left: " 
				+ (left==null? "null": left.getKeyHash())
						+ ", right: " 
						+ (right==null? "null": right.getKeyHash())
								+ ", # transactions: " + (set == null? "null": set.size())
								+ ", fee: " + getFee().toNumberOfCoins() + ", isMined=" + isMined();
	}
	
	public String getKeyHash() {
		// do not remove previousBlockHash, competing blocks might have buckethashes with the same key and hash but different previousBlockHash
		// and getKeyHash() used in several places as unique key that store buckethash elements.
		String str = String.format("'%s'='%s'-%s", key.getKey(), hash, previousBlockHash);
		return str;
	}

	public synchronized Set<Transaction> getTransactions() {
		Set<Transaction> set = getTransactionsIncludingCoinbase();
		if(set == null) {
			return null;
		}

		for (Iterator<Transaction> iterator = set.iterator(); iterator.hasNext();) {
			if (iterator.next().isCoinbase()) {
				iterator.remove();
			}
		}
		return set;
	}
	
	public synchronized Set<Transaction> getTransactionsIncludingCoinbase() {
		Set<Transaction> set = transactions;
		if(set == null) {
			return null;
		}
		set = Transaction.filter(getBinaryStringKey(), cloneTransactions(set));

		return set;
	}
	
	public synchronized Set<Transaction> getAllTransactions() {
		return cloneTransactions(transactions);
	}
	
	public int getNumberOfTransactions() {
		Set<Transaction> transactions = getTransactions();
		if(transactions == null) {
			return 0;
		}
		return transactions.size();
	}

	public synchronized void setTransactions(Set<Transaction> transactions) {
		Set<Transaction> set = Transaction.filter(getBinaryStringKey(), transactions);
		Set<Transaction> cloneSet = new HashSet<>();
		for(Transaction transaction: set) {
			cloneSet.add(transaction.clone());
		}
		this.transactions = cloneSet;
		recalculateHashFromTransactions();
		setFee(Transaction.collectFees(getTransactionsIncludingCoinbase()));
	}
	
	//include also cross shard transactions
	public synchronized void setAllTransactions(Set<Transaction> transactions) {
		this.transactions = cloneTransactions(transactions);
		setFee(Transaction.collectFees(getTransactionsIncludingCoinbase()));
	}
	
	public synchronized boolean isValid() {
		
		if(left !=null && right!=null) {
			String parentHash;
			if("".equals(left.getHash())) {
				parentHash = right.getHash();
			} else if("".equals(right.getHash())) {
				parentHash = left.getHash();
			} else {
				parentHash = CryptoUtil.getHashBase58Encoded((left.getHash() + right.getHash()));
			}
			if(hash != null && !hash.equals(parentHash)) {
				logger.info("hash != left + right");
				logger.info("hash: {}", toStringFull());
				logger.info("", new RuntimeException());
				return false;
			}
			
		}
		if(!hasNullTransactions()) {
			if(!isHashForTransactionsValid()) {
				logger.info("hash != hash of transactions for hash={}", toStringFull());
				for(Transaction transaction: getTransactions()) {
					logger.info("transaction {}", transaction);
				}
				return false;
			} else {
				Coin fee = Transaction.collectFees(getTransactions());
				if(!getFee().equals(fee)) {
					logger.info("!getFee().equals(fee) for hash={}", toStringFull());
					logger.info("getFee()={}, fee={}", getFee(), fee);
					logger.info("", new RuntimeException());
					return false;
				}
			}
		}
		
		if(left == null && right == null && !isHashForTransactionsValid() && isMyHash()) {
			logger.info("hash != hash of transactions for hash={}", toStringFull());
			logger.info("", new RuntimeException());
			return false;
		}
		
		return true;
	}
	
	public boolean isMyHash() {
		return DhcAddress.getMyDhcAddress().isMyKey(getBinaryStringKey());
	}

	public boolean isHis(DhcAddress dhcAddress) {
		return dhcAddress.isMyKey(getBinaryStringKey());
	}
	
	public boolean isHashForTransactionsValid() {
		return hash.equals(Transaction.computeHash(getKey().getKey(), getTransactions()));
	}
	
	public void recalculateHashFromTransactions() {
		hash = Transaction.computeHash(getKey().getKey(), getTransactions());
	}

	public BigDecimal getAveragePower() {
		return averagePower;
	}

	public void setAveragePower(BigDecimal averagePower) {
		this.averagePower = averagePower;
	}
	
	public int getPower() {
		return getBinaryStringKey().length();
	}

	public String getPreviousBlockHash() {
		return previousBlockHash;
	}

	public synchronized void addTransaction(Transaction transaction) {
		Transaction coinbase = getCoinbase();
		if(transaction.isCoinbase() && coinbase != null) {
			logger.info("Cannot add another coinbase {}", transaction);
			logger.info("There is already a coinbase {}", coinbase);
			throw new RuntimeException("Cannot add another coinbase");
		}
		Set<Transaction> set = transactions;
		if(set == null) {
			set = new HashSet<>();
		}
		set.add(transaction.clone());
		transactions = set;
	}

	public synchronized void setBlockHash(String blockHash, long blockIndex) {
		Set<Transaction> set = transactions;
		if(set == null || set.isEmpty()) {
			return;
		}
		set = cloneTransactions(set);
		for(Transaction transaction: set) {
			transaction.setBlockHash(blockHash, blockIndex);
		}
		transactions = set;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (obj == null || !(obj instanceof BucketHash)) {
			return false;
		}
		BucketHash other = (BucketHash)obj;
		return key.equals(other.key) && hash.equals(other.hash) && previousBlockHash.equals(other.previousBlockHash);
	}

	@Override
	public int hashCode() {
		return (key + "=" + hash + "-" + previousBlockHash).hashCode();
	}

	public boolean hasChild(BucketHash child) {
		if(child.equals(left) || child.equals(right)) {
			return true;
		}
		return false;
	}
	
	public boolean hasBothChildren() {
		return left != null & right != null;
	}
	
	public synchronized boolean hasOnlyOneChild() {
		return (left != null & right == null) || (left == null & right != null);
	}

	public BucketHash getOtherChild(BucketHash child) {
		if(!hasChild(child)) {
			logger.error("{} does not contain child {}", toStringFull(), child.toStringFull());
			throw new RuntimeException("Does not contain this child");
		}
		if(child.equals(left)) {
			return right;
		}
		return left;
	}

	public Coin getFee() {
		return fee;
	}

	public void setFee(Coin fee) {
		this.fee = fee;
	}
	
	public synchronized Transaction getCoinbase() {
		
		if(transactions == null) {
			return null;
		}
		
		Set<Transaction> result = new HashSet<>();
		for(Transaction transaction: transactions) {
			if(transaction.isCoinbase()) {
				result.add(transaction);
			}
		}
		if(result.size() > 1) {
			logger.info("There are multiple coinbases {}", result);
			throw new RuntimeException("There are multiple coinbases");
		}
		if(!result.isEmpty()) {
			return result.iterator().next();
		}
		return null;
	}
	
	public synchronized boolean hasNullTransactions() {
		return transactions == null;
	}

	public synchronized void removeCoinbase() {
		Set<Transaction> set = transactions;
		if(set == null || set.isEmpty()) {
			return;
		}
		set = cloneTransactions(set);
		for(Iterator<Transaction> iterator = set.iterator(); iterator.hasNext();) {
			Transaction transaction = iterator.next();
			if(transaction.isCoinbase()) {
				iterator.remove();
			}
		}
		transactions = set;
	}

	public synchronized void setTransactionsToNull() {
		transactions = null;
	}

	public void cleanCoinbase(String coinbaseTransactionId) {
		Set<Transaction> set = transactions;
		if(set == null || set.isEmpty()) {
			return;
		}
		set = cloneTransactions(set);
		for(Iterator<Transaction> iterator = set.iterator(); iterator.hasNext();) {
			Transaction transaction = iterator.next();
			if(transaction.isCoinbase() && !transaction.getTransactionId().equals(coinbaseTransactionId)) {
				iterator.remove();
			}
		}
		transactions = set;
	}

	public void setPreviousBlockHash(String previousBlockHash) {
		this.previousBlockHash = previousBlockHash;
	}
	
	public Set<Transaction> getReceivedTx() {
		Set<Transaction> set = getAllTransactions();
		if(set == null) {
			return new HashSet<>();
		}
		for(Iterator<Transaction> iterator = set.iterator(); iterator.hasNext();) {
			Transaction transaction = iterator.next();
			if(!transaction.getReceiver().isMyKey(getBinaryStringKey())) {
				iterator.remove();
			}
		}
		return set;
	}

	public Set<Transaction> getReceivedTx(String bucketKey) {
		Set<Transaction> set = getAllTransactions();
		if(set == null) {
			return new HashSet<>();
		}
		for(Iterator<Transaction> iterator = set.iterator(); iterator.hasNext();) {
			Transaction transaction = iterator.next();
			if(!transaction.getReceiver().isMyKey(bucketKey)) {
				iterator.remove();
			}
		}
		return set;
	}
	
	public boolean isBranchValid() {
		Set<Transaction> transactions = getTransactions();
		if(transactions == null || transactions.isEmpty()) {
			return true;
		}
		Block block = Blockchain.getInstance().getByHash(getPreviousBlockHash());
		if(block == null) {
			return true; // have not received this block so can not validate yet
		}
		Set<Transaction> myBranchTransactions = block.filterBranchTransactions(transactions);
		if (!myBranchTransactions.containsAll(transactions)) {
			logger.info("****************************************************************");
			logger.info("Invalid transactions, not all transactions have inputs from the branch for block {}", block);
			logger.info("Invalid transactions bucketHash {} {}", block.getIndex(), toStringFull());
			return false;
		}
		return true;
	}

	public Object getRealHashCode() {
		return super.hashCode();
	}
	
	public long getBits() {
		return bits;
	}

	public synchronized void mine(long blockchainIndex, long newBits) {
		logger.trace("{} {} \t mine START buckethash={} isMined={}", blockchainIndex, getRealHashCode(), getKeyHash(), isMined());
		long timestamp = this.timestamp;
		int nonce = this.nonce;
		String miningHash = CryptoUtil.getHashBase58Encoded(getKeyHash() + timestamp + nonce);
		bits = Difficulty.convertDifficultyToBits(Difficulty.getDifficulty(newBits) / Math.pow(2, getPower()));
		if(Difficulty.checkProofOfWork(bits, miningHash)) {
			logger.trace("{} {} \t mine END   buckethash={} isMined={}", blockchainIndex, getRealHashCode(), getKeyHash(), isMined());
			return;
		}
		do {
			if(stop) {
				break;
			}
			long index = Blockchain.getInstance().getIndex();
			if(blockchainIndex < index) {
				break;
			}
			nonce++;
			if(nonce == Integer.MAX_VALUE) {
				nonce = 0;
				timestamp++;
			}
			miningHash = CryptoUtil.getHashBase58Encoded(getKeyHash() + timestamp + nonce);
		} while(!Difficulty.checkProofOfWork(bits, miningHash));
		if(!stop) {
			this.timestamp = timestamp;
			this.nonce = nonce;
		}
		logger.trace("{} {} \t mine END   buckethash={} isMined={}", blockchainIndex, getRealHashCode(), getKeyHash(), isMined());
	}
	
	public boolean isMined() {
		long bits = getBits();
		if(bits == 0) {
			return false;
		}
		String miningHash = CryptoUtil.getHashBase58Encoded(getKeyHash() + timestamp + nonce);
		return Difficulty.checkProofOfWork(bits, miningHash);
	}

	public void stopMining() {
		stop = true;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getNonce() {
		return nonce;
	}

	public void setNonce(int nonce) {
		this.nonce = nonce;
	}


}
