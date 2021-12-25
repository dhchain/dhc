package org.dhc.persistence;

import java.sql.DatabaseMetaData;
import java.util.concurrent.atomic.AtomicLong;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.TransactionData;
import org.dhc.util.DhcLogger;

public class TransactionDataStore {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static TransactionDataStore instance = new TransactionDataStore();
	
	private final AtomicLong id = new AtomicLong();
	
	public static TransactionDataStore getInstance() {
		return instance;
	}
	
	private TransactionDataStore() {
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
					String sql = "select coalesce(max(id) + 1, 0) as id from transaction_data";
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
	
	private void verifyTable() throws Exception {
		String tableName = "transaction_data".toUpperCase();
		new DBExecutor() {
			public void doWork() throws Exception {
				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getTables(null, ConnectionPool.USER, "TRANSACTION_DATA", null);
				if (!rs.next()) {
					s.execute("create table transaction_data (id bigint NOT NULL CONSTRAINT transaction_data_PK PRIMARY KEY, "
							+ " transactionId varchar(64) NOT NULL, blockhash varchar(64) NOT NULL, "
							+ " hash varchar(64) NOT NULL, "
							+ " validForNumberOfBlocks bigint not null, "
							+ " data varchar(32672) , expirationIndex bigint not null "
							+ " )");
					s.execute("CREATE INDEX transaction_data_transactionId ON transaction_data(transactionId)");
					s.execute("CREATE INDEX transaction_data_expirationIndex ON transaction_data(expirationIndex)");
					s.execute("ALTER TABLE transaction_data ADD CONSTRAINT transaction_data_tr_id_BLOCKHASH_FK FOREIGN KEY (transactionId, blockHash) REFERENCES trans_action(transaction_id, blockHash) ON DELETE CASCADE");
				}
				rs.close();
				addIndex(dbmd, tableName, "transaction_data_expirationIndex", "CREATE INDEX transaction_data_expirationIndex ON " + tableName + "(expirationIndex)");
			}
		}.execute();
		setId();
	}

	public void save(TransactionData expiringData) throws Exception {
		expiringData.setExpirationIndex();
		new DBExecutor() {
			public void doWork() throws Exception {
	            String sql = "insert into transaction_data (id,transactionId, blockhash, hash, validForNumberOfBlocks, data, expirationIndex) values (?,?,?,?,?,?,?)";
	            ps = conn.prepareStatement(sql);
	            int i = 1;
	            ps.setLong(i++, id.getAndIncrement());
	            ps.setString(i++, expiringData.getTransactionId());
	            ps.setString(i++, expiringData.getBlockHash());
	            ps.setString(i++, expiringData.getHash());
	            ps.setLong(i++, expiringData.getValidForNumberOfBlocks());
	            ps.setString(i++, expiringData.getData());
	            ps.setLong(i++, expiringData.getExpirationIndex());
	            ps.executeUpdate();
			}
		}.execute();
	}

	public TransactionData getExpiringData(String transactionId) {
		TransactionData[] holder = new TransactionData[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from transaction_data where transactionId = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, transactionId);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					if (rs.next()) {
						TransactionData transactionData = new TransactionData(rs.getString("data"));
						transactionData.setTransactionId(rs.getString("transactionId"));
						transactionData.setBlockHash(rs.getString("blockhash"));
						transactionData.setHash(rs.getString("hash"));
						transactionData.setValidForNumberOfBlocks(rs.getLong("validForNumberOfBlocks"));
						transactionData.setData(rs.getString("data"));
						transactionData.setExpirationIndex(rs.getLong("expirationIndex"));
						holder[0] = transactionData;
					}
					logger.trace("Query getExpiringData() took {} ms. sql '{}' ", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return holder[0];
	}
	
	public void remove() {
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "update transaction_data set data = null where expirationIndex < ?";
					ps = conn.prepareStatement(sql);
					ps.setLong(1, Blockchain.getInstance().getIndex());
					long start = System.currentTimeMillis();
					ps.executeUpdate();
					logger.trace("Query TransactionDataStore.remove() took {} ms. '{}' ", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
