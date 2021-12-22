package org.dhc.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionInput;
import org.dhc.blockchain.TransactionOutput;

public class TransactionDependencySorter {
	
	private Map<String, Transaction> inputMap;
	private Map<String, Transaction> outputMap;
	private Set<Transaction> set;
	
	public TransactionDependencySorter(Set<Transaction> transactions) {
		inputMap = new HashMap<>();
		outputMap = new HashMap<>();
		for(Transaction transaction: transactions) {
			for(TransactionInput input: transaction.getInputs()) {
				inputMap.put(input.getOutputId(), transaction);
			}
			for(TransactionOutput output: transaction.getOutputs()) {
				outputMap.put(output.getOutputId(), transaction);
			}
		}
		set = new HashSet<>(transactions);
	}
	
	
	public Set<Transaction> sortByInputsOutputs() {
		Set<Transaction> result = new LinkedHashSet<>();
		List<Transaction> list = new ArrayList<>();
		while(!set.isEmpty()) {
			Transaction transaction = set.iterator().next();
			list.addAll(getList(transaction));
		}
		result.addAll(list);
		return result;
	}
	
	private List<Transaction> getList(Transaction transaction) {
		set.remove(transaction);
		for(TransactionInput input: transaction.getInputs()) {
			inputMap.remove(input.getOutputId());
		}
		for(TransactionOutput output: transaction.getOutputs()) {
			outputMap.remove(output.getOutputId());
		}
		List<Transaction> list = new ArrayList<>();
		list.add(transaction);
		for(TransactionInput input: transaction.getInputs()) {
			Transaction t = outputMap.remove(input.getOutputId());
			if(t != null) {
				list.addAll(0, getList(t));
			}
		}
		for(TransactionOutput output: transaction.getOutputs()) {
			Transaction t = inputMap.remove(output.getOutputId());
			if(t != null) {
				list.addAll(getList(t));
			}
		}
		
		return list;
	}

}
