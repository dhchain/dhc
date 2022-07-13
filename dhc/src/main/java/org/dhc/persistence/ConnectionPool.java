package org.dhc.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;

public class ConnectionPool {

	private static final DhcLogger logger = DhcLogger.getLogger();
	public static String USER = "USER1";
    private static String PASSWORD = "USER1";
    private static ConnectionPool instance = new ConnectionPool();
    private static Map<String, ConnectionPool> instances = new HashMap<>();
	
	private DataSource ds;
	private ThreadLocal<Connection> connections = new ThreadLocal<Connection>();
	private ThreadLocal<Boolean> transactionStarted = new ThreadLocal<Boolean>();
	
	public static ConnectionPool getInstance() {
		return instance;
	}
	
	public synchronized static ConnectionPool getInstance(String poolName) {
		ConnectionPool pool = instances.get(poolName);
		if(pool == null) {
			pool = new ConnectionPool();
		}
		return pool;
	}
	
	private ConnectionPool() {
		init();
	}

	private void init() {
		EmbeddedDataSource embeddedDataSource = new EmbeddedDataSource();
		embeddedDataSource.setDatabaseName("data/" + Constants.DATABASE);
		embeddedDataSource.setUser(USER);
		embeddedDataSource.setPassword(PASSWORD);
		embeddedDataSource.setCreateDatabase("create");
		ds = embeddedDataSource;
	}
	
	public Connection getConnection() throws SQLException {
		Connection conn = connections.get();
		if (conn == null || conn.isClosed()) {
			conn = ds.getConnection();
			connections.set(conn);
		}
		conn.setAutoCommit(false);
		return conn;
	}
	
	public void begin() {
		transactionStarted.set(Boolean.TRUE);
	}
	
	public void commit() {
		transactionStarted.remove();
		try {
			tryCommit();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public void tryCommit() throws SQLException {
		if(Boolean.TRUE.equals(transactionStarted.get())) {
			return;
		}
		getConnection().commit();
		close();
	}
	
	public void close() throws SQLException {
		if(Boolean.TRUE.equals(transactionStarted.get())) {
			return;
		}
		getConnection().close();
		connections.remove();
	}
	
	public void tryRollback() throws SQLException {
		if(Boolean.TRUE.equals(transactionStarted.get())) {
			return;
		}
		getConnection().rollback();
		close();
	}
	
	public void rollback() {
		transactionStarted.remove();
		try {
			tryRollback();
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
