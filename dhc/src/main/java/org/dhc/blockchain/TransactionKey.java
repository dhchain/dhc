package org.dhc.blockchain;

public class TransactionKey {
	
	private String transactionId;
	private String blockHash;
	
	public TransactionKey(String transactionId, String blockHash) {
		this.transactionId = transactionId;
		this.blockHash = blockHash;
	}

	@Override
	public int hashCode() {
		return (transactionId + blockHash).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof TransactionKey)) {
			return false;
		}
		TransactionKey other = (TransactionKey) obj;
		return (transactionId + blockHash).equals(other.transactionId + other.blockHash);
	}

	@Override
	public String toString() {
		return "TransactionKey [transactionId=" + transactionId + ", blockHash=" + blockHash + "]";
	}
	
	
	
}
