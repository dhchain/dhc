package org.dhc.network.consensus;

import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class GetBucketHashReply extends Message {
	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();

	private BucketHash bucketHash;
	private long blockchainIndex;

	public GetBucketHashReply(BucketHash bucketHash, long blockchainIndex) {
		this.bucketHash = bucketHash;
		this.blockchainIndex = blockchainIndex;
	}
	
	@Override
	public void process(Peer peer) {
		if(bucketHash == null) {
			return;
		}
		ThreadExecutor.getInstance().execute(new DhcRunnable("GetBucketHashReply") {
			public void doRun() {
				Registry.getInstance().getBucketConsensuses().put(bucketHash, blockchainIndex);
			}
		});
		
	}

	public BucketHash getBucketHash() {
		return bucketHash;
	}

}
