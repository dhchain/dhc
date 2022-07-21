package org.dhc.blockchain;

import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class SendBlockMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private Block block;

	public SendBlockMessage(Block block) {
		this.block = block;
	}

	@SuppressWarnings("unused")
	@Override
	public void process(Peer peer) {
		logger.trace("START SendBlockMessage block {}", block);
		if (alreadySent(toString())) {
			return;
		}
		
		if(!block.isMined()) {
			logger.info("*********************************************************");
			logger.info("Block is not mined {}", block);
			return;
		}
		
		if(Registry.getInstance().getBannedBlockhashes().contains(block.getBlockHash())) {
			return;
		}
		
		logger.trace("Has coinbase: {}, block {}", block.getCoinbase(), block);
		
		Network.getInstance().sendToSomePeers(this, peer);
		//need to clone because RecoveringBlocks might change shard and we lose coinbase transaction when forwarding to a peer from the same shard as the miner
		Block block = this.block.clone();
		
		Blockchain blockchain = Blockchain.getInstance();
		if(blockchain.getIndex() + 1 < block.getIndex()) {
			blockchain.syncAsync();
		}
		
		int power = Network.getInstance().getPower();
		
		if(block.isMine() && power == block.getPower()) {
			String str = String.format("Saving block %s-%s-%s", block.getBucketKey(), block.getIndex(), block.getBlockHash().substring(0, 7));
			ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
				public void doRun() {
					if (blockchain.add(block)) {
						Network.getInstance().sendToKey(block.getBucketKey(), new SendMyBlockMessage(block), peer);
						new SendCShardTxMessage().send(block);
					}
				}
			});
			
			return;
		}
		
		if(ChainSync.getInstance().isRunning()) {
			return;
		}
		
		RecoveringBlocks.getInstance().run(block);

		new MissingBlock(block.getBlockHash(), block.getIndex(), block.getBits());

	}
	
	public String toString() {
		return String.format("SendBlockMessage %s-%s", block.getIndex(), block.getBlockHash());
	}

	@Override
	public void failedToSend(Peer peer, Exception e) {
		logger.trace("Failed to send SendBlockMessage {}", block);
		logger.trace("Failed to send to peer {}", peer);
		Network.getInstance().sendToSomePeers(this);
	}

}
