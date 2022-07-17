package org.dhc.network;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import org.dhc.util.DhcLogger;
import org.dhc.util.TAddress;

public class Bootstrap {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private String peersFile = "config/peers.config";
	private volatile boolean running;
	private Set<Peer> candidatePeers = Collections.newSetFromMap(new ConcurrentHashMap<Peer, Boolean>());
	
	private static Bootstrap instance =  new Bootstrap();
	
	public static Bootstrap getInstance() {
		return instance;
	}
	
	private Bootstrap() {
		
	}
	
	private Set<Peer> getBootstrapPeers() {
		Set<Peer> set = new HashSet<Peer>();
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
					set.add(peer);
				} catch (Exception e) {
					logger.trace(e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return set;
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
		Set<Peer> set = getBootstrapPeers();
		set.addAll(getCandidatePeers());
		connect(set);
		set = new HashSet<>(Peer.getPeers());
		logger.info("number of bootstrap peers {}", set.size());
	}
	
	private void navigate(Peer bootPeer, TAddress tAddress) {
		if(bootPeer.isClosed()) {
			return;
		}
		new NavigateMessage(tAddress, 0).send(bootPeer);
	}
	
	public void navigate(List<Peer> list, TAddress tAddress) {
		//logger.trace("Navigate for number of peers {}", list.size());
		List<Callable<Boolean>> calls = new ArrayList<>();
		for (Peer peer : list) {
			calls.add(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					navigate(peer, tAddress);
					return null;
				}
			});
			
		}
		ForkJoinPool.commonPool().invokeAll(calls);
	}

	private void connect(Set<Peer> set) {
		if(set.isEmpty()) {
			return;
		}
		TAddress tAddress = TAddress.getMyTAddress();
		logger.info("Connecting to number of peers {}", set.size());
		List<Callable<Boolean>> calls = new ArrayList<>();
		for (Peer peer : set) {
			
			if(!peer.isClosed()) {
				logger.trace("connect(Set<Peer> set) - peer Peer@{} already connected: {}", peer.getSuperHashCode(), peer);
				continue;
			}
			String networkIdentifier = peer.getNetworkIdentifier();
			if(networkIdentifier != null) {
				if(!Peer.getPeersByNetworkIdentifier(networkIdentifier).isEmpty()) {
					logger.trace("connect(Set<Peer> set) - peer Peer@{} already exists: {}", peer.getSuperHashCode(), peer);
					continue;
				}
				if(Network.getInstance().getNetworkIdentifier().equals(networkIdentifier)) {
					logger.trace("connect(Set<Peer> set) - Peer@{} cannot connect to yourself: {}", peer.getSuperHashCode(), peer);
					continue;
				}
			}
			
			if(!Network.getInstance().shouldAddPeer(peer.getTAddress())) {
				logger.trace("Should not add peer {}", peer);
				continue;
			}
			
			peer.setTAddress(null);
			
			calls.add(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					if(connect(peer)) {
						navigate(peer, tAddress);
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
				if(!peer.connectSocket()) {
					return false;
				}
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
			if(networkIdentifier.equals(Network.getInstance().getNetworkIdentifier())) {
				logger.trace("Bootstrap.addPeers() - Cannot connect to yourself");
				continue;
			}
			
			TAddress tAddress = p.getTAddress();
			if(tAddress == null) {
				continue;
			}
			
			if(!Network.getInstance().shouldAddPeer(tAddress)) {
				logger.trace("Should not add peer {}", p);
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

	public Set<Peer> getCandidatePeers() {
		return candidatePeers;
	}

}
