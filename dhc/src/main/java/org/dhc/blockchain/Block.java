package org.dhc.blockchain;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dhc.network.ChainSync;
import org.dhc.network.consensus.BlockchainIndexStaleException;
import org.dhc.network.consensus.BucketHash;
import org.dhc.persistence.BlockStore;
import org.dhc.util.Base58;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Difficulty;
import org.dhc.util.Wallet;

public class Block {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private String blockHash;
	private String previousHash;
	private BucketHashes bucketHashes;
	private PublicKey miner;
	private long index;
	private transient Long receivedTime;
	private String coinbaseTransactionId = "";
	private String minerSignature;
	private String consensus;
	private long timeStamp;
	private int nonce;
	private long bits;
	
	public void prune() {
		bucketHashes = null;
		miner = null;
		receivedTime = null;
		coinbaseTransactionId = null;
		minerSignature = null;
	}
	
	public Block() {
		
	}

	public Block clone() {
		Block clone = new Block();

		clone.blockHash = blockHash;
		clone.previousHash = previousHash;
		if(!isPruned()) {
			clone.bucketHashes = bucketHashes.clone();
		}
		clone.miner = miner;
		clone.index = index;
		clone.receivedTime = receivedTime;
		clone.coinbaseTransactionId = coinbaseTransactionId;
		clone.minerSignature = minerSignature;
		clone.consensus = consensus;
		clone.timeStamp = timeStamp;
		clone.nonce = nonce;
		clone.bits = bits;
		return clone;
	}

	public String getBlockHash() {
		return blockHash;
	}

	public void setBlockHash() {
		if (isGenesis()) {
			getBucketHashes().getLastBucketHash().setBlockHash(blockHash, getIndex());
			return;
		}
		if(isPruned()) {
			return;
		}
		//blockHash = calculateHash();
		getBucketHashes().getLastBucketHash().setBlockHash(blockHash, getIndex());
	}

	public void setBlockHash(String blockHash) {
		this.blockHash = blockHash;
	}

	public String getPreviousHash() {
		return previousHash;
	}

	public void setPreviousHash(String previousHash) {
		this.previousHash = previousHash;
	}

	public String getConsensus() {
		if(consensus == null) {
			consensus = bucketHashes.getConsensus();
		}
		return consensus;
	}
	
	public void setConsensus(String consensus) {
		this.consensus = consensus;
	}

	public DhcAddress getDhcAddress() {
		if(isPruned()) {
			return null;
		}
		return CryptoUtil.getDhcAddressFromKey(miner);
	}

	public long getIndex() {
		return index;
	}

	public void setIndex(long index) {
		this.index = index;
	}

	@Override
	public String toString() {
		try {
			long lastIndex = 0;
			if (!isGenesis()) {
				lastIndex = Math.max(Blockchain.getInstance().getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
			}
			BucketHashes hashes = getBucketHashes();

			return getBucketKey() + "-" + index + (index == lastIndex ? "" : "/" + lastIndex) + " power=" + getPower() + " " + Wallet.getInstance().getDhcAddress().getBinary(6)
					+ " hash=" + blockHash + " previous="
					+ previousHash + " miner=" + getDhcAddress() + " " + hashes
					+ ", fee=" + (hashes == null ? "": hashes.getFirstBucketHash().getFee().toNumberOfCoins())
					+ ", nonce=" + nonce + ", bits=" + Long.toString(getBits(), 16) + ", isMined=" + isMined();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	public BucketHashes getBucketHashes() {
		return bucketHashes;
	}

	public void setBucketHashes(BucketHashes bucketHashes) {
		this.bucketHashes = bucketHashes;
	}

	public boolean isBranchValid() {
		if(isPruned()) {
			return true;
		}

		Set<Transaction> allTransactions = getAllTransactions();

		if (allTransactions == null) {
			return true;
		}

		Blockchain blockchain = Blockchain.getInstance();// blockchain might return null if not initialized yet, so check if it is not
															// null
		if (blockchain == null) {
			return true;
		}

		Block block = BlockStore.getInstance().getByBlockhash(getPreviousHash());
		if(block == null) {
			return false;
		}
		Set<Transaction> myBranchTransactions = block.filterBranchTransactions(allTransactions);
		if (!myBranchTransactions.containsAll(allTransactions)) {
			logger.info("****************************************************************");
			logger.info("Invalid block, not all transactions have inputs from the branch {}", this);
			allTransactions.removeAll(myBranchTransactions);
			new InvalidTransactions(this, allTransactions).process();
			return false;
		}

		return true;
	}

	private String calculateHash() {
		return CryptoUtil.getHashBase58Encoded(getPreHash() + minerSignature);
	}
	
	public void sign() {
		Signature dsa = CryptoUtil.getSignature(Wallet.getInstance().getPrivateKey());
		String data = getPreHash();
		
		try {
			dsa.update(data.getBytes(StandardCharsets.UTF_8));
			byte[] signature = dsa.sign();
			minerSignature = Base58.encode(signature);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public boolean verifySignature() {
		String data = getPreHash();
		boolean result = CryptoUtil.verifyECDSASig(miner, data, Base58.decode(minerSignature));
		if(result) {
			return true;
		}
		return false;
	}
	
	private String getPreHash() {
		return getPreviousHash() + getConsensus() + getCoinbaseTransactionId() + getBits() + getTimeStamp() + getNonce();
	}

	public boolean isValid() {
		
		if(isPruned() && blockHash.equals(calculateHash())) {
			return true;
		}

		if (!bucketHashes.isValid()) {
			return false;
		}
		
		if (!verifySignature()) {
			logger.error("Block signature is not valid");
			return false;
		}
		
		if (!blockHash.equals(calculateHash())) {
			return false;
		}

		Set<Transaction> allTransactions = getAllTransactions();

		if (allTransactions == null) {
			return true;
		}

		for (Transaction transaction : allTransactions) {
			if (!blockHash.equals(transaction.getBlockHash())) {
				logger.info("****************************************************************");
				logger.info("block.isValid()=false, blockHash={}, transaction.getBlockHash()={}", blockHash, transaction.getBlockHash());
				logger.info("Transaction {}", transaction);
				return false;
			}
			if(!isGenesis() && !transaction.inputsOutputsValid()) {
				logger.error("Inputs Outputs are not valid for transaction {}", transaction);
				return false;
			}
		}
		
		if(!isCoinBaseValid()) {
			logger.error("Coinbase is not valid");
			return false;
		}

		return true;
	}
	
	private boolean isCoinBaseValid() {
		if(isGenesis()) {
			return true;
		}
		
		if(!getDhcAddress().isFromTheSameShard(DhcAddress.getMyDhcAddress(), getPower())) {
			return true;
		}
		
		Transaction coinbase = getCoinbase();
		
		if(coinbase == null) {
			return false;
		}
		
		if(!coinbase.getTransactionId().equals(getCoinbaseTransactionId())) {
			return false;
		}

		return true;
	}

	public int getPower() {
		if(isPruned() || bucketHashes == null) {
			return 0;
		}
		return bucketHashes.getPower();
	}

	public long getShard() {
		return bucketHashes.getShard();
	}

	public String getShardOutOf() {
		return bucketHashes.getShardOutOf();
	}

	public boolean isMine() {
		if(isPruned()) {
			return true;
		}
		return bucketHashes.isMine();
	}

	public boolean isHis(DhcAddress dhcAddress) {
		return bucketHashes.isHis(dhcAddress);
	}
	
	public boolean isMineBucketKeyTransaction(Transaction transaction) {
		if(!isHis(transaction.getReceiver())) {
			return false;
		}
		if(!isHis(transaction.getSenderDhcAddress())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return blockHash.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Block)) {
			return false;
		}
		Block other = (Block) obj;
		return blockHash.equals(other.blockHash);
	}

	public String getBucketKey() {
		if(isPruned()) {
			return "";
		}
		BucketHashes bucketHashes = getBucketHashes();
		if(bucketHashes == null) {
			return "";
		}
		return bucketHashes.getLastBucketHash().getBinaryStringKey();
	}

	public Block combine(Block otherBlock) {
		Set<Transaction> set = getAllTransactions();
		logger.trace("Displaying all transactions for block {}", this);
		displayTransactions(set);
		Set<Transaction> otherTransactions = otherBlock.getAllTransactions();
		logger.trace("Displaying all transactions for block {}", otherBlock);
		displayTransactions(otherTransactions);
		if(set != null) {
			if(otherTransactions != null) {
				set.addAll(otherTransactions);
			}
		} else {
			set = otherTransactions;
		}
		Block block = clone();
		block.getBucketHashes().trim(getPower() - 1);
		block.getBucketHashes().getLastBucketHash().setAllTransactions(set);
		block.cleanCoinbase();
		return block;
	}
	
	private void displayTransactions(Collection<Transaction> transactions) {
		if(transactions == null) {
			return;
		}
		for(Transaction transaction: transactions) {
			logger.trace("Transaction {}", transaction);
		}
	}

	public void cleanCoinbase() {
		if(isPruned()) {
			return;
		}
		getBucketHashes().getLastBucketHash().cleanCoinbase(getCoinbaseTransactionId());
	}
	
	public Set<Transaction> getTransactions() {
		return bucketHashes.getTransactions();
	}

	public Set<Transaction> getAllTransactions() {
		if(isPruned()) {
			return null;
		}
		return bucketHashes.getAllTransactions();
	}
	
	public Set<Transaction> getReceivedTx() {
		if(isPruned()) {
			return null;
		}
		return bucketHashes.getReceivedTx();
	}
	
	public Set<Transaction> getReceivedTx(String bucketKey) {
		if(isPruned()) {
			return null;
		}
		return bucketHashes.getReceivedTx(bucketKey);
	}

	public int getAveragePower() {
		return getBucketHashes().getAveragePower();
	}

	public Set<Transaction> getCrossShardTransactions() {
		Set<Transaction> set = getTransactions();
		Set<Transaction> result = new HashSet<>();
		if (set == null) {
			return result;
		}

		for (Transaction transaction : set) {
			if (transaction.isCrossShard()) {
				result.add(transaction);
			}
		}
		return result;
	}
	
	public Set<Transaction> getNonHashedTransactions() {
		Set<Transaction> result = getAllTransactions();
		if(result == null) {
			return null;
		}
		Set<Transaction> transactions = getTransactions();
		if(transactions != null) {
			result.removeAll(transactions);
		}
		
		return result;
	}

	public void setMiner(PublicKey miner) {
		this.miner = miner;
	}

	public PublicKey getMiner() {
		return miner;
	}

	public Long getReceivedTime() {
		if(miner == null) {
			return null;
		}
		if(receivedTime == null) {
			receivedTime = System.currentTimeMillis();
		}
		return receivedTime;
	}

	public void setReceivedTime(long receivedTime) {
		this.receivedTime = receivedTime;
	}

	public boolean isGenesis() {
		return index == 0;
	}

	public Set<Transaction> filterBranchTransactions(Set<Transaction> transactions) {

		Set<Transaction> result = new HashSet<>();
		Set<String> blockhashes = Blockchain.getInstance().getBranchBlockhashes(this);
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		long branchIndex = minCompeting == 0 || minCompeting > getIndex() ? getIndex() : minCompeting;
		for (Transaction transaction : transactions) {
			boolean inputsValid = true;
			for (TransactionInput input : transaction.getInputs()) {
				// this checks that all transactions inputs come from the branch, no other
				// branches
				// but there is a possibility that some inputs already used in the branch
				if (input.getOutputBlockIndex() >= branchIndex && !blockhashes.contains(input.getOutputBlockHash())) {
					logger.trace("Input does not come from the branch, input{} transaction {} this block {}", input, transaction, this);
					logger.trace("branchIndex = {}, blockhashes = {}", branchIndex, blockhashes);
					logger.trace("Output for the input comes from block {}-{}, it is not from the same branch as this block {}-{}", input.getOutputBlockIndex(), input.getOutputBlockHash(), getIndex(), getBlockHash());
					inputsValid = false;
					break;
				}
				if (input.wasUsedInBranch(branchIndex, blockhashes)) {
					logger.trace("Input was already used in the branch {}, transaction {}, this block {}", input, transaction, this);
					inputsValid = false;
					break;
				}
			}
			if (inputsValid) {
				result.add(transaction);
			}
		}

		return result;

	}

	public void addTransaction(Transaction coinbase) {
		bucketHashes.addTransaction(coinbase);
	}
	
	/**
	 * Not all blocks will have coinbase transaction. Only if the miner of the block is in the same shard with you.
	 * 
	 * @return coinbase transaction for this block
	 */
	public Transaction getCoinbase() {
		if(bucketHashes == null) {
			return null;
		}
		return bucketHashes.getCoinbase();
	}

	public String getCoinbaseTransactionId() {
		if(miner == null) {
			return null;
		}
		return coinbaseTransactionId;
	}

	public void setCoinbaseTransactionId(String coinbaseTransactionId) {
		this.coinbaseTransactionId = coinbaseTransactionId;
	}

	public void removeCoinbase() {
		bucketHashes.removeCoinbase();
	}
	
	public boolean hasOutputsForAllInputs() {
		Set<Transaction>  set = getAllTransactions();
		if(set == null) {
			return true;
		}
		for(Transaction transaction: set) {
			if(!transaction.hasOutputsForAllInputs(this)) {
				logger.info("block {}", this);
				return false;
			}
		}
		return true;
	}
	
	public String getAllTxHash() {
		return Transaction.getMerkleRoot(new ArrayList<>(getAllTransactions()));
	}
	
	public String getPreviousAllTxHash() {
		Block previousBlock  = Blockchain.getInstance().getByHash(getPreviousHash());
		if(previousBlock == null) {
			return "previousBlock == null for hash " + getPreviousHash();
		}
		return previousBlock.getAllTxHash();
	}

	public String getMinerSignature() {
		if(miner == null) {
			return null;
		}
		return minerSignature;
	}

	public void setMinerSignature(String minerSignature) {
		this.minerSignature = minerSignature;
	}

	public void computeFullMerklePath() {
		Set<Transaction> transactions = getTransactions();
		for(Transaction transaction: transactions) {
			transaction.computeFullMerklePath(this);
		}
	}

	public boolean isPruned() {
		return miner == null;
	}
	
	public String getReceivedTxHash() {
		return Transaction.getMerkleRoot(new ArrayList<>(getReceivedTx()));
	}
	
	public String getReceivedTxHash(String bucketKey) {
		return Transaction.getMerkleRoot(new ArrayList<>(getReceivedTx(bucketKey)));
	}

	/**
	 * This method is safe to be called even if block if from other shard
	 * @return true is block last buckethash has correct hash for its transactions
	 */
	public boolean isValidHashForTransactions() {
		BucketHashes bucketHashes = getBucketHashes();
		if(bucketHashes == null) {
			return true;
		}
		BucketHash lastHash = bucketHashes.getLastBucketHash();
		if(lastHash == null) {
			return true;
		}
		if(!lastHash.isHashForTransactionsValid()) {
			logger.info("Hash {} for {} transactions is not valid", lastHash.getHash(), lastHash.getNumberOfTransactions());
			Set<Transaction> transactions = lastHash.getTransactions();
			if(transactions != null) {
				for(Transaction transaction: transactions) {
					logger.info("\ttransaction {}", transaction);
				}
			}
			return false;
		}
		return true;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public int getNonce() {
		return nonce;
	}

	public void setNonce(int nonce) {
		this.nonce = nonce;
	}

	public long getBits() {
		return bits;
	}

	public void setBits(long bits) {
		this.bits = bits;
	}

	public void mine() {
		do {
			if(!isGenesis()) {
				long index = Blockchain.getInstance().getIndex();
				if (getIndex() != index + 1) {
					throw new BlockchainIndexStaleException("Blockchain index is stale");
				}
			}
			nonce++;
			if(nonce == Integer.MAX_VALUE) {
				nonce = 0;
				timeStamp++;
			}
			sign();
			setBlockHash(calculateHash());
		} while(!isMined());
		
	}
	
	public boolean isMined() {
		if(getBits() == 0 || getBlockHash() == null) {
			return false;
		}
		return Difficulty.checkProofOfWork(getBits(), getBlockHash());
	}
	

}
