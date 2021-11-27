package org.dhc.blockchain;

import java.util.List;

import org.dhc.network.ChainSync;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.DhcLogger;

public class SyncMessage extends Message {

	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private long index;
	
	public SyncMessage(long index) {
		this.index = index;
	}

	@Override
	public void process(Peer peer) {
		Blockchain blockchain = Blockchain.getInstance();
		
		String reason = null;
		
		List<Block> blocks = blockchain.getBlocks(index);
		if(blocks == null || blocks.isEmpty()) {
			if(ChainSync.getInstance().isRunning()) {
				reason = "This peer is still synchronizing";
			} else {
				reason = String.format("No blocks found for index=%s", index);
			}
		}
		
		peer.send(new SyncReplyMessage(blocks, index, blockchain.getIndex(), reason));

	}

}
