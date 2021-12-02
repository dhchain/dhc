package org.dhc.network.consensus;

import org.dhc.blockchain.Block;

public class MiningLoop {
	
	private Block block;

	public MiningLoop(Block block) {
		this.block = block;
	}

	public Block getBlock() {
		return block;
	}

}
