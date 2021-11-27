package org.dhc.persistence;

import java.sql.DatabaseMetaData;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.dhc.blockchain.TransactionInput;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class TransactionInputStore {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static TransactionInputStore instance = new TransactionInputStore();
	
	private final AtomicLong id = new AtomicLong();
	
	private TransactionInputStore() {
		try {
			verifyTable();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static TransactionInputStore getInstance() {
		return instance;
	}
	
	private void setId() {

		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select coalesce(max(id) + 1, 0) as id from transaction_input";
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

	public Set<TransactionInput> getByInputTransactionId(String inputTransactionId, String inputBlockHash) {
		Set<TransactionInput> inputs = new LinkedHashSet<TransactionInput>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from transaction_input where inputTransactionId = ? and inputBlockHash = ?";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setString(i++, inputTransactionId);
					ps.setString(i++, inputBlockHash);
					rs = ps.executeQuery();
					while (rs.next()) {
						TransactionInput input = new TransactionInput();
						input.setOutputId(rs.getString("outputId"));
						input.setSender(new DhcAddress(rs.getString("sender")));
						input.setValue(new Coin(rs.getLong("value")));
						input.setInputTransactionId(rs.getString("inputTransactionId"));
						input.setInputBlockIndex(rs.getLong("inputBlockIndex"));
						input.setInputBlockHash(rs.getString("inputBlockHash"));
						input.setOutputBlockIndex(rs.getLong("outputBlockIndex"));
						input.setOutputBlockHash(rs.getString("outputBlockHash"));
						input.setOutputTransactionId(rs.getString("outputTransactionId"));
						
						inputs.add(input);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return inputs;
	}

	public Set<TransactionInput> getByOutputId(String outputId) {
		Set<TransactionInput> inputs = new LinkedHashSet<TransactionInput>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "select * from transaction_input where outputId = ?";
					ps = conn.prepareStatement(sql);
					ps.setString(1, outputId);
					rs = ps.executeQuery();
					while (rs.next()) {
						TransactionInput input = new TransactionInput();
						input.setOutputId(rs.getString("outputId"));
						input.setSender(new DhcAddress(rs.getString("sender")));
						input.setValue(new Coin(rs.getLong("value")));
						input.setInputTransactionId(rs.getString("inputTransactionId"));
						input.setInputBlockIndex(rs.getLong("inputBlockIndex"));
						input.setInputBlockHash(rs.getString("inputBlockHash"));
						input.setOutputBlockIndex(rs.getLong("outputBlockIndex"));
						input.setOutputBlockHash(rs.getString("outputBlockHash"));
						input.setOutputTransactionId(rs.getString("outputTransactionId"));
						
						inputs.add(input);
					}
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return inputs;
	}

	private void verifyTable() throws Exception {

		new DBExecutor() {
			public void doWork() throws Exception {
				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getTables(null, ConnectionPool.USER, "TRANSACTION_INPUT", null);
				if (!rs.next()) {
					s.execute(" create table transaction_input ( " 
							+ " ID BIGINT NOT NULL CONSTRAINT transaction_input_PK PRIMARY KEY, " 
							+ " outputId varchar(64) NOT NULL, "
							+ " sender varchar(64) NOT NULL, " 
							+ " value bigint NOT NULL, " 
							+ " inputTransactionId varchar(64) NOT NULL, " 
							+ " inputBlockIndex bigint NOT NULL, "
							+ " inputBlockHash varchar(64) NOT NULL, " 
							+ " outputBlockIndex bigint NOT NULL, " 
							+ " outputBlockHash varchar(64) NOT NULL, "
							+ " outputTransactionId varchar(64) NOT NULL " 
							+ " )");
					s.execute("CREATE INDEX transaction_input_inputTransactionId ON transaction_input(inputTransactionId)");
					s.execute("CREATE INDEX transaction_input_outputId ON transaction_input(outputId)");
					s.execute("CREATE INDEX transaction_input_inputBlockHash ON transaction_input(inputBlockHash)");
					s.execute("CREATE INDEX transaction_input_inputBlockIndex ON transaction_input(inputBlockIndex)");
					s.execute("ALTER TABLE transaction_input ADD CONSTRAINT trans_input_outputId_inbhash UNIQUE (outputId, inputBlockHash)");
					s.execute("ALTER TABLE transaction_input ADD CONSTRAINT trans_input_intr_id_inbhash_FK FOREIGN KEY (inputTransactionId, inputBlockHash) "
							+ "REFERENCES trans_action(transaction_id, blockHash) ON DELETE CASCADE");
					s.execute("ALTER TABLE transaction_input ADD CONSTRAINT trans_input_outbhash_FK FOREIGN KEY (outputBlockHash) "
							+ "REFERENCES block(blockHash) ON DELETE CASCADE");
/*					s.execute("ALTER TABLE transaction_input ADD CONSTRAINT trans_input_outputId_outbhash_FK FOREIGN KEY (outputId, outputBlockHash) "
							+ "REFERENCES transaction_output(outputId, outputBlockHash) ON DELETE CASCADE");
							
							* Can not reference on output if it is from another shard
							*/
				}
				rs.close();
				
				addIndex(dbmd, "TRANSACTION_INPUT", "transaction_input_inputBlockIndex", "CREATE INDEX transaction_input_inputBlockIndex ON transaction_input(inputBlockIndex)");

			}
		}.execute();
		setId();
	}

	public void save(TransactionInput input) throws Exception {
		logger.trace("Will try to save input {}", input);
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					String sql = "insert into transaction_input (id, outputId, sender, value, inputTransactionId, inputBlockIndex, inputBlockHash, outputBlockIndex, outputBlockHash, outputTransactionId) "
							+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
					ps = conn.prepareStatement(sql);
					int i = 1;
					ps.setLong(i++, id.getAndIncrement());
					ps.setString(i++, input.getOutputId());
					ps.setString(i++, input.getSender().toString());
					ps.setLong(i++, input.getValue().getValue());
					ps.setString(i++, input.getInputTransactionId());
					ps.setLong(i++, input.getInputBlockIndex());
					ps.setString(i++, input.getInputBlockHash());
					ps.setLong(i++, input.getOutputBlockIndex());
					ps.setString(i++, input.getOutputBlockHash());
					ps.setString(i++, input.getOutputTransactionId());
					
					long start = System.currentTimeMillis();
		            ps.executeUpdate();
		            logger.trace("Query saveTransactionInput took {} ms. '{}' ", System.currentTimeMillis() - start, sql);
				}

				
			}.execute();
		} catch (Exception e) {
			logger.error("Exception when saving {}", input);
			throw e;
		}
		logger.trace("Saved input {}", input);
	}

}
