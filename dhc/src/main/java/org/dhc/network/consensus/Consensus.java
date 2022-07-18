package org.dhc.network.consensus;

import java.util.HashSet;
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
import org.dhc.blockchain.SendTransactionMessage;
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
import org.dhc.util.Difficulty;
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
	private Set<BucketHash> nextProposals = new HashSet<>();
	
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
		nextProposals.clear();
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
		logger.trace("{} ********************* initiate() START *************************", blockchainIndex);
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
			throw new BlockchainIndexStaleException("Blockchain index is stale");
		}
		
		
		transactions = TransactionMemoryPool.getInstance().getTransactions();
		List<Block> lastBlocks = blockchain.getLastBlocks();
		String key = dhcAddress.getBinary(power);

		for (Block block : lastBlocks) {

			initiatePerLastBlock(block, key);
			
		}
		readyConsensuses.process(blockchainIndex);
		logger.trace("{} #blocks={} ********************* initiate() END *************************", blockchainIndex, lastBlocks.size());
		
	}
	
	private void init() {
		clear();
		power = network.getPower();
		bucketKeys =  network.getBucketKeys();
		bucketKeys.add(0, "");
		
		if(power < Blockchain.getInstance().getPower()) {
			ChainRest.getInstance().executeAsync();// At this moment consensus holds shared lock so we don't want to wait long
			logger.info("Network power is less than blockchain power");
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
			Set<Transaction> transactionsToResend = new HashSet<>(myBranchTransactions);
			transactionsToResend.removeAll(earlierBucketHash.getTransactions());
			if(!transactionsToResend.isEmpty()) {
				logger.info("Found {} transactions to resend from mempool {}", transactionsToResend.size(), transactionsToResend);
				for(Transaction transaction: transactionsToResend) {
					network.sendToAllMyPeers(new SendTransactionMessage(transaction));
				}
			}
			
			bucketHash = earlierBucketHash;
		}

		if(!bucketHash.isValid()) {
			throw new ResetMiningException("Not valid " + bucketHash.toStringFull());
		}
		
		if(getConsensusNoWait(bucketHash.getBinaryStringKey(), block.getBlockHash()) != null) {
			logger.trace("{} key='{}' ********************* initiatePerLastBlock() END ************************* {}", blockchainIndex, key, block.getBlockHash());
			return;
		}
		put(bucketHash);

		
		logger.trace("Initial proposal {} {}", blockchainIndex, bucketHash.toStringFull());
		if(power > 0) {
			ProposeMessage message = new ProposeMessage(bucketHash.cloneWithoutTransactions(), blockchainIndex);
			message.setReply(true);
			network.sendToAllPeersInBucket(power - 1, message);
		}
		
		if(bucketHash.isMined()) {
			network.sendToKey(bucketHash.getBinaryStringKey(), new SendBucketHashMessage(bucketHash, blockchainIndex));
		} else {
			logger.trace("{} {} recover() Not mined bucketHash.getKeyHash()={} bucketHash.isMined={}", blockchainIndex, bucketHash.getRealHashCode(), 
					bucketHash.getKeyHash(), bucketHash.isMined());
		}
		
		logger.trace("{} key='{}' ********************* initiatePerLastBlock() END ************************* {}", blockchainIndex, key, block.getBlockHash());
		
	}
	
	private void notifyConsensusReady() {
		
		if(!isConsensusReady() && blockchainIndex == blockchain.getIndex()) {
			return;
		}
		
		lock.lock();
		try {
			consensusReadyCondition.signal();
			logger.trace("{} Consensus after consensusReadyCondition.signal.", blockchainIndex);
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
		while (!isConsensusReady()) {
			waitForConsensusReady();
			if (blockchainIndex != blockchain.getIndex()) {
				throw new BlockchainIndexStaleException("Blockchain index is stale");
			}
		}
		logger.trace("{} To get consensus took {} ms.", blockchainIndex, System.currentTimeMillis() - start);
		
		return complete();
	}
	
	private boolean isConsensusReady() {
		
		if(readyBucketHashes != null) {
			return true;
		}
		
		Set<String> set = consensuses.getBySecondKey("").keySet();
		if(set.isEmpty()) {
			return false;
		}

		List<String> lastBlockHashes = blockchain.getLastBlockHashes();
		
		lastBlockHashes.retainAll(set);
		if(lastBlockHashes.isEmpty()) {
			return false;
		}
		return true;
	}
	
	private String getEarliestBlockHash() {
		List<String> lastBlockHashes = blockchain.getLastBlockHashes();
		
		if (blockchainIndex != blockchain.getIndex()) {
			throw new BlockchainIndexStaleException("Blockchain index is stale");
		}
		
		Set<String> set = consensuses.getBySecondKey("").keySet();
		for(String blockHash: lastBlockHashes) {
			if(set.contains(blockHash)) {
				return blockHash;
			}
		}
		logger.info("lastBlockHashes={}", lastBlockHashes);
		logger.info("consensusHashes={}", set);
		logger.info("blockchainIndex={}, blockchain.getIndex()={}", blockchainIndex, blockchain.getIndex());
		throw new ResetMiningException("Consensus state is stale, last blocks hashes do not contain consensus block hashes");
	}
	
	private BucketHashes computeBucketHashes() {
		String blockHash = getEarliestBlockHash();
		
		BucketHashes bucketHashes = new BucketHashes();
		
		BucketHash bucketHash = null;

		for(String bucketKey: bucketKeys) {
			bucketHash = consensuses.get(blockHash, bucketKey);
			if(bucketHash == null) {
				String str = String.format("bucketHash is null for blockHash %s and bucketKey %s", blockHash, bucketKey);
				throw new ResetMiningException(str);
			}
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
		long bits = previousBlock.getNextBits();
		block.setBits(bits);

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
			throw new BlockchainIndexStaleException("Blockchain index is stale");
		}
		
		logger.trace("{} ********************* Ready to mine *************************", blockchainIndex);
		logger.info("'{}'-{} Ready to mine # peers {} bucketPeers {} myPeers {} pp {} #c {} ap {} {}\n", block.getBucketKey(), block.getIndex(), Peer.getTotalPeerCount(), 
				network.getAllPeers().size(), network.getMyBucketPeers().size(), network.getPossiblePower(), numberOfConsensuses, blockchain.getAveragePower(), block);
		Listeners.getInstance().sendEvent(new BlockEvent(block, "Ready to mine"));
		
		for(BucketHash b: consensuses.getBySecondKey("").values()) {
			if(!b.isMined()) {
				logger.info("consensus isMined={}", b.isMined());
			}
		}
		

		if(Constants.showSum && myAddresses.get(0).equals(DhcAddress.getMyDhcAddress())) {
			Registry.getInstance().getTotalBalance().process();
		}


		if (blockchainIndex != blockchain.getIndex()) {
			throw new BlockchainIndexStaleException("Blockchain index is stale");
		}
		
		Registry.getInstance().getCompactor().addPrunings();
		
		block.setTimeStamp(System.currentTimeMillis());
		
		waitForMiningLock();
		
		if(blockchainIndex != blockchain.getIndex()) {
			String str = String.format("Blockchain index is stale blockchainIndex=%s, blockchain.getIndex()=%s", blockchainIndex, blockchain.getIndex());
			throw new BlockchainIndexStaleException(str);
		}
		
		block.setBlockHash();
		return block;
	}
	
	private void waitForMiningLock() {

		long index = blockchain.getIndex();
		if (blockchainIndex != index) {
			throw new BlockchainIndexStaleException("Blockchain index is stale");
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
	
	private void sendNextProposal(BucketHash bucketHash) {
		
		logger.trace("{} sendNextProposal() START bucketHash={}", blockchainIndex, bucketHash.getKeyHash());
		
		if("".equals(bucketHash.getBinaryStringKey())) {
			logger.info("************************************************");
			logger.info("bucketHash={}", bucketHash.toStringFull());
			return;
		}
		
		if(nextProposals.contains(bucketHash)) {
			return;
		}
		nextProposals.add(bucketHash);

		String consensusKey = dhcAddress.getBinary(bucketHash.getBinaryStringKey().length());
		
		BucketHash consensusHash = getConsensusNoWait(consensusKey, bucketHash.getPreviousBlockHash());
		if(consensusHash == null) {
			return;
		}
		BucketHash parentBucketHash = new BucketHash(consensusHash, bucketHash);
		
		logger.trace("{} {} sendNextProposal parentBucketHash {}", blockchainIndex, parentBucketHash.isMined(), parentBucketHash.toStringFull());
		
		long localIndex = blockchainIndex;
		BucketHash earlierBucketHash = findEarlierBuckethashWithTheSameKey(parentBucketHash);
		if(localIndex != blockchainIndex) {
			return;
		}
		
		if(earlierBucketHash != null && earlierBucketHash.hasBothChildren() && earlierBucketHash.isMined()) {
			if(earlierBucketHash.hasChild(consensusHash)) {
				logger.trace("{} {} sendNextProposal replace with earlier {}", blockchainIndex, earlierBucketHash.isMined(), earlierBucketHash.toStringFull());
				parentBucketHash = earlierBucketHash;
				bucketHash = parentBucketHash.getOtherChild(consensusHash);
				bucketHash = replace(bucketHash);
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
		} else {
			logger.trace("earlierBucketHash is null or not mined or does not have both children so cannot use it");
		}

		
		String nextConsensusKey = parentBucketHash.getBinaryStringKey();

		if(consensuses.get(bucketHash.getPreviousBlockHash(), nextConsensusKey) != null) {
			logger.trace("Returning because consensuses.get({}, {}) != null", bucketHash.getPreviousBlockHash(), nextConsensusKey);
			return;
		}
		
		if(!parentBucketHash.isMined()) {
			logger.trace("{} {} {} sendNextProposal() Not mined parentBucketHash.getKeyHash()={}", blockchainIndex, parentBucketHash.isMined(), parentBucketHash.getRealHashCode(), 
					parentBucketHash.getKeyHash());
			logger.trace("{} return from sendNextProposal() because can not process with non mined hash", blockchainIndex);
			return;
		}
		
		network.sendToKey(parentBucketHash.getBinaryStringKey(), new SendBucketHashMessage(parentBucketHash, blockchainIndex));
		
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
			logger.trace("{} return from sendNextProposal()", blockchainIndex);
			return;
		}
		String nextBucketKey = network.getBucketKey(i);
		if(nextBucketKey == null) {//this can happen if the number of buckets changed and there is no bucket with index i
			logger.trace("{} return from sendNextProposal() because nextBucketKey == null", blockchainIndex);
			return;
		}
		BucketHash bHash = consensuses.get(bucketHash.getPreviousBlockHash(), nextBucketKey);
		if(bHash == null) {
			logger.trace("{} return from sendNextProposal() because bHash == null", blockchainIndex);
			return;
		}
		String nextBucketHash = bHash.getHash();
		if(nextBucketHash == null) {
			logger.trace("{} return from sendNextProposal() because bHash == nextBucketHash", blockchainIndex);
			return;
		}
		sendNextProposal(bHash);
		
		logger.trace("{} sendNextProposal() END bucketHash={}", blockchainIndex, bucketHash.getKeyHash());
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
		if(blockchainIndex > index) {
			logger.trace("blockchainIndex {} > propose index {}", blockchainIndex, index);
			return;
		}
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		long duration;
		BucketHash hash;
		try {
			String blockHash = bucketHash.getPreviousBlockHash();
			String key = bucketHash.getBinaryStringKey();
			
			long bits = bucketHash.getBits();
			if(bits != 0 && bits <= Difficulty.INITIAL_BITS) {
				bits = Difficulty.convertDifficultyToBits(Difficulty.getDifficulty(bits) * Math.pow(2, bucketHash.getPower()));
				new MissingBlock(blockHash, index, bits);
			}
			
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

			logger.trace("{} {} Proposal bucketHash {}", index, bucketHash.isMined(), bucketHash.toStringFull());
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
			
			BucketHash buckethash = readyBucketHashes.getBucketHash("");

			if(buckethash.isMined()) {
				peer.send(new SendBucketHashMessage(buckethash, blockchainIndex));
			} else {
				logger.info("{} {} reply() Not mined buckethash.getKeyHash()={} buckethash.isMined={}", blockchainIndex, buckethash.getRealHashCode(), 
						buckethash.getKeyHash(), buckethash.isMined());
			}
			
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
	
	private BucketHash replace(BucketHash bucketHash) {
		BucketHash result = consensuses.replace(bucketHash);
		if("".equals(bucketHash.getBinaryStringKey()) || !consensuses.getBySecondKey("").isEmpty()) {
			notifyConsensusReady();
		}
		return result;
	}

	private void waitForConsensusReady() {
		logger.trace("{} ********************* waitForConsensusReady() START *************************", blockchainIndex);
		if (blockchainIndex != blockchain.getIndex()) {
			throw new BlockchainIndexStaleException("Blockchain index is stale");
		}
		try {
			long waitTime = Constants.MINUTE * 2;
			logger.trace("Consensus before consensusReadyCondition.await.");
			boolean waitExpired = false;
			lock.lock();
			try {
				waitExpired = !consensusReadyCondition.await(waitTime, TimeUnit.MILLISECONDS);
				logger.trace("{} ********************* waitForConsensusReady() END *************************", blockchainIndex);
				logger.trace("{} Consensus after consensusReadyCondition.await.", blockchainIndex);
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
		
		logger.trace("{} {} processReadyBucketHashes START consensus={}", index, consensus.isMined(), consensus.toStringFull());
		
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
		logger.trace("{} processReadyBucketHashesCallback START", index);
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

/*			if (bucketHashes.getNumberOfTransactions() == 0 && transactions != null && !transactions.isEmpty()) {

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
			}*/
			
			readyBucketHashes = bucketHashes;
			notifyConsensusReady();
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.trace("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
		logger.trace("processReadyBucketHashesCallback END");
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
		logger.trace("{} {} processReadyBucketHash START bucketHash={}", index, bucketHash.isMined(), bucketHash.toStringFull());
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
		logger.trace("{} {} processReadyBucketHash END bucketHash={}", index, bucketHash.isMined(), bucketHash.toStringFull());
	}
	
	private void recover(BucketHash bucketHash, long index) {
		logger.trace("{} {} recover START bucketHash={}", index, bucketHash.isMined(), bucketHash.toStringFull());

		Set<Transaction> transactions = bucketHash.getTransactionsIncludingCoinbase();
		
		if(transactions == null && !"".equals(bucketHash.getHash())) {
			return;
		}
		
		String key = bucketHash.getBinaryStringKey();
		
		String checkKey = key;
		
		while(!"".equals(checkKey)) {
			if(consensuses.get(bucketHash.getPreviousBlockHash(), checkKey) != null) {
				String myKey = DhcAddress.getMyDhcAddress().getBinary(power);
				if(myKey.startsWith(checkKey) && !myKey.equals(checkKey)) {
					logger.trace("Consensus.recover blockchainIndex: {}, bucketHash: {}", index, bucketHash.toStringFull());
					Registry.getInstance().getBucketConsensuses().recover(bucketHash, index);
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
		
		logger.trace("{} {} Consensus.recover {}", index, bucketHash.isMined(), bucketHash.toStringFull());
		
		String otherKey = null;
		BucketHash otherBucketHash =null;
		BucketHash parent = null;
		while(!key.equals(myKey)) {
			if(readyBucketHashes != null) {
				return;
			}
			BucketHash myBucketHash = new BucketHash(myKey, transactions, bucketHash.getPreviousBlockHash());
			Registry.getInstance().getBucketConsensuses().put(myBucketHash, index, true);
			myBucketHash = replace(myBucketHash);

			otherKey = myBucketHash.getKey().getOtherBucketKey().getKey();
			otherBucketHash = new BucketHash(otherKey, transactions, bucketHash.getPreviousBlockHash());
			Registry.getInstance().getBucketConsensuses().put(otherBucketHash, index, true);
			otherBucketHash = replace(otherBucketHash);
			logger.trace("{} {} Calling from recover() processPropose otherBucketHash={}", index, otherBucketHash.isMined(), otherBucketHash.toStringFull());
			processPropose(otherBucketHash, true, null, index);

			parent = new BucketHash(myBucketHash, otherBucketHash);
			parent.setTransactions(transactions);

			Registry.getInstance().getBucketConsensuses().put(parent, index, true);
			
			if(bucketHash.equals(parent)) {
				parent = bucketHash; //bucketHash is already mined so use it
			}
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
