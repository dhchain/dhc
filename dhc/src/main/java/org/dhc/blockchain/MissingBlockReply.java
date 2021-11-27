package org.dhc.blockchain;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class MissingBlockReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Block block;
	private String key;
	private DhcAddress originDhcAddress;

	public MissingBlockReply(Block block, String key, DhcAddress originDhcAddress) {
		logger.info("MissingBlockReply.init() originDhcAddress={} block key={} {}", originDhcAddress, key, block);
		this.block = block;
		this.key = key;
		this.originDhcAddress = originDhcAddress;
	}

	@Override
	public void process(Peer peer) {
		logger.info("MissingBlockReply.process(peer) originDhcAddress={} block key={} {}", originDhcAddress, key, block);
		if(Registry.getInstance().getBannedBlockhashes().contains(block.getBlockHash())) {
			return;
		}
		if (alreadySent(toString())) {
			return;
		}
		Network.getInstance().sendToAllPeers(this, peer);
		logger.info("MissingBlockReply found originDhcAddress={} block key={} {}", originDhcAddress, key, block);
		MissingBlocks.getInstance().removeMissingBlocks(block.getBlockHash(), key);
		MissingBlocks.getInstance().putFoundBlocks(block.getBlockHash(), key, block);
		Blockchain blockchain = Blockchain.getInstance();
		if(block.isMine()) {
			if(blockchain.add(block)) {
				
			}
		}
		
	}
	
	public String toString() {
		return String.format("MissingBlockReply-%s-%s", block.getBlockHash(), key);
	}

}
