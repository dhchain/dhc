package org.dhc.blockchain;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.dhc.gui.promote.JoinLine;
import org.dhc.lite.SecureMessage;
import org.dhc.persistence.BlockStore;
import org.dhc.persistence.ConnectionPool;
import org.dhc.persistence.TransactionInputStore;
import org.dhc.persistence.TransactionOutputStore;
import org.dhc.persistence.TransactionStore;
import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Registry;
import org.dhc.util.SharedLock;

public class Tree {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private long lastIndex;
	private final SharedLock readWriteLock = SharedLock.getInstance();
	private List<Block> lastBlocks;
	private int averagePower = -1;
	private int lastAveragePower;
	private int power;

	public boolean add(Node node) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		boolean result = true;;
		try {

			long localLastIndex = lastIndex;

			if (!node.isGenesis()) {
				Block parentBlock = getByHash(node.getBlock().getPreviousHash());
				if (parentBlock == null) {
					return false;
				}
				if (node.getBlock().getIndex() != parentBlock.getIndex() + 1) {
					return false;
				}
				Node parent = new Node();
				parent.setBlock(parentBlock);
				node.setParent(parent);
				parent.getChildren().add(node);
			}
			
			if(!node.getBlock().isPruned() && node.getBlock().getTransactions() != null) {
				TransactionMemoryPool.getInstance().removeAll(node.getBlock().getTransactions());
			}
			try {
				ConnectionPool.getInstance().begin();
				result = BlockStore.getInstance().saveBlock(node.getBlock());
				ConnectionPool.getInstance().commit();
			} catch (RuntimeException e) {
				logger.error(e.getMessage(), e);
				ConnectionPool.getInstance().rollback();
				remove(node.getBlock().getBlockHash());
				throw e;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				ConnectionPool.getInstance().rollback();
				logger.error("Could not save block {}", node.getBlock());
				remove(node.getBlock().getBlockHash());
				throw new RuntimeException(e);
			}
			
			if(result) {
				long index = node.getBlock().getIndex();
				setLastIndex(index);
				if(localLastIndex != index - 1) {
					long minCompeting = BlockStore.getInstance().getMinCompeting();
					if(minCompeting == 0) {
						BlockStore.getInstance().setMinCompeting(index);
					} else {
						BlockStore.getInstance().setMinCompeting(Math.min(index, minCompeting));
					}
				}
				Registry.getInstance().getCompactor().pruneBlockchain();
			}
			
			return result;
		} finally {
			writeLock.unlock();long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public List<Block> getByIndex(long index) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			List<Block> list = BlockStore.getInstance().getByIndex(index);
			return list;
		} finally {
			readLock.unlock();long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
		
	}

	public Block getByHash(String hash) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return BlockStore.getInstance().getByBlockhash(hash);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public long getLastIndex() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return lastIndex;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	private void setLastIndex(long lastIndex) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			if (lastIndex > this.lastIndex) {
				this.lastIndex = lastIndex;
			}
			lastBlocks = null;
			averagePower = -1;
			lastAveragePower = BlockStore.getInstance().getLastAveragePower(this.lastIndex);
			power = BlockStore.getInstance().getMaxPower();
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public void resetLastIndex() {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			lastIndex = BlockStore.getInstance().getLastIndex();
			lastBlocks = null;
			averagePower = -1;
			lastAveragePower = BlockStore.getInstance().getLastAveragePower(lastIndex);
			power = BlockStore.getInstance().getMaxPower();
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public void remove(String blockhash) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			BlockStore.getInstance().removeBlock(blockhash);
			TransactionMemoryPool.getInstance().removeByOutputBlockhash(blockhash);
			ConnectionPool.getInstance().commit();
			resetLastIndex();
		} catch (Exception e) {
			logger.info(e.getMessage(), e);
			ConnectionPool.getInstance().rollback();
		} finally {
			writeLock.unlock();long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public void replace(Block combinedBlock) throws Exception {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			BlockStore.getInstance().replace(combinedBlock);
			ConnectionPool.getInstance().commit();
			power = BlockStore.getInstance().getMaxPower();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			ConnectionPool.getInstance().rollback();
			logger.error("Error replacing combinedBlock {}", combinedBlock);
			throw e;
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
		boolean result = false;
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			result = TransactionStore.getInstance().saveTransaction(transaction);
			ConnectionPool.getInstance().commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("Error saving transaction {}", transaction);
			ConnectionPool.getInstance().rollback();
			throw e;
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
		return result;
	}

	public boolean contains(Transaction transaction) {
		return TransactionStore.getInstance().contains(transaction);
	}

	public List<Block> getLastBlocks() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			if (lastBlocks == null || lastBlocks.isEmpty()) {
				lastBlocks = BlockStore.getInstance().getLastBlocks();
			}
			return lastBlocks;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public int getPower() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return power;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public void pretrim() {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			Set<String> list = BlockStore.getInstance().getOrphants(lastIndex);
			if (list.isEmpty()) {
				return;
			}
			logger.info("Pretrim blocks {}", list);
			try {
				ConnectionPool.getInstance().begin();
				for (String blockHash : list) {
					BlockStore.getInstance().removeBlock(blockHash);
					Registry.getInstance().getBannedBlockhashes().add(blockHash);
				}
				ConnectionPool.getInstance().commit();
				resetLastIndex();
			} catch (Exception e) {
				logger.info(e.getMessage(), e);
				ConnectionPool.getInstance().rollback();
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
	
	public List<Block> getByPreviousHash(String previousHash) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return BlockStore.getInstance().getByPreviousHash(previousHash);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public List<String> getBlockhashesByPreviousHash(String previousHash) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return BlockStore.getInstance().getBlockhashesByPreviousHash(previousHash);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	
	public int getLastAveragePower() {
		return lastAveragePower;
	}
	
	public int getAveragePower() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			if(averagePower == -1) {
				averagePower = BlockStore.getInstance().getAveragePower(lastIndex);
			}
			ConnectionPool.getInstance().commit();
			return averagePower;
		} catch (Exception e) {
			ConnectionPool.getInstance().rollback();
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public boolean contains(String blockhash) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			ConnectionPool.getInstance().begin();
			boolean result =  BlockStore.getInstance().contains(blockhash);
			ConnectionPool.getInstance().commit();
			return result;
		} catch (Exception e) {
			ConnectionPool.getInstance().rollback();
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public void addPendingCrossShardTransaction(Transaction transaction) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			Registry.getInstance().getPendingCrossShardTransactions().process(transaction);
		} finally {
			writeLock.unlock();
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
			ConnectionPool.getInstance().begin();
			boolean result = TransactionStore.getInstance().saveTransactions(transactions);
			ConnectionPool.getInstance().commit();
			return result;
		} catch (Exception e) {
			ConnectionPool.getInstance().rollback();
			logger.error(e.getMessage(), e);
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
		return false;
	}
	
	public Set<TransactionInput> getByOutputId(String outputId) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return TransactionInputStore.getInstance().getByOutputId(outputId);
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
			return TransactionStore.getInstance().getTransaction(transactionId);
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
			return BlockStore.getInstance().getBranchBlockhashes(block);
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
			return BlockStore.getInstance().getAncestor(block, depth);
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
			return TransactionStore.getInstance().getPublicKey(address);
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
			return TransactionStore.getInstance().getTransactionsByAddress(address);
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
			return TransactionOutputStore.getInstance().getTransactionOutputs(dhcAddress);
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
			return TransactionStore.getInstance().getTransactionsForApp(app, address);
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
			return TransactionStore.getInstance().getJoinLines(address);
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
			return BlockStore.getInstance().getAverageMiningTime(block);
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
			return BlockStore.getInstance().getAverageBits(block);
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
			return BlockStore.getInstance().getNextBits(blockhash);
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
			return BlockStore.getInstance().getBits(blockhash);
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
			return TransactionStore.getInstance().getSecureMessages(dhcAddress);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public PublicKey getPublicKey(DhcAddress dhcAddress) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return TransactionStore.getInstance().getPublicKey(dhcAddress);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public Set<Transaction> getFindFaucetTransactions(DhcAddress dhcAddress, String ip) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return TransactionStore.getInstance().getFindFaucetTransactions(dhcAddress, ip);
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
