package org.dhc.blockchain;

import java.util.Set;

import org.dhc.persistence.TransactionOutputStore;

public class TransactionOutputFinder {
	
	public static TransactionOutput getByOutputId(String outputId, Set<TransactionOutput> pendingOutputs) {
		if(pendingOutputs != null) {
			for(TransactionOutput output: pendingOutputs) {
				if(outputId.equals(output.getOutputId())) {
					return output;
				}
			}
		}
		return TransactionOutputStore.getInstance().getByOutputId(outputId);
	}

}
