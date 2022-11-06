package org.dhc.blockchain;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;

import org.dhc.gui.promote.JoinLine;
import org.dhc.lite.SecureMessage;
import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.consensus.BucketHash;
import org.dhc.network.consensus.Consensus;
import org.dhc.persistence.BlockStore;
import org.dhc.persistence.ConnectionPool;
import org.dhc.persistence.TransactionOutputStore;
import org.dhc.persistence.TransactionStore;
import org.dhc.plugin.PluginRegistry;
import org.dhc.util.BlockEvent;
import org.dhc.util.Coin;
import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Difficulty;
import org.dhc.util.DoubleMap;
import org.dhc.util.Listeners;
import org.dhc.util.Registry;
import org.dhc.util.SharedLock;
import org.dhc.util.ThreadExecutor;

public class Blockchain {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final Blockchain instance = new Blockchain();
	
	public static Blockchain getInstance() {
		return instance;
	}
	
	private Tree tree = new Tree();
	private DoubleMap<String, String, Node> pendingNodes = new DoubleMap<>();
	private Map<Long, Long> pendingIndexes = new HashMap<>();
	private Object consensus;
	private boolean started;
	private BlockingQueue<Node> queue = new LinkedBlockingQueue<>();
	private final SharedLock readWriteLock = SharedLock.getInstance();

	public void setConsensus(Object consensus) {
		this.consensus = consensus;
	}

	private Blockchain() {
		init();
	}
	
	private void pendingNodesLoader() {
		ThreadExecutor.getInstance().execute(new DhcRunnable("pendingNodesLoader") {
			public void doRun() {
				while(true) {
					Node node = null;
					try {
						node = queue.take();
						addChildren(node);
					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		});
	}
	
	private void init() {
		PluginRegistry.getInstance().init();
		pendingNodesLoader();
		tree.resetLastIndex();
		long lastIndex = tree.getLastIndex();
		if(lastIndex > 0) {
			logger.info("Blockchain initialized, last index {}", lastIndex);
			return;
		}
		Block block = new Block();
		block.setIndex(0);
		block.setMiner(Constants.PUBLIC_KEY);
		block.setBits(Difficulty.getBits(block));

		try {
			
			MyAddresses myAddresses = new MyAddresses();

			Transaction transaction = new Transaction();
			transaction.setType(TransactionType.GENESIS);
			
			int i = 0;
			
			TransactionOutput output = new TransactionOutput(myAddresses.get(i++), new Coin(Long.MAX_VALUE));
			output.setOutputId("DNiMXMBFXw4vPSSVYsRmKDgFaSvfyT4RGx6yZWwYdbkx");
			transaction.getOutputs().add(output);
			
			transaction.setValue(transaction.getOutputsValue());
			transaction.setFee(Coin.ZERO);
			transaction.setBlockHash(block.getBlockHash(), block.getIndex());
			transaction.setSender(block.getMiner());
			transaction.setReceiver(transaction.getSenderDhcAddress());
			transaction.setSignature("AN1rKoRqz88686BLMJk3Qc3N3MkB9LT9gS4ZZVVr6SC2csF1oms3f1GdETMgnXsmPFzbVRxSA7FnyhchoUivpWe6zgfXnue96");
			transaction.setTransactionId();

			logger.info("transaction signature valid={}", transaction.verifySignature());
			
			BucketHash buckethash = new BucketHash();
			buckethash.setBinaryStringKey("");
			buckethash.addTransaction(transaction);
			buckethash.recalculateHashFromTransactions();
			buckethash.setNonce(263982);
			buckethash.setTimestamp(1639450588766L);
			buckethash.setBits(Difficulty.INITIAL_BITS);

			BucketHashes bucketHashes = new BucketHashes();
			bucketHashes.put(buckethash);
			block.setBucketHashes(bucketHashes);
			
			block.setBlockHash("11RLNshrqDRuKWGs6gU7pG7BTxRcZce3McDnEW1gsqG");
			block.setTimeStamp(1639450594080L);
			block.setNonce(15574);

			block.setMinerSignature("iKx1CJN1oQsDuhJpw9ufuJ173js2o6xW1ZvKPNQB2hgMJ4rbQwmES9u8zyZfigYJQSjh8kCRPybTof82P4svQ6LYuQWw6gUBmu");
			block.setBlockHash();
			buckethash.setPreviousBlockHash(block.getPreviousHash());
			
			logger.info("block signature valid={}", block.verifySignature());
			
			add(block);
			
			PluginRegistry.getInstance().init();
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}

		logger.info("Blockchain initialized");
	}
	
	private void addChildren(Node parent) {

		List<Node> list = getPendingChildren(parent);
		while (!list.isEmpty()) {
			Node child = list.remove(0);
			if (tree.add(child)) {
				Block block = child.getBlock();
				logger.info("'{}'-{} Added block {}", block.getBucketKey(), block.getIndex(), block);
				list.addAll(getPendingChildren(child));
			}
		}

	}
	
	private List<Node> getPendingChildren(Node parent) {
		List<Node> list = new ArrayList<>();
		String hash = parent.getBlock().getBlockHash();
		synchronized(pendingNodes) {
			Map<String, Node> map = pendingNodes.removeByFirstKey(hash);// get nodes with previousHash equal this hash, in other words, get children of the passed parameter parent
			if(map != null && !map.isEmpty()) {
				list.addAll(map.values());
			}
			for(Node node: list) {
				pendingIndexes.remove(node.getBlock().getIndex());
			}
		}
		
		return list;
	}
	
	public boolean add(Block block) {

		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {

			block = block.clone();

			if (!block.isMine()) {
				logger.info("**********************************************************************");
				logger.info("Not my block {}", block);
				return false;
			}

			long index = getIndex();

			if (contains(block.getBlockHash())) {
				return false;
			}

			block.setBlockHash();

			block.cleanCoinbase();

			if (!block.isValid()) {
				logger.info("**********************************************************************");
				logger.info("Block is not valid {}", block);
				return false;
			}

			if (!block.isPruned() && !block.getBucketHashes().isFeeValid()) {
				logger.info("Fee is not valid");
				logger.info("CoinbaseTransactionId {}, Coinbase {}", block.getCoinbaseTransactionId(), block.getCoinbase());
				// return false;
			}
			
			if (!block.isPruned()) {
				block.getBucketHashes().cleanTransactions();
			}
			
			logger.trace("Has coinbase: {}", block.getCoinbase());
			
/*			if(block.getCoinbase() == null) {
				logger.info("Coinbase is null", new RuntimeException());
			}*/

			Node node = new Node();
			node.setBlock(block);
			if (!tree.add(node)) {
				synchronized (pendingNodes) {
					pendingNodes.put(block.getPreviousHash(), block.getBlockHash(), node);
					pendingIndexes.put(block.getIndex(), block.getIndex());
				}
				return false;
			}

			logger.info("'{}'-{} Added block {}", block.getBucketKey(), block.getIndex(), block);
			if(ChainSync.getInstance().isRunning() && block.getIndex() % 100 == 0) {
				Listeners.getInstance().sendEvent(new BlockEvent(block, "Added"));
			}
			try {
				queue.put(node);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
			if (getIndex() == index) {
				return true;// index was not increased so no need to wake up consensus or reset mining
			}
			notifyConsensus();
			
			long lastIndex = Math.max(getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
			if(block.getIndex() == lastIndex) {
				Network.getInstance().sendToAllMyPeers(new SendSyncMyBlockMessage(getByHash(block.getPreviousHash())));
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
	
	private void notifyConsensus() {
		Object object = this.consensus;
		if(object != null) {
			Consensus consensus = (Consensus) object;
			consensus.notifyMiningLock();
		}
	}
	
	public boolean isValid() {
		return true;
	}
	
	public List<Block> getLastBlocks() {
		List<Block> list = tree.getLastBlocks();
		
		return list;
	}
	
	public List<String> getLastBlockHashes() {
		List<Block> list = getLastBlocks();

		List<String> result = new ArrayList<String>();
		for(Block block: list) {
			result.add(block.getBlockHash());
		}
		return result;
	}
	
	public long getIndex() {
		return tree.getLastIndex();
	}

	public boolean contains(String blockhash) {
		return tree.contains(blockhash);
	}

	public Block getByHash(String hash) {
		if(hash == null) {
			return null;
		}
		Block block = tree.getByHash(hash);
		if(block!= null && !block.isValid()) {
			throw new RuntimeException("getByHash invalid block " + block);
		}
		return block;
	}

	public List<Block> getByPreviousHash(String previousHash) {
		return tree.getByPreviousHash(previousHash);
	}
	
	public List<String> getBlockhashesByPreviousHash(String previousHash) {
		return tree.getBlockhashesByPreviousHash(previousHash);
	}

	private void sync() {
		ChainSync.getInstance().sync();
	}
	
	public void syncAsync() {
		ThreadExecutor.getInstance().execute(new DhcRunnable("ChainSynchronizer sync") {
			public void doRun() {
				sync();
			}
		});
	}

	public List<Block> getBlocks(long index) {
		return tree.getByIndex(index);
	}

	public void remove(String blockhash) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			tree.remove(blockhash);
			logger.info("Removed blockhash {}", blockhash);
			Registry.getInstance().getBannedBlockhashes().add(blockhash);
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public void start() {
		sync();
		started = true;
	}
	
	public int getAveragePower() {
		return tree.getAveragePower();
	}
	
	public int getLastAveragePower() {
		return tree.getLastAveragePower();
	}

	public void replace(Block combinedBlock) throws Exception {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			tree.replace(combinedBlock);
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public boolean addTransaction(Transaction transaction) throws Exception {
		return tree.addTransaction(transaction);
	}

	public boolean contains(Transaction transaction) {
		return tree.contains(transaction);
	}

	public int getNumberOfPendingBlocks() {
		synchronized(pendingNodes) {
			return pendingNodes.size();
		}
	}

	public void removeBranch(String blockHash) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			if(!contains(blockHash)) {
				return;
			}
			List<String> blockhashes = getBlockhashesByPreviousHash(blockHash);
			for(String hash: blockhashes) {
				removeBranch(hash);
			}
			remove(blockHash);
			notifyConsensus();
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public void pretrim() {
		tree.pretrim();
		
	}

	public void removeByIndex(long index) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			long blockchainIndex  = getIndex();
			while(index <= blockchainIndex) {
				List<Block> blocks = getBlocks(blockchainIndex);
				for(Block block: blocks) {
					removeBranch(block.getBlockHash());
				}
				blockchainIndex  = getIndex();
			}
			
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public boolean isStarted() {
		return started;
	}
	
	public int getQueueSize() {
		return queue.size();
	}
	
	public Set<TransactionInput> getInputs(Coin coin, Block block) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			Set<TransactionInput> inputs = TransactionOutputStore.getInstance().getInputs(coin, block);
			ConnectionPool.getInstance().commit();
			return inputs;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			ConnectionPool.getInstance().rollback();
			throw e;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public Set<TransactionInput> getInputs(Block block, String recipient) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			Set<TransactionInput> inputs = TransactionOutputStore.getInstance().getInputs(block, recipient);
			ConnectionPool.getInstance().commit();
			return inputs;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			ConnectionPool.getInstance().rollback();
			throw e;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public Coin sumByRecipient(String recipient, Block block) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			Coin result = TransactionOutputStore.getInstance().sumByRecipient(recipient, block);
			ConnectionPool.getInstance().commit();
			return result;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public Coin getBalanceForAll(String blockhash, String key) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			Coin result = TransactionOutputStore.getInstance().getBalanceForAll(blockhash, key);
			ConnectionPool.getInstance().commit();
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			ConnectionPool.getInstance().rollback();
			throw e;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public Transaction getTransaction(String outputTransactionId, String outputBlockHash) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			Transaction result = TransactionStore.getInstance().getTransaction(outputTransactionId, outputBlockHash);
			ConnectionPool.getInstance().commit();
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			ConnectionPool.getInstance().rollback();
			throw e;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
		
	}

	public Set<String> getPruningRecipients() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			Set<String> result = TransactionOutputStore.getInstance().getPruningRecipients();
			ConnectionPool.getInstance().commit();
			return result;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public long getMinUnprunedIndex() {
		return BlockStore.getInstance().getMinUnprunedIndex();
	}

	public void addPendingCrossShardTransaction(Transaction transaction) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();

		try {
			tree.addPendingCrossShardTransaction(transaction);
			
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			// logger.trace("unlock");
		}

	}
	
	public String getBucketKey(String blockhash) {
		return BlockStore.getInstance().getBucketKey(blockhash);
	}
	
	public int getPower() {
		
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getPower();
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public boolean saveTransactions(Set<Transaction> transactions) {
		
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();

		try {
			return tree.saveTransactions(transactions);
			
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			// logger.trace("unlock");
		}
		
	}
	
	public Set<TransactionInput> getByOutputId(String outputId) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getByOutputId(outputId);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public Set<Transaction> getTransaction(String transactionId) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getTransaction(transactionId);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public Set<String> getBranchBlockhashes(Block block) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getBranchBlockhashes(block);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public String getAncestor(Block block, long depth) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getAncestor(block, depth);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public PublicKey getPublicKey(String address) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getPublicKey(address);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public Set<Transaction> getTransactions(DhcAddress address) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getTransactions(address);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public Set<TransactionOutput> getTransactionOutputs(DhcAddress dhcAddress) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getTransactionOutputs(dhcAddress);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public Set<Transaction> getTransactionsForApp(String app, DhcAddress address) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getTransactionsForApp(app, address);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public Set<JoinLine> getJoinLines(DhcAddress address) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getJoinLines(address);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public long getAverageMiningTime(Block block) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getAverageMiningTime(block);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public long getAverageBits(Block block) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getAverageBits(block);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public long getNextBits(String blockhash) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getNextBits(blockhash);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public long getBits(String blockhash) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getBits(blockhash);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public List<SecureMessage> getSecureMessages(DhcAddress dhcAddress) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getSecureMessages(dhcAddress);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public PublicKey getPublicKey(DhcAddress address) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getPublicKey(address);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public boolean pendingIndexesHasIndex(long index) {
		synchronized(pendingNodes) {
			return pendingIndexes.containsKey(index);
		}
	}

	public Set<Transaction> getFindFaucetTransactions(DhcAddress dhcAddress, String ip) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getFindFaucetTransactions(dhcAddress, ip);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

}
