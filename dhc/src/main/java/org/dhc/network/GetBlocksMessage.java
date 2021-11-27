package org.dhc.network;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.util.Message;
import org.dhc.util.DhcLogger;

public class GetBlocksMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private String key;
	private String blockHash;

	public GetBlocksMessage(String key, String blockHash) {
		this.key = key;
		this.blockHash = blockHash;
	}

	@Override
	public void process(Peer peer) {
	
		Block block = Blockchain.getInstance().getByHash(blockHash);
		if(block != null) {
			String blockKey = block.getBucketKey();
			logger.trace("requestKey: {}, blockKey: {}, blockHash: {}, block: {}, peer: {}", key, blockKey, blockHash, block, peer);
		}
		
		Message message  = new GetBlocksReplyMessage(block);
		message.setCorrelationId(getCorrelationId());
		peer.send(message);
	}


}
