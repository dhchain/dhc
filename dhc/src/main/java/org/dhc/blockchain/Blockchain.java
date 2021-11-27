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
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("82n7H7SartN39soihEZzwvQ9sWpAX8gRSyJQJvPwS3ys");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("Etc8sTEiqfLPMf8A8ftC8XBnH4FHUHvHeBNsQb2spd9k");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("HxuxPhQmozKfrmXA4CJnKJUGZwYbe41HxfvrTqinAZUz");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("D9CGB6QvPvaaPUZZqzHhombBLYCs29Sw5Cs27UiuKAKw");		
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("ArEuNqdiGpDsV9FCez7dNSKyXPFCab4nUnXkAQz1AWdq");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("BLyAEwQF2juDk6SCAnpq6n52RFUQtU3iMmPPkuvgDxGq");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("C51koCct1wQYgQRMcLmPra7psU8oNiXUy7xHLcy2edz7");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("8gmJ6DBHbP12LwVuHPWjh9dDdzVGsuxwyrpRXiJhnMoW");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("ExukyYSevo9Q7Ucqkwrr8kV3BUoGiYK3voG1A1tbZoGC");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("FNA2MX9euGaWUmid1XdXvFXLfcrYVXRyJvELCvw4Kx3n");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("GbTNXUnHCwxLjL1jTyoAEfaYpEpYCSdZTtAc6gJagYkZ");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("8Pr5bzVfdFHZtc7Ca86Rio1xJusNJREjbYfgcSAXogyM");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("C6atr81yHsA1jfhPfkY8GyLAwGHE1fZkxdh3kngakUWW");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("FxtVbJ3XymAFAoqY2jTfUMxwfEbaaZrccxnEBUgaZyFG");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("6fZaDYrhG51ZkD9gGWQ3i1V4YhJpgH9fbVyrvV6hNndc");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("HgorqbfsorPj1Q49vR7jKkTVRngoXTQRYA3BvhLu2aFg");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("5ck6zH5oSMWhCPHC8ZuxXM6ReaU2AmtsBsow2M3tXdTi");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("CuBjyTCMBQqMHv1tsbxzDwbF5FTPHyRMPNN1AKfhY7iJ");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("5hJyTRX9pnDT3wRJ6hTZhNXMYXnbhN3DLinDsPzpdXbT");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("EGaknhdWrRMvY1AxFxyNJufUJTgaURv2ZzGshuSzRFro");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("61mDVzd6DUuENtCM9LRDEyydAQxCfG8GbStU4VhLSmDP");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("9f6QUXFdbSinwPRoTCGh1nmzooET34W7j2k2b2wAphAi");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("EdaKEwCQbrHTLhJ6becjrwNKbHfYovrdKQpCrQyi5nSK");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("8ARKHyFaTBPjEysaFPVXbSL2sHguicdvG8mYg2mW6CEV");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("9ARdLMN8E76kpAcjngZ3nKnMnFE1XqK98Y7nNM5QaKnh");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("AinRMM8TxcVbmDmspgHt6QWNVdUKeVZQA49wQw3qSVQN");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("DZ66TnAMrngc91L7mhqUuAKz4YeH4StBNXGYHc5ME3ZF");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("Bc13VvBrNy7vhbKcHjyhoRKpAn7QqZ2gzhjC8qDiWdZx");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("EzGmKGm19VavAL62ULR9q5FBBJ3UGiAd9QonG2Etu2yk");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("3TLeTjjMZFnJJCswwxD885WsKWYg9sxsWBXwbZDsJcwT");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("JBTRf3cyjpiKtvFswxnjZf3EJHSdBHm4EixULMnEAX3b");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("AvrsdxUcsyys7fzoWEn3SGbowwQhe5oexoU89rakbbxi");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("7ZsdaTZQ3GW51muhvDxzqMBZSDQhQAXyc5RJpgwQEZwR");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("9rszy4Ax3f1A5SwBJcbJK4mPKgAjB8YcLM54CTwHiVXs");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("DgyLh5HU1RQVxubYWCj5sKDjT15yxU6UUVF8tjyFZTz4");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("3tfHxowQjSjQmLfG7hJB8wuB2MZ8Zkksfg3g7gHrQxNz");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("CghyUnuK6R1YKaykcqPcpyxDVenEpbXUSLZGzVDcyVA7");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("DQZTsDpGbJ9WrVfACVRdZ8gfQ6vh3eAefstbPRXx5oTL");
			transaction.getOutputs().add(output);

			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("9yyNAsiADMwSRQkwiAzbqTDLwbCcqHxjydu7UbSNnjLV");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("ZAedjvohoYkWmBWKSGkerp9yhiJnpFJoH5KJFwa4qAc");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("4T9khLhAoLLt3QL5HDvVEPYhck4fTyNJqxpxAoAGqJfU");
			transaction.getOutputs().add(output);
			
			output = new TransactionOutput(myAddresses.get(i++), Coin.ONE.multiply(1000000));
			output.setOutputId("FzpypsERB7kKwBRYe7nmJmr8MrnC7J7tP51unpFxyNUG");
			transaction.getOutputs().add(output);

			transaction.setValue(transaction.getOutputsValue());
			transaction.setFee(Coin.ZERO);
			transaction.setBlockHash(block.getBlockHash(), block.getIndex());
			transaction.setSender(block.getMiner());
			transaction.setReceiver(transaction.getSenderDhcAddress());
			// comment out START
				//transaction.signTransaction(Wallet.getInstance().getPrivateKey());
				//transaction.setTransactionId();
			    //logger.info("signature={}", transaction.getSignature());
			// comment out END
			transaction.setSignature("iKx1CJMpGMdZZjsszCrtdDkh5MJytSr6aUCxntL2Acn567C9XQEHBDwhSoTMWPonhZfidZK7KHrN3qTHUGKSZ9cM3faHaeGTsQ");
			transaction.setTransactionId();
			
			BucketHash buckethash = new BucketHash();
			buckethash.setBinaryStringKey("");
			buckethash.addTransaction(transaction);
			buckethash.recalculateHashFromTransactions();

			BucketHashes bucketHashes = new BucketHashes();
			bucketHashes.put(buckethash);
			block.setBucketHashes(bucketHashes);
			
			// comment out START
			    //block.sign();
				//logger.info("block signature={}", block.getMinerSignature());
				//logger.info("block signature valid={}", block.verifySignature());
			// comment out END
			block.setMinerSignature("iKx1CJNXqvN3cKXRSdwKeeti9KPtYmtYRHHnBJ9Wd9XeUvWtCCgAs89c7NNWBgcXZG4u1xp3U1TY2tYnSpT74sCiQbZTcL6EFQ");
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

}
