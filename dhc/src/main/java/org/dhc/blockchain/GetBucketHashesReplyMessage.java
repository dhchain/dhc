package org.dhc.blockchain;

import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcRunnable;

public class GetBucketHashesReplyMessage extends Message {

	private BucketHashes bucketHashes;
	private String blockHash;

	public GetBucketHashesReplyMessage(String blockHash, BucketHashes bucketHashes) {
		this.blockHash = blockHash;
		this.bucketHashes = bucketHashes;
	}

	@Override
	public void process(Peer peer) {
		
		if(bucketHashes == null || !bucketHashes.isMine()) {
			return;
		}
		
		ThreadExecutor.getInstance().execute(new DhcRunnable(Thread.currentThread().getName()) {
			public void doRun() {
				RecoveringBlocks.getInstance().set(blockHash, bucketHashes);
			}
		});
		
	}

}
