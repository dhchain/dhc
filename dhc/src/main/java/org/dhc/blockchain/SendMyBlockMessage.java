package org.dhc.blockchain;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class SendMyBlockMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private Block block;

	public SendMyBlockMessage(Block block) {
		this.block = block;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("START SendMyBlockMessage block {}", block);
		if (alreadySent(toString())) {
			return;
		}
		
		if(!block.isMine()) {
			return;
		}
		
		if(Registry.getInstance().getBannedBlockhashes().contains(block.getBlockHash())) {
			return;
		}
		
		logger.trace("Has coinbase: {}, block {}", block.getCoinbase(), block);

		Blockchain blockchain = Blockchain.getInstance();
		
		if(blockchain.getIndex() + 1 < block.getIndex()) {
			blockchain.syncAsync();
		}
		
		int power = Network.getInstance().getPower();
		if(power != block.getPower()) {
			return;
		}

		String str = String.format("Saving my block %s-%s-%s", block.getBucketKey(), block.getIndex(), block.getBlockHash().substring(0, 7));
		ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
			public void doRun() {
				if (blockchain.add(block)) {
					logger.trace("SendMyBlockMessage saved block");
					Network.getInstance().sendToKey(block.getBucketKey(), new SendMyBlockMessage(block), peer);
					new SendCShardTxMessage().send(block);
				}
			}
		});

	}
	
	public String toString() {
		return String.format("SendMyBlockMessage %s-%s-%s", block.getIndex(), block.getBucketKey(), block.getBlockHash());
	}

}
