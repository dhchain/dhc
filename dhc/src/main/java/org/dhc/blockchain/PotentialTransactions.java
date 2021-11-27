package org.dhc.blockchain;

import org.dhc.util.BoundedMap;
import org.dhc.util.DhcLogger;

public class PotentialTransactions {

	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private final BoundedMap<String, PotentialBlockTransactions> pendingTransactions =  new BoundedMap<>(10);

	public synchronized void put(Transaction transaction) {
		PotentialBlockTransactions potentialBlockTransactions = pendingTransactions.get(transaction.getBlockHash());
		if(potentialBlockTransactions == null) {
			potentialBlockTransactions = new PotentialBlockTransactions();
			potentialBlockTransactions.setBlockhash(transaction.getBlockHash());
			pendingTransactions.put(transaction.getBlockHash(), potentialBlockTransactions);
		}
		potentialBlockTransactions.add(transaction);
	}

	public void addPotentialTransactions(String blockhash) {
		PotentialBlockTransactions potentialBlockTransactions;
		synchronized(this) {
			potentialBlockTransactions = pendingTransactions.get(blockhash);
			Blockchain blockchain = Blockchain.getInstance();
			if(potentialBlockTransactions == null || blockchain == null || !blockchain.contains(blockhash)) {
				return;
			}
			pendingTransactions.remove(blockhash);
		}
		
		potentialBlockTransactions.addTransactions();
	}

}
