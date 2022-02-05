package org.dhc.persistence;

import java.sql.DatabaseMetaData;
import java.util.concurrent.atomic.AtomicLong;

import org.dhc.blockchain.BucketHashes;
import org.dhc.network.consensus.BucketHash;
import org.dhc.util.BoundedMap;
import org.dhc.util.Coin;
import org.dhc.util.DhcLogger;

public class BucketHashStore {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static BucketHashStore instance = new BucketHashStore();
	
	private final BoundedMap<String, BucketHashes> map = new BoundedMap<>(10);
	private final AtomicLong id = new AtomicLong();
	
	public static BucketHashStore getInstance() {
		return instance;
	}

	private BucketHashStore() {
		try {
			verifyTable();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private void setId() {

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select coalesce(max(buckethash_id) + 1, 0) as id from buckethash";
					ps = conn.prepareStatement(sql);
					rs = ps.executeQuery();
					while (rs.next()) {
						
						id.set(rs.getLong("id"));
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}
	
	private void saveBucketHash(BucketHash bucketHash) throws Exception {
		new DBExecutor() {
			public void doWork() throws Exception {
	            String sql = "insert into buckethash (buckethash_id,bucket_key, bucket_hash, left_hash, right_hash, blockIndex, blockHash, previousHash, averagePower, fee) values (?,?,?,?,?,?,?,?,?,?)";
	            ps = conn.prepareStatement(sql);
	            int i = 1;
	            ps.setLong(i++, id.getAndIncrement());
	        	ps.setString(i++, bucketHash.getBinaryStringKey());
	        	ps.setString(i++, bucketHash.getHash());
	        	ps.setString(i++, bucketHash.getLeft() == null? null: bucketHash.getLeft().getHash());
	        	ps.setString(i++, bucketHash.getRight() == null? null: bucketHash.getRight().getHash());
	        	ps.setLong(i++, bucketHash.getBlockIndex());
	        	ps.setString(i++, bucketHash.getBlockHash());
	        	ps.setString(i++, bucketHash.getPreviousBlockHash());
	        	ps.setBigDecimal(i++, bucketHash.getAveragePower());
	        	ps.setLong(i++, bucketHash.getFee().getValue());

	            ps.executeUpdate();
			}
		}.execute();
	}
	
	public BucketHashes getBucketHashes(String blockHash) {
		BucketHashes cachedBucketHashes = map.get(blockHash);
		if(cachedBucketHashes != null) {
			return cachedBucketHashes;
		}
		BucketHashes[] bucketHashes = new BucketHashes[1];
		bucketHashes[0] = new BucketHashes();
		
		logger.trace("BucketHashStore.getBucketHashes by {}", blockHash);
		
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from buckethash where blockHash = ? order by buckethash_id";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, blockHash);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					while (rs.next()) {
						String previousHash = rs.getString("previousHash");
						BucketHash bucketHash = new BucketHash(rs.getString("bucket_key"), rs.getString("bucket_hash"), previousHash);
						bucketHash.setAveragePower(rs.getBigDecimal("averagePower"));
						bucketHash.setFee(new Coin(rs.getLong("fee")));

						long blockIndex = rs.getLong("blockIndex");
						bucketHash.setBlockIndex(blockIndex);
						bucketHash.setBlockHash(blockHash);

						String leftHash = rs.getString("left_hash");
						String rightHash = rs.getString("right_hash");
						if(leftHash != null && rightHash != null) {
							BucketHash left = new BucketHash(bucketHash.getKey().getLeftKey().getKey(), leftHash, previousHash);
							left.setBlockIndex(blockIndex);
							left.setBlockHash(blockHash);
							BucketHash right = new BucketHash(bucketHash.getKey().getRightKey().getKey(), rightHash, previousHash);
							right.setBlockIndex(blockIndex);
							right.setBlockHash(blockHash);
							bucketHash.setLeftRight(left, right);
						}
						bucketHashes[0].set(bucketHash);
					}
					logger.trace("Query getBucketHashes() took {} ms. sql '{}' ", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if(bucketHashes[0].isEmpty()) {
			String message = String.format("Could not find any bucketHashes for blockhash %s", blockHash);
			logger.trace(message);
			bucketHashes[0] = null;
		} else {
			bucketHashes[0].getLastBucketHash().setAllTransactions(TransactionStore.getInstance().getTransactions(blockHash));
		}
		
		map.put(blockHash, bucketHashes[0]);
		return bucketHashes[0];
	}
	
	public void saveBucketHashes(long blockIndex, String blockHash, BucketHashes bucketHashes) throws Exception {
		if(bucketHashes == null) {
			return;
		}
		for(BucketHash bucketHash: bucketHashes.getBucketHashes()) {
			bucketHash.setBlockIndex(blockIndex);
			bucketHash.setBlockHash(blockHash);
			saveBucketHash(bucketHash);
		}
		map.put(blockHash, bucketHashes);
	}
	
	private void verifyTable() throws Exception {
		
		String tableName = "buckethash".toUpperCase();

		new DBExecutor() {
			public void doWork() throws Exception {
				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getTables(null, ConnectionPool.USER, tableName, null);
				if (!rs.next()) {
					s.execute("create table buckethash ( buckethash_id bigint NOT NULL CONSTRAINT buckethash_PK PRIMARY KEY, "
							+ "bucket_key varchar(256) NOT NULL, bucket_hash varchar(64) NOT NULL, left_hash varchar(64), right_hash varchar(64), "
							+ "blockHash varchar(64) NOT NULL, previousHash varchar(64), averagePower DECIMAL DEFAULT 0, fee BIGINT )");
					s.execute("CREATE INDEX buckethash_blockHash ON buckethash(blockHash)");
					s.execute("ALTER TABLE buckethash ADD CONSTRAINT buckethash_BLOCKHASH_FK FOREIGN KEY (blockHash) REFERENCES block(blockHash) ON DELETE CASCADE");
				}
				rs.close();

				addNewColumn(dbmd, tableName, "fee", "ALTER TABLE " + tableName + " ADD fee BIGINT");
				
				addNewColumn(dbmd, tableName, "blockIndex", "ALTER TABLE " + tableName + " ADD blockIndex BIGINT");
				addIndex(dbmd, tableName, "buckethash_blockIndex", "CREATE INDEX buckethash_blockIndex ON " + tableName + "(blockIndex)");
	
			}
		}.execute();
		setId();

	}

	public void remove(String blockHash) throws Exception {
		new DBExecutor() {
			public void doWork() throws Exception {
	            String sql = "delete from buckethash where blockHash = ?";
	            ps = conn.prepareStatement(sql);
	            int i = 1;
	        	ps.setString(i++, blockHash);
	            ps.executeUpdate();
			}
		}.execute();
		invalidateCachedValue(blockHash);
	}
	
	public void invalidateCachedValue(String blockHash) {
		map.remove(blockHash);
	}


}
