package org.dhc.network.consensus;

import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.DhcLogger;

public class GetBucketHash extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private BucketHash consensus;
	private long blockchainIndex;
	
	
	public GetBucketHash(BucketHash consensus, long blockchainIndex) {
		this.consensus = consensus;
		this.blockchainIndex = blockchainIndex;
	}

	@Override
	public void process(Peer peer) {
		BucketHash result = Registry.getInstance().getBucketConsensuses().get(consensus, blockchainIndex);
		logger.trace("GetBucketHash result={}", result);
		GetBucketHashReply message = new GetBucketHashReply(result, blockchainIndex);
		message.setCorrelationId(getCorrelationId());
		peer.send(message);
	}

}
