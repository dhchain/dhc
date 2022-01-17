package org.dhc.blockchain;

import org.dhc.util.CryptoUtil;
import org.dhc.util.StringUtil;
import org.dhc.util.DhcLogger;

public class TransactionData {

	private static final DhcLogger logger = DhcLogger.getLogger();
	public static final int DATA_LENGTH = 32672;
	private static final int YEAR = 525600;
	
	private String transactionId;
	private String hash;
	private String data;
	private long validForNumberOfBlocks;
	private String blockHash;
	private long expirationIndex;
	
	public TransactionData(String data, long validForNumberOfBlocks) {
		setData(data);
		this.validForNumberOfBlocks = validForNumberOfBlocks < YEAR? validForNumberOfBlocks: YEAR;
		if(data != null) {
			hash = CryptoUtil.getHashBase58Encoded(getData());
		}
	}
	
	public TransactionData(String data) {
		this(data, YEAR);
	}
	
	public TransactionData clone() {
		TransactionData clone = new TransactionData(data, validForNumberOfBlocks);
		clone.transactionId = transactionId;
		clone.blockHash = blockHash;
		clone.expirationIndex = expirationIndex;
		return clone;
	}
	
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getData() {
		return StringUtil.substring(data, 0, DATA_LENGTH);
	}

	public long getValidForNumberOfBlocks() {
		return validForNumberOfBlocks;
	}

	public String toString() {
		return "{data='" +getData() + "', validForNumberOfBlocks=" + validForNumberOfBlocks + "}";
	}
	
	public boolean isValid() {
		
		if(getData() !=null && !getHash().equals(CryptoUtil.getHashBase58Encoded(getData()))) {
			logger.debug("Expiring data hash is not valid");
			return false;
		}
		
		return true;
	}

	public void setData(String data) {
		this.data = StringUtil.substring(data, 0, DATA_LENGTH);
	}

	public void setValidForNumberOfBlocks(long validForNumberOfBlocks) {
		this.validForNumberOfBlocks = validForNumberOfBlocks;
	}

	public String getBlockHash() {
		return blockHash;
	}

	public void setBlockHash(String blockHash) {
		this.blockHash = blockHash;
	}

	public long getExpirationIndex() {
		return expirationIndex;
	}

	public void setExpirationIndex(long expirationIndex) {
		this.expirationIndex = expirationIndex;
	}

	public void setExpirationIndex() {
		long expirationIndex = getExpirationIndex();
		expirationIndex = expirationIndex == 0? Blockchain.getInstance().getIndex() + getValidForNumberOfBlocks(): expirationIndex;
		setExpirationIndex(expirationIndex);		
	}
	
}
