package org.dhc.network.consensus;

import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.DhcLogger;

public class GatherTransactionsReplyMessage extends Message {
	
	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();

	private BucketHash bucketHash;
	private long blockchainIndex;
	private BucketHash subBucketHash;
	
	public GatherTransactionsReplyMessage(BucketHash bucketHash, long blockchainIndex, BucketHash subBucketHash) {
		this.bucketHash = bucketHash;
		this.blockchainIndex = blockchainIndex;
		this.subBucketHash = subBucketHash;
	}

	@Override
	public void process(Peer peer) {
		GatherTransactions.getInstance().process(bucketHash, blockchainIndex, subBucketHash);
	}

}
