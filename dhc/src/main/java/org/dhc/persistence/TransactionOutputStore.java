package org.dhc.persistence;

import java.sql.DatabaseMetaData;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.CreateTransactionException;
import org.dhc.blockchain.TransactionInput;
import org.dhc.blockchain.TransactionOutput;
import org.dhc.util.Coin;
import org.dhc.util.Constants;
import org.dhc.util.StringUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class TransactionOutputStore {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final TransactionOutputStore instance = new TransactionOutputStore();
	
	private final AtomicLong id = new AtomicLong();

	private TransactionOutputStore() {
		try {
			verifyTable();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static TransactionOutputStore getInstance() {
		return instance;
	}
	
	private void setId() {
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select coalesce(max(id) + 1, 0) as id from transaction_output";
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
	
	// Return unspent output for outputId or null if there is no output or if output is already spent
	public TransactionOutput getUnspentByOutputId(String outputId) {
		TransactionOutput[] transactionOutput = new TransactionOutput[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {

					String sql = "select tro.recipient, tro.value, tro.outputTransactionId, tro. outputId, tro.outputBlockIndex, tro.outputBlockHash "
							+ " from transaction_output tro left outer join transaction_input tri on tro.outputId = tri.outputId "
							+ " where tro.outputId = ? and tri.id is null";
					ps = conn.prepareStatement(sql);
					ps.setString(1, outputId);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					if (rs.next()) {
						String recipient = rs.getString("recipient");
						Coin value = new Coin(rs.getLong("value"));
						transactionOutput[0] = new TransactionOutput(new DhcAddress(recipient), value);
						transactionOutput[0].setOutputId(rs.getString("outputId"));
						transactionOutput[0].setOutputBlockIndex(rs.getLong("outputBlockIndex"));
						transactionOutput[0].setOutputBlockHash(rs.getString("outputBlockHash"));
						transactionOutput[0].setOutputTransactionId(rs.getString("outputTransactionId"));
					}
					logger.trace("Query TransactionOutputStore.get({}) took {} ms. '{}' ", outputId, System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return transactionOutput[0];
	}
	
	//this APi is needed in case output was spent in a competing block
	public TransactionOutput getByOutputId(String outputId) {
		TransactionOutput[] transactionOutput = new TransactionOutput[1];
		try {
			new DBExecutor() {
				public void doWork() throws Exception {

					String sql = "select recipient, value, outputTransactionId, outputId, outputBlockIndex, outputBlockHash from transaction_output where outputId = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, outputId);
					rs = ps.executeQuery();
					if (rs.next()) {
						String recipient = rs.getString("recipient");
						Coin value = new Coin(rs.getLong("value"));
						transactionOutput[0] = new TransactionOutput(new DhcAddress(recipient), value);
						transactionOutput[0].setOutputId(rs.getString("outputId"));
						transactionOutput[0].setOutputBlockIndex(rs.getLong("outputBlockIndex"));
						transactionOutput[0].setOutputBlockHash(rs.getString("outputBlockHash"));
						transactionOutput[0].setOutputTransactionId(rs.getString("outputTransactionId"));
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return transactionOutput[0];
	}
	
	public void save(TransactionOutput transactionOutput) throws Exception {
		logger.trace("Will try to save transactionOutput {}", transactionOutput);
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "insert into transaction_output (id, outputId, recipient, value, outputTransactionId, outputBlockIndex, outputBlockHash, recipient_binary) "
							+ "values( ?, ?, ?, ?, ?, ?, ?, ?) ";

					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setLong(i++, id.getAndIncrement());
					ps.setString(i++, transactionOutput.getOutputId());
					ps.setString(i++, transactionOutput.getRecipient().toString());
					ps.setLong(i++, transactionOutput.getValue().getValue());
					ps.setString(i++, transactionOutput.getOutputTransactionId());
					ps.setLong(i++, transactionOutput.getOutputBlockIndex());
					ps.setString(i++, transactionOutput.getOutputBlockHash());
					ps.setString(i++, transactionOutput.getRecipient().getBinary(20));

					long start = System.currentTimeMillis();
		            ps.executeUpdate();
		            logger.trace("Query saveTransactionOutput took {} ms. '{}' ", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error("Exception when saving {}", transactionOutput);
			throw e;
		}
		logger.trace("Saved transactionOutput {}", transactionOutput);
	}

	public Set<TransactionOutput> getByOutputTransactionId(String outputTransactionId, String outputBlockHash) {
		Set<TransactionOutput> inputs = new LinkedHashSet<TransactionOutput>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from transaction_output where outputTransactionId = ? and outputBlockHash = ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, outputTransactionId);
					ps.setString(i++, outputBlockHash);
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					while (rs.next()) {
						TransactionOutput transactionOutput = new TransactionOutput(new DhcAddress(rs.getString("recipient")), new Coin(rs.getLong("value")));
						transactionOutput.setOutputId(rs.getString("outputId"));
						transactionOutput.setOutputBlockIndex(rs.getLong("outputBlockIndex"));
						transactionOutput.setOutputBlockHash(rs.getString("outputBlockHash"));
						transactionOutput.setOutputTransactionId(rs.getString("outputTransactionId"));
						
						inputs.add(transactionOutput);
					}
					logger.trace("Query getByOutputTransactionId() took {} ms. sql '{}' ", System.currentTimeMillis() - start, sql);
					
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return inputs;
	}

	private void verifyTable() throws Exception {
		TransactionStore.getInstance();
		new DBExecutor() {
			public void doWork() throws Exception {
				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getTables(null, ConnectionPool.USER, "TRANSACTION_OUTPUT", null);
				if (!rs.next()) {
					s.execute("create table transaction_output (ID BIGINT NOT NULL CONSTRAINT transaction_output_PK PRIMARY KEY, " 
							+ " outputId varchar(64) NOT NULL, "
							+ " recipient varchar(64) NOT NULL, " 
							+ " value bigint not null, " 
							+ " outputTransactionId varchar(64) NOT NULL, "
							+ " outputBlockIndex bigint not null," 
							+ " outputBlockHash varchar(64) NOT NULL " 
							+ " )");
					s.execute("CREATE INDEX transaction_output_outputTransactionId ON transaction_output(outputTransactionId)");
					s.execute("CREATE INDEX transaction_output_outputBlockHash ON transaction_output(outputBlockHash)");
					s.execute("CREATE INDEX transaction_output_recipient ON transaction_output(recipient)");
					s.execute("CREATE INDEX transaction_output_outputId ON transaction_output(outputId)");
					s.execute("CREATE INDEX transaction_output_outputBlockIndex ON transaction_output(outputBlockIndex)");
					s.execute("ALTER TABLE transaction_output ADD CONSTRAINT trans_output_outputId_bhash UNIQUE (outputId, outputBlockHash)");
					s.execute("ALTER TABLE transaction_output ADD CONSTRAINT transaction_output_tr_id_BLOCKHASH_FK FOREIGN KEY (outputTransactionId, outputBlockHash) "
							+ "REFERENCES trans_action(transaction_id, blockHash) ON DELETE CASCADE");
					
					
				}
				rs.close();
				
				addIndex(dbmd, "TRANSACTION_OUTPUT", "transaction_output_outputBlockIndex", "CREATE INDEX transaction_output_outputBlockIndex ON transaction_output(outputBlockIndex)");
				
				addNewColumn(dbmd, "TRANSACTION_OUTPUT", "recipient_binary", "ALTER TABLE TRANSACTION_OUTPUT ADD recipient_binary varchar(20)");
				addIndex(dbmd, "TRANSACTION_OUTPUT", "trout_recipient_binaryIndex", "CREATE INDEX trout_recipient_binaryIndex ON transaction_output(recipient_binary)");
			}
		}.execute();
		setId();
		TransactionInputStore.getInstance();
	}
	
	public Coin sumByRecipient(String recipient, Block block) {

		Set<String> blockhashes = Blockchain.getInstance().getBranchBlockhashes(block);
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		long branchIndex = minCompeting == 0 ? block.getIndex() : minCompeting;
		logger.trace("\t\tbranchIndex={}, blockhashes={}", branchIndex, blockhashes);
		Coin[] coin = new Coin[1];
		long startSearchIndex = Blockchain.getInstance().getIndex() - Constants.MAX_NUMBER_OF_BLOCKS;
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String inClause = StringUtil.createInClause(blockhashes.size());
					String sql = "select sum(tro.value) sumByRecipient from transaction_output tro "
							+ " left outer join transaction_input tri on tro.outputId = tri.outputId and tri.inputBlockIndex > ? "
							+ " and ( tri.inputBlockIndex < ? or tri.inputBlockHash in " + inClause + " )"
							+ " where tro.recipient = ? and tri.id is null and tro.outputBlockIndex > ?"
							+ " and ( tro.outputBlockIndex < ? or tro.outputBlockHash in " + inClause + " ) ";
					ps = conn.prepareStatement(sql);
					int i = 1;
					
					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, branchIndex);
					
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}

					ps.setString(i++, recipient);
					
					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, branchIndex);
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}

					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					if (rs.next()) {
						coin[0] = new Coin(rs.getLong("sumByRecipient"));
					}
					logger.trace("Query sumByRecipient took {} ms. '{}'", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.trace("Returning balance {}", coin[0].toNumberOfCoins());
		return coin[0];

	}
	
	public Coin getBalanceForAll(String blockhash, String key) {
		Block block = Blockchain.getInstance().getByHash(blockhash);
		Set<String> blockhashes = Blockchain.getInstance().getBranchBlockhashes(block);
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		long branchIndex = minCompeting == 0 ? block.getIndex() : minCompeting;
		logger.trace("\t\tbranchIndex={}, blockhashes={}", branchIndex, blockhashes);
		Coin[] coin = new Coin[1];
		long startSearchIndex = Blockchain.getInstance().getIndex() - Constants.MAX_NUMBER_OF_BLOCKS;
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String inClause = StringUtil.createInClause(blockhashes.size());
					String sql = "select sum(tro.value) sumByRecipient from transaction_output tro "
							+ " left outer join transaction_input tri on tro.outputId = tri.outputId and tri.inputBlockIndex > ? "
							+ " and ( tri.inputBlockIndex < ? or tri.inputBlockHash in " + inClause + " )"
							+ " where tro.recipient_binary like ? and tri.id is null and tro.outputBlockIndex > ?"
							+ " and ( tro.outputBlockIndex < ? or tro.outputBlockHash in " + inClause + " ) ";
					ps = conn.prepareStatement(sql);
					int i = 1;
					
					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, branchIndex);
					
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}

					ps.setString(i++, key + "%");
					
					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, branchIndex);
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}

					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					if (rs.next()) {
						coin[0] = new Coin(rs.getLong("sumByRecipient"));
					}
					logger.trace("Query getBalanceForAll took {} ms. '{}'", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.trace("Returning balance {}", coin[0].toNumberOfCoins());
		return coin[0];

	}
	
	public Set<TransactionInput> getInputs(Block block, String recipient) {

		Set<String> blockhashes = Blockchain.getInstance().getBranchBlockhashes(block);
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		long branchIndex = minCompeting == 0 ? block.getIndex() : minCompeting;
		logger.trace("\t\tbranchIndex={}, blockhashes={}", branchIndex, blockhashes);
		Set<TransactionInput> set = new HashSet<TransactionInput>();
		long startSearchIndex = Blockchain.getInstance().getIndex() - Constants.MAX_NUMBER_OF_BLOCKS;
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String inClause = StringUtil.createInClause(blockhashes.size());
					
					String sql = "select tro.recipient, tro.value, tro.outputId, tro.outputBlockHash, tro.outputBlockIndex, tro.outputTransactionId from transaction_output tro "
							+ " left outer join transaction_input tri on tro.outputId = tri.outputId and tri.inputBlockIndex > ? "
							+ " and ( tri.inputBlockIndex < ? or tri.inputBlockHash in " + inClause + " )"
							+ " where tro.recipient = ? and tri.id is null and tro.outputBlockIndex > ? "
							+ " and ( tro.outputBlockIndex < ? or tro.outputBlockHash in " + inClause + " ) ";
					ps = conn.prepareStatement(sql);
					int i = 1;
					
					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, branchIndex);
					
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}

					ps.setString(i++, recipient);
					
					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, branchIndex);
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}
					
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();

					while (rs.next()) {
						long blockIndex = rs.getLong("outputBlockIndex");
						String outputBlockHash = rs.getString("outputBlockHash");
						if (branchIndex <= blockIndex && !blockhashes.contains(outputBlockHash)) {
							continue;
						}
						TransactionInput input = new TransactionInput();
						input.setOutputId(rs.getString("outputId"));
						input.setSender(new DhcAddress(rs.getString("recipient")));
						input.setValue(new Coin(rs.getLong("value")));
						input.setOutputBlockIndex(blockIndex);
						input.setOutputBlockHash(outputBlockHash);
						input.setOutputTransactionId(rs.getString("outputTransactionId"));
						set.add(input);

					}
					logger.trace("Query getInputs took {} ms. '{}'", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new CreateTransactionException(e.getMessage());
		}

		return set;

	}
	
	public Set<TransactionInput> getInputs(Coin coin, Block block) {

		Set<String> blockhashes = Blockchain.getInstance().getBranchBlockhashes(block);
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		long branchIndex = minCompeting == 0 ? block.getIndex() : minCompeting;
		logger.trace("\t\tbranchIndex={}, blockhashes={}", branchIndex, blockhashes);
		Set<TransactionInput> set = new HashSet<TransactionInput>();
		Coin[] sum = new Coin[1];
		long startSearchIndex = Blockchain.getInstance().getIndex() - Constants.MAX_NUMBER_OF_BLOCKS;
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String inClause = StringUtil.createInClause(blockhashes.size());
					
					String sql = "select tro.recipient, tro.value, tro.outputId, tro.outputBlockHash, tro.outputBlockIndex, tro.outputTransactionId from transaction_output tro "
							+ " left outer join transaction_input tri on tro.outputId = tri.outputId "
							+ " and ( tri.inputBlockIndex < ? or tri.inputBlockHash in " + inClause + " ) and tri.inputBlockIndex > ? "
							+ " where tro.recipient = ? and tri.id is null "
							+ " and ( tro.outputBlockIndex < ? or tro.outputBlockHash in " + inClause + " ) "
							+ " and tro.outputBlockIndex > ? ";
					
					ps = conn.prepareStatement(sql);
					int i = 1;

					ps.setLong(i++, branchIndex);
					
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}
					
					ps.setLong(i++, startSearchIndex);
					
					String recipient = DhcAddress.getMyDhcAddress().toString();
					logger.trace("\t\trecipient={}", recipient);
					ps.setString(i++, recipient);

					ps.setLong(i++, branchIndex);
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}
					
					ps.setLong(i++, startSearchIndex);
					
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();
					
					sum[0] = Coin.ZERO;
					while (rs.next()) {
						long blockIndex = rs.getLong("outputBlockIndex");
						String outputBlockHash = rs.getString("outputBlockHash");
						if (branchIndex <= blockIndex && !blockhashes.contains(outputBlockHash)) {
							continue;
						}
						TransactionInput input = new TransactionInput();
						input.setOutputId(rs.getString("outputId"));
						input.setSender(new DhcAddress(rs.getString("recipient")));
						input.setValue(new Coin(rs.getLong("value")));
						input.setOutputBlockIndex(blockIndex);
						input.setOutputBlockHash(outputBlockHash);
						input.setOutputTransactionId(rs.getString("outputTransactionId"));
						set.add(input);
						sum[0] = sum[0].add(input.getValue());
					}
					logger.trace("Query getInputs took {} ms. '{}'", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new CreateTransactionException(e.getMessage());
		}
		if (sum[0].less(coin)) {
			String message = String.format("Sum of found input %s is less than required amount %s", sum[0].toNumberOfCoins(), coin.toNumberOfCoins());
			throw new CreateTransactionException(message);
		}
		return set;

	}
	
	public Set<String> getPruningRecipients() {
		
		Set<String> set = new HashSet<>();
		long startSearchIndex = Blockchain.getInstance().getIndex() - Constants.MAX_NUMBER_OF_BLOCKS;

		try {
			new DBExecutor() {
				public void doWork() throws Exception {

					String sql = " select distinct tro.recipient " + 
							" from transaction_output tro " + 
							" left outer join transaction_input tri on tro.outputId = tri.outputId  and tri.INPUTBLOCKINDEX > ? " + 
							" where tri.id is null and tro.OUTPUTBLOCKINDEX > ? "
							+ " and tro.outputBlockIndex < ? and tro.recipient_binary like ? and tro.value <> 0";

					ps = conn.prepareStatement(sql);
					int i = 1;

					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, startSearchIndex + Constants.MAX_NUMBER_OF_BLOCKS / 2);
					int power = Blockchain.getInstance().getPower();
					DhcAddress myAddress = DhcAddress.getMyDhcAddress();
					ps.setString(i++, myAddress.getBinary(power) + "%");

					
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();

					while (rs.next()) {
						String recipient = rs.getString("recipient");
						if(!myAddress.isFromTheSameShard(new DhcAddress(recipient), power)) {
							continue;
						}
						set.add(recipient);
					}
					String str = sql.replace("?", "%s");
					str = String.format(str, startSearchIndex, startSearchIndex, startSearchIndex + Constants.MAX_NUMBER_OF_BLOCKS / 2, myAddress.getBinary(power) + "%");
					logger.trace("Query getPruningRecipients took {} ms. '{}'", System.currentTimeMillis() - start, str);
					if(!set.isEmpty()) {
						logger.info("Will prune these addresses: {}", set);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new CreateTransactionException(e.getMessage());
		}

		return set;
	}

	public Set<TransactionOutput> getTransactionOutputs(DhcAddress dhcAddress) {
		
		String recipient = dhcAddress.toString();
		
		Blockchain blockchain = Blockchain.getInstance();
		Block block = blockchain.getLastBlocks().get(0);
		
		Set<String> blockhashes = blockchain.getBranchBlockhashes(block);
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		long branchIndex = minCompeting == 0 ? block.getIndex() : minCompeting;
		logger.trace("\t\tbranchIndex={}, blockhashes={}", branchIndex, blockhashes);
		Set<TransactionOutput> set = new HashSet<TransactionOutput>();
		long startSearchIndex = blockchain.getIndex() - Constants.MAX_NUMBER_OF_BLOCKS;
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String inClause = StringUtil.createInClause(blockhashes.size());
					
					String sql = "select tro.recipient, tro.value, tro.outputId, tro.outputBlockHash, tro.outputBlockIndex, tro.outputTransactionId from transaction_output tro "
							+ " left outer join transaction_input tri on tro.outputId = tri.outputId and tri.inputBlockIndex > ? "
							+ " and ( tri.inputBlockIndex < ? or tri.inputBlockHash in " + inClause + " )"
							+ " where tro.recipient = ? and tri.id is null and tro.outputBlockIndex > ? "
							+ " and ( tro.outputBlockIndex < ? or tro.outputBlockHash in " + inClause + " ) ";
					ps = conn.prepareStatement(sql);
					int i = 1;
					
					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, branchIndex);
					
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}

					ps.setString(i++, recipient);
					
					ps.setLong(i++, startSearchIndex);
					ps.setLong(i++, branchIndex);
					for (String blockhash : blockhashes) {
						ps.setString(i++, blockhash);
					}
					
					long start = System.currentTimeMillis();
					rs = ps.executeQuery();

					while (rs.next()) {
						long blockIndex = rs.getLong("outputBlockIndex");
						String outputBlockHash = rs.getString("outputBlockHash");
						if (branchIndex <= blockIndex && !blockhashes.contains(outputBlockHash)) {
							continue;
						}
						TransactionOutput output = new TransactionOutput();
						output.setOutputId(rs.getString("outputId"));
						output.setRecipient(new DhcAddress(rs.getString("recipient")));
						output.setValue(new Coin(rs.getLong("value")));
						output.setOutputBlockIndex(blockIndex);
						output.setOutputBlockHash(outputBlockHash);
						output.setOutputTransactionId(rs.getString("outputTransactionId"));
						set.add(output);

					}
					logger.trace("Query getOutputs took {} ms. '{}'", System.currentTimeMillis() - start, sql);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new CreateTransactionException(e.getMessage());
		}

		return set;
	}


}
