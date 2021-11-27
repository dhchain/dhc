package org.dhc.gui;

import org.dhc.blockchain.Block;
import org.dhc.util.BlockEvent;
import org.dhc.util.Event;
import org.dhc.util.EventListener;

public class BlockEventListener implements EventListener {
	
	private Main main;

	public BlockEventListener(Main main) {
		this.main = main;
	}

	@Override
	public void onEvent(Event event) {
		BlockEvent blockEvent = (BlockEvent)event;
		Block block = blockEvent.getBlock();
		main.getStatus().setText(blockEvent.getMessage() + " block " + block.getIndex());
	}

}
