package org.dhc.network.consensus;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.BucketHashes;
import org.dhc.blockchain.MissingBlock;
import org.dhc.blockchain.MyAddresses;
import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionMemoryPool;
import org.dhc.network.BucketKey;
import org.dhc.network.ChainRest;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.BlockEvent;
import org.dhc.util.Callback;
import org.dhc.util.Coin;
import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Registry;
import org.dhc.util.SharedLock;
import org.dhc.util.Wallet;

public class Consensus {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final Consensus instance = new Consensus();
	@SuppressWarnings("unused")
	private static final long WAIT_TIME = Constants.MINUTE * 60;
	
	private final SharedLock readWriteLock = SharedLock.getInstance();
	private Network network;
	private int power;
	private BucketHashesMap consensuses;
	private BucketHashesMap nextConsensuses;
	private DhcAddress dhcAddress;
	private long blockchainIndex;
	private Blockchain blockchain;
	private Set<Transaction> transactions;
	
	private BucketHashes readyBucketHashes;
	private ReentrantLock miningLock = new ReentrantLock();
	private Condition miningLockCondition = miningLock.newCondition();
	private ReentrantLock lock = new ReentrantLock();
	private Condition consensusReadyCondition = lock.newCondition();
	private Block blockToMine;
	private List<String> bucketKeys;
	private InitialBucketHashes initialBucketHashes = new InitialBucketHashes();
	private ReadyBucketHashes readyConsensuses = new ReadyBucketHashes();
	private MyAddresses myAddresses = new MyAddresses();
	
	public static Consensus getInstance() {
		return instance;
	}
	
	private Consensus() {
		network = Network.getInstance();
		dhcAddress = DhcAddress.getMyDhcAddress();
		blockchain = Blockchain.getInstance();
		blockchain.setConsensus(this);
		nextConsensuses = new BucketHashesMap(DhcAddress.getMyDhcAddress());
		consensuses = new BucketHashesMap(DhcAddress.getMyDhcAddress());
	}
	

	private void clear() {
		initialBucketHashes.clear();
		readyBucketHashes = null;
		if(blockchainIndex + 1 == blockchain.getIndex()) {
			consensuses = nextConsensuses;
			nextConsensuses = new BucketHashesMap(dhcAddress);
			blockchainIndex++;
			return;
		}
		if(blockchainIndex == blockchain.getIndex()) {
			consensuses.clear();
			return;
		}
		consensuses = new BucketHashesMap(dhcAddress);
		nextConsensuses = new BucketHashesMap(dhcAddress);
		blockchainIndex = blockchain.getIndex();
	}
	
	private void initiate() {

		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			init();
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
		
		if (blockchainIndex != blockchain.getIndex()) {
			throw new ResetMiningException("Blockchain index is stale");
		}
		
		
		transactions = TransactionMemoryPool.getInstance().getTransactions();
		List<Block> lastBlocks = blockchain.getLastBlocks();
		String key = dhcAddress.getBinary(power);

		for (Block block : lastBlocks) {
			initiatePerLastBlock(block, key);
		}
		readyConsensuses.process(blockchainIndex);
	}
	
	private void init() {
		clear();
		network.reloadBuckets();
		power = network.getPower();
		bucketKeys =  network.getBucketKeys();
		bucketKeys.add(0, "");
		
		if(power < Blockchain.getInstance().getPower()) {
			ChainRest.getInstance().execute();
			throw new ResetMiningException("Network power is less than blockchain power");
			// if previous blocks has higher power (smaller shards) then new mining block might contain inputs that were already spent
			// in shards that not in our blockchain. So we can not mine a block which have wider shard than any previous blocks
		}
	}
	
	private BucketHash findEarlierBuckethashWithTheSameKey(BucketHash bucketHash) {
		return initialBucketHashes.waitForBucketHash(bucketHash, blockchainIndex);
	}
	
	private void initiatePerLastBlock(Block block, String key) {
		Set<Transaction> myBranchTransactions = block.filterBranchTransactions(transactions);
		
		BucketHash bucketHash = new BucketHash(key, myBranchTransactions, block.getBlockHash());
		
		logger.trace("initiatePerLastBlock bucketHash {}", bucketHash.toStringFull());
		
		// Check if another node already send a proposal for the same index, previousBlockhash, key, if yes then use it
		BucketHash earlierBucketHash = findEarlierBuckethashWithTheSameKey(bucketHash);
		if(earlierBucketHash != null && earlierBucketHash.isBranchValid()) {
			logger.trace("initiatePerLastBlock replace with earlier {}", earlierBucketHash.toStringFull());
			bucketHash = earlierBucketHash;
		}

		if(!bucketHash.isValid()) {
			throw new ResetMiningException("Not valid " + bucketHash.toStringFull());
		}
		
		if(getConsensusNoWait(bucketHash.getBinaryStringKey(), block.getBlockHash()) != null) {
			return;
		}
		put(bucketHash);

		
		logger.trace("Initial proposal {} {}", blockchainIndex, bucketHash.toStringFull());
		if(power > 0) {
			ProposeMessage message = new ProposeMessage(bucketHash.cloneWithoutTransactions(), blockchainIndex);
			message.setReply(true);
			network.sendToAllPeersInBucket(power - 1, message);
		}
		network.sendToAllMyPeers(new SendBucketHashMessage(bucketHash, blockchainIndex));
	}
	
	private void notifyConsensusReady() {
		if(readyBucketHashes == null && consensuses.getBySecondKey("").isEmpty() && blockchainIndex == blockchain.getIndex()) {
			return;
		}
		lock.lock();
		try {
			consensusReadyCondition.signal();
			logger.trace("Consensus after consensusReadyCondition.signal.");
		} finally {
			lock.unlock();
		}
	}
	
	public void notifyMiningLock() {

		long index = blockchain.getIndex();
		logger.trace("Calling miningLockCondition.signal() blockchain.getIndex()={}", index);
		miningLock.lock();
		try {
			miningLockCondition.signal();
		} finally {
			miningLock.unlock();
		}

		notifyConsensusReady();
	}

	public Block getMiningBlock() {
		initiate();

		long start = System.currentTimeMillis();
		while (readyBucketHashes == null && consensuses.getBySecondKey("").isEmpty()) {
			waitForConsensusReady();
			if (blockchainIndex != blockchain.getIndex()) {
				throw new ResetMiningException("Blockchain index is stale");
			}
		}
		logger.trace("To get consensus took {} ms.", System.currentTimeMillis() - start);
		
		return complete();
	}
	
	private String getEarliestBlockHash() {
		List<String> lastBlockHashes = blockchain.getLastBlockHashes();
		Set<String> set = consensuses.getBySecondKey("").keySet();
		for(String blockHash: lastBlockHashes) {
			if(set.contains(blockHash)) {
				return blockHash;
			}
		}
		throw new ResetMiningException("Consensus state is stale, last blocks hashes do not contain consensus block hashes");
	}
	
	private BucketHashes computeBucketHashes() {
		String blockHash = getEarliestBlockHash();
		
		BucketHashes bucketHashes = new BucketHashes();
		
		BucketHash bucketHash = null;

		for(String bucketKey: bucketKeys) {
			bucketHash = consensuses.get(blockHash, bucketKey);
			BucketHash left = consensuses.get(blockHash, bucketHash.getKey().getLeftKey().getKey());
			BucketHash right = consensuses.get(blockHash, bucketHash.getKey().getRightKey().getKey());
			bucketHash.setLeftRight(left, right);
			if(!bucketHash.isValid()) {
				throw new ResetMiningException("Not valid " + bucketHash.toStringFull());
			}
			bucketHashes.replace(bucketHash);
		}
		
		if(!bucketHashes.isValid()) {
			logger.info("**********************************************************************");
			logger.info("bucketHashes are not valid {}", bucketHashes);
			bucketHashes.displayHashes();
			throw new ResetMiningException("bucketHashes are not valid " + bucketHashes);
		}
		return bucketHashes;
	}

	private Block complete() {
		
		

		if(readyBucketHashes != null) {
			consensuses.replace(readyBucketHashes);
			logger.trace("******* Using readyBucketHashes *******");
		} else {
			readyBucketHashes = computeBucketHashes();
		}
		
		BucketHashes readyBucketHashes = this.readyBucketHashes.clone();

		BucketHash lastBucketHash = readyBucketHashes.getLastBucketHash();
		logger.trace("lastBucketHash.isHashForTransactionsValid()={}", lastBucketHash.isHashForTransactionsValid());
		logger.trace("lastBucketHash.isValid()={}", lastBucketHash.isValid());
		logger.trace("lastBucketHash.getFee()={}", lastBucketHash.getFee());
		logger.trace("Transaction.collectFees(lastBucketHash.getTransactions()) = {}", Transaction.collectFees(lastBucketHash.getTransactionsIncludingCoinbase()));
		
		Block previousBlock = blockchain.getByHash(readyBucketHashes.getPreviousBlockHash());
		
		Block block = new Block();
		block.setMiner(Wallet.getInstance().getPublicKey());
		block.setIndex(previousBlock.getIndex() + 1);
		block.setBucketHashes(readyBucketHashes);
		block.setPreviousHash(readyBucketHashes.getPreviousBlockHash());
		Coin fee = readyBucketHashes.getFirstBucketHash().getFee();
		Transaction coinbase = Transaction.createCoinbase(fee);
		logger.trace("coinbase {}", coinbase);
		block.removeCoinbase();
		block.addTransaction(coinbase);
		block.setCoinbaseTransactionId(coinbase.getTransactionId());

		if(!block.getBucketHashes().isFeeValid()) {
			logger.info("Fee is not valid");
			throw new ResetMiningException("Fee is not valid");
		}
		
		block.getBucketHashes().cleanTransactions();
		
		if (!block.isBranchValid()) {
			throw new ResetMiningException("!block.isBranchValid()");
		}
		
		blockToMine = block;
		
		int numberOfConsensuses = Registry.getInstance().getBucketConsensuses().getNumberOfConsensuses(blockchainIndex - 1);
		
		if (blockchainIndex != blockchain.getIndex()) {
			throw new ResetMiningException("Blockchain index is stale");
		}
		
		logger.info("'{}'-{} Ready to mine # peers {} bucketPeers {} myPeers {} pp {} #c {} ap {} {}\n", block.getBucketKey(), block.getIndex(), Peer.getTotalPeerCount(), 
				network.getAllPeers().size(), network.getMyBucketPeers().size(), network.getPossiblePower(), numberOfConsensuses, blockchain.getAveragePower(), block);
		Listeners.getInstance().sendEvent(new BlockEvent(block, "Ready to mine"));

		if(Constants.showSum && myAddresses.get(0).equals(DhcAddress.getMyDhcAddress())) {
			Registry.getInstance().getTotalBalance().process();
		}


		if (blockchainIndex != blockchain.getIndex()) {
			throw new ResetMiningException("Blockchain index is stale");
		}
		
		Registry.getInstance().getCompactor().addPrunings();
		//createAndSendTransaction(previousBlock);
		
		block.setTimeStamp(System.currentTimeMillis());
		waitForMiningLock();
		
		if(blockchainIndex != blockchain.getIndex()) {
			String str = String.format("Blockchain index is stale blockchainIndex=%s, blockchain.getIndex()=%s", blockchainIndex, blockchain.getIndex());
			throw new ResetMiningException(str);
		}
		
		block.setBlockHash();
		return block;
	}
	
	private void waitForMiningLock() {

		long index = blockchain.getIndex();
		if (blockchainIndex != index) {
			throw new ResetMiningException("Blockchain index is stale");
		}

		try {

			
			logger.trace("Before blockToMine.mine() blockchain.getIndex()={}", index);
			blockToMine.mine();

			logger.trace("After blockToMine.mine()");
		} finally {
			blockToMine = null;

		}
	}
	
	private BucketHash getConsensusNoWait(String consensusKey, String blockHash) {
		BucketHash bucketHash = consensuses.get(blockHash, consensusKey);
		return bucketHash;
	}
	
	private void sendBucketHash(BucketHash bucketHash, BucketHash consensus) {
		//logger.trace("bucketHash {} consensus {}", bucketHash.toStringFull(), consensus.toStringFull());
		
		BucketHash nextBucketHash = new BucketHash(bucketHash, consensus);

		network.sendToKey(nextBucketHash.getBinaryStringKey(), new SendBucketHashMessage(nextBucketHash, blockchainIndex));
		//logger.trace("SendBucketHashMessage \"{}\"-{}", nextBucketHash.getKey(), nextBucketHash.getHash());
	}
	
	private void sendNextProposal(BucketHash bucketHash) {
		
		if("".equals(bucketHash.getBinaryStringKey())) {
			logger.info("************************************************");
			logger.info("bucketHash={}", bucketHash.toStringFull());
			return;
		}

		String consensusKey = dhcAddress.getBinary(bucketHash.getBinaryStringKey().length());
		
		BucketHash consensusHash = getConsensusNoWait(consensusKey, bucketHash.getPreviousBlockHash());
		if(consensusHash == null) {
			return;
		}
		BucketHash parentBucketHash = new BucketHash(consensusHash, bucketHash);
		
		logger.trace("sendNextProposal parentBucketHash {}", parentBucketHash.toStringFull());
		BucketHash earlierBucketHash = findEarlierBuckethashWithTheSameKey(parentBucketHash);
		if(earlierBucketHash != null && earlierBucketHash.hasBothChildren()) {
			if(earlierBucketHash.hasChild(consensusHash)) {
				logger.trace("sendNextProposal replace with earlier {}", earlierBucketHash.toStringFull());
				parentBucketHash = earlierBucketHash;
				bucketHash = parentBucketHash.getOtherChild(consensusHash);
				replace(bucketHash);
			} else {
				//logger.info("consensusHash {}", consensusHash.toStringFull());
				//logger.info("earlierBucketHash {}", earlierBucketHash.toStringFull());
				boolean hasAllChildren = Registry.getInstance().getBucketConsensuses().hasAllChildren(earlierBucketHash, blockchainIndex);
				logger.trace("hasAllChildren={}", hasAllChildren);
				if(hasAllChildren) {
					parentBucketHash = earlierBucketHash;
					replaceChildren(parentBucketHash);
				}
			}
		}
		
		String nextConsensusKey = parentBucketHash.getBinaryStringKey();

		if(consensuses.get(bucketHash.getPreviousBlockHash(), nextConsensusKey) != null) {
			return;
		}
		
		sendBucketHash(consensusHash, bucketHash);
		
		int i = bucketHash.getBinaryStringKey().length() - 1;
		put(parentBucketHash);
		if(!"".equals(nextConsensusKey)) {
			parentBucketHash = parentBucketHash.cloneWithoutTransactions();
			ProposeMessage message = new ProposeMessage(parentBucketHash, blockchainIndex);
			message.setReply(true);
			network.sendToAllPeersInBucket(i - 1, message);
		}
		i--;
		if(i < 0 && !consensuses.getBySecondKey("").isEmpty()) {
			notifyConsensusReady();
			return;
		}
		String nextBucketKey = network.getBucketKey(i);
		if(nextBucketKey == null) {//this can happen if the number of buckets changed and there is no bucket with index i
			return;
		}
		BucketHash bHash = consensuses.get(bucketHash.getPreviousBlockHash(), nextBucketKey);
		if(bHash == null) {
			return;
		}
		String nextBucketHash = bHash.getHash();
		if(nextBucketHash == null) {
			return;
		}
		sendNextProposal(bHash);
	}

	private void replaceChildren(BucketHash bucketHash) {
		String myKey = DhcAddress.getMyDhcAddress().getBinary(Blockchain.getInstance().getPower());
		BucketHash runningBucketHash = bucketHash;
		while(true) {
			if(myKey.equals(runningBucketHash.getBinaryStringKey())) {
				break;
			}
			BucketHash left = runningBucketHash.getLeft();
			BucketHash right = runningBucketHash.getRight();
			if(myKey.startsWith(left.getBinaryStringKey())) {
				runningBucketHash = left;
			} else if(myKey.startsWith(right.getBinaryStringKey())) {
				runningBucketHash = right;
			} else {
				break;
			}
			runningBucketHash = Registry.getInstance().getBucketConsensuses().get(runningBucketHash, blockchainIndex);
			if(runningBucketHash == null) {
				break;
			}
			consensuses.set(runningBucketHash);
			if(runningBucketHash.equals(right)) {
				consensuses.set(left);
			} else {
				consensuses.set(right);
			}
		}
		
	}

	private boolean saveProposal(BucketHash bucketHash) {
		int index = bucketHash.getBinaryStringKey().length() - 1;
		if(index < 0) {
			return false; // this can happen when bucketKey="" which means full consensus was sent
		}
		if(!bucketHash.getBinaryStringKey().equals(network.getBucketKey(index))) {	//this can also happen if the number of buckets changed and there is no bucket with this index, so network.getBucketKey(index) == null
			//logger.trace("*****************");
			//logger.trace("index={}, power={}, bucketKey={}, network.getBucketKey(index)={}", index, power, bucketHash.getBinaryStringKey(), network.getBucketKey(index));
			return false;
		}
		
		if(consensuses.get(bucketHash.getPreviousBlockHash(), bucketHash.getBinaryStringKey()) != null) {
			String nextConsensusKey = dhcAddress.getBinary(bucketHash.getBinaryStringKey().length() - 1);
			if(consensuses.get(bucketHash.getPreviousBlockHash(), nextConsensusKey) == null) {
				
				return true;
			}
			return false;
		}
		put(bucketHash);

		return true;
	}
	
	@SuppressWarnings("unused")
	public void processPropose(BucketHash bucketHash, boolean noReply, Peer peer, long index) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		long duration;
		BucketHash hash;
		try {
			String blockHash = bucketHash.getPreviousBlockHash();
			String key = bucketHash.getBinaryStringKey();
			new MissingBlock(blockHash, index);
			if (blockchainIndex + 1 == index && key.equals(new BucketKey(dhcAddress.getBinary(key.length())).getOtherBucketKey().getKey())) {
				if (nextConsensuses.get(blockHash, key) == null) {
					nextConsensuses.put(bucketHash);
				}
				return;
			}

			if (noReply == false) {
				reply(key, blockHash, peer, index);
			}
			if(index == blockchainIndex && readyBucketHashes != null) {
				return;
			}
			if (blockchainIndex != blockchain.getIndex()) {
				return;
			}
			if (!blockchain.getLastBlockHashes().contains(blockHash)) {
				return;
			}

			logger.trace("Proposal {} '{}'={}, previous blockHash {}, #tx {}", index, key, bucketHash, blockHash, bucketHash.getNumberOfTransactions());
			if (!saveProposal(bucketHash)) {
				return;
			}
			hash = consensuses.get(blockHash, bucketHash.getBinaryStringKey());// saveProposal allows to proceed even if bucketKey is already saved as long as
		} finally {
			writeLock.unlock();
			duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
		sendNextProposal(hash);
	}
	
	private void reply(String key, String blockHash, Peer peer, long index) {
		String consensusKey = new BucketKey(key).getOtherBucketKey().getKey();
		BucketHash consensusHash = getConsensusNoWait(consensusKey, blockHash);
		if(consensusHash != null) {
			if(consensusHash.hasOnlyOneChild()) {
				logger.info("bucketHash {}", consensusHash.toStringFull());
				logger.info("", new RuntimeException());
			}
			consensusHash = consensusHash.cloneWithoutTransactions();
			ProposeMessage message = new ProposeMessage(consensusHash, blockchainIndex);
			peer.send(message);
		}
		if(index == blockchainIndex && readyBucketHashes != null) {
			peer.send(new SendBucketHashMessage(readyBucketHashes.getBucketHash(""), blockchainIndex));
		}
	}
	
	private void put(BucketHash bucketHash) {
		consensuses.put(bucketHash);
		if("".equals(bucketHash.getBinaryStringKey())) {
			notifyConsensusReady();
			return;
		}
		if(!consensuses.getBySecondKey("").isEmpty()) {
			notifyConsensusReady();
			return;
		}
	}
	
	private void replace(BucketHash bucketHash) {
		consensuses.replace(bucketHash);
		if("".equals(bucketHash.getBinaryStringKey())) {
			notifyConsensusReady();
			return;
		}
		if(!consensuses.getBySecondKey("").isEmpty()) {
			notifyConsensusReady();
			return;
		}
	}

	private void waitForConsensusReady() {
		try {
			long waitTime = Constants.SECOND * 60;
			logger.trace("Consensus before consensusReadyCondition.await.");
			boolean waitExpired = false;
			lock.lock();
			try {
				waitExpired = !consensusReadyCondition.await(waitTime, TimeUnit.MILLISECONDS);
				logger.trace("Consensus after consensusReadyCondition.await.");
			} finally {
				lock.unlock();
			}
			if(readyBucketHashes != null) {
				return;
			}
			if(waitExpired) {
				if(!consensuses.getBySecondKey("").isEmpty()) {
					logger.info("Timed out but ready to mine");
					return;
				}
				
				if(blockchainIndex == blockchain.getIndex()) {
					logger.info("\n");
					logger.info("{}-{} Reset # peers {} bucketPeers {} myPeers {} pp {} power={} {} size={} consensuses={}", 
							network.getBucketKey(), blockchainIndex, Peer.getTotalPeerCount(), 
							network.getAllPeers().size(), network.getMyBucketPeers().size(), network.getPossiblePower(), 
							power, Wallet.getInstance().getDhcAddress().getBinary(6), consensuses.longestSize(), consensuses);
					logger.info("\n");
				}
				
				ResetMiningException e = new ResetMiningException("Wait longer than " + waitTime + " ms.");
				throw e;
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void processReadyBucketHashes(BucketHash consensus, long index) {
		if(consensus == null) {
			return;
		}
		if(index > blockchainIndex) {
			readyConsensuses.add(consensus, index);
			return;
		}
		
		if (!recoverFromConsensusCheck(index, consensus.getPreviousBlockHash())) {
			return;
		}
		
		Block blockToMine = this.blockToMine;
		if(blockToMine != null && blockToMine.getIndex() > index) {
			return;
		}

		// can not call recoverFromConsensus and hold lock on consensus because it will
		// block other receiver pool threads and we will need them to process
		// GetBucketHashReply
		BucketHashes bucketHashes = Registry.getInstance().getBucketConsensuses().recoverFromConsensus(consensus, index);

		if (bucketHashes == null) {
			return;
		}
		

		GatherTransactions.getInstance().run(bucketHashes.getLastBucketHash(), index, new Callback() {

			@Override
			public void expire() {

			}

			@Override
			public void callBack(Object object) {
				processReadyBucketHashesCallback(bucketHashes, index);
			}
		});
	}
	
	private void processReadyBucketHashesCallback(BucketHashes bucketHashes, long index) {
		if (!bucketHashes.isValid()) {
			return;
		}

		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			if (!recoverFromConsensusCheck(index, bucketHashes.getPreviousBlockHash())) {
				return;
			}
			logger.trace("************ Constructed readyBucketHashes ******************");
			if (bucketHashes.getNumberOfTransactions() == 0 && transactions != null && !transactions.isEmpty()) {

				Block block = blockchain.getByHash(bucketHashes.getPreviousBlockHash());
				Set<Transaction> myBranchTransactions = block.filterBranchTransactions(transactions);
				//Transaction.collectFees(myBranchTransactions);

				bucketHashes.addMyTransactions(myBranchTransactions, index);
				BucketHash lastBucketHash = bucketHashes.getLastBucketHash();
				logger.trace("lastBucketHash.isHashForTransactionsValid()={}", lastBucketHash.isHashForTransactionsValid());
				logger.trace("lastBucketHash.isValid()={}", lastBucketHash.isValid());
				logger.trace("lastBucketHash.getFee()={}", lastBucketHash.getFee());
				logger.trace("Transaction.collectFees(lastBucketHash.getTransactions()) = {}", Transaction.collectFees(lastBucketHash.getTransactionsIncludingCoinbase()));
				logger.trace("index {}, lastBucketHash {}", index, lastBucketHash.toStringFull());
			}
			readyBucketHashes = bucketHashes;
			notifyConsensusReady();
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	private boolean recoverFromConsensusCheck(long index, String previousBlockHash) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			if (index != blockchainIndex) {
				return false;
			}
			if (!blockchain.getLastBlockHashes().contains(previousBlockHash)) {
				return false;
			}
			if (!consensuses.getBySecondKey("").isEmpty()) {
				return false;
			}
			if (readyBucketHashes != null) {
				return false;
			}
			return true;
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public void processReadyBucketHash(BucketHash bucketHash, long index) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			if (index != blockchainIndex) {
				return;
			}
			if(readyBucketHashes != null) {
				return;
			}
			if (!blockchain.getLastBlockHashes().contains(bucketHash.getPreviousBlockHash())) {
				return;
			}
			
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
		recover(bucketHash, index);
	}
	
	private void recover(BucketHash bucketHash, long blockchainIndex) {

		Set<Transaction> transactions = bucketHash.getTransactionsIncludingCoinbase();
		if(transactions == null) {
			return;
		}
		
		String key = bucketHash.getBinaryStringKey();
		
		String checkKey = key;
		
		while(!"".equals(checkKey)) {
			if(consensuses.get(bucketHash.getPreviousBlockHash(), checkKey) != null) {
				String myKey = DhcAddress.getMyDhcAddress().getBinary(power);
				if(myKey.startsWith(checkKey) && !myKey.equals(checkKey)) {
					logger.trace("Consensus.recover blockchainIndex: {}, bucketHash: {}", blockchainIndex, bucketHash.toStringFull());
					Registry.getInstance().getBucketConsensuses().recover(bucketHash, blockchainIndex);
				}
				return;
			}
			checkKey = checkKey.substring(0, checkKey.length() - 1);
		}
		
		
		
		String dhcAddressBinary = DhcAddress.getMyDhcAddress().getBinary();
		String myKey = dhcAddressBinary.substring(0, power);
		if(key.equals(myKey) || !myKey.startsWith(key)) {
			return; //bucket for key does not strictly contain bucket for myKey
		}
		
		//logger.trace("Consensus.recover {}", bucketHash.toStringFull());
		
		String otherKey = null;
		BucketHash otherBucketHash =null;
		BucketHash parent = null;
		while(!key.equals(myKey)) {
			if(readyBucketHashes != null) {
				return;
			}
			BucketHash myBucketHash = new BucketHash(myKey, transactions, bucketHash.getPreviousBlockHash());
			Registry.getInstance().getBucketConsensuses().put(myBucketHash, blockchainIndex);
			replace(myBucketHash);

			otherKey = myBucketHash.getKey().getOtherBucketKey().getKey();
			otherBucketHash = new BucketHash(otherKey, transactions, bucketHash.getPreviousBlockHash());
			Registry.getInstance().getBucketConsensuses().put(otherBucketHash, blockchainIndex);
			replace(otherBucketHash);

			parent = new BucketHash(myBucketHash, otherBucketHash);
			parent.setTransactions(transactions);

			Registry.getInstance().getBucketConsensuses().put(parent, blockchainIndex);
			
			replace(parent);
			
			parent.isValid();

			myKey = parent.getBinaryStringKey();
		}
		if(parent == null) {
			return;
		}
		bucketHash.setLeftRight(parent.getLeft(), parent.getRight());
		bucketHash.isValid();

		//We recovered up to received bucketHash, so we can try to resume consensus build from that level
		otherBucketHash = consensuses.get(bucketHash.getKey().getOtherBucketKey().getKey(), bucketHash.getPreviousBlockHash());
		//logger.trace("bucketHash.getKey().getOtherBucketKey().getKey()={}", bucketHash.getKey().getOtherBucketKey().getKey());
		if(otherBucketHash != null) {
			logger.info("************************************************************************");
			logger.info("otherBucketHash={}",otherBucketHash.toStringFull());
			sendNextProposal(otherBucketHash);//I have not seen code going here yet
		}

	}

	public Block getBlockToMine() {
		return blockToMine;
	}
	
	public InitialBucketHashes getInitialBucketHashes() {
		return initialBucketHashes;
	}
	
}
