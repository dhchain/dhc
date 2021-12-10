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
		logger.trace("{} START ProposeMessage.process() key={}", index, bucketHash.getBinaryStringKey());
		if(ChainSync.getInstance().isRunning()) {
			return;
		}
		logger.trace("{} Propose received {}", index, bucketHash.toStringFull());
		if(bucketHash.hasOnlyOneChild()) {
			logger.info("bucketHash {}", bucketHash.toStringFull());
			logger.info("", new RuntimeException());
			return;
		}
		if(!bucketHash.isMined()) {
			logger.trace(
					"{} {} process() Not mined bucketHash.getKeyHash()={} bucketHash.isMined={}", index, bucketHash.getRealHashCode(),
					bucketHash.getKeyHash(), bucketHash.isMined());
			return;
		}
		
		
		
		//should not hold lock on consensus in receiver thread, it will slow it down and potentially cause a deadlock with other threads
		ThreadExecutor.getInstance().execute(new DhcRunnable("processPropose") {
			public void doRun() {
				Consensus.getInstance().processPropose(bucketHash, !reply, peer, index);
			}
		});

	}

	public void setReply(boolean reply) {
		this.reply = reply;
	}



}
