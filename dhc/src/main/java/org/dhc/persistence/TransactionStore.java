package org.dhc.persistence;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.dhc.blockchain.Keywords;
import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionData;
import org.dhc.blockchain.TransactionInput;
import org.dhc.blockchain.TransactionOutput;
import org.dhc.blockchain.TransactionType;
import org.dhc.gui.promote.JoinLine;
import org.dhc.gui.promote.JoinTransactionEvent;
import org.dhc.lite.SecureMessage;
import org.dhc.lite.post.Ratee;
import org.dhc.lite.post.Rating;
import org.dhc.network.ChainSync;
import org.dhc.util.Applications;
import org.dhc.util.Base58;
import org.dhc.util.Coin;
import org.dhc.util.Constants;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Registry;

public class TransactionStore {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static TransactionStore instance = new TransactionStore();
	
	private final AtomicLong id = new AtomicLong();
	
	public static TransactionStore getInstance() {
		return instance;
	}

	private TransactionStore() {
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
					String sql = "select coalesce(max(id) + 1, 0) as id from trans_action";
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
	
	public boolean saveTransaction(Transaction transaction) throws Exception {
		logger.trace("Will try to add transaction {}", transaction);

		if(!transaction.outputBlockHashExist(null)) {
			return false;
		}
		if(!transaction.isValid(null)) {
			logger.info("Transaction is not valid: {}", transaction);
			return false;
		}
		if(contains(transaction)) {
			logger.trace("Already contain transaction {}", transaction);
			return false;
		}

		new DBExecutor() {
			public void doWork() throws Exception {
	            String sql = "insert into trans_action (id, transaction_id, sender, senderAddress, receiver, value, fee, type, blockhash, blockIndex, signature, app, timeStamp) "
	            		+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

	            ps = conn.prepareStatement(sql);
	            int i = 1;
	            ps.setLong(i++, id.getAndIncrement());
	        	ps.setString(i++, transaction.getTransactionId());
	        	PublicKey publicKey = transaction.getSender();
	        	if(publicKey != null) {
	        		ps.setString(i++, Base58.encode(transaction.getSender().getEncoded()));
	        	} else {
	        		ps.setString(i++, null);
	        	}
	        	ps.setString(i++, transaction.getSenderDhcAddress().toString());
	        	ps.setString(i++, transaction.getReceiver().toString());
	        	ps.setLong(i++, transaction.getValue().getValue());
	        	ps.setLong(i++, transaction.getFee().getValue());
	        	ps.setString(i++, transaction.getType().toString());
	        	ps.setString(i++, transaction.getBlockHash());
	        	ps.setLong(i++, transaction.getBlockIndex());
	        	ps.setString(i++, transaction.getSignature());
	            ps.setString(i++, transaction.getApp());
	            ps.setLong(i++, transaction.getTimeStamp());
	        	
	        	long start = System.currentTimeMillis();
	            ps.executeUpdate();
	            logger.trace("Query saveTransaction took {} ms. '{}' ", System.currentTimeMillis() - start, sql);
			}
		}.execute();

		List<SimpleEntry<String, String>> merklePath = transaction.getMerklePath();
		if(merklePath != null && !merklePath.isEmpty()) {
			MerklePathEntryStore.getInstance().saveMerklePath(transaction.getBlockHash(), transaction.getTransactionId(), merklePath);
		}

		for(TransactionInput transactionInput: transaction.getInputs()) {
			TransactionInputStore.getInstance().save(transactionInput);
		}

		for(TransactionOutput transactionOutput: transaction.getOutputs()) {
			TransactionOutputStore.getInstance().save(transactionOutput);
		}

		TransactionData expiringData = transaction.getExpiringData();
		if(expiringData != null) {
			TransactionDataStore.getInstance().save(expiringData);
		}
		
		KeywordStore.getInstance().saveKeywords(transaction.getKeywords());
		BlockStore.getInstance().invalidateCachedValue(transaction.getBlockHash());
		logger.trace("Saved transaction {}", transaction);
		DhcAddress myAddress = DhcAddress.getMyDhcAddress();
		if(myAddress.equals(transaction.getReceiver())) {
			logger.trace("Received {} coins from {}", transaction.getValue().toNumberOfCoins(), transaction.getSenderDhcAddress());
		}
		if(myAddress.equals(transaction.getSenderDhcAddress())) {
			logger.trace("Send {} coins to {}", transaction.getValue().toNumberOfCoins(), transaction.getReceiver());
		}
		Registry.getInstance().getMissingOutputsForTransaction().process(transaction.getTransactionId(), transaction.getBlockHash());
		if(!ChainSync.getInstance().isRunning()) {
			Listeners.getInstance().sendEvent(new JoinTransactionEvent(transaction));
			Registry.getInstance().getThinPeerNotifier().notifyTransaction(transaction);
		}
		
		return true;
	}
	
	private Transaction populateTransaction(ResultSet rs) throws SQLException, GeneralSecurityException {
		Transaction transaction = new Transaction();
		String blockhash = rs.getString("blockhash");
		transaction.setBlockHash(blockhash, rs.getLong("blockIndex"));
		transaction.setTransactionId(rs.getString("transaction_id"));
		transaction.setSender(rs.getString("sender"));
		transaction.setSenderDhcAddress(new DhcAddress(rs.getString("senderAddress")));
		transaction.setReceiver(new DhcAddress(rs.getString("receiver")));
		transaction.setValue(new Coin(rs.getLong("value")));
		transaction.setFee(new Coin(rs.getLong("fee")));
		transaction.setType(TransactionType.valueOf(rs.getString("type")));
		transaction.setSignature(rs.getString("signature"));
		transaction.setApp(rs.getString("app"));
		transaction.setTimeStamp(rs.getLong("timeStamp"));
		return transaction;
	}

	public Set<Transaction> getTransactions(String blockhash) {
		Set<Transaction> transactions = new HashSet<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select t.* from trans_action t where t.blockhash = ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, blockhash);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					while (rs.next()) {
						Transaction transaction = populateTransaction(rs);
						transactions.add(transaction);
					}
					logger.trace("Query getTransactions() took {} ms. sql '{}' ", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		for(Transaction transaction: transactions) {
			List<SimpleEntry<String, String>> merklePath = MerklePathEntryStore.getInstance().getMerklePath(blockhash, transaction.getTransactionId());
			if(merklePath != null && !merklePath.isEmpty()) {
				transaction.setMerklePath(merklePath);
			}
		}
		complete(transactions);
		return transactions;
	}
	
	public boolean saveTransactions(Set<Transaction> transactions) throws Exception {
		if (transactions == null || transactions.isEmpty()) {
			return false;
		}
		boolean result = false;
		
		Set<Transaction> sortedDependencySet = new TransactionDependencySorter(transactions).sortByInputsOutputs();
		
		for (Transaction transaction : sortedDependencySet) {
			try {
				if(saveTransaction(transaction)) {
					result = true;
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				logger.error("Exception when saving transaction {}", transaction);
				throw e;
			}
		}
		return result;
	}
	

	
	private void verifyTable() throws Exception {
		
		String tableName = "TRANS_ACTION".toUpperCase();

		new DBExecutor() {
			public void doWork() throws Exception {
				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getTables(null, ConnectionPool.USER, tableName, null);
				if (!rs.next()) {
					s.execute("create table trans_action ("
							+ "ID BIGINT NOT NULL CONSTRAINT trans_action_PK PRIMARY KEY, "
							+ "transaction_id varchar(64) NOT NULL, sender varchar(256), senderAddress varchar(64) NOT NULL, "
							+ " receiver varchar(64) NOT NULL, "
							+ " value bigint not null, "
							+ " fee bigint not null, "
							+ " type varchar(8) not null, "
							+ " blockhash varchar(64) NOT NULL, "
							+ " blockIndex bigint NOT NULL, "
							+ " signature varchar(256) NOT NULL, APP VARCHAR(5), timeStamp bigint NOT NULL "
							+ ")");
					s.execute("ALTER TABLE trans_action ADD CONSTRAINT trans_action_tr_id_bhash UNIQUE (transaction_id, blockhash)");
					s.execute("CREATE INDEX trans_action_blockHash ON trans_action(blockHash)");
					s.execute("CREATE INDEX trans_action_APP ON trans_action(APP)");
					s.execute("CREATE INDEX trans_action_senderAddress ON trans_action(senderAddress)");
					
					s.execute("ALTER TABLE trans_action ADD CONSTRAINT transaction_bhash_FK FOREIGN KEY (blockhash) "
							+ "REFERENCES block(blockhash) ON DELETE CASCADE");
				}
				rs.close();
				
				
				addNewColumn(dbmd, tableName, "timeStamp", "ALTER TABLE " + tableName + " ADD timeStamp bigint NOT NULL");
				addIndex(dbmd, tableName, "trans_action_receiver", "CREATE INDEX trans_action_receiver ON " + tableName + "(receiver)");
				addIndex(dbmd, tableName, "trans_action__id_desc", "CREATE UNIQUE INDEX trans_action_id_desc ON " + tableName + "(id desc)");
				//addView(dbmd, "trans_action_desc".toUpperCase(), "CREATE VIEW trans_action_desc AS SELECT * FROM trans_action order by id desc FETCH FIRST 100 ROWS ONLY");
				
				//s.execute("ALTER TABLE trans_action ALTER COLUMN sender NULL");
			}
		}.execute();
		setId();
		TransactionOutputStore.getInstance();
		TransactionDataStore.getInstance();
		KeywordStore.getInstance();
	}

	public boolean contains(Transaction transaction) {
		boolean[] result = new boolean[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select 1 from trans_action where transaction_id = ? and blockhash = ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
		        	ps.setString(i++, transaction.getTransactionId());
		        	ps.setString(i++, transaction.getBlockHash());
					rs = ps.executeQuery();
					if (rs.next()) {
						result[0] =true;
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result[0];
	}
	
	public void remove(String blockhash) throws Exception {

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "delete from trans_action where blockhash = ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
		        	ps.setString(i++, blockhash);
					ps.executeUpdate();
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("Error removing transaction with blockhash {}", blockhash);
			throw e;
		}
		BlockStore.getInstance().invalidateCachedValue(blockhash);
	}

	private void complete(Transaction transaction) {
		transaction.getInputs().addAll(TransactionInputStore.getInstance().getByInputTransactionId(transaction.getTransactionId(), transaction.getBlockHash()));
		transaction.getOutputs().addAll(TransactionOutputStore.getInstance().getByOutputTransactionId(transaction.getTransactionId(), transaction.getBlockHash()));
		transaction.setExpiringData(TransactionDataStore.getInstance().getExpiringData(transaction.getTransactionId()));
		Keywords keywords = KeywordStore.getInstance().getKeywords(transaction.getTransactionId());
		transaction.setKeywords(keywords);
	}
	
	private void complete(Collection<Transaction> transactions) {
		for (Transaction transaction : transactions) {
			complete(transaction);
		}
	}

	public Set<Transaction> getTransaction(String transactionId) {
		Set<Transaction> transactions = new HashSet<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select t.* from trans_action t where t.transaction_id = ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, transactionId);
					rs = ps.executeQuery();
					while (rs.next()) {
						Transaction transaction = populateTransaction(rs);
						transactions.add(transaction);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		for(Transaction transaction: transactions) {
			List<SimpleEntry<String, String>> merklePath = MerklePathEntryStore.getInstance().getMerklePath(transaction.getBlockHash(), transaction.getTransactionId());
			if(merklePath != null && !merklePath.isEmpty()) {
				transaction.setMerklePath(merklePath);
			}
		}
		complete(transactions);
		return transactions;
	}
	
	public PublicKey getPublicKey(String address) {
		PublicKey[] publicKey = new PublicKey[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select sender from trans_action where senderAddress = ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, address);
					rs = ps.executeQuery();
					if (rs.next()) {

						publicKey[0] = CryptoUtil.loadPublicKey(rs.getString("sender"));

					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return publicKey[0];
	}

	public Transaction getTransaction(String transactionId, String blockhash) {
		Set<Transaction> transactions = new HashSet<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select t.* from trans_action t where t.transaction_id = ? and t.blockhash = ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, transactionId);
					ps.setString(i++, blockhash);
					rs = ps.executeQuery();
					while (rs.next()) {
						Transaction transaction = populateTransaction(rs);
						transactions.add(transaction);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		for(Transaction transaction: transactions) {
			List<SimpleEntry<String, String>> merklePath = MerklePathEntryStore.getInstance().getMerklePath(transaction.getBlockHash(), transaction.getTransactionId());
			if(merklePath != null && !merklePath.isEmpty()) {
				transaction.setMerklePath(merklePath);
			}
		}
		complete(transactions);
		if(transactions.isEmpty()) {
			return null;
		}
		return transactions.iterator().next();
	}

	public void pruneTransactions() {
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "delete from trans_action where blockhash in (select blockHash from block where miner is null)";
					ps = conn.prepareStatement(sql);
					ps.executeUpdate();
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
	}
	
	// returns first 100 transactions without inputs, outputs for display purpose
	public Set<Transaction> getTransactionsByAddress(DhcAddress dhcAddress) {
		long start = System.currentTimeMillis();
		Map<String, Transaction> transactions = new LinkedHashMap<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select t.*, td.*, k.name, k.keyword from ( " + 
							"select * from (select * from trans_action t where t.receiver = ? order by id desc FETCH FIRST 100 ROWS ONLY) a " + 
							"union " + 
							"select * from (select * from trans_action t where t.senderAddress = ? order by id desc FETCH FIRST 100 ROWS ONLY) b ) t " + 
							"left outer join transaction_data td on td.transactionId = t.transaction_id   " + 
							"left outer join keyword k on k.transactionId = t.transaction_id " + 
							"order by t.id desc  FETCH FIRST 100 ROWS ONLY WITH UR";
					logger.trace("Function getTransactionsByAddress sql = {}, dhcAddress = {}", sql, dhcAddress.toString());
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, dhcAddress.toString());
					ps.setString(i++, dhcAddress.toString());
					
					rs = ps.executeQuery();
					Keywords keywords;
					while (rs.next()) {
						String transactionId = rs.getString("transaction_id");
						Transaction transaction = transactions.get(transactionId);
						String name = rs.getString("name");
						if(transaction == null) {
							transaction = populateTransaction(rs);
							String data = rs.getString("data");
							if(data != null) {
								TransactionData transactionData = new TransactionData(data);
								transactionData.setTransactionId(transactionId);
								transactionData.setBlockHash(rs.getString("blockhash"));
								transactionData.setHash(rs.getString("hash"));
								transactionData.setValidForNumberOfBlocks(rs.getLong("validForNumberOfBlocks"));
								transactionData.setExpirationIndex(rs.getLong("expirationIndex"));
								transaction.setExpiringData(transactionData);
							}
							
							transactions.put(transactionId, transaction);
							if(name != null) {
								keywords = new Keywords();
								keywords.setTransactionId(transactionId);
								keywords.setBlockHash(rs.getString("blockhash"));
								String keyword = rs.getString("keyword");
								keywords.put(name, keyword);
								transaction.setKeywords(keywords);
							}
							
						}
						if(name != null) {
							keywords = transaction.getKeywords();
							String keyword = rs.getString("keyword");
							keywords.put(name, keyword);
						}
						
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		logger.trace("Function getTransactionsByAddress took {} ms.", System.currentTimeMillis() - start);
		return new LinkedHashSet<>(transactions.values());
	}

	public Set<Transaction> getTransactionsForApp(String app, DhcAddress address) {
		Map<String, Transaction> transactions = new HashMap<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select t.*, td.*, k.name, k.keyword from trans_action t "
							+ " left outer join transaction_data td on td.transactionId = t.transaction_id "
							+ " left outer join keyword k on k.transactionId = t.transaction_id "
							+ " where t.app = ? and t.senderAddress = ? "
							+ " order by t.id desc FETCH FIRST 100 ROWS ONLY WITH UR";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, app);
					ps.setString(i++, address.toString());
					rs = ps.executeQuery();
					Keywords keywords;
					while (rs.next()) {
						String transactionId = rs.getString("transaction_id");
						Transaction transaction = transactions.get(transactionId);
						String name = rs.getString("name");
						if(transaction == null) {
							transaction = populateTransaction(rs);
							String data = rs.getString("data");
							if(data != null) {
								TransactionData transactionData = new TransactionData(data);
								transactionData.setTransactionId(transactionId);
								transactionData.setBlockHash(rs.getString("blockhash"));
								transactionData.setHash(rs.getString("hash"));
								transactionData.setValidForNumberOfBlocks(rs.getLong("validForNumberOfBlocks"));
								transactionData.setExpirationIndex(rs.getLong("expirationIndex"));
								transaction.setExpiringData(transactionData);
							}
							
							transactions.put(transactionId, transaction);
							if(name != null) {
								keywords = new Keywords();
								keywords.setTransactionId(transactionId);
								keywords.setBlockHash(rs.getString("blockhash"));
								String keyword = rs.getString("keyword");
								keywords.put(name, keyword);
								transaction.setKeywords(keywords);
							}
							
						}
						if(name != null) {
							keywords = transaction.getKeywords();
							String keyword = rs.getString("keyword");
							keywords.put(name, keyword);
						}
						
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return new HashSet<>(transactions.values());
	}

	public Set<JoinLine> getJoinLines(DhcAddress dhcAddress) {
		Set<JoinLine> lines = new LinkedHashSet<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String address = dhcAddress.toString();
					String sql = "select sum(t.value) as amount, k.keyword, count(1) as counter from trans_action t "
							+ " join keyword k on k.transactionId = t.transaction_id "
							+ " where t.app = 'JOIN' and t.receiver = ? and k.name = 'myPosition' "
							+ " group by k.keyword order by k.keyword ";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, address);
					rs = ps.executeQuery();
					while (rs.next()) {
						JoinLine line = new JoinLine(rs.getString("keyword"), rs.getString("counter"), rs.getString("amount"));
						lines.add(line);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return lines;
	}

	public List<SecureMessage> getSecureMessages(DhcAddress dhcAddress) {
		List<SecureMessage> messages = new ArrayList<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select t.*, td.* from trans_action t "
							+ " left outer join transaction_data td on td.transactionId = t.transaction_id "
							+ " where t.app = ? and t.receiver = ? "
							+ " order by timeStamp desc "
							+ " FETCH FIRST 100 ROWS ONLY WITH UR ";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, Applications.MESSAGING);
					ps.setString(i++, dhcAddress.toString());
					rs = ps.executeQuery();
					while (rs.next()) {
						
						SecureMessage message = new SecureMessage();
						message.setExpire(rs.getLong("validForNumberOfBlocks"));
						message.setFee(new Coin(rs.getLong("fee")));
						message.setRecipient(rs.getString("receiver"));
						message.setSender(rs.getString("sender"));
						message.setText(rs.getString("data"));
						message.setTimeStamp(rs.getLong("timeStamp"));
						message.setTransactionId(rs.getString("transaction_id"));
						message.setValue(new Coin(rs.getLong("value")));
						messages.add(message);
						
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return messages;
	}

	public PublicKey getPublicKey(DhcAddress dhcAddress) {
		PublicKey[] key = new PublicKey[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select t.sender from trans_action t "
							+ " where t.senderAddress = ? "
							+ " WITH UR";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, dhcAddress.toString());
					rs = ps.executeQuery();
					if (rs.next()) {
						key[0] = CryptoUtil.loadPublicKey(rs.getString("sender"));
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return key[0];
	}

	public Ratee getRatee(String transactionId) {
		Ratee[] result = new Ratee[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select b.timeStamp, k.keyword accountName, tdata.data description, t.transaction_id, t.senderAddress, " + 
							"	sum( " + 
							"	CASE " + 
							"		WHEN k2.keyword='Yes' THEN t1.fee " + 
							"		WHEN k2.keyword='No' THEN -t1.fee " + 
							"		ELSE 0 " + 
							"	END) totalRating " + 
							" from trans_action t " + 
							" join block b on b.blockHash = t.blockhash " + 
							" join keyword k on t.transaction_id = k.transactionid and k.name ='create' " + 
							" join keyword kk on t.transaction_id = kk.transactionid and kk.name ='first' and k.keyword = kk.keyword " + 
							" left outer join transaction_data tdata on tdata.transactionId = t.transaction_id " + 
							" left outer join keyword kt on t.transaction_id = kt.transactionid and kt.name = 'transactionId' " +
							" left outer join keyword k1 on t.transaction_id = k1.keyword and k1.name ='transactionId' " + 
							" left outer join keyword k3 on k1.transactionid = k3.transactionid and k3.name = 'rater' " +
							" left outer join keyword k2 on k3.transactionid = k2.transactionid and k2.name ='rating' " + 
							" left outer join trans_action t1 on t1.transaction_id = k2.transactionid and t1.receiver=t.receiver " + 
							" left outer join keyword kd on t.transaction_id = kd.keyword and kd.name ='delete' " + 
							" left outer join trans_action td on td.transaction_id = kd.transactionid and t.senderAddress = td.senderAddress " +
							" where t.app = 'ratee' and t.transaction_id = ? and td.transaction_id is null and kt.name is null " + 
							" group by b.timeStamp, k.keyword, tdata.data, t.transaction_id, t.senderAddress " + 
							" order by totalRating desc, b.timeStamp FETCH FIRST 100 ROWS ONLY WITH UR ";
					logger.trace("transactionId={}, sql={}", transactionId, sql);
					ps = conn.prepareStatement(sql);
					ps.setString(1, transactionId);
					rs = ps.executeQuery();
					if (rs.next()) {
						Ratee ratee = new Ratee();
						ratee.setName(rs.getString("accountName"));
						ratee.setDescription(rs.getString("description"));
						ratee.setTimeStamp(rs.getLong("timeStamp"));
						ratee.setTransactionId(rs.getString("transaction_id"));
						ratee.setTotalRating(rs.getLong("totalRating"));
						ratee.setCreatorDhcAddress(rs.getString("senderAddress"));
						result[0] = ratee;
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result[0];
	}
	
	public List<Ratee> getPosts(DhcAddress dhcAddress) {
		Map<String, Ratee> map = new LinkedHashMap<String, Ratee>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select b.timeStamp, k.keyword accountName, tdata.data description, t.transaction_id " + 
							"	, sum( " + 
							"	CASE " + 
							"		WHEN k2.keyword='Yes' THEN t1.fee " + 
							"		WHEN k2.keyword='No' THEN -t1.fee " + 
							"		ELSE 0 " + 
							"	END) totalRating " + 
							" from trans_action t " + 
							" join block b on b.blockHash = t.blockhash " + 
							" join keyword k on t.transaction_id = k.transactionid and k.name ='create' and t.senderAddress = ? " + 
							" join keyword kk on t.transaction_id = kk.transactionid and kk.name ='first' and k.keyword = kk.keyword " + 
							" left outer join transaction_data tdata on tdata.transactionId = t.transaction_id " + 
							" left outer join keyword kt on t.transaction_id = kt.transactionid and kt.name ='transactionId' " +
							" left outer join keyword k1 on t.transaction_id = k1.keyword and k1.name ='transactionId' " + 
							" left outer join keyword k3 on k1.transactionid = k3.transactionid and k3.name = 'rater' " +
							" left outer join keyword k2 on k3.transactionid = k2.transactionid and k2.name ='rating' " + 
							" left outer join trans_action t1 on t1.transaction_id = k2.transactionid and t1.receiver = t.receiver " + 
							" left outer join keyword kd on t.transaction_id = kd.keyword and kd.name ='delete' " +
							" left outer join trans_action td on td.transaction_id = kd.transactionid and td.senderAddress = t.senderAddress " +
							" where t.app = 'ratee' and td.transaction_id is null and kt.name is null " + 
							" group by b.timeStamp, k.keyword, tdata.data, t.transaction_id " + 
							" order by totalRating desc, b.timeStamp FETCH FIRST 100 ROWS ONLY WITH UR ";
					logger.trace("dhcAddress={}, sql={}", dhcAddress, sql);
					ps = conn.prepareStatement(sql);
					ps.setString(1, dhcAddress.getAddress());
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					logger.trace("query took {} ms to execute", System.currentTimeMillis() - start);
					while (rs.next()) {
						Ratee ratee = new Ratee();
						ratee.setName(rs.getString("accountName"));
						ratee.setDescription(rs.getString("description"));
						ratee.setTimeStamp(rs.getLong("timeStamp"));
						ratee.setTransactionId(rs.getString("transaction_id"));
						ratee.setTotalRating(rs.getLong("totalRating"));
						ratee.setCreatorDhcAddress(dhcAddress.getAddress());
						map.put(ratee.getTransactionId(), ratee);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return new ArrayList<Ratee>(map.values());
	}

	public List<Ratee> getRatees(String account, Set<String> words) {
		if(!"".equals(account.trim())) {
			return getRatees(account);
		}
		return getRatees(words);
	}
	
	private List<Ratee> getRatees(Set<String> words) {
		String first = words.iterator().next();
		words.remove(first);
		List<Ratee> list = new ArrayList<Ratee>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select b.timeStamp, k.keyword accountName, tdata.data description, kt.keyword transactionId, t.senderAddress, "
							+ "	sum( "
							+ "	CASE " 
							+ "		WHEN kw2.keyword='Yes' THEN t1.fee "
							+ "		WHEN kw2.keyword='No' THEN -t1.fee "
							+ "		ELSE 0 "
							+ "	END) totalRating "
							+ " from trans_action t " // t is transaction for keyword first
							+ " join block b on b.blockHash = t.blockhash "
							+ " join keyword k on t.transaction_id = k.transactionid and k.name ='create' "
							+ " join keyword kk on t.transaction_id = kk.transactionid and kk.name ='first' "
							+ " join keyword kt on t.transaction_id = kt.transactionid and kt.name ='transactionId' "
							
							
							;
					
					int i = 0;
					String where = "";
					for(@SuppressWarnings("unused") String word: words) {
						i++;
						sql = sql + " join keyword k" + i + " on t.transaction_id = k" + i + ".transactionid ";
						where = where + " and k" + i + ".name = ? ";
					}
					
					sql = sql + " left outer join keyword kw1 on kt.keyword = kw1.keyword and kw1.name ='transactionId' "
							+ " left outer join keyword kw2 on kw1.transactionid = kw2.transactionid and kw2.name ='rating' "
							+ " left outer join keyword kw3 on kw3.transactionid = kw2.transactionid and kw3.name ='rater' "
							+ " left outer join trans_action t1 on t1.transaction_id = kw2.transactionid and t1.receiver = t.receiver "; //t1 is transaction for rating
					
					
					sql = sql + " left outer join keyword kd on kt.keyword = kd.keyword and kd.name ='delete' "
							+ " left outer join trans_action td on td.transaction_id = kd.transactionid and td.senderAddress = t.senderAddress "
							+ " left outer join transaction_data tdata on tdata.transactionId = t.transaction_id "
					        + " where t.app = 'ratee' and kk.keyword = ? and td.transaction_id is null and kw3.name is null " + where
							+ " group by b.timeStamp, k.keyword, tdata.data, kt.keyword, t.senderAddress "
							+ " order by totalRating desc, b.timeStamp "
							+ " FETCH FIRST 100 ROWS ONLY WITH UR ";
					
					logger.trace("first={}, words={}, sql={}", first, words, sql);
					ps = conn.prepareStatement(sql);
					
					i = 1;
					ps.setString(i++, first);
					for(String word: words) {
						ps.setString(i++, word);
					}
					
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					logger.trace("query took {} ms", System.currentTimeMillis() - start);
					while (rs.next()) {
						Ratee ratee = new Ratee();
						ratee.setName(rs.getString("accountName"));
						ratee.setDescription(rs.getString("description"));
						ratee.setTimeStamp(rs.getLong("timeStamp"));
						ratee.setTransactionId(rs.getString("transactionId"));
						ratee.setTotalRating(rs.getLong("totalRating"));
						ratee.setCreatorDhcAddress(rs.getString("senderAddress"));
						list.add(ratee);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return list;
	}

	private List<Ratee> getRatees(String account) {
		Map<String, Ratee> map = new LinkedHashMap<String, Ratee>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select b.timeStamp, k.keyword accountName, tdata.data description, t.transaction_id, t.senderAddress " + 
							"	, sum( " + 
							"	CASE " + 
							"		WHEN k2.keyword='Yes' THEN t1.fee " + 
							"		WHEN k2.keyword='No' THEN -t1.fee " + 
							"		ELSE 0 " + 
							"	END) totalRating " + 
							" from trans_action t " + 
							" join block b on b.blockHash = t.blockhash " + 
							" join keyword k on t.transaction_id = k.transactionid and k.name = 'create' " + 
							" join keyword kk on t.transaction_id = kk.transactionid and kk.name = 'first' and k.keyword = kk.keyword " + 
							" left outer join transaction_data tdata on tdata.transactionId = t.transaction_id " + 
							" left outer join keyword kt on t.transaction_id = kt.transactionid and kt.name = 'transactionId' " + 
							" left outer join keyword k1 on t.transaction_id = k1.keyword and k1.name = 'transactionId' " + 
							" left outer join keyword k3 on k1.transactionid = k3.transactionid and k3.name = 'rater' " +
							" left outer join keyword k2 on k3.transactionid = k2.transactionid and k2.name = 'rating' " + 
							" left outer join trans_action t1 on t1.transaction_id = k2.transactionid and t1.receiver = t.receiver " + 
							" left outer join keyword kd on t.transaction_id = kd.keyword and kd.name = 'delete' " +
							" left outer join trans_action td on td.transaction_id = kd.transactionid and t.senderAddress = td.senderAddress " +
							" where t.app = 'ratee' and k.keyword = ? and td.transaction_id is null and kt.name is null " + 
							" group by b.timeStamp, k.keyword, tdata.data, t.transaction_id, t.senderAddress " + 
							" order by totalRating desc, b.timeStamp FETCH FIRST 100 ROWS ONLY WITH UR ";
					logger.trace("account={}, sql={}", account, sql);
					ps = conn.prepareStatement(sql);
					ps.setString(1, account);
					
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					logger.trace("query took {} ms", System.currentTimeMillis() - start);
					
					while (rs.next()) {
						Ratee ratee = new Ratee();
						ratee.setName(rs.getString("accountName"));
						ratee.setDescription(rs.getString("description"));
						ratee.setTimeStamp(rs.getLong("timeStamp"));
						ratee.setTransactionId(rs.getString("transaction_id"));
						ratee.setTotalRating(rs.getLong("totalRating"));
						ratee.setCreatorDhcAddress(rs.getString("senderAddress"));
						map.put(ratee.getTransactionId(), ratee);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return new ArrayList<Ratee>(map.values());
	}

	public List<Rating> getRatings(String rater) {
		List<Rating> list = new ArrayList<Rating>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select b.timeStamp, k.keyword ratee, tdata.data comment, kk.keyword rate, kt.keyword transactionId"
							+ " from trans_action t "
							+ " join block b on b.blockHash = t.blockhash "
							+ " join keyword kr on t.transaction_id = kr.transactionid and kr.name ='rater'"
							+ " join keyword k on t.transaction_id = k.transactionid and k.name ='ratee' "
							+ " join keyword kk on t.transaction_id = kk.transactionid and kk.name ='rating' "
							+ " join keyword kt on t.transaction_id = kt.transactionid and kt.name ='transactionId' "
							+ " left outer join transaction_data tdata on tdata.transactionId = t.transaction_id "
							+ " left outer join keyword kd on kt.keyword = kd.keyword and kd.name = 'delete' "
							+ " left outer join trans_action td on td.transaction_id = kd.transactionid and t.senderAddress = ? "
							+ " where t.app = 'ratee' and kr.keyword = ? and td.transaction_id is null "
							+ " order by b.timeStamp desc FETCH FIRST 100 ROWS ONLY WITH UR ";
					logger.trace("rater={}, sql={}", rater, sql);
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, rater);
					ps.setString(i++, rater);
					rs = ps.executeQuery();
					while (rs.next()) {
						Rating rating = new Rating();
						
						rating.setRater(rater);
						rating.setComment(rs.getString("comment"));
						rating.setRate(rs.getString("rate"));
						rating.setRatee(rs.getString("ratee"));
						rating.setTimeStamp(rs.getLong("timeStamp"));
						rating.setTransactionId(rs.getString("transactionid"));
						list.add(rating);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return list;
	}

	public List<Rating> getRatings(String account, String transactionId) {
		Set<Rating> set = new LinkedHashSet<Rating>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select b.timeStamp, k.keyword ratee, tdata.data comment, kk.keyword rate, t.sender, kt.keyword transactionId "
							+ " from trans_action t "
							+ " join block b on b.blockHash = t.blockhash "
							+ " join keyword k on t.transaction_id = k.transactionid and k.name ='ratee' "
							+ " join keyword kk on t.transaction_id = kk.transactionid and kk.name ='rating' "
							+ " join keyword kt on t.transaction_id = kt.transactionid and kt.name ='transactionId' "
							+ " left outer join keyword kd on kt.keyword = kd.keyword and kd.name = 'delete' "
							+ " left outer join transaction_data tdata on tdata.transactionId = t.transaction_id "
							+ " left outer join trans_action td on td.transaction_id = kd.transactionid and t.senderAddress = td.senderAddress "
							+ " where t.app = 'ratee' and k.keyword = ? and kt.keyword = ? and t.receiver = ? and td.transaction_id is null "
							+ " order by b.timeStamp desc FETCH FIRST 100 ROWS ONLY WITH UR ";
					logger.trace("account={}, transactionId={}, sql={}", account, transactionId, sql);
					String receiver = CryptoUtil.getDhcAddressFromString(account).getAddress();
					
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, account);
					ps.setString(i++, transactionId);
					ps.setString(i++, receiver);
					
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					logger.trace("query took {} ms", System.currentTimeMillis() - start);
					
					while (rs.next()) {
						Rating rating = new Rating();
						
						Transaction transaction = new Transaction();
						transaction.setSender(rs.getString("sender"));
						rating.setRater(transaction.getSenderDhcAddress().getAddress());
						rating.setComment(rs.getString("comment"));
						rating.setRate(rs.getString("rate"));
						rating.setRatee(rs.getString("ratee"));
						rating.setTimeStamp(rs.getLong("timeStamp"));
						rating.setTransactionId(rs.getString("transactionid"));
						set.add(rating);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return new ArrayList<Rating>(set);
	}

	public Set<Transaction> getFindFaucetTransactions(DhcAddress dhcAddress, String ip) {
		Set<Transaction> transactions = new HashSet<>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = 
							  " select t.* from trans_action t "
							+ " join keyword k on k.transactionId = t.transaction_id and k.name = 'ip' "
							+ " where t.app = ? and t.receiver = ? and timeStamp > ? "
							+ " UNION ALL "
							+ " select t.* from trans_action t "
							+ " join keyword k on k.transactionId = t.transaction_id and k.name = 'ip' "
							+ " where t.app = ? and k.keyword = ? and timeStamp > ? "
							+ " FETCH FIRST 100 ROWS ONLY WITH UR ";
					ps = conn.prepareStatement(sql);
					int i = 1;
					long searchTimeFrom = System.currentTimeMillis() - Constants.HOUR * 24;
					ps.setString(i++, Applications.FAUCET);
					ps.setString(i++, dhcAddress.getAddress());
					ps.setLong  (i++, searchTimeFrom);
					ps.setString(i++, Applications.FAUCET);
					ps.setString(i++, ip);
					ps.setLong  (i++, searchTimeFrom);
					logger.info("app={}, receiver={}, ip={}, sql={}", Applications.FAUCET, dhcAddress.getAddress(), ip, sql);
					rs = ps.executeQuery();
					while (rs.next()) {
						Transaction transaction = populateTransaction(rs);
						transactions.add(transaction);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.info("# transactions: {}", transactions.size());
		return transactions;
	}


}
