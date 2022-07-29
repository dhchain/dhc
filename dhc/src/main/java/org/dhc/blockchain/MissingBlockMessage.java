package org.dhc.blockchain;

import java.math.BigInteger;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Constants;
import org.dhc.util.CryptoUtil;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Difficulty;

public class MissingBlockMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private String blockHash;
	private String key;
	private long index;
	private DhcAddress originDhcAddress;
	private long bits;
	private long time = System.currentTimeMillis();
	private int nonce;
	
	public MissingBlockMessage(String blockHash, String key, long index, DhcAddress originDhcAddress, long bits) {
		this.blockHash = blockHash;
		this.key = key;
		this.index = index;
		this.originDhcAddress = originDhcAddress;
		this.bits = bits;
	}

	@Override
	public void process(Peer peer) {
		logger.info("MissingBlockMessage.process(peer) index={}, key={}, blockHash={}, originDhcAddress={}", index, key, blockHash, originDhcAddress);
		if (alreadySent(toString())) {
			return;
		}
		if(MissingBlocks.getInstance().getFoundBlocks(blockHash, key) != null) {
			return;
		}
		
		if(!isMined() || bits > Difficulty.INITIAL_BITS) {
			return;
		}

		Blockchain blockchain = Blockchain.getInstance();
		
		long testBits = blockchain.getBits(blockHash);
		if((testBits != 0) && (testBits != bits)) {
			return;
		}
		
		
		
		Block block = blockchain.getByHash(blockHash);
		if(block != null) {
			if(key.startsWith(block.getBucketKey())) {
				Network.getInstance().sendToAllPeers(new MissingBlockReply(block, key, originDhcAddress), peer);
				return;
			}
			if(block.getBucketKey().startsWith(key)) {
				Network.getInstance().sendToAllPeers(new MissingBlockReply(block, block.getBucketKey(), originDhcAddress), peer);
			}
			
		}
		Network.getInstance().sendToAllPeers(this, peer);
		
		if(block == null) {
			return;
		}
		
		MissingBlocks.getInstance().putMissingBlocks(block.getBlockHash(), key, block);
		schedule();
	}
	
	private void schedule() {
		String str = String.format("Missing block index=%s hash=%s", index, blockHash);
		ThreadExecutor.getInstance().schedule(new DhcRunnable(str) {
			
			@Override
			public void doRun() {
				process();
			}
		}, Constants.MINUTE * 1);
		
	}

	private void process() {
		if(MissingBlocks.getInstance().getMissingBlocks(blockHash, key) == null) {
			return;
		}
		if(MissingBlocks.getInstance().getFoundBlocks(blockHash, key) != null) {
			return;
		}
		

		
		Blockchain blockchain = Blockchain.getInstance();
		Block block = blockchain.getByHash(blockHash);
		if(block == null) {
			return;
		}

		if(block.getIndex() < blockchain.getIndex()) {
			return;
		}
		logger.info("MissingBlockMessage removeBranch() index={}, key={}, blockHash={}, originDhcAddress={}", block.getIndex(), key, blockHash, originDhcAddress);
		blockchain.removeBranch(blockHash);
	}
	
	
	public void send() {
		if (alreadySent(toString())) {
			return;
		}
		if(MissingBlocks.getInstance().getFoundBlocks(blockHash, key) != null) {
			return;
		}

		Network.getInstance().sendToAllPeers(this);
		logger.info("*************************************************************************");
		logger.info("Concensus ALERT Missing Block index={} key={} blockHash={} originDhcAddress={}", index, key, blockHash, originDhcAddress);
	}
	
	public String toString() {
		return String.format("MissingBlockMessage %s-%s-%s", index, key, blockHash);
	}

	public void mine() {
		BigInteger target = Difficulty.getTarget(bits);
		String miningHash;
		String prehash = blockHash + key + index + originDhcAddress;
		do {
			nonce++;
			if(nonce == Integer.MAX_VALUE) {
				nonce = 0;
				time++;
			}
			
			miningHash = CryptoUtil.getHashBase58Encoded(prehash + time + nonce);
		} while(!Difficulty.checkProofOfWork(miningHash, target));
	}
	
	private boolean isMined() {
		String prehash = blockHash + key + index + originDhcAddress;
		String miningHash = CryptoUtil.getHashBase58Encoded(prehash + time + nonce);
		return Difficulty.checkProofOfWork(bits, miningHash);
	}

}
