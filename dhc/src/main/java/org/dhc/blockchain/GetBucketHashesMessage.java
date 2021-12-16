package org.dhc.blockchain;

import org.dhc.network.Peer;
import org.dhc.util.Message;

public class GetBucketHashesMessage extends Message {

	private String blockHash;

	public GetBucketHashesMessage(String blockHash) {
		this.blockHash = blockHash;
	}

	@Override
	public void process(Peer peer) {
		Blockchain blockchain = Blockchain.getInstance();
		Block block = blockchain.getByHash(blockHash);
		if(block == null) {
			return;
		}
		if(block != null && !block.isHis(peer.getTAddress())) {
			return;
		}
		GetBucketHashesReplyMessage message = new GetBucketHashesReplyMessage(blockHash, block.getBucketHashes());
		peer.send(message);
	}

}
