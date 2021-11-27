package org.dhc.network.consensus;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class SendBucketHashMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private BucketHash bucketHash;
	private long index;

	public SendBucketHashMessage(BucketHash bucketHash, long index) {
		if(!bucketHash.isBranchValid()) {
			throw new RuntimeException();
		}
		this.bucketHash = Registry.getInstance().getBucketConsensuses().put(bucketHash, index).clone();
		this.index = index;
		if(bucketHash.hasOnlyOneChild()) {
			logger.info("bucketHash {}", bucketHash.toStringFull());
			logger.info("", new RuntimeException());
		}
	}

	@Override
	public void process(Peer peer) {
		logger.trace("START");
		if(index < Blockchain.getInstance().getIndex()) {
			return;
		}
		if(!bucketHash.isBranchValid()) {
			return;
		}
		logger.trace("Received SendBucketHashMessage bucketHash {} {}", index, bucketHash.toStringFull());
		
		if(bucketHash.hasOnlyOneChild()) {
			logger.info("bucketHash {}", bucketHash.toStringFull());
			logger.info("", new RuntimeException());
		}
		
		if(bucketHash != Registry.getInstance().getBucketConsensuses().put(bucketHash, index)) {
			return;
		}
		Network.getInstance().sendToKey(bucketHash.getBinaryStringKey(), this, peer);
		
		Block blockToMine = Consensus.getInstance().getBlockToMine();
		if(blockToMine != null && index < blockToMine.getIndex()) {
			return;
		}

		//should not hold lock on consensus in receiver thread, it will slow it down and potentially cause a deadlock with other threads
		String str = String.format("SendBucketHashMessage %s-%s", index, bucketHash.getPreviousBlockHash());
		ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
			public void doRun() {
				processReadyBucketHashes();
			}
		});
		
	}
	
	private void processReadyBucketHashes() {
		BucketHash bucketHash = this.bucketHash.clone();
		Consensus.getInstance().processReadyBucketHash(bucketHash, index);
		if(!"".equals(bucketHash.getBinaryStringKey())) {
			return;
		}
		//Can not hold receiver pool threads because processReadyBucketHashes will do send and expect reply back which would need to use these threads
		String str = String.format("Ready BHashes %s-%s %s", index, bucketHash, hashCode());
		logger.trace("SendBucketHashMessage {}", str);
		Consensus.getInstance().processReadyBucketHashes(bucketHash, index);
	}
	
	@Override
	public String toString() {
		return String.format("SendBucketHashMessage index %s bucketHash %s=%s %s", index, bucketHash.getKey(), bucketHash.getHash(), bucketHash.isHashForTransactionsValid());
	}


}
