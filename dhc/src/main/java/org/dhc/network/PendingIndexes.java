package org.dhc.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.dhc.blockchain.Blockchain;
import org.dhc.util.Constants;
import org.dhc.util.Expiring;
import org.dhc.util.ExpiringMap;
import org.dhc.util.DhcLogger;

public class PendingIndexes {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private AtomicLong atomicLong = new AtomicLong();
	private ExpiringMap<Long, Expiring> map = new ExpiringMap<>(Constants.SECOND * 10);
	private List<Long> indexes = new ArrayList<Long>();
	
	public synchronized long next(Peer peer) {
		if(!indexes.isEmpty()) {
			long index = indexes.remove(0);
			Blockchain blockchain = Blockchain.getInstance();
			long lastBlockchainIndex = blockchain.getIndex();
			if(index > lastBlockchainIndex) {
				if(!blockchain.pendingIndexesHasIndex(index)) {
					logger.info("PendingIndexes retrying index={}", index);
					return register(index, peer);
				}
			}
		}
		long index  = register(atomicLong.incrementAndGet(), peer);
		return index;
	}
	
	public synchronized long register(long index, Peer peer) {
		map.put(index, new Expiring() {
			
			@Override
			public void expire() {
				ChainSync.getInstance().incNotify(peer, "Peer timed out getting block");
				logger.info("Peer timed out getting block for index {} peer {}", index, peer);
				addIndex(index);
				long nextBlockchainIndex = Blockchain.getInstance().getIndex() + 1;
				if(index > nextBlockchainIndex && !indexes.contains(nextBlockchainIndex)) {
					addIndexFirst(nextBlockchainIndex);
				}
			}
		});
		logger.trace("register index={}", index);
		return index;
	}
	
	private synchronized void addIndex(long index) {
		indexes.add(index);
	}
	
	private synchronized void addIndexFirst(long index) {
		indexes.add(0, index);
	}
	
	public synchronized void unRegister(long index) {
		logger.trace("unregister index={}", index);
		map.remove(index);
	}

	public synchronized void setIndex(long index) {
		atomicLong.set(index);
		indexes.clear();
		map.clear();
	}

}
