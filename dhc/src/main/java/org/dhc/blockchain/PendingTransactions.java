package org.dhc.blockchain;

import java.util.HashSet;
import java.util.Set;

import org.dhc.network.Network;
import org.dhc.util.DhcLogger;

public class PendingTransactions {

	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();

	private final Set<Transaction> pendingTransactions = new HashSet<>();

	public synchronized void add(Transaction transaction) {
		pendingTransactions.add(transaction);
	}

	public void process(long index) {
		Set<Transaction> set = new HashSet<>();
		synchronized (this) {
			for (Transaction transaction : pendingTransactions) {
				if (transaction.getInputOutputBlockIndex() <= index) {
					set.add(transaction);
				}
			}
		}
		for (Transaction transaction : set) {
			addTransaction(transaction);
		}
		synchronized (this) {
			pendingTransactions.removeAll(set);
		}
	}

	private void addTransaction(Transaction transaction) {
		if (TransactionMemoryPool.getInstance().add(transaction)) {
			Network.getInstance().sendToAllMyPeers(new SendTransactionMessage(transaction));
		}
	}

	public void processTransaction(Transaction transaction) {
		if(transaction.getInputOutputBlockIndex() > Blockchain.getInstance().getIndex()) {
			add(transaction);
			return;
		}
		addTransaction(transaction);
		
	}

}
