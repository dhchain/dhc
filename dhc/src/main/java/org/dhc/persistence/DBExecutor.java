package org.dhc.persistence;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.dhc.util.DhcLogger;

public abstract class DBExecutor {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
    
    protected Connection conn = null;
    protected PreparedStatement ps;
    protected ResultSet rs = null;
    protected Statement s;
    protected ConnectionPool pool;
    protected CallableStatement cs;
    
    public abstract void doWork() throws Exception;
    
    public DBExecutor() {
    	pool = ConnectionPool.getInstance();
    }
    
    public DBExecutor(String poolName) {
    	pool = ConnectionPool.getInstance(poolName);
    }
	
	public void execute() throws Exception {
        try {
        	conn = pool.getConnection();
        	s = conn.createStatement();
        	doWork();
        	pool.tryCommit();
        } catch (Exception e) {
			throw e;
		} finally {
			try {
            	pool.tryRollback();
            } catch (SQLException e) {
                printSQLException(e);
                throw e;
            }
        }
	}
	
	public static void shutdownDerby() {
		try {
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		} catch (SQLException se) {
			if (((se.getErrorCode() == 50000) && ("XJ015".equals(se.getSQLState())))) {
				logger.debug("Derby shut down normally");
			} else {
				logger.error("Derby did not shut down normally");
				printSQLException(se);
			}
		}
	}

	private static void printSQLException(SQLException e) {
		// Unwraps the entire exception chain to unveil the real cause of the
		// Exception.
		while (e != null) {
			logger.error("\n----- SQLException -----");
			logger.error("  SQL State:  {}", e.getSQLState());
			logger.error("  Error Code: {}", e.getErrorCode());
			logger.error("  Message:    {}", e.getMessage());
			// for stack traces, refer to derby.log or uncomment this:
			logger.error(e.getMessage(), e);
			e = e.getNextException();
		}
	}
	
	public void addNewColumn(DatabaseMetaData dbmd, String tableName, String columnName, String alterStatement) throws Exception {
		rs = dbmd.getColumns(null, ConnectionPool.USER, tableName, null);
		boolean dataColumnAlreadyExists = false;
		while (rs.next()) {
			if (rs.getString("COLUMN_NAME").equalsIgnoreCase(columnName)) {
				dataColumnAlreadyExists = true;
				break;
			}
		}
		if(!dataColumnAlreadyExists) {
			s.execute(alterStatement);
		}
		rs.close();
	}
	
	public void addIndex(DatabaseMetaData dbmd, String tableName, String indexName, String createIndexStatement) throws Exception {
		rs = dbmd.getIndexInfo(null, ConnectionPool.USER, tableName, false, false);
		boolean dataIndexAlreadyExists = false;
		while (rs.next()) {
			if (rs.getString("INDEX_NAME").equalsIgnoreCase(indexName)) {
				dataIndexAlreadyExists = true;
				break;
			}
		}
		if(!dataIndexAlreadyExists) {
			s.execute(createIndexStatement);
		}
		rs.close();
	}
	
	public void incrementSequence(String sequence) {
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					s.execute("values NEXT VALUE FOR " + sequence);
				}
			}.execute();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public static void rebuildIndexes() {
		logger.debug("Rebuilding indexes");
		List<String> tables = new ArrayList<String>();
		try {
			new DBExecutor() {
				public void doWork() throws Exception {
					DatabaseMetaData metaData = conn.getMetaData();
					String[] types = {"TABLE"};
					rs = metaData.getTables(null, ConnectionPool.USER, "%", types);
					
					while (rs.next()) {
					    // 1: none
					    // 2: schema
					    // 3: table name
					    // 4: table type (TABLE, VIEW) 
					    String tableName = rs.getString(3);
					    tables.add(tableName);
					}
					rs.close();
				}
			}.execute();
			for(String table: tables) {
				logger.debug("Rebuilding table {}", table);
				new DBExecutor() {
					public void doWork() throws Exception {
						cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)");
						cs.setString(1, ConnectionPool.USER);
						cs.setString(2, table);
						cs.setShort(3, (short) 1);
						cs.execute();
					}
				}.execute();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
