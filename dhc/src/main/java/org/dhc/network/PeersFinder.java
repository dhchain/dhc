package org.dhc.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;
import org.dhc.util.TAddress;

public class PeersFinder {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final PeersFinder instance = new PeersFinder();
	
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final List<Peer> peers =  new ArrayList<>();
	
	public static PeersFinder getInstance() {
		return instance;
	}
	
	private PeersFinder() {
		
	}
	
	public synchronized void peerComplete(Peer peer) {
		peers.remove(peer);
		removedClosed(peers);
		if(peers.isEmpty()) {
			notify();
		}
	}
	
	private void findPeers(List<Peer> peers, TAddress tAddress) {
		this.peers.clear();
		this.peers.addAll(peers);
		FindNodeRequestMessage message = new FindNodeRequestMessage(tAddress);
		for(Peer peer: peers) {
			peer.send(message);
		}
		synchronized (this) {
			try {
				if(this.peers.isEmpty()) {
					return;
				}
				wait(Constants.SECOND * 10);
				if(!this.peers.isEmpty()) {
					logger.info("Number of peers that have not responded to FindNodeRequestMessage within 10 second: {}", this.peers.size());
					for(Peer peer: this.peers) {
						logger.info("peer {}", peer);
					}
				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	public void findPeers() {
		Lock writeLock = readWriteLock.writeLock();
		if(!writeLock.tryLock()) {
			return;
		}

		try {
			findPeers(Peer.getPeers(), TAddress.getMyTAddress());
		} finally {
			writeLock.unlock();
		}
	}
	
	private void getPeers(List<Peer> peers) {
		this.peers.clear();
		this.peers.addAll(peers);
		GetPeersMessage message = new GetPeersMessage();
		message.setDontSkip(true);
		for(Peer peer: peers) {
			peer.send(message);
		}
		synchronized (this) {
			try {
				if(this.peers.isEmpty()) {
					return;
				}
				wait(Constants.SECOND * 10);
				if(!this.peers.isEmpty()) {
					logger.info("Number of peers that have not responded to GetPeersMessage within 10 second: {}", this.peers.size());
					for(Peer peer: this.peers) {
						logger.info("peer {}", peer);
					}
				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	public void getPeers() {
		
		Lock writeLock = readWriteLock.writeLock();
		if(!writeLock.tryLock()) {
			return;
		}

		try {
			getPeers(Peer.getPeers());
		} finally {
			writeLock.unlock();
		}
	}
	
	private void removedClosed(List<Peer> peers) {
		List<Peer> list = new ArrayList<>(peers);
		for(Peer peer: list) {
			if(peer.isClosed()) {
				peers.remove(peer);
			}
		}
	}

}
