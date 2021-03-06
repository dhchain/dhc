package org.dhc.network;

import java.util.ArrayList;
import java.util.List;

import org.dhc.blockchain.Blockchain;
import org.dhc.util.DhcLogger;
import org.dhc.util.TAddress;

public class PeerSync {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final PeerSync instance = new PeerSync();
	
	public static PeerSync getInstance() {
		return instance;
	}

	private volatile boolean running;
	
	private PeerSync() {
		
	}
	
	public void executeAndWait() {
		synchronized (this) {
			if (running) {
				try {
					wait();
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
				}
				return;
			}
		}
		executeNow();
	}
	
	public void executeNow() {
		if (running) {
			return;
		}
		synchronized (this) {
			if (running) {
				return;
			}
			running = true;
		}
		logger.info("PeerSynchronizer START networkPower={}, blockchainPower={}", Network.getInstance().getPower(), Blockchain.getInstance().getPower());
		long start = System.currentTimeMillis();
		try {
			Network network = Network.getInstance();
			Bootstrap.getInstance().bootstrap();
			int myPeersCount = network.getMyBucketPeers().size();
			while(true) {
				Bootstrap.getInstance().navigate(Network.getInstance().getAllPeers(), TAddress.getMyTAddress());
				int count = network.getMyBucketPeers().size();
				if(myPeersCount == count) {
					break;
				}
				myPeersCount = count;
			}
			
		} finally {
			synchronized (this) {
				running = false;
				logger.info("PeerSynchronizer END took {}ms networkPower={}, blockchainPower={}", System.currentTimeMillis() - start, Network.getInstance().getPower(), Blockchain.getInstance().getPower());
				notifyAll();
			}
		}
	}
	
	
	public void closeUnusedPeers() {
		List<Peer> peerList = new ArrayList<>(Peer.getPeers());
		peerList.removeAll(Network.getInstance().getAllPeers());
		for(Peer peer: peerList) {
			if(peer.isClosed() && peer.getTAddress() == null) {
				continue;
			}
			if(!peer.getInUse()) {
				peer.close("Close peer because it is unused");
			}
		}
	}

}
