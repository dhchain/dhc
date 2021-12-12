package org.dhc.network.consensus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.BucketHashes;
import org.dhc.blockchain.GetBucketHashesMessage;
import org.dhc.blockchain.RecoveringBlocks;
import org.dhc.blockchain.SendCShardTxMessage;
import org.dhc.blockchain.SendMyBlockMessage;
import org.dhc.blockchain.Transaction;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Callback;
import org.dhc.util.Constants;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class BucketConsensuses {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	// Multi key map to store BucketHash by index, previousBlockHash, key, hash
	private Map<Long, Map<String, Map<String, Map<String, BucketHash>>>> bucketHashes =  new LinkedHashMap<>();
	
	public BucketHash get(BucketHash bucketHash, long blockchainIndex) {
		Map<String, Map<String, Map<String, BucketHash>>> mapByIndex = bucketHashes.get(blockchainIndex);
		if(mapByIndex == null) {
			return null;
		}
		Map<String, Map<String, BucketHash>> mapByPreviousBlockHash = mapByIndex.get(bucketHash.getPreviousBlockHash());
		if(mapByPreviousBlockHash == null) {
			return null;
		}
		Map<String, BucketHash> mapByKey = mapByPreviousBlockHash.get(bucketHash.getBinaryStringKey());
		if(mapByKey == null) {
			return null;
		}
		BucketHash result = mapByKey.get(bucketHash.getHash());
		if(result != null) {
			logger.trace("{} {} BucketConsensuses.get return buckethash {}", blockchainIndex, bucketHash.isMined(), bucketHash.toStringFull());
		}
		return result;
	}
	
	public Collection<BucketHash> getByIndexByPreviousBlockHashByKey(BucketHash bucketHash, long blockchainIndex) {
		Map<String, Map<String, Map<String, BucketHash>>> mapByIndex = bucketHashes.get(blockchainIndex);
		if(mapByIndex == null) {
			return null;
		}
		Map<String, Map<String, BucketHash>> mapByPreviousBlockHash = mapByIndex.get(bucketHash.getPreviousBlockHash());
		if(mapByPreviousBlockHash == null) {
			return null;
		}
		Map<String, BucketHash> mapByKey = mapByPreviousBlockHash.get(bucketHash.getBinaryStringKey());
		if(mapByKey == null) {
			return null;
		}

		synchronized(mapByKey) {
			return new ArrayList<>(mapByKey.values());
		}
	}
	
	public int getNumberOfConsensuses(long blockchainIndex) {
		Map<String, Map<String, Map<String, BucketHash>>> mapByIndex = bucketHashes.get(blockchainIndex);
		if(mapByIndex == null) {
			return 0;
		}
		int result = 0;
		
		for(Map<String, Map<String, BucketHash>> mapByPreviousBlockHash: mapByIndex.values()) {
			Map<String, BucketHash> mapByKey = mapByPreviousBlockHash.get("");
			if(mapByKey == null) {
				continue;
			}
			result = result + mapByKey.size();
		}
		
		return result;
	}
	

	public synchronized BucketHash put(BucketHash bucketHash, long blockchainIndex, boolean notifyForce) {
		
		if(bucketHashes.size() > 10) {
			bucketHashes.remove(bucketHashes.keySet().iterator().next());
		}
		BucketHash other = get(bucketHash, blockchainIndex);
		//replace it only if more specific
		if (other != null) {
			if (other.getLeft() != null || bucketHash.getLeft() == null) {
				if(!other.isHashForTransactionsValid() && bucketHash.isHashForTransactionsValid()) {
					other.setAllTransactions(bucketHash.getAllTransactions());
					if(!other.isHashForTransactionsValid()) {
						throw new RuntimeException("After copying transactions other hash is still not valid for its transactions");
					}
				}
				return other;
			}
		}
		
		if(!bucketHash.isHashForTransactionsValid() && other != null && other.isHashForTransactionsValid()) {
			bucketHash.setAllTransactions(other.getAllTransactions());
			if(!bucketHash.isHashForTransactionsValid()) {
				throw new RuntimeException("After copying transactions bucketHash hash is still not valid for its transactions");
			}
		}
		
		Map<String, Map<String, Map<String, BucketHash>>> mapByIndex = bucketHashes.get(blockchainIndex);
		if(mapByIndex == null) {
			mapByIndex = new HashMap<>();
			bucketHashes.put(blockchainIndex, mapByIndex);
		}
		Map<String, Map<String, BucketHash>> mapByPreviousBlockHash = mapByIndex.get(bucketHash.getPreviousBlockHash());
		if(mapByPreviousBlockHash == null) {
			mapByPreviousBlockHash = new HashMap<>();
			mapByIndex.put(bucketHash.getPreviousBlockHash(), mapByPreviousBlockHash);
		}
		Map<String, BucketHash> mapByKey = mapByPreviousBlockHash.get(bucketHash.getBinaryStringKey());
		if(mapByKey == null) {
			mapByKey = Collections.synchronizedMap(new LinkedHashMap<>());
			mapByPreviousBlockHash.put(bucketHash.getBinaryStringKey(), mapByKey);
		}
		
		logger.trace("{} {} BucketConsensuses.put bucketHash {}", blockchainIndex, bucketHash.isMined(), bucketHash.toStringFull());
		mapByKey.put(bucketHash.getHash(), bucketHash);
		
		if(notifyForce) {
			Consensus.getInstance().getInitialBucketHashes().notifyForBucketHashFromRecover(bucketHash, blockchainIndex);
		} else {
			Consensus.getInstance().getInitialBucketHashes().notifyForBucketHash(bucketHash, blockchainIndex);
		}
		return bucketHash;
	}
	
	private void expire(Block block) {
		long realBlockchainIndex = Blockchain.getInstance().getIndex();
		String str = String.format("Recovering block %s-%s-%s", block.getBucketKey(), block.getIndex(), block.getBlockHash().substring(0, 7));
		ThreadExecutor.getInstance().schedule(new DhcRunnable(str) {
			
			@Override
			public void doRun() {
				if(Blockchain.getInstance().getIndex() == realBlockchainIndex && RecoveringBlocks.getInstance().containsKey(block.getBlockHash())) {
					logger.info("Recover expired, retrying recovering block {}", block);
					recover(block);
				}
			}
		}, Constants.SECOND * 5);
	}
	
	public void recover(Block block) {
		
		if(Blockchain.getInstance().contains(block.getBlockHash())) {
			RecoveringBlocks.getInstance().remove(block.getBlockHash());
			return;
		}
		
		
		logger.trace("Trying to recover block {}", block);
		BucketHash consensus = block.getBucketHashes().getBucketHash("");
		BucketHashes hashes = recoverFromConsensus(consensus, block.getIndex() - 1);
		
		if(hashes == null) {
			retry(block);
			expire(block);
			return;
		}
		
		BucketHash lastBucketHash= hashes.getLastBucketHash();
		if (!lastBucketHash.isHashForTransactionsValid()) {
			logger.trace("***********************************************************************************************************************");
			logger.trace("lastBucketHash.isHashForTransactionsValid()=false");
			logger.trace("GatherTransactions below should add transactions and make it valid");
			hashes.traceHashes();
		}
		
		GatherTransactions.getInstance().run(hashes.getLastBucketHash(), block.getIndex() - 1, new Callback() {
			
			@Override
			public void expire() {
				if (Blockchain.getInstance().contains(block.getBlockHash())) {
					return;
				}
				logger.info("expire() block {}", block);
			}
			
			@Override
			public void callBack(Object object) {
				recoverComplete(block, hashes);
			}
		});
		
	}
	
	private void recoverComplete(Block block, BucketHashes hashes) {
		if(!hashes.isValid()) {
			return;
		}
		logger.trace("Has coinbase: {}", block.getCoinbase());
		
		Transaction coinbase = block.getCoinbase();
		block.setBucketHashes(hashes); // I suspect this might remove coinbase in some cases so we need to add it back
		block.cleanCoinbase();
		if(coinbase != null && coinbase.getReceiver().isMyKey(block.getBucketKey()) && block.getCoinbase() == null) {
			block.addTransaction(coinbase);
		}
		
		logger.trace("BucketConsensuses.recoverComplete() will try to add block {}", block);
		
		if (Blockchain.getInstance().add(block)) {
			Network.getInstance().sendToKey(block.getBucketKey(), new SendMyBlockMessage(block));
			new SendCShardTxMessage().send(block);
		}
		RecoveringBlocks.getInstance().remove(block.getBlockHash());
	}
	
	private void retry(Block block) {
		logger.info("Could not recover immediately, will ask my peers to get bucket hashes for block {}", block);
		Network.getInstance().sendToAllMyPeers(new GetBucketHashesMessage(block.getBlockHash()));
	}
	
	public BucketHashes recoverFromConsensus(BucketHash consensus, long blockchainIndex) {
		int power = Network.getInstance().getPower();
		String myDhcAddress = DhcAddress.getMyDhcAddress().getBinary();
		BucketHashes hashes = new BucketHashes();
		
		BucketHash bucketHash = consensus;
		bucketHash = hashes.replace(bucketHash);
		//logger.trace("recovered bucketHash={}", bucketHash.toStringFull());
		
		while(hashes.size() <= power) {
			
			recover(consensus, blockchainIndex);// will create and set children if missing for this power
			
			if(consensus.getLeft() == null) {
				logger.info("consensus.getLeft() is null, break loop");
				logger.info("consensus {}", consensus.toStringFull());
				break;
			}
			
			if(myDhcAddress.startsWith(consensus.getLeft().getBinaryStringKey())) {
				bucketHash = consensus.getRight();
				consensus = consensus.getLeft();
			} else {
				bucketHash = consensus.getLeft();
				consensus = consensus.getRight();
			}
			
			if(bucketHash == null) {
				logger.info("bucketHash is null");
				return null;
			}
			
			bucketHash = hashes.replace(bucketHash);
			
			if(consensus == null) {
				logger.info("consensus is null");
				return null;
			}
			
			if(consensus.isHashForTransactionsValid()) {
				put(consensus, blockchainIndex, false);
			} else if(consensus.getPower() < power && consensus.getLeft() != null && consensus.getRight() != null) {
				put(consensus, blockchainIndex, false);
			}
			
			BucketHash originalConsensus = consensus;
			consensus = get(consensus, blockchainIndex);
			if(consensus == null) {
				consensus = getBucketHash(originalConsensus, blockchainIndex);
			}
			if(consensus == null) {
				logger.info("***********************************************************************************************************************");
				logger.info("Could not find BucketHash for {}", originalConsensus.toStringFull());
				logger.info("power={}", power);
				hashes.displayHashes();
				return null;
			}

			
			
			if(consensus.isMined()) {
				Network.getInstance().sendToKey(consensus.getBinaryStringKey(), new SendBucketHashMessage(consensus, blockchainIndex));
			} else {
				logger.trace("{} {} recoverFromConsensus() Not mined consensus.getKeyHash()={} consensus.isMined={}", blockchainIndex, consensus.getRealHashCode(), 
						consensus.getKeyHash(), consensus.isMined());
			}
			
		}

		consensus = hashes.replace(consensus);
		
		int difference = hashes.getPower() == 0? 1: 2;
		if(hashes.getPower() + difference != hashes.size()) {
			logger.info("***********************************************************************************************************************");
			logger.info("Number of hashes does not match: power={}, hashes={}", hashes.getPower(), hashes);
			hashes.displayHashes();
			logger.info("Cannot not use these hashes");
			return null;
		}
		
		return hashes;
	}
	
	public void gatherTransactions(BucketHash consensus, long blockchainIndex) {
		final BucketHash bucketHash = getBucketHash(consensus, blockchainIndex);
		if(bucketHash == null) {
			return;
		}
		
		if(bucketHash.isHashForTransactionsValid()) {
			consensus.setTransactions(bucketHash.getTransactionsIncludingCoinbase());
			return;
		}
		
		List<Callable<Boolean>> calls = new ArrayList<>();

		calls.add(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				gatherTransactions(bucketHash.getLeft(), blockchainIndex);
				return null;
			}
		});
		
		calls.add(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				gatherTransactions(bucketHash.getRight(), blockchainIndex);
				return null;
			}
		});
			
		ForkJoinPool.commonPool().invokeAll(calls);
		
		Set<Transaction> set = new HashSet<>();
		
		if(bucketHash.getLeft().getTransactionsIncludingCoinbase() != null) {
			set.addAll(bucketHash.getLeft().getTransactionsIncludingCoinbase());
		}
		if(bucketHash.getRight().getTransactionsIncludingCoinbase() != null) {
			set.addAll(bucketHash.getRight().getTransactionsIncludingCoinbase());
		}
		
		consensus.setTransactions(set);
	}
	
	// remote call to other peers
	private BucketHash getBucketHash(BucketHash consensus, long blockchainIndex) {
		List<Peer> peers = Network.getInstance().getPeersWithKey(consensus.getBinaryStringKey());
		for(Peer peer: peers) {
			GetBucketHashReply message = (GetBucketHashReply) peer.sendSync(new GetBucketHash(consensus, blockchainIndex), Constants.SECOND * 20);
			if(message != null) {
				BucketHash bucketHash = message.getBucketHash();
				if(bucketHash != null) {
					logger.trace("BucketConsensuses.getBucketHash {} {} return buckethash {}", blockchainIndex, bucketHash.getPreviousBlockHash(), bucketHash.toStringFull());
					return bucketHash;
				}
			}
		}
		return null;
	}

	public void recover(BucketHash bucketHash, long blockchainIndex) {
		logger.trace("Trying to recover bucketHash={}", bucketHash.toStringFull());
		Network network = Network.getInstance();
		Set<Transaction> transactions = bucketHash.getTransactionsIncludingCoinbase();
		if(transactions == null && !"".equals(bucketHash.getHash())) {
			return;
		}

		String key = bucketHash.getBinaryStringKey();
		String myKey = DhcAddress.getMyDhcAddress().getBinary(Network.getInstance().getPower());
		if(key.equals(myKey) || !myKey.startsWith(key)) {
			return; //bucket for key does not strictly contain bucket for myKey
		}

		String otherKey = null;
		BucketHash otherBucketHash =null;
		BucketHash parent = null;
		while(!key.equals(myKey)) {
			BucketHash myBucketHash = new BucketHash(myKey, transactions, bucketHash.getPreviousBlockHash());
			//logger.trace("put myBucketHash={}", myBucketHash.toStringFull());
			myBucketHash = put(myBucketHash, blockchainIndex, true);
			
			if(myBucketHash.isMined()) {
				network.sendToKey(myBucketHash.getBinaryStringKey(), new SendBucketHashMessage(myBucketHash, blockchainIndex));
			} else {
				logger.trace("{} {} recover() Not mined myBucketHash.getKeyHash()={} myBucketHash.isMined={}", blockchainIndex, myBucketHash.getRealHashCode(), 
						myBucketHash.getKeyHash(), myBucketHash.isMined());
			}
			
			

			otherKey = myBucketHash.getKey().getOtherBucketKey().getKey();
			otherBucketHash = new BucketHash(otherKey, transactions, bucketHash.getPreviousBlockHash());
			//logger.trace("put otherBucketHash={}", otherBucketHash.toStringFull());
			otherBucketHash = put(otherBucketHash, blockchainIndex, true);
			
			if(otherBucketHash.isMined()) {
				network.sendToKey(otherBucketHash.getBinaryStringKey(), new SendBucketHashMessage(otherBucketHash, blockchainIndex));
			} else {
				logger.trace("{} {} recover() Not mined otherBucketHash.getKeyHash()={}  otherBucketHash.isMined={}", blockchainIndex, otherBucketHash.getRealHashCode(), 
						otherBucketHash.getKeyHash(), otherBucketHash.isMined());
			}
			
			

			parent = new BucketHash(myBucketHash, otherBucketHash);
			parent.setTransactions(transactions);

			//logger.trace("put myBucketHash={}", myBucketHash.toStringFull());
			parent = put(parent, blockchainIndex, true);
			
			if(parent.isMined()) {
				network.sendToKey(parent.getBinaryStringKey(), new SendBucketHashMessage(parent, blockchainIndex));
			} else {
				logger.trace("{} {} recover() Not mined parent.getKeyHash()={} parent.isMined={}", blockchainIndex, parent.getRealHashCode(), 
						parent.getKeyHash(), parent.isMined());
			}
			
			

			myKey = parent.getBinaryStringKey();
		}
		if(parent == null) {
			return;
		}
		bucketHash.setLeftRight(parent.getLeft(), parent.getRight());
	}

	public void trim(Block clone) {
		BucketHashes hashes = clone.getBucketHashes();
		int power = Blockchain.getInstance().getPower();
		if(hashes.getPower() <= power) {
			return;
		}
		hashes.trim(power);
	}

	public boolean hasAllChildren(BucketHash bucketHash, long blockchainIndex) {
		String myKey = DhcAddress.getMyDhcAddress().getBinary(Blockchain.getInstance().getPower());
		BucketHash runningBucketHash = bucketHash;
		while(true) {
			if(myKey.equals(runningBucketHash.getBinaryStringKey())) {
				if(runningBucketHash.hasNullTransactions()) {
					break;
				}
				return true;
			}
			BucketHash left = runningBucketHash.getLeft();
			if(left == null) {
				break;
			}
			BucketHash right = runningBucketHash.getRight();
			if(right == null) {
				break;
			}
			if(myKey.startsWith(left.getBinaryStringKey())) {
				runningBucketHash = left;
			} else if(myKey.startsWith(right.getBinaryStringKey())) {
				runningBucketHash = right;
			} else {
				break;
			}
			runningBucketHash = get(runningBucketHash, blockchainIndex);
			if(runningBucketHash == null) {
				break;
			}
		}
		return false;
	}

}
