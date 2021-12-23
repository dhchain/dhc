package org.dhc.blockchain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.dhc.network.Network;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.ExpiringMap;

public class TransactionMemoryPool {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final TransactionMemoryPool instance = new TransactionMemoryPool();
	
	public static TransactionMemoryPool getInstance() {
		return instance;
	}
	
	private Set<Transaction> transactions = Collections.synchronizedSet(new HashSet<>());
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private ExpiringMap<String, Transaction> dependantTransactions = new ExpiringMap<>();
	
	private TransactionMemoryPool() {
		
	}
	
	private boolean dependencyNeeded(Transaction transaction) {
		Set<TransactionOutput> outputs = getOutputs();
		for(TransactionInput input: transaction.getInputs()) {
			if(input.getOutputBlockHash() != null) {
				continue;
			}
			TransactionOutput output = TransactionOutputFinder.getByOutputId(input.getOutputId(), outputs);
			if(output == null) {
				dependantTransactions.put(input.getOutputId(), transaction);
				return true;
			}
		}
		return false;
	}
	
	public boolean add(Transaction transaction) {
		if(transactions.contains(transaction)) {
			return false;
		}
		Blockchain blockchain = Blockchain.getInstance();
		if (blockchain.contains(transaction)) {
			return false;
		}
		if(dependencyNeeded(transaction)) {
			return false;
		}
		if (!transaction.inputAlreadySpent(getOutputs())) {
			return false;
		}
		if (!transaction.isValid(getOutputs())) {
			return false;
		}		
		
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			if(transaction.isPruning()) { //pruning trx should have priority
				removeByInputs(transaction);
			}
			Set<TransactionInput> set = getInputs();
			set.retainAll(transaction.getInputs());
			if (!set.isEmpty()) {
				return false;
			}
			boolean result = transactions.add(transaction);
			if(result) {
				
				for(TransactionOutput output: transaction.getOutputs()) {
					Transaction dependantTransaction = dependantTransactions.remove(output.getOutputId());
					if(dependantTransaction != null) {
						add(dependantTransaction);
					}
				}
				
			}
			return result;
		} finally {
			writeLock.unlock();
		}
	}
	
	private void removeByInputs(Transaction transaction) {
		Set<Transaction> set = new HashSet<>(transactions);
		for(Transaction t: set) {
			Set<TransactionInput> inputs = new HashSet<>(t.getInputs());
			inputs.retainAll(transaction.getInputs());
			if(!inputs.isEmpty()) {
				transactions.remove(t);
			}
		}
	}
	
	public void add(Set<Transaction> transactions) {
		Network network = Network.getInstance();
		for(Transaction transaction: transactions) {
			boolean result = add(transaction);
			if(!result) {
				logger.info("Could not add transaction {}", transaction);
			} else {
				network.sendToAllMyPeers(new SendTransactionMessage(transaction));
			}
		}
	}
	
	public Set<Transaction> getTransactions() {
		Set<Transaction> set = getCopyOfTransactions();
		Set<Transaction> result = new HashSet<>(set);

		for (Transaction transaction : set) {
			if (!transaction.isValid(getOutputs())) {
				result.remove(transaction);
				remove(transaction);
			}
			if (!transaction.inputAlreadySpent(getOutputs())) {
				result.remove(transaction);
				remove(transaction);
			}
			if(!DhcAddress.getMyDhcAddress().isFromTheSameShard(transaction.getSenderDhcAddress(), Network.getInstance().getPower())) {
				result.remove(transaction);//when power increases a transaction might now have a sender from a different shard. We should remove such transaction from the memory pool.
				remove(transaction);
			}
			if (!transaction.hasOutputsForAllInputs(getOutputs())) {
				result.remove(transaction);
				remove(transaction);
			}
		}
		return result;

	}
	
	public Set<Transaction> getCopyOfTransactions() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			return new HashSet<>(transactions);
		} finally {
			readLock.unlock();
			//logger.trace("unlock");
		}
	}
	
	public void removeAll(Set<Transaction> transactions) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			this.transactions.removeAll(transactions);
			//logger.trace("Number of transactions left in memory pool: {}", transactions.size());
		} finally {
			writeLock.unlock();
		}
	}
	
	private Set<TransactionInput> getInputs() {
		Set<Transaction> set = getCopyOfTransactions();
		Set<TransactionInput> inputs = new HashSet<>();
		for (Transaction transaction : set) {
			inputs.addAll(transaction.getInputs());
		}
		return inputs;
	}
	
	private Set<TransactionOutput> getOutputs() {
		Set<Transaction> set = getCopyOfTransactions();
		Set<TransactionOutput> outputs = new HashSet<>();
		for (Transaction transaction : set) {
			outputs.addAll(transaction.getOutputs());
		}
		return outputs;
	}

	public void removeByOutputBlockhash(String outputBlockhash) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			Set<Transaction> set = new HashSet<>(transactions);
			for (Transaction transaction : set) {
				for(TransactionInput input: transaction.getInputs()) {
					if(outputBlockhash.equals(input.getOutputBlockHash())) {
						transactions.remove(transaction);
						break;
					}
				}
			}
		} finally {
			writeLock.unlock();
		}
		
	}
	
	public void remove(Transaction transaction) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			transactions.remove(transaction);
		} finally {
			writeLock.unlock();
		}
	}
	
	

}
