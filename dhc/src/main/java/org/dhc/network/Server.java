package org.dhc.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ThreadExecutor;

public class Server {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private int port;
	private boolean run;
	private ServerSocket serverSocket;

	public Server() {

	}
	
	public void start() {
		if(run) {
			return;
		}
		run = true;
		ThreadExecutor.getInstance().execute(new DhcRunnable("Server accept") {
			public void doRun() {
				process();
			}
		});
	}
	
	public void stop() {
		run = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private void process() {
		logger.info("Started Distributed Hash Chain on port {}", port);
		try (
				ServerSocket serverSocket = new ServerSocket(port, 5);
		) {
			this.serverSocket = serverSocket;
			while (run) {
				try {
					Socket clientSocket = serverSocket.accept();
					reply(clientSocket);
				} catch (SocketException e) {
					logger.info("closed serverSocket {}", serverSocket);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private void reply(Socket clientSocket) {
/*		InetSocketAddress iNetAddress  = new InetSocketAddress( clientSocket.getInetAddress(), clientSocket.getPort());
		logger.debug("Connection from {}", iNetAddress);*/
		Peer fromPeer = Peer.getInstance(clientSocket);
		fromPeer.setType(PeerType.FROM);
		fromPeer.startReceiver();

	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
