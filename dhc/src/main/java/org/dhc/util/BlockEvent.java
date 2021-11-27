package org.dhc.util;

import org.dhc.blockchain.Block;

public class BlockEvent implements Event {
	
	private Block block;
	private String message;

	
	public BlockEvent(Block block, String message) {
		this.block = block;
		this.message = message;
		
	}


	public Block getBlock() {
		return block;
	}


	public String getMessage() {
		return message;
	}
	
	

}
