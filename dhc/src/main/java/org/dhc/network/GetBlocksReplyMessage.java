package org.dhc.network;

import org.dhc.blockchain.Block;
import org.dhc.util.Message;

public class GetBlocksReplyMessage extends Message {
	
	private Block block;

	public GetBlocksReplyMessage(Block block) {
		this.block = block;
	}

	@Override
	public void process(Peer peer) {
		

	}

	public Block getBlock() {
		if(!block.isMined()) {
			return null;
		}
		return block;
	}

}
