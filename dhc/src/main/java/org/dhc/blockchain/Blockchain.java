package org.dhc.blockchain;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;

import org.dhc.gui.promote.JoinLine;
import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.consensus.BucketHash;
import org.dhc.network.consensus.Consensus;
import org.dhc.persistence.BlockStore;
import org.dhc.persistence.ConnectionPool;
import org.dhc.persistence.TransactionOutputStore;
import org.dhc.persistence.TransactionStore;
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
		block.setBits(Difficulty.getBits());

		try {
			
			MyAddresses myAddresses = new MyAddresses();

			Transaction transaction = new Transaction();
			
			int i = 0;
			
			TransactionOutput output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("DNiMXMBFXw4vPSSVYsRmKDgFaSvfyT4RGx6yZWwYdbkx");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("6PYyPvY9LiM8rWVvjiWpc8onVJiA7J6tvuwFBn2e4daj");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("CUhgqbPNk72BpVEWzqiYPMdf4dK9PKVnbvSgt3au8V98");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("Av55WA5ed1fixLjVTc1r1JrRE9VaSwaFLbzUEZBwUcFt");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("EeE7L3nu2iLbtLEPj3XfUDd6QbTkhLrJvBYb93LNjb25");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("BAg3xhro4XvaeXpG9BbVNVCf5cafqT3McpsiMR4s7GM5");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("4Y5CqPJ1dyKLSCEwn63hMJSGi6HVfA3bDomDzmqX4qoy");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("GgcemwSPjrsTyYwR9F8QjsE3tmuDsegMLmRzT9goLhiX");
			transaction.getOutputs().add(output);
			
			transaction.setValue(transaction.getOutputsValue());
			transaction.setFee(Coin.ZERO);
			transaction.setBlockHash(block.getBlockHash(), block.getIndex());
			transaction.setSender(block.getMiner());
			transaction.setReceiver(transaction.getSenderDhcAddress());
			// comment out START
/*				transaction.signTransaction(Wallet.getInstance().getPrivateKey());
				transaction.setTransactionId();
			    logger.info("transaction signature={}", transaction.getSignature());*/
			// comment out END
			transaction.setSignature("381yXYxjxWa8x1m1WC2gUtT2AyHykwVQeoMfuAFKiFUTnvZJZAe2RTheFPXDsBhDHtDBLq1hCcX39CwpL89qkGLMmAPciyzC");
			transaction.setTransactionId();

			logger.info("transaction signature valid={}", transaction.verifySignature());
			
			BucketHash buckethash = new BucketHash();
			buckethash.setBinaryStringKey("");
			buckethash.addTransaction(transaction);
			buckethash.recalculateHashFromTransactions();

			BucketHashes bucketHashes = new BucketHashes();
			bucketHashes.put(buckethash);
			block.setBucketHashes(bucketHashes);
			
			// comment out START
/*				block.setBlockHash(CryptoUtil.getHashBase58Encoded(""));
				block.setTimeStamp(System.currentTimeMillis());
				block.mine();*/
			// comment out END
			
			block.setBlockHash("11XV1g9ZFzAd6HKds7BCsBzqvV12dxNWFWVmn4tMAHe");
			block.setTimeStamp(1639169236052L);
			block.setNonce(88960);
			
			// comment out START
/*				logger.info("block signature={}", block.getMinerSignature());
				logger.info("block signature valid={}", block.verifySignature());
				logger.info("nonce={}, timestamp={}", block.getNonce(), block.getTimeStamp());*/
			// comment out END
			block.setMinerSignature("AN1rKvtPitHqWtbjJv28ofrZA52CLACT62KTMWMaZ2VFuwcURLY6cRcSzbChpkZnYW4aUHgz2npAxJqvJarbTsXV6X7sASkxF");
			block.setBlockHash();
			buckethash.setPreviousBlockHash(block.getPreviousHash());
			
			logger.info("block signature valid={}", block.verifySignature());
			
			add(block);
			
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

			Node node = new Node();
			node.setBlock(block);
			if (!tree.add(node)) {
				synchronized (pendingNodes) {
					pendingNodes.put(block.getPreviousHash(), block.getBlockHash(), node);
				}
				return false;
			}

			logger.info("'{}'-{} Added block {}", block.getBucketKey(), block.getIndex(), block);
			Listeners.getInstance().sendEvent(new BlockEvent(block, "Added"));

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
			throw new RuntimeException("getByHash c " + block);
		}
		return block;
	}

	public List<Block> getByPreviousHash(String previousHash) {
		return tree.getByPreviousHash(previousHash);
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

	public void remove(Block block) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			tree.remove(block);
			logger.info("{}-{} Removed block {}", block.getBucketKey(), block.getIndex(), block);
			Registry.getInstance().getBannedBlockhashes().add(block.getBlockHash());
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
			Block block = getByHash(blockHash);
			if(block == null) {
				return;
			}
			List<Block> blocks = getByPreviousHash(blockHash);
			for(Block b: blocks) {
				removeBranch(b.getBlockHash());
			}
			remove(block);
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
			List<Block> blocks = getBlocks(index);
			for(Block block: blocks) {
				removeBranch(block.getBlockHash());
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

	public long getAverageMiningTime() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getAverageMiningTime();
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public long getAverageBits() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return tree.getAverageBits();
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

}
