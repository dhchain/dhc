package org.dhc.blockchain;

import org.dhc.network.ChainSync;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class SendSyncMyBlockMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private Block block;

	public SendSyncMyBlockMessage(Block block) {
		this.block = block;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("START SendSyncMyBlockMessage block {}", block);
		if (alreadySent(toString())) {
			return;
		}
		
		if(!block.isMine()) {
			return;
		}
		
		if(!block.isMined()) {
			return;
		}

		Blockchain blockchain = Blockchain.getInstance();
		
		long lastIndex = Math.max(blockchain.getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
		if(block.getIndex() + 1 != lastIndex) {
			return;
		}
		
		Block myBlock = blockchain.getByHash(block.getBlockHash());
		if(myBlock == null || myBlock.getReceivedTxHash().equals(block.getReceivedTxHash(myBlock.getBucketKey()))) {
			return;
		}

		String str = String.format("Sync my block %s-%s-%s", block.getIndex(), block.getBucketKey(), block.getBlockHash().substring(0, 7));
		ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
			public void doRun() {
				if(blockchain.saveTransactions(block.getReceivedTx(myBlock.getBucketKey()))) {
					logger.info("Sync transactions for previous block");
				}
			}
		});

	}
	
	public String toString() {
		return String.format("SendSyncMyBlockMessage %s-%s-%s-%s", block.getIndex(), block.getBucketKey(), block.getBlockHash(), block.getReceivedTxHash());
	}

}
