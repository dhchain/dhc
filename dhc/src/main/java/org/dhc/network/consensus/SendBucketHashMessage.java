package org.dhc.network.consensus;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;

public class SendBucketHashMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private BucketHash bucketHash;
	private long index;

	public SendBucketHashMessage(BucketHash bucketHash, long index) {
		if(!bucketHash.isBranchValid()) {
			throw new RuntimeException();
		}
		this.bucketHash = Registry.getInstance().getBucketConsensuses().put(bucketHash, index, false).clone();
		this.index = index;
		if(bucketHash.hasOnlyOneChild()) {
			logger.info("bucketHash {}", bucketHash.toStringFull());
			logger.info("", new RuntimeException());
		}
		
		if(!bucketHash.isMined()) {
			logger.info("{} init() Not mined bucketHash.getKeyHash()={} index={} bucketHash.getRealHashCode()={} bucketHash.isMined={}", index, 
					bucketHash.getKeyHash(), bucketHash.getRealHashCode(), bucketHash.isMined());
			logger.info("", new RuntimeException());
		}
		
		if(!bucketHash.areBitsValid()) {
			logger.info(
					"{} {} {} SendBucketHashMessage.init() bits not valid bucketHash.getKeyHash()={}", index, bucketHash.isMined(), bucketHash.getRealHashCode(),
					bucketHash.getKeyHash());
		}
	}

	@Override
	public void process(Peer peer) {
		logger.trace("{} {} START SendBucketHashMessage.process() key={}", index, bucketHash.isMined(), bucketHash.getBinaryStringKey());
		if(index < Blockchain.getInstance().getIndex()) {
			return;
		}
		if(!bucketHash.isBranchValid()) {
			return;
		}
		if(!bucketHash.isMined()) {
			logger.info("{} {} process() Not mined bucketHash.getKeyHash()={} bucketHash.getRealHashCode()={}", index, bucketHash.isMined(), 
					bucketHash.getKeyHash(), bucketHash.getRealHashCode());
			return;
		}
		
		if(!bucketHash.areBitsValid()) {
			logger.info(
					"{} {} {} SendBucketHashMessage.process() bits not valid bucketHash.getKeyHash()={}", index, bucketHash.isMined(), bucketHash.getRealHashCode(),
					bucketHash.getKeyHash());
			//return;
		}
		
		logger.trace("{} {} Received SendBucketHashMessage bucketHash {}", index, bucketHash.isMined(), bucketHash.toStringFull());
		
		if(bucketHash.hasOnlyOneChild()) {
			logger.info("bucketHash {}", bucketHash.toStringFull());
			logger.info("", new RuntimeException());
		}
		

		if(ChainSync.getInstance().isRunning()) {
			return;
		}
		//should not hold lock on consensus in receiver thread, it will slow it down and potentially cause a deadlock with other threads
		String str = String.format("SendBucketHashMessage@%s %s-%s '%s'='%s'", hashCode(), index, bucketHash.getPreviousBlockHash().substring(0, 7), 
				bucketHash.getBinaryStringKey(), bucketHash.getHash().substring(0, Math.min(7, bucketHash.getHash().length())));
		ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
			public void doRun() {
				processReadyBucketHashes(peer);
			}
		});
		
	}
	
	private void processReadyBucketHashes(Peer peer) {
		
		if(bucketHash != Registry.getInstance().getBucketConsensuses().put(bucketHash, index, false)) {
			return;
		}
		Network.getInstance().sendToKey(bucketHash.getBinaryStringKey(), this, peer);
		
		Block blockToMine = Consensus.getInstance().getBlockToMine();
		if(blockToMine != null && index < blockToMine.getIndex()) {
			return;
		}
		
		BucketHash bucketHash = this.bucketHash.clone();
		Consensus.getInstance().processReadyBucketHash(bucketHash, index);
		if(!"".equals(bucketHash.getBinaryStringKey())) {
			return;
		}
		//Can not hold receiver pool threads because processReadyBucketHashes will do send and expect reply back which would need to use these threads
		
		logger.trace("{} {} SendBucketHashMessage@{}.processReadyBucketHashes buckethash {}", index, bucketHash.isMined(), hashCode(), bucketHash.toStringFull());
		Consensus.getInstance().processReadyBucketHashes(bucketHash, index);
	}
	
	@Override
	public String toString() {
		return String.format("SendBucketHashMessage index %s bucketHash %s=%s %s", index, bucketHash.getKey(), bucketHash.getHash(), bucketHash.isHashForTransactionsValid());
	}


}
