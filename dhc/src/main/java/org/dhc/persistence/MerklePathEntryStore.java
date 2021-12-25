package org.dhc.persistence;

import java.sql.DatabaseMetaData;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.dhc.util.DhcLogger;

public class MerklePathEntryStore {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static MerklePathEntryStore instance = new MerklePathEntryStore();
	
	private final AtomicLong id = new AtomicLong();
	
	public static MerklePathEntryStore getInstance() {
		return instance;
	}

	private MerklePathEntryStore() {
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
					String sql = "select coalesce(max(merkle_path_entry_id) + 1, 0) as id from merkle_path_entry";
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

	}
	
	public void saveMerklePathEntry(String blockHash, String transactionId, SimpleEntry<String, String> entry) throws Exception {
		new DBExecutor() {
			public void doWork() throws Exception {
	            String sql = "insert into merkle_path_entry (merkle_path_entry_id, entry_key, entry_hash, transaction_id, blockHash) values (?,?,?,?,?)";
	            ps = conn.prepareStatement(sql);
	            int i = 1;
	            ps.setLong(i++, id.getAndIncrement());
	        	ps.setString(i++, entry.getKey());
	        	ps.setString(i++, entry.getValue());
	        	ps.setString(i++, transactionId);
	        	ps.setString(i++, blockHash);
	        	
	        	
	            ps.executeUpdate();
			}
		}.execute();
	}
	
	public List<SimpleEntry<String, String>> getMerklePath(String blockHash, String transactionId) {
		List<SimpleEntry<String, String>> merklePath = new ArrayList<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from merkle_path_entry where transaction_id = ? and blockHash = ? order by merkle_path_entry_id";
					ps = conn.prepareStatement(sql);
					int i = 1;
					
					ps.setString(i++, transactionId);
					ps.setString(i++, blockHash);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					while (rs.next()) {
						SimpleEntry<String, String> entry = new SimpleEntry<String, String>(rs.getString("entry_key"), rs.getString("entry_hash"));
						merklePath.add(entry);
					}
					logger.trace("Query getMerklePath() took {} ms. sql '{}' ", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return merklePath;
	}
	
	private void verifyTable() throws Exception {
		
		String tableName = "merkle_path_entry".toUpperCase();

		new DBExecutor() {
			public void doWork() throws Exception {
				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getTables(null, ConnectionPool.USER, tableName, null);
				if (!rs.next()) {
					s.execute("create table merkle_path_entry ( merkle_path_entry_id BIGINT NOT NULL CONSTRAINT merkle_path_entry_PK PRIMARY KEY, "
							+ "entry_key varchar(256) NOT NULL, entry_hash varchar(64) NOT NULL, "
							+ "transaction_id varchar(64) NOT NULL, blockHash varchar(64) NOT NULL)");
					s.execute("CREATE INDEX merkle_path_entry_blockHash ON merkle_path_entry(blockHash)");
					s.execute("CREATE INDEX merkle_path_entry_tr_id_bhash ON merkle_path_entry(transaction_id, blockhash)");
					s.execute("ALTER TABLE merkle_path_entry ADD CONSTRAINT merkle_pe_tr_id_BLOCKHASH_FK FOREIGN KEY (transaction_id, blockHash) REFERENCES trans_action(transaction_id, blockHash) ON DELETE CASCADE");
				}
				rs.close();
			}
		}.execute();
		setId();

	}

	public void saveMerklePath(String blockHash, String transactionId, List<SimpleEntry<String, String>> merklePath) throws Exception {
		for(SimpleEntry<String, String> entry: merklePath) {
			saveMerklePathEntry(blockHash, transactionId, entry);
		}
		
	}


}
