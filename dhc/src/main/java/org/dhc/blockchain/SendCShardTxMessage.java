package org.dhc.blockchain;

import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class SendCShardTxMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Transaction transaction;
	
	public SendCShardTxMessage() {

	}
	
	public SendCShardTxMessage(Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public void process(Peer peer) {
		
		if (alreadySent(toString())) {
			return;
		}
		
		if(!send()) {
			return;
		}

		if(Registry.getInstance().getBannedBlockhashes().contains(transaction.getBlockHash())) {
			return;
		}
		
		logger.trace("SendCShardTxMessage process() {} {}", transaction.getBlockHash(), transaction);
		
		Blockchain.getInstance().addPendingCrossShardTransaction(transaction);
	}

	public boolean send() {

		long lastIndex = Math.max(Blockchain.getInstance().getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
		
		if(!transaction.isValid()) {
			logger.info("{}-{} SendCShardTxMessage transaction not valid {}", Network.getInstance().getBucketKey(), lastIndex, transaction);
			return false;
		}
		
		if(!transaction.inputsOutputsValid()) {
			logger.info("{}-{} SendCShardTxMessage transaction not valid inputsOutputs {}", Network.getInstance().getBucketKey(), lastIndex, transaction);
			return false;
		}
		
		Network network = Network.getInstance();
		
		String bucketKey = network.getBucketKey(transaction.getReceiver());// It gets key of the bucket containing receiver address, 
		// so with each hop bucket gets smaller and eventually transaction will be delivered to the shard containing receiver
		network.sendToAddress(transaction.getReceiver(), this);
		logger.trace("{}-{} SendCShardTxMessage sent to bucketKey={} {}", Network.getInstance().getBucketKey(), lastIndex, bucketKey, transaction);
		
		return true;
	}
	
	public void send(Block block) {
		String str = String.format("Send SendCShardTxMessage %s-%s", block.getIndex(), block.getBlockHash());
		ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
			
			@Override
			public void doRun() {
				resend(block.getBlockHash());
				resend(Blockchain.getInstance().getAncestor(block, 10));
			}
		});
		
		
	}
	
	private void doSend(Block block) {
		for(Transaction transaction: block.getCrossShardTransactions()) {
			Transaction clone = transaction.clone();
			clone.setBlockHash(block.getBlockHash(), block.getIndex());
			
			new SendCShardTxMessage(clone).send();
		}
	}
	
	private void resend(String blockhash) {
		Block block = Blockchain.getInstance().getByHash(blockhash);
		if(block == null) {
			return;
		}
		doSend(block);
	}
	
	@Override
	public String toString() {
		String str = String.format("SendCShardTxMessage %s-%s", transaction.getBlockHash(), transaction.getTransactionId());
		return str;
	}

	@Override
	public void failedToSend(Peer peer, Exception e) {
		logger.trace("Failed to send SendCShardTxMessage {}", transaction);
		logger.trace("Failed to send to bucket {} to peer {}", Network.getInstance().getBucketKey(peer.getTAddress()), peer);
		new SendCShardTxMessage(transaction).send();
	}
	
	

}
