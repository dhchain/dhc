package org.dhc.network.consensus;

import org.dhc.network.ChainSync;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

/**
 * expects ProposeReplyMessage in return
 *
 */
public class ProposeMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private BucketHash bucketHash;
	private long index;
	private boolean reply;
	
	public ProposeMessage(BucketHash bucketHash, long index) {
		this.bucketHash = bucketHash;
		this.index = index;
		if(bucketHash.hasOnlyOneChild()) {
			logger.info("bucketHash {}", bucketHash.toStringFull());
			logger.info("", new RuntimeException());
		}
	}
	
	@Override
	public void process(Peer peer) {
		logger.trace("{} {} START ProposeMessage.process() key={}", index, bucketHash.isMined(), bucketHash.getBinaryStringKey());
		if(ChainSync.getInstance().isRunning()) {
			return;
		}
		logger.trace("{} {} Propose received {}", index, bucketHash.isMined(), bucketHash.toStringFull());
		if(bucketHash.hasOnlyOneChild()) {
			logger.info("bucketHash {}", bucketHash.toStringFull());
			logger.info("", new RuntimeException());
			return;
		}
		if(!bucketHash.isMined()) {
			logger.trace(
					"{} {} {} process() Not mined bucketHash.getKeyHash()={}", index, bucketHash.isMined(), bucketHash.getRealHashCode(),
					bucketHash.getKeyHash());
			return;
		}
		
		if(!bucketHash.areBitsValid()) {
			logger.info(
					"{} {} {} ProposeMessage.process() bits not valid bucketHash.getKeyHash()={}", index, bucketHash.isMined(), bucketHash.getRealHashCode(),
					bucketHash.getKeyHash());
			//return;
		}
		
		
		
		
		//should not hold lock on consensus in receiver thread, it will slow it down and potentially cause a deadlock with other threads
		String str = String.format("ProposeMessage@%s %s-%s '%s'='%s' reply=%s", hashCode(), index, bucketHash.getPreviousBlockHash().substring(0, 7), 
				bucketHash.getBinaryStringKey(), bucketHash.getHash().substring(0, Math.min(7, bucketHash.getHash().length())), reply);
		ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
			public void doRun() {
				Consensus.getInstance().processPropose(bucketHash, !reply, peer, index);
			}
		});

	}

	public void setReply(boolean reply) {
		this.reply = reply;
	}



}
