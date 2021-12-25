package org.dhc.persistence;

import java.sql.DatabaseMetaData;
import java.util.concurrent.atomic.AtomicLong;

import org.dhc.blockchain.Keywords;
import org.dhc.util.DhcLogger;

public class KeywordStore {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static KeywordStore instance = new KeywordStore();
	
	private final AtomicLong id = new AtomicLong();
	
	private KeywordStore() {
		try {
			verifyTable();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public static KeywordStore getInstance() {
		return instance;
	}
	
	private void setId() {
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select coalesce(max(id) + 1, 0) as id from keyword";
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

		new DBExecutor() {
			public void doWork() throws Exception {
				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getTables(null, ConnectionPool.USER, "KEYWORD", null);
				if (!rs.next()) {
					s.execute("create table keyword (ID BIGINT NOT NULL CONSTRAINT KEYWORD_PK PRIMARY KEY, "
							+ " transactionId varchar(64) NOT NULL, blockhash varchar(64) NOT NULL, "
							+ " keyword varchar(64) NOT NULL, NAME VARCHAR(64) "
							+ " )");
					s.execute("CREATE INDEX keyword_transactionId ON keyword(transactionId)");
					s.execute("CREATE INDEX KEYWORD_NAME ON KEYWORD(NAME)");
					s.execute("CREATE INDEX KEYWORD_KEYWORD ON KEYWORD(KEYWORD)");
					s.execute("ALTER TABLE keyword ADD CONSTRAINT keyword_tr_id_BLOCKHASH_FK FOREIGN KEY (transactionId, blockHash) REFERENCES trans_action(transaction_id, blockHash) ON DELETE CASCADE");
				}
				rs.close();

			}
		}.execute();
		setId();
	}
	
	public int deleteAll() {
		int[] result = new int[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "delete from keyword";
					ps = conn.prepareStatement(sql);
					result[0] = ps.executeUpdate();
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result[0];
	}
	
	public void saveKeywords(Keywords keywords) throws Exception {
		if(keywords == null || keywords.isEmpty()) {
			return;
		}
		new DBExecutor() {
			public void doWork() throws Exception {
	            String sql = "insert into keyword (id, transactionId, name, keyword, blockhash) values (?,?,?,?,?)";
	            ps = conn.prepareStatement(sql);
	            for(String name: keywords.keySet()) {
	            	String keyword = keywords.get(name);
	            	int i = 1;
	            	ps.setLong(i++, id.getAndIncrement());
		            ps.setString(i++, keywords.getTransactionId());
		            ps.setString(i++, name);
		            ps.setString(i++, keyword);
		            ps.setString(i++, keywords.getBlockHash());
		            ps.addBatch();
	            }
	            ps.executeBatch();
			}
		}.execute();
	}
	
	public Keywords getKeywords(String transactionId) {
		final Keywords keywords = new Keywords();
		keywords.setTransactionId(transactionId);
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from keyword where transactionId = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, transactionId);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					while (rs.next()) {
						String name = rs.getString("name");
						String keyword = rs.getString("keyword");
						String blockhash = rs.getString("blockhash");
						keywords.put(name, keyword);
						keywords.setBlockHash(blockhash);
					}
					logger.trace("Query getKeywords() took {} ms. sql '{}' ", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if(keywords.isEmpty()) {
			return null;
		}
		return keywords;
	}



}
