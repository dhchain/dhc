package org.dhc.persistence;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.BucketHashes;
import org.dhc.blockchain.Trimmer;
import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.util.Base58;
import org.dhc.util.BoundedMap;
import org.dhc.util.Constants;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcLogger;
import org.dhc.util.Difficulty;
import org.dhc.util.Registry;

public class BlockStore {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static BlockStore instance = new BlockStore();

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private long minCompeting = -1;
	private final BoundedMap<String, Block> latestCachedBlocks = new BoundedMap<>(10);
	private final AtomicLong id = new AtomicLong();

	private BlockStore() {
		try {
			verifyTable();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static BlockStore getInstance() {
		return instance;
	}
	
	private void setId() {

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select coalesce(max(block_id) + 1, 0) as id from block";
					ps = conn.prepareStatement(sql);
					rs = ps.executeQuery();
					if (rs.next()) {
						id.set(rs.getLong("id"));
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.trace("************************ id was set to {}", id.get());

	}

	public boolean saveBlock(Block block) throws Exception {
		long start = System.currentTimeMillis();
		if(!doSaveBlock(block)) {
			return false;
		}
		
		updateNextBits(block);
		
		BucketHashStore.getInstance().saveBucketHashes(block.getIndex(), block.getBlockHash(), block.getBucketHashes());
		TransactionStore.getInstance().saveTransactions(block.getAllTransactions());

		long minCompeting = getMinCompeting();
		if(minCompeting != 0) {
			logger.info("minCompeting={}", minCompeting);
			Blockchain blockchain = Blockchain.getInstance();// blockchain might return null if not initialized yet, so check if it is not null
			if (blockchain != null && blockchain.getIndex() > minCompeting + 10) {
				if(!ChainSync.getInstance().isRunning()) {
					Trimmer.getInstance().runAsync();
				} else if(blockchain.getIndex() > minCompeting + 10000) {
					Trimmer.getInstance().runAsync();
				}
			}
		}

		Registry.getInstance().getPendingCrossShardTransactions().addCrossShardTransactions(block.getBlockHash());
		Registry.getInstance().getPendingTransactions().process(block.getIndex());
		
		TransactionDataStore.getInstance().remove();
		
		latestCachedBlocks.put(block.getBlockHash(), block);
		
		logger.trace("Function saveBlock took {} ms.", System.currentTimeMillis() - start);
		
		return true;
	}
	
	private void updateNextBits(Block block) throws Exception {
		long blockNextBits = block.getNextBits();
		long calculatedNextBits;
		if(blockNextBits != 0) {
			calculatedNextBits = Difficulty.getBits(block);
			if(blockNextBits != calculatedNextBits) {
				logger.info("blockNextBits={} != calculatedNextBits={}", blockNextBits, calculatedNextBits);
				throw new RuntimeException("nextbits are not correct");
			}
			return;
		}
		calculatedNextBits = Difficulty.getBits(block);
		block.setNextBits(calculatedNextBits);
		new DBExecutor() {
			public void doWork() throws Exception {
				String sql = "update block set nextBits = ? where blockhash = ?";
				ps = conn.prepareStatement(sql);
				int i = 1;
				ps.setLong(i++, block.getNextBits());
				ps.setString(i++, block.getBlockHash());
				long start = System.currentTimeMillis();
				ps.executeUpdate();
				logger.trace("Query updateNextBits took {} ms. '{}' ", System.currentTimeMillis() - start, sql);
			}
		}.execute();
	}

	private boolean doSaveBlock(Block block) throws Exception {
		long start = System.currentTimeMillis();
		if (contains(block.getBlockHash())) {
			return false;
		}
		if (!block.isValid()) {
			logger.info("*********************************************************");
			logger.info("Block is invalid {}", block);
			return false;
		}
		if (!block.isBranchValid()) {
			return false;
		}
		
		if (!block.hasOutputsForAllInputs()) {
			return false;
		}
		
		String previousHash = block.getPreviousHash();
		
		long nextBits = getNextBits(previousHash);
		if(!block.isGenesis() &&  (block.getBits() != nextBits)) {
			logger.info("previous block next bits {} != {} bits of this block {}", nextBits, block.getBits(), block);
			
			return false;
		}

		new DBExecutor() {
			public void doWork() throws Exception {
				String sql = "insert into block (block_id, blockHash, miner, index, previousHash, receivedTime, power, coinbaseTransactionId, minerSignature, consensus, timeStamp, nonce, bits, nextBits) "
						+ " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

				ps = conn.prepareStatement(sql);
				int i = 1;
				ps.setLong(i++, id.getAndIncrement());
				ps.setString(i++, block.getBlockHash());
				if(block.isPruned()) {
					ps.setString(i++, null);
				} else {
					ps.setString(i++, Base58.encode(block.getMiner().getEncoded()));
				}
				
				ps.setLong(i++, block.getIndex());
				ps.setString(i++, block.getPreviousHash());
				Long receivedTime = block.getReceivedTime();
				if(receivedTime != null) {
					ps.setLong(i++, receivedTime);
				} else {
					ps.setNull(i++, java.sql.Types.BIGINT);
				}
				
				if(block.isPruned()) {
					ps.setInt(i++, 0);
				} else {
					ps.setInt(i++, block.getPower());
				}
				
				ps.setString(i++, block.getCoinbaseTransactionId());
				ps.setString(i++, block.getMinerSignature());
				ps.setString(i++, block.getConsensus());
				ps.setLong(i++, block.getTimeStamp());
				ps.setInt(i++, block.getNonce());
				ps.setLong(i++, block.getBits());
				ps.setLong(i++, block.getNextBits());
				long start = System.currentTimeMillis();
				ps.executeUpdate();
				logger.trace("Query doSaveBlock took {} ms. '{}' ", System.currentTimeMillis() - start, sql);
			}
		}.execute();
		logger.trace("Function doSaveBlock took {} ms.", System.currentTimeMillis() - start);
		return true;
	}

	public void removeBlock(String blockHash) throws Exception {
		logger.trace("START removeBlock() blockHash={}", blockHash);
		Block block = getByBlockhash(blockHash);
		new DBExecutor() {
			public void doWork() throws Exception {
				String sql = "delete from block where blockhash = ?";
				ps = conn.prepareStatement(sql);
				int i = 1;
				ps.setString(i++, blockHash);
				long start = System.currentTimeMillis();
				ps.executeUpdate();
				logger.trace("Query removeBlock took {} ms. '{}' ", System.currentTimeMillis() - start, sql);
			}
		}.execute();
		BucketHashStore.getInstance().remove(blockHash);
		TransactionStore.getInstance().remove(blockHash);
		setMinCompeting(-1);
		invalidateCachedValue(blockHash);
		logger.trace("END removeBlock() blockHash={}", blockHash);
		logger.info("{}-{} Removed block {}", block.getBucketKey(), block.getIndex(), block);
	}

	public List<Block> getByIndex(long index) {
		List<Block> result = new ArrayList<>();

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from block where index = ? order by receivedTime";
					ps = conn.prepareStatement(sql);
					ps.setLong(1, index);
					rs = ps.executeQuery();
					while (rs.next()) {
						result.add(getBlock(rs));
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		List<Block> list = new ArrayList<>(result);
		for (Block block : list) {
			logger.trace("Will call getBucketHashes by {}", block.getBlockHash());
			BucketHashes bucketHashes = BucketHashStore.getInstance().getBucketHashes(block.getBlockHash());
			block.setBucketHashes(bucketHashes);
		}

		return result;
	}

	public Block getByBlockhash(String blockhash) {
		
		if(blockhash == null) {
			return null;
		}
		Block[] result = new Block[1];
		result[0] = latestCachedBlocks.get(blockhash);
		if(result[0] != null) {
			return result[0];
		}

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from block where blockhash = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, blockhash);
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] = getBlock(rs);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		if (result[0] != null && result[0].getMiner() != null) {
			logger.trace("Will call getBucketHashes by {}", result[0].getBlockHash());
			BucketHashes bucketHashes = BucketHashStore.getInstance().getBucketHashes(result[0].getBlockHash());
			result[0].setBucketHashes(bucketHashes);
		}
		
		latestCachedBlocks.put(blockhash, result[0]);
		return result[0];
	}
	
	private Block getBlock(ResultSet rs) throws Exception {
		Block block = new Block();
		block.setBlockHash(rs.getString("blockhash"));
		String miner = rs.getString("miner");
		block.setMiner(CryptoUtil.loadPublicKey(miner));
		block.setIndex(rs.getLong("index"));
		block.setPreviousHash(rs.getString("previousHash"));
		block.setReceivedTime(rs.getLong("receivedTime"));
		block.setCoinbaseTransactionId(rs.getString("coinbaseTransactionId"));
		block.setMinerSignature(rs.getString("minerSignature"));
		block.setConsensus(rs.getString("consensus"));
		block.setTimeStamp(rs.getLong("timeStamp"));
		block.setNonce(rs.getInt("nonce"));
		block.setBits(rs.getLong("bits"));
		block.setNextBits(rs.getLong("nextBits"));
		return block;
	}

	public List<Block> getByPreviousHash(String previousHash) {
		List<Block> result = new ArrayList<>();

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from block where previousHash = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, previousHash);
					rs = ps.executeQuery();
					while (rs.next()) {
						result.add(getBlock(rs));
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		List<Block> list = new ArrayList<>(result);
		for (Block block : list) {
			logger.trace("Will call getBucketHashes by {}", block.getBlockHash());
			BucketHashes bucketHashes = BucketHashStore.getInstance().getBucketHashes(block.getBlockHash());
			block.setBucketHashes(bucketHashes);
		}

		return result;
	}
	
	public List<String> getBlockhashesByPreviousHash(String previousHash) {
		List<String> result = new ArrayList<>();

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select blockHash from block where previousHash = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, previousHash);
					rs = ps.executeQuery();
					while (rs.next()) {
						result.add(rs.getString("blockHash"));
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result;
	}

	public long getLastIndex() {
		long[] result = new long[1];

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select max(index) counter from block";
					ps = conn.prepareStatement(sql);
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] = rs.getLong("counter");
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result[0];
	}

	private void verifyTable() throws Exception {

		String tableName = "block".toUpperCase();

		new DBExecutor() {
			public void doWork() throws Exception {
				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getTables(null, ConnectionPool.USER, tableName, null);
				if (!rs.next()) {
					s.execute("CREATE SEQUENCE block_id_seq AS BIGINT START WITH 1");
					s.execute("create table block ( block_id bigint NOT NULL CONSTRAINT block_PK PRIMARY KEY, blockHash varchar(64) NOT NULL, "
							+ "miner varchar(256), index bigint NOT NULL, previousHash varchar(64), receivedTime bigint, "
							+ "power int NOT NULL, coinbaseTransactionId varchar(64), minerSignature varchar(256), "
							+ "consensus varchar(64) NOT NULL, timeStamp bigint, nonce int, bits bigint, nextBits bigint DEFAULT " + Difficulty.INITIAL_BITS + ")");

					s.execute("ALTER TABLE block ADD CONSTRAINT block_blockHash UNIQUE (blockhash)");
					s.execute("CREATE INDEX block_index ON block(index)");
					s.execute("CREATE INDEX block_previousHash ON block(previousHash)");
					s.execute("CREATE INDEX block_power ON block(power)");
					s.execute("CREATE INDEX block_receivedTime ON block(receivedTime)");
					s.execute("ALTER TABLE BLOCK ADD CONSTRAINT BLOCKHASH_FK FOREIGN KEY (previousHash) REFERENCES block(blockHash) ON DELETE CASCADE");
				}
				rs.close();

				addNewColumn(dbmd, tableName, "coinbaseTransactionId", "ALTER TABLE " + tableName + " ADD coinbaseTransactionId VARCHAR(64)");
				
				addNewColumn(dbmd, tableName, "timeStamp", "ALTER TABLE " + tableName + " ADD timeStamp bigint");
				addIndex(dbmd, tableName, "block_timeStamp", "CREATE INDEX block_timeStamp ON " + tableName + "(timeStamp)");
				addNewColumn(dbmd, tableName, "nextBits", "ALTER TABLE " + tableName + " ADD nextBits bigint DEFAULT " + Difficulty.INITIAL_BITS);
				addIndex(dbmd, tableName, "block_miner", "CREATE INDEX block_miner ON " + tableName + "(miner)");
				
			}
		}.execute();
		setId();
		TransactionStore.getInstance();
		BucketHashStore.getInstance();
		DBExecutor.rebuildIndexes();
	}

	public List<Block> restore() {
		List<Block> result = new ArrayList<Block>();
		Network network = Network.getInstance();
		int power = network.getPower();

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from block where power > ? order by index FETCH FIRST 100 ROWS ONLY WITH UR";
					ps = conn.prepareStatement(sql);
					ps.setInt(1, power);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					logger.trace("Query BlockStore.restore() took {} ms. '{}' ", System.currentTimeMillis() - start, sql);
					int counter = 0;
					while (rs.next()) {
						counter++;
						if (power < network.getPower()) {// network power increased so we need to rerun the query
							logger.info("network power increased from {} to {} so we need to rerun the query", power + 1, network.getPower());
							break;
						}
						
						Block block = getBlock(rs);
						logger.trace("restore() block {}-{} rs.getInt(\"power\")={}", block.getIndex(), block.getBlockHash(), rs.getInt("power"));
						result.add(block);
					}
					logger.trace("restore() counter={} power={}", counter, power);
				}
			}.execute();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}
	
	public Map<Long, Long> getPowerReport() {
		Map<Long, Long> map = new LinkedHashMap<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select power, count(*) counter from block group by power order by power";
					ps = conn.prepareStatement(sql);
					rs = ps.executeQuery();
					while (rs.next()) {
						map.put(rs.getLong("power"), rs.getLong("counter"));
					}
				}
			}.execute();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return map;
	}

	public int getMaxPower() {
		int[] result = new int[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select max(power) counter from block";
					ps = conn.prepareStatement(sql);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					
					if (rs.next()) {
						result[0] = rs.getInt("counter");
					}
					logger.trace("Query getMaxPower took {} ms. result {} sql '{}' ", System.currentTimeMillis() - start, result[0], sql);
				}
			}.execute();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result[0];
	}

	public long getMinCompeting() {
		long start = System.currentTimeMillis();
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			if(minCompeting != -1) {
				return minCompeting;
			}
			long[] result = new long[1];
			try {
				new DBExecutor() {
					public void doWork() throws Exception {
						String sql = "select index from block group by index HAVING COUNT(*) > 1 order by index";
						ps = conn.prepareStatement(sql);
						long start = System.currentTimeMillis();
						rs = ps.executeQuery();
						
						if (rs.next()) {
							result[0] = rs.getLong("index");
						}
						long duration = System.currentTimeMillis() - start;
						if(duration > Constants.SECOND * 10) {
							logger.info("Query getMinCompeting took {} ms. Result = {}, sql = '{}' ", duration, result[0], sql);
						} else {
							logger.trace("Query getMinCompeting took {} ms. Result = {}, sql = '{}' ", duration, result[0], sql);
						}
					}
				}.execute();

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			setMinCompeting(result[0]);
			return result[0];
		} finally {
			logger.trace("getMinCompeting took {} ms, minCompeting = {}", System.currentTimeMillis() - start, minCompeting);
			readLock.unlock();
		}
	}

	public List<Block> getLastBlocks() {
		List<Block> result = new ArrayList<>();

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from block where index = (select max(index) counter from block) order by receivedTime";
					ps = conn.prepareStatement(sql);
					rs = ps.executeQuery();
					while (rs.next()) {
						result.add(getBlock(rs));
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		List<Block> list = new ArrayList<>(result);
		for (Block block : list) {
			if(block.isPruned()) {
				continue;
			}
			logger.trace("Will call getBucketHashes by {}", block.getBlockHash());
			BucketHashes bucketHashes = BucketHashStore.getInstance().getBucketHashes(block.getBlockHash());
			block.setBucketHashes(bucketHashes);
		}

		return result;
	}

	public void replace(Block block) throws Exception {
		try {
			String blockHash = block.getBlockHash();
			int[] numberOfUpdates = new int[1];
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "update block set power = ?, miner = ?, COINBASETRANSACTIONID = ?, MINERSIGNATURE = ?, RECEIVEDTIME = ? where blockHash = ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setInt(i++, block.getPower());
					if(block.getMiner() == null) {
						ps.setString(i++, null);
					} else {
						ps.setString(i++, Base58.encode(block.getMiner().getEncoded()));
					}
					ps.setString(i++, block.getCoinbaseTransactionId());
					ps.setString(i++, block.getMinerSignature());
					Long receivedTime = block.getReceivedTime();
					if(receivedTime != null) {
						ps.setLong(i++, receivedTime);
					} else {
						ps.setNull(i++, java.sql.Types.BIGINT);
					}
					ps.setString(i++, blockHash);
					numberOfUpdates[0] = ps.executeUpdate();
				}
			}.execute();
			if(numberOfUpdates[0] == 0) {
				return;
			}

			BucketHashStore.getInstance().remove(blockHash);

			BucketHashStore.getInstance().saveBucketHashes(block.getIndex(), blockHash, block.getBucketHashes());
			if(block.isPruned()) {
				TransactionStore.getInstance().remove(blockHash);
			} else {
				TransactionStore.getInstance().saveTransactions(block.getAllTransactions());
			}
			latestCachedBlocks.put(block.getBlockHash(), block);
			
			Registry.getInstance().getPotentialTransactions().addPotentialTransactions(block.getBlockHash());
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("Exception when replacing block {}", block);
			throw e;
		}
	}

	public int getAveragePower(long lastIndex) {
		// select avg(power) as power from block where index > (select max(index) - 10
		// counter from block)
		int[] result = new int[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select coalesce(avg(averagePower), 0) averagePower from buckethash bh where bh.bucket_key='' and bh.blockIndex > ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setLong(i++, lastIndex - 10);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] = rs.getBigDecimal("averagePower").intValue();
					}
					logger.trace("Query getAveragePower took {} ms. result {} sql '{}' ", System.currentTimeMillis() - start, result[0], sql);
				}
			}.execute();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result[0];
	}

	public int getLastAveragePower(long lastIndex) {
		int[] result = new int[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select bh.averagePower from block b join buckethash bh on bh.blockHash=b.blockHash where index = ? order by buckethash_id  WITH UR";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setLong(i++, lastIndex);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					
					if (rs.next()) {
						result[0] = rs.getInt("averagePower");
					}
					logger.trace("Query getLastAveragePower took {} ms. result {} sql '{}' ", System.currentTimeMillis() - start, result[0], sql);
				}
			}.execute();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result[0];
	}

	public Set<String> getOrphants(long index) {
		Set<String> result = new HashSet<>();

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select b.blockhash from block b left outer join block b1 on b.blockhash=b1.previousHash "
							+ "where b1.block_id is null and b.index < ? and b.index > ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setLong(i++, index - 10);
					ps.setLong(i++, index - 100);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					while (rs.next()) {
						result.add(rs.getString("blockhash"));
					}
					logger.trace("Query getOrphants() took {} ms. sql '{}', orphants {}", System.currentTimeMillis() - start, sql, result);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result;
	}

	public Set<String> getBranchBlockhashes(Block block) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			Set<String> blockhashes = new HashSet<>();
			blockhashes.add(block.getBlockHash());
			long minCompeting = getMinCompeting();
			if (minCompeting == 0) {
				return blockhashes;
			}

			long index = minCompeting;
			try {
				new DBExecutor() {
					public void doWork() throws Exception {
						String sql = "select * from block where index >= ? and index <= ? order by index desc";
						ps = conn.prepareStatement(sql);
						int i = 1;
						ps.setLong(i++, index);
						ps.setLong(i++, block.getIndex());
						long start = System.currentTimeMillis();
						rs = ps.executeQuery();
						String branchHash = block.getBlockHash();
						while (rs.next()) {
							String hash = rs.getString("blockhash");
							if (branchHash.equals(hash)) {
								blockhashes.add(branchHash);
								branchHash = rs.getString("previousHash");
							}
						}
						long duration = System.currentTimeMillis() - start;
						if(duration > Constants.SECOND * 10) {
							logger.info("took {} ms to execute sql={}, index={}, block.getIndex()={}", duration, sql, index, block.getIndex());
						}
					}
				}.execute();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

			return blockhashes;
		} finally {
			readLock.unlock();
		}
	}
	
	public String getAncestor(Block block, long depth) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			String[] result = new String[1];
			try {
				new DBExecutor() {
					public void doWork() throws Exception {
						String sql = "select * from block where index >= ? and index <= ? order by index desc";
						ps = conn.prepareStatement(sql);
						int i = 1;
						ps.setLong(i++, block.getIndex() - depth);
						ps.setLong(i++, block.getIndex());
						rs = ps.executeQuery();
						String branchHash = block.getBlockHash();
						while (rs.next()) {
							String hash = rs.getString("blockhash");
							if (branchHash.equals(hash)) {
								branchHash = rs.getString("previousHash");
							}
						}
						result[0] = branchHash;
					}
				}.execute();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

			return result[0];
		} finally {
			readLock.unlock();
		}
	}

	public boolean contains(String blockhash) {
		boolean[] result = new boolean[1];
		result[0] = false;
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select 1 from block where blockhash = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, blockhash);
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] = true;
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result[0];
	}

	public void setMinCompeting(long minCompeting) {
		Lock writeLock = readWriteLock.readLock();
		writeLock.lock();
		try {
			this.minCompeting = minCompeting;
		} finally {
			writeLock.unlock();
		}
	}
	
	public void invalidateCachedValue(String blockHash) {
		latestCachedBlocks.remove(blockHash);
		BucketHashStore.getInstance().invalidateCachedValue(blockHash);
	}

	public long getMinUnprunedIndex() {
		long[] result = new long[1];

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select coalesce(min(index), -1) counter from block where miner is not null";
					ps = conn.prepareStatement(sql);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] = rs.getLong("counter");
					}
					logger.trace("Query getMinUnprunedIndex() took {} ms. sql '{}' ", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result[0];
	}
	
	public String getBucketKey(String blockhash) {
		String[] result = new String[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select bucket_key from buckethash where blockHash = ? order by buckethash_id desc";
					ps = conn.prepareStatement(sql);
					ps.setString(1, blockhash);
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] = rs.getString("bucket_key");
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result[0];
	}

	public long getAverageMiningTime(Block block) {
		long lastIndex = block.getIndex();
		long firstIndex = Math.max(0, lastIndex - 100);
		long[] timestamp = new long[1];
		try {
			
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select blockhash, timeStamp, previousHash from block where index >= ? and index <= ? order by index desc";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setLong(i++, firstIndex);
					ps.setLong(i++, lastIndex);
					rs = ps.executeQuery();
					String branchHash = block.getBlockHash();
					while (rs.next()) {
						String hash = rs.getString("blockhash");
						if (branchHash.equals(hash)) {
							timestamp[0] = rs.getLong("timeStamp");
							branchHash = rs.getString("previousHash");
						}
					}
				}
			}.execute();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return (block.getTimeStamp() - timestamp[0]) / (lastIndex - firstIndex);
	}
	
	public long getAverageBits(Block block) {
		long lastIndex = block.getIndex();
		long firstIndex = Math.max(0, lastIndex - 100);
		long[] result = new long[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select blockhash, bits, previousHash from block where index >= ? and index <= ? order by index desc";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setLong(i++, firstIndex);
					ps.setLong(i++, lastIndex);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					String branchHash = block.getBlockHash();
					double sum = 0;
					while (rs.next()) {
						String hash = rs.getString("blockhash");
						if (branchHash.equals(hash)) {
							sum = sum + Difficulty.getDifficulty(rs.getLong("bits"));
							branchHash = rs.getString("previousHash");
						}
					}
					result[0] = Difficulty.convertDifficultyToBits(sum / (lastIndex - firstIndex));
					logger.trace("Query getAverageBits() took {} ms. sql '{}' ", System.currentTimeMillis() - start, sql);
				}
			}.execute();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return result[0];
	}
	
	public long getNextBits(String blockhash) {
		long[] result = new long[1];
		result[0] = 0;
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select nextBits from block where blockHash = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, blockhash);
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] = rs.getLong("nextBits");
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result[0];
	}
	
	public long getIndexByBlockhash(String blockhash) {
		long[] result = new long[1];
		result[0] = 0;
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select index from block where blockHash = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, blockhash);
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] = rs.getLong("index");
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result[0];
	}

	public long getBits(String blockhash) {
		long[] result = new long[1];
		result[0] = 0;
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select bits from block where blockHash = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, blockhash);
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] = rs.getLong("bits");
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result[0];
	}

}
