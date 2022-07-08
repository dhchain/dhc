package org.dhc.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.SyncMessage;
import org.dhc.blockchain.Trimmer;
import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;
import org.dhc.util.TAddress;
import org.dhc.util.ThreadExecutor;

public class ChainSync {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final ChainSync instance = new ChainSync();
	
	private Map<Peer, Peer> myPeers = new ConcurrentHashMap<Peer, Peer>();
	private long lastBlockchainIndex;
	private PendingIndexes pendingIndexes = new PendingIndexes();
	private List<Peer> skipPeers = new ArrayList<>();
	private volatile boolean running;
	private ReentrantLock lock = MajorRunnerLock.getInstance().getLock();
	
	public static ChainSync getInstance() {
		return instance;
	}
	
	private ChainSync() {
		
	}

	public void sync() {
		if(!lock.tryLock()) {
			return;
		}
		
		if(running) {
			return;
		}
		
		running = true;
		
		try {
			Trimmer.getInstance().runImmediately();
			Network network = Network.getInstance();
			network.reloadBuckets();
			if(network.getPower() < Blockchain.getInstance().getPower()) {
				ChainRest.getInstance().execute();
			}
			
			List<Peer> myPeers;
			while(true) {
				network.reloadBuckets();
				myPeers = network.getMyBucketPeers();
				logger.info("START ChainSynchronizer.sync() networkPower={} blockchainPower={} # myPeers {}", network.getPower(), Blockchain.getInstance().getPower(), myPeers.size());
				if(myPeers.size() > 0) {
					break;
				}
				PeerSync.getInstance().executeAndWait();
			}
			
			start();
			Bootstrap.getInstance().navigate(network.getAllPeers(), TAddress.getMyTAddress());
			logger.info("END ChainSynchronizer.sync() power={} # myPeers {} lastBlockchainIndex {} pendingBlocks {}", network.getPower(), myPeers.size(), lastBlockchainIndex, Blockchain.getInstance().getNumberOfPendingBlocks());
			
			logger.info("Pending nodes queue size: {}", Blockchain.getInstance().getQueueSize());
			logger.info("Pending blocks size: {}", Blockchain.getInstance().getNumberOfPendingBlocks());
			
			
		} finally {
			running = false;
			lock.unlock();
			
			myPeers.clear();
		}
	}
	
	private void start() {
		Network network = Network.getInstance();
		Blockchain blockchain = Blockchain.getInstance();
		skipPeers.clear();
		int count = 0;
		while(count++ < 100) { //limit number of attempts to synchronize blockchain to 100
			for(Peer peer: network.getMyBucketPeers()) {
				myPeers.put(peer, peer);
			}
			for(Peer peer: skipPeers) {
				myPeers.remove(peer);
			}
			if(myPeers.size() == 0) {
				logger.info("myPeers.size()={} network.getMyBucketPeers().size()={} skipPeers.size()={}", myPeers.size(), network.getMyBucketPeers().size(), skipPeers.size());
				skipPeers.clear();
				int totalPeerCount = Peer.getTotalPeerCount();
				if(totalPeerCount == 0) {
					PeerSync.getInstance().executeAndWait();
				}
				int i = 0;
				do {
					PeersFinder.getInstance().findPeers();
					logger.info("totalPeerCount={}", Peer.getTotalPeerCount());
					ThreadExecutor.sleep(Constants.SECOND);
				} while(totalPeerCount == Peer.getTotalPeerCount() && i++ < 10);
				continue;
			}
			run();
			synchronized(this) {
				long index = blockchain.getIndex();
				if(lastBlockchainIndex <= index) {
					logger.info("exit start() because lastBlockchainIndex={} <= blockchain.getIndex()={}", lastBlockchainIndex, index);
					return;
				}
				PeersFinder.getInstance().findPeers();
			}
		} 
	}
	
	private synchronized void run() {
		List<Peer> list = new ArrayList<>(myPeers.values());
		Collections.shuffle(list); //some of the peer might have problems so try a random peer next time
		Blockchain blockchain = Blockchain.getInstance();
		pendingIndexes.setIndex(blockchain.getIndex());
		for(Peer peer: list) {
			peer.send(new SyncMessage(next(peer)));
			logger.trace("ChainSynchronizer to peer {}", peer);
		}

		try {
			int count = 0;
			while(!myPeers.isEmpty() || blockchain.getQueueSize() > 0) {
				
				long index = blockchain.getIndex();
				wait(Constants.SECOND * 1);
				if(blockchain.getIndex() > index) {
					count = 0; //reset count since blockchain still growing
					continue;
				}
				removedClosed(myPeers);
				if(!myPeers.isEmpty()) {
					logger.info("myPeers {}: ", myPeers.size());
					for(Peer peer: myPeers.keySet()) {
						logger.info("\t peer {}", peer);
					}
				}
				if(blockchain.getQueueSize() > 0) {
					logger.info("blockchain.getQueueSize() {}", blockchain.getQueueSize());
				}
				if(lastBlockchainIndex <= blockchain.getIndex()) {
					break;
				}
				if(count++ > 10) {
					break;
				}
			}
			logger.trace("ChainSynchronizer was notified by {} peer(s) networkPower={}, blockchainPower={}:", list.size(), Network.getInstance().getPower(), blockchain.getPower());
			for (Peer peer : list) {
				logger.trace("\t{} {}", peer.getTAddress().getBinary(), peer);
			}
		} catch (InterruptedException e) {
			
		}
		Network.getInstance().reloadBuckets();
		
	}
	
	public synchronized void incNotify(Peer peer, String reason) {
		if(reason != null && (
				reason.startsWith("Not my blocks") 
				|| reason.startsWith("This peer is still synchronizing")
				|| reason.startsWith("Block is not valid")
				|| reason.startsWith("Peer timed out getting block")
		)) {
			skipPeers.add(peer);
		}
		if(myPeers.remove(peer) != null) {
			logger.trace("ChainSynchronizer removed reason={}, peer {}", reason, peer);
		}
		removedClosed(myPeers);
		if(myPeers.isEmpty()) {
			notify();
		}
	}
	
	private void removedClosed(Map<Peer, Peer> peers) {
		List<Peer> list = new ArrayList<>(peers.keySet());
		for(Peer peer: list) {
			if(peer.isClosed()) {
				if(peers.remove(peer) != null) {
					logger.info("ChainSynchronizer removed closed peer {}", peer);
				}
			}
		}
	}

	public long next(Peer peer) {
		return pendingIndexes.next(peer);
	}
	
	public void unRegister(long index) {
		pendingIndexes.unRegister(index);
	}

	public synchronized void setLastBlockchainIndex(long lastBlockchainIndex) {
		if(this.lastBlockchainIndex < lastBlockchainIndex) {
			this.lastBlockchainIndex = lastBlockchainIndex;
		}
	}

	public boolean isRunning() {
		if(!lock.tryLock()) {
			return true;
		}
		lock.unlock();
		return false;
	}

	public long getLastBlockchainIndex() {
		return lastBlockchainIndex;
	}

	public synchronized void refreshMyPeers(Peer peer) {
		myPeers.put(peer, peer);
	}
	
	

}
