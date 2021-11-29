package org.dhc.network;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class Bootstrap {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private String peersFile = "config/peers.config";
	private volatile boolean running;
	
	private static Bootstrap instance =  new Bootstrap();
	
	public static Bootstrap getInstance() {
		return instance;
	}
	
	private Bootstrap() {
		
	}
	
	private List<Peer> getBootstrapPeers() {
		List<Peer> list = new ArrayList<Peer>();
		try (
			InputStream is = new FileInputStream(peersFile);
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
		) {
			String line;
			while ((line = in.readLine()) != null) {
				try {
					line = line.trim();
					String[] strs = line.split(":");
					if (strs.length != 2) {
						continue;
					}
					String ip = strs[0].trim();
					int port = Integer.parseInt(strs[1].trim());
					InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByName(ip), port);
					Peer peer = Peer.getInstance(inetSocketAddress);
					peer.setType(PeerType.TO);
					list.add(peer);
				} catch (Exception e) {
					logger.trace(e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return list;
	}
	
	public void bootstrap() {
		if (running) {
			return;
		}
		synchronized (this) {
			if (running) {
				return;
			}
			running = true;
		}
		try {
			doBootstrap();
			
		} finally {
			running = false;
		}
	}
	
	private void doBootstrap() {
		try {
			connectPeers();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void connectPeers() {
		logger.info("START");
		List<Peer> list = getBootstrapPeers();
		connect(list);
		list = Peer.getPeers();
		logger.info("number of bootstrap peers {}", list.size());
	}
	
	private void navigate(Peer bootPeer, DhcAddress dhcAddress) {
		if(bootPeer.isClosed()) {
			return;
		}
		bootPeer.send(new NavigateMessage(dhcAddress, 0));
	}
	
	public void navigate(List<Peer> list, DhcAddress dhcAddress) {
		//logger.trace("Navigate for number of peers {}", list.size());
		List<Callable<Boolean>> calls = new ArrayList<>();
		for (Peer peer : list) {
			calls.add(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					navigate(peer, dhcAddress);
					return null;
				}
			});
			
		}
		ForkJoinPool.commonPool().invokeAll(calls);
	}

	private void connect(List<Peer> list) {
		if(list.isEmpty()) {
			return;
		}
		DhcAddress dhcAddress = DhcAddress.getMyDhcAddress();
		logger.info("Connecting to number of peers {}", list.size());
		List<Callable<Boolean>> calls = new ArrayList<>();
		for (Peer peer : list) {
			calls.add(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					if(connect(peer)) {
						navigate(peer, dhcAddress);
					}
					return null;
				}
			});
			
		}
		ForkJoinPool.commonPool().invokeAll(calls);
	}
	
	public boolean connect(Peer peer) {
		logger.trace("Connecting to peer {}", peer);
		if (peer.isClosed()) {
			try {
				peer.connectSocket();
				peer.connect();
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}
	
	public void addPeers(List<Peer> peers) {
		List<Peer> list = Peer.getPeers();
		Set<Peer> foundPeers = new HashSet<Peer>(peers);
		foundPeers.removeAll(list);
		
		List<Callable<Boolean>> calls = new ArrayList<>();
		for (Peer p : foundPeers) {
			String networkIdentifier = p.getNetworkIdentifier();
			if(networkIdentifier == null) {
				continue;
			}
			if(!Peer.getPeersByNetworkIdentifier(networkIdentifier).isEmpty()) {
				continue;
			}
			Peer foundPeer = Peer.getInstance(p.getInetSocketAddress());
			foundPeer.setType(PeerType.TO);
			calls.add(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					connect(foundPeer);
					return null;
				}
			});
			
		}
		ForkJoinPool.commonPool().invokeAll(calls);
	}

}
