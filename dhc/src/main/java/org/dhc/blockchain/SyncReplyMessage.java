package org.dhc.blockchain;

import java.util.List;

import org.dhc.network.ChainSync;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class SyncReplyMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private List<Block> blocks;
	private long blockchainIndex;
	private long lastBlockchainIndex;

	private String reason;
	
	public SyncReplyMessage(List<Block> blocks, long blockchainIndex, long lastBlockchainIndex, String reason) {
		this.blocks = blocks;
		this.blockchainIndex = blockchainIndex;
		this.lastBlockchainIndex = lastBlockchainIndex;
		this.reason = reason;
	}

	@Override
	public void process(Peer peer) {

		ChainSync synchronizer = ChainSync.getInstance();
		synchronizer.setLastBlockchainIndex(lastBlockchainIndex);
		synchronizer.refreshMyPeers(peer);
		
		if(blocks == null || blocks.isEmpty()) {
			if(blockchainIndex > synchronizer.getLastBlockchainIndex()) {
				synchronizer.unRegister(blockchainIndex);
			}
			
			synchronizer.incNotify(peer, reason);
			return;
		}
		
		if(!myBlocks(blocks)) {
			// We don't want to unregister here so it will timeout and other peers will try to get blocks for blockchainIndex
			String blocksKey = blocks.get(0).getBucketKey();
			logger.info("Not my blocks {}", blocks.get(0));
			synchronizer.incNotify(peer, String.format("Not my blocks blocksKey=%s, peersKey=%s, myKey=%s", blocksKey, peer.getTAddress().getBinary(blocksKey.length()), DhcAddress.getMyDhcAddress().getBinary(blocksKey.length())) );
			return;
		}
		
		for(Block block: blocks) {
			if(!block.isValid()) {
				logger.info("*********************************************************");
				logger.info("Block is invalid {}", block);
				synchronizer.incNotify(peer, String.format("Block is not valid: %s", block ));
				return;
			}
			if(!block.isMined()) {
				logger.info("*********************************************************");
				logger.info("Block is not mined {}", block);
				return;
			}
		}

		synchronizer.unRegister(blockchainIndex);

		String str = String.format("SyncReplyMessage.doIt() blockchainIndex=%s", blockchainIndex);
		ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
			public void doRun() {
				doIt(peer);
			}
		});
	}
	
	private void doIt(Peer peer) {
		ChainSync synchronizer = ChainSync.getInstance();
		Blockchain blockchain = Blockchain.getInstance();

		for(Block block: blocks) {
			blockchain.add(block);
		}

		if(blockchain.getIndex() + 1 >= blockchainIndex && !blockchain.contains(blocks.iterator().next().getBlockHash())) {
			peer.send(new SyncMessage(blockchainIndex - 1));
			return;
		}
		
		while(blockchain.getNumberOfPendingBlocks() > 1000) {
			ThreadExecutor.sleep(10000);
		}
		
		peer.send(new SyncMessage(synchronizer.next(peer)));
		
	}

	private boolean myBlocks(List<Block> blocks) {
		if(blocks == null) {
			return true;
		}
		for(Block block: blocks) {
			if(!block.isMine()) {
				return false;
			}
		}
		return true;
	}

}
