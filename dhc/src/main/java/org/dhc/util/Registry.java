package org.dhc.util;

import org.dhc.blockchain.BannedBlockhashes;
import org.dhc.blockchain.Compactor;
import org.dhc.blockchain.GetTransaction;
import org.dhc.blockchain.Miner;
import org.dhc.blockchain.MissingOutputs;
import org.dhc.blockchain.MissingOutputsForTransaction;
import org.dhc.blockchain.PendingCrossShardTransactions;
import org.dhc.blockchain.PendingTransactions;
import org.dhc.blockchain.PotentialTransactions;
import org.dhc.network.consensus.BucketConsensuses;

public class Registry {
	
	private static Registry instance = new Registry();
	
	public static Registry getInstance() {
		return instance;
	}
	
	private Registry() {
		
	}
	
	private final MissingOutputs missingOutputs = new MissingOutputs();

	public MissingOutputs getMissingOutputs() {
		return missingOutputs;
	}
	
	private final PendingCrossShardTransactions pendingCrossShardTransactions = new PendingCrossShardTransactions();

	public PendingCrossShardTransactions getPendingCrossShardTransactions() {
		return pendingCrossShardTransactions;
	}
	
	private final PotentialTransactions potentialTransactions = new PotentialTransactions();

	public PotentialTransactions getPotentialTransactions() {
		return potentialTransactions;
	}
	
	private final PendingTransactions pendingTransactions = new PendingTransactions();

	public PendingTransactions getPendingTransactions() {
		return pendingTransactions;
	}
	
	private final MissingOutputsForTransaction missingOutputsForTransaction = new MissingOutputsForTransaction();

	public MissingOutputsForTransaction getMissingOutputsForTransaction() {
		return missingOutputsForTransaction;
	}
	
	private final BucketConsensuses bucketConsensuses = new BucketConsensuses();

	public BucketConsensuses getBucketConsensuses() {
		return bucketConsensuses;
	}
	
	private final BannedBlockhashes bannedBlockhashes = new BannedBlockhashes();

	public BannedBlockhashes getBannedBlockhashes() {
		return bannedBlockhashes;
	}
	
	private final Compactor compactor = new Compactor();

	public Compactor getCompactor() {
		return compactor;
	}
	
	private final TotalBalance totalBalance = new TotalBalance();

	public TotalBalance getTotalBalance() {
		return totalBalance;
	}
	
	private final GetTransaction getTransaction = new GetTransaction();

	public GetTransaction getGetTransaction() {
		return getTransaction;
	}

	private Miner miner;

	public synchronized Miner getMiner() {
		if(miner == null) {
			miner = new Miner();
		}
		return miner;
	}
	
	private final ThinPeerNotifier thinPeerNotifier = new ThinPeerNotifier();

	public ThinPeerNotifier getThinPeerNotifier() {
		return thinPeerNotifier;
	}
	

}
