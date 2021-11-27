package org.dhc.blockchain;

import org.dhc.util.Coin;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;

public class TransactionOutput {

	private String outputId;
	private DhcAddress recipient;
	private Coin value;
	private String outputTransactionId; //the id of the transaction this output was created in
	private long outputBlockIndex;
	private String outputBlockHash;
	
	public TransactionOutput() {
		
	}
	
	public TransactionOutput(DhcAddress recipient, Coin value) {
		this.recipient = recipient;
		this.value = value;
		this.outputId = CryptoUtil.getRandomString(32);
	}
	
	public TransactionOutput clone() {
		TransactionOutput clone = new TransactionOutput();
		clone.outputBlockHash = outputBlockHash;
		clone.outputBlockIndex = outputBlockIndex;
		clone.outputId = outputId;
		clone.value = value;
		clone.recipient = recipient;
		clone.outputTransactionId = outputTransactionId;
		return clone;
	}
	
	public TransactionInput toInput() {
		TransactionInput input = new TransactionInput();
		
		input.setOutputId(outputId);
		input.setSender(recipient);
		input.setValue(value);
		input.setOutputBlockIndex(outputBlockIndex);
		input.setOutputBlockHash(outputBlockHash);
		input.setOutputTransactionId(outputTransactionId);
		
		return input;
	}

	public String getOutputId() {
		return outputId;
	}

	public Coin getValue() {
		return value;
	}

	public DhcAddress getRecipient() {
		return recipient;
	}

	public String getOutputTransactionId() {
		return outputTransactionId;
	}

	public void setValue(Coin value) {
		this.value = value;
	}

	public boolean equals(Object object) {
		if(object == null || ! (object instanceof TransactionOutput)) {
			return false;
		}
		TransactionOutput output = (TransactionOutput)object;
		return getOutputId().equals(output.getOutputId());
	}

	public int hashCode() {
		return getOutputId().hashCode();
	}
	
	public boolean isRecipientValid() {
		return recipient.isDhcAddressValid();
	}

	public void setOutputId(String outputId) {
		this.outputId = outputId;
	}

	public String toString() {
		return "TransactionOutput: {outputId: " + outputId + ", recipient: " + recipient + ", value: " + value + ", outputTransactionId: " + outputTransactionId + ", outputBlockHash: " + outputBlockHash + "}";
	}

	public String getOutputBlockHash() {
		return outputBlockHash;
	}

	public void setOutputBlockHash(String outputBlockHash) {
		this.outputBlockHash = outputBlockHash;
	}

	public void setOutputTransactionId(String outputTransactionId) {
		this.outputTransactionId = outputTransactionId;
	}

	public long getOutputBlockIndex() {
		return outputBlockIndex;
	}

	public void setOutputBlockIndex(long outputBlockIndex) {
		this.outputBlockIndex = outputBlockIndex;
	}

	public void setRecipient(DhcAddress recipient) {
		this.recipient = recipient;
	}
	
}
