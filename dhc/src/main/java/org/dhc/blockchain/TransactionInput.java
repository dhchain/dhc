package org.dhc.blockchain;

import java.util.Set;

import org.dhc.network.ChainSync;
import org.dhc.persistence.BlockStore;
import org.dhc.util.Coin;
import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Registry;

public class TransactionInput {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private String outputId; // the id of the output for this input
	private DhcAddress sender;
	private Coin value;
	private String inputTransactionId;  //the id of the transaction containing this input
	private long inputBlockIndex;
	private String inputBlockHash;
	private long outputBlockIndex;
	private String outputBlockHash;
	private String outputTransactionId;  //the id of the transaction containing outputId as output
	
	public TransactionInput() {
		
	}
	
	public TransactionInput clone() {
		TransactionInput clone = new TransactionInput();
		clone.inputBlockHash = inputBlockHash;
		clone.inputBlockIndex = inputBlockIndex;
		clone.inputTransactionId = inputTransactionId;
		clone.outputBlockHash = outputBlockHash;
		clone.outputBlockIndex = outputBlockIndex;
		clone.outputId = outputId;
		clone.sender = sender;
		clone.value = value;
		clone.outputTransactionId = outputTransactionId;
		return clone;
	}
	
	public String getOutputId() {
		return outputId;
	}
	public void setOutputId(String outputId) {
		this.outputId = outputId;
	}
	public DhcAddress getSender() {
		return sender;
	}
	public void setSender(DhcAddress sender) {
		this.sender = sender;
	}
	public Coin getValue() {
		return value;
	}
	public void setValue(Coin value) {
		this.value = value;
	}
	public String getInputTransactionId() {
		return inputTransactionId;
	}
	public void setInputTransactionId(String inputTransactionId) {
		this.inputTransactionId = inputTransactionId;
	}
	public String getInputBlockHash() {
		return inputBlockHash;
	}
	public void setInputBlockHash(String inputBlockHash) {
		this.inputBlockHash = inputBlockHash;
	}
	
	public boolean equals(Object object) {
		if(object == null || ! (object instanceof TransactionInput)) {
			return false;
		}
		TransactionInput output = (TransactionInput)object;
		return getOutputId().equals(output.getOutputId());
	}

	public int hashCode() {
		return getOutputId().hashCode();
	}
	
	public String toString() {
		return "TransactionInput: {outputId: " + outputId + ", sender: " + sender + ", value: " + value + ", inputTransactionId: " + inputTransactionId + ", inputBlockHash: " + inputBlockHash
				+ ", inputBlockIndex: " + inputBlockIndex + ", outputBlockHash: " + outputBlockHash + ", outputBlockIndex: " + outputBlockIndex + ", outputTransactionId: " + outputTransactionId + "}";
	}
	public long getOutputBlockIndex() {
		return outputBlockIndex;
	}
	public void setOutputBlockIndex(long outputBlockIndex) {
		this.outputBlockIndex = outputBlockIndex;
	}
	public String getOutputBlockHash() {
		return outputBlockHash;
	}
	public void setOutputBlockHash(String outputBlockHash) {
		this.outputBlockHash = outputBlockHash;
	}

	public boolean wasUsedInBranch(long branchIndex, Set<String> blockhashes) {
		Set<TransactionInput> set = Blockchain.getInstance().getByOutputId(outputId);
		for(TransactionInput input: set) {
			if(input.getInputBlockIndex() < branchIndex || blockhashes.contains(input.getInputBlockHash())) {
				Set<Transaction> transactions = Blockchain.getInstance().getTransaction(input.getInputTransactionId());
				logger.trace("Found {} transaction for input transaction id {}", transactions.size(), input.getInputTransactionId());
				for (Transaction transaction : transactions) {
					logger.trace("Transaction {}", transaction);
				}
				logger.trace("Existing input that was already used {}", input);
				return true;
			}
		}
		return false;
	}
	
	public boolean hasOutput(Set<TransactionOutput> pendingOutputs) {
		
		long lastIndex = Math.max(Blockchain.getInstance().getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
		
		if(lastIndex - getOutputBlockIndex() > Constants.MAX_NUMBER_OF_BLOCKS) {
			return true;
		}
		
		long prunedIndex = Registry.getInstance().getCompactor().getPrunedIndex();
		if(getOutputBlockIndex() <= prunedIndex) {
			return true;
		}
		
		TransactionOutput output = TransactionOutputFinder.getByOutputId(getOutputId(), pendingOutputs);
		
		if(output == null) {
			if(!outputBlockHashExists()) {
				logger.info("Output blockhash does not exists for input: {}", this);
				return false;
			}
		}
		
		if (output == null) {
			logger.info("No output for {}", this);
			logger.info("missing transactionId {}", this.getOutputTransactionId());
			Block block = BlockStore.getInstance().getByBlockhash(getOutputBlockHash());
			if (block != null) {
				logger.info("Output block {}", block);
			}
			if (block.isPruned()) {
				logger.info("Output block is pruned, returning true because can not verify outputs in it");
				return true;
			}
			return false;
		}

		return true;
	}
	
	public boolean outputBlockHashExists() {
		if(getOutputBlockHash() == null) {
			return false;
		}
		return Blockchain.getInstance().contains(getOutputBlockHash());
	}

	public long getInputBlockIndex() {
		return inputBlockIndex;
	}

	public void setInputBlockIndex(long inputBlockIndex) {
		this.inputBlockIndex = inputBlockIndex;
	}

	public String getOutputTransactionId() {
		return outputTransactionId;
	}

	public void setOutputTransactionId(String outputTransactionId) {
		this.outputTransactionId = outputTransactionId;
	}

	public void findMissingOutput() {
		Registry.getInstance().getGetTransaction().send(this);
	}

	/**
	 * only finds duplicate input if there are no branches
	 * @return true if there is duplicate input and false otherwise
	 */
	public boolean inputAlreadyUsed() {
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		if(minCompeting != 0) {
			return false;
		}
		Set<TransactionInput> set = Blockchain.getInstance().getByOutputId(getOutputId());
		for(TransactionInput input: set) {
			Set<Transaction> transactions = Blockchain.getInstance().getTransaction(input.getInputTransactionId());
			logger.trace("Found {} transaction for input transaction id {}", transactions.size(), input.getInputTransactionId());
			for (Transaction transaction : transactions) {
				logger.trace("Transaction {}", transaction);
			}
			logger.trace("Existing input that was already used {}", input);
			return true;
		}
		
		return false;
	}

	
}
