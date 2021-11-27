package org.dhc.network.consensus;

import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.DhcLogger;

public class GatherTransactionsMessage extends Message {
	
	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();

	private BucketHash bucketHash;
	private long blockchainIndex;
	private BucketHash subBucketHash;
	
	
	public GatherTransactionsMessage(BucketHash bucketHash, long blockchainIndex, BucketHash subBucketHash) {
		this.bucketHash = bucketHash;
		this.blockchainIndex = blockchainIndex;
		this.subBucketHash = subBucketHash;
	}

	@Override
	public void process(Peer peer) {
		BucketHash result = Registry.getInstance().getBucketConsensuses().get(subBucketHash, blockchainIndex);
		GatherTransactionsReplyMessage message = new GatherTransactionsReplyMessage(bucketHash, blockchainIndex, result);
		peer.send(message);
	}

}
