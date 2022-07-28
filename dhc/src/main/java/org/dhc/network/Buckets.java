package org.dhc.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;

import org.dhc.blockchain.Blockchain;
import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ExpiringMap;
import org.dhc.util.SharedLock;
import org.dhc.util.TAddress;
import org.dhc.util.ThreadExecutor;

public class Buckets {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final ExpiringMap<String, String> expiringMap =  new ExpiringMap<>(Constants.MINUTE);

	private List<Bucket> buckets = new CopyOnWriteArrayList<Bucket>();
	private final SharedLock readWriteLock = SharedLock.getInstance();
	private long navigateTime;

	public int getPower() {
		List<Bucket> buckets = this.buckets;
		int size = buckets == null ? 0 : buckets.size();
		return size == 0 ? 0 : size - 1;
	}
	
	public Bucket getMyBucket() {
		if(!buckets.isEmpty()) {
			return buckets.get(buckets.size() - 1);
		}
		return null;
	}
	
	private void collapseToPower() {
		int averagePower = Blockchain.getInstance().getAveragePower();
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			if (getPower() <= averagePower) {
				return;
			}

			Bucket bucket = buckets.get(averagePower);
			while (averagePower < getPower()) {
				Bucket b = buckets.get(averagePower + 1);
				bucket.addAll(b.getPeers());
				remove(averagePower + 1);
			}
		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	private void checkWithMyPeers() {
		List<Peer> myPeers = getMyBucketPeers();
		List<Peer> myToPeersInLastTwoBuckets = getToPeersInLastTwoBuckets();//we are sending last two buckets because to help to split a peer would need both last buckets
		for(Peer peer: myPeers) {
			String key = "mypeers-" + peer.getNetworkIdentifier();
			if(expiringMap.get(key) != null) {
				continue;
			}
			expiringMap.put(key, key);
			peer.send(new SendMyToPeersMessage(myToPeersInLastTwoBuckets));
		}
		
	}
	
	private List<Peer> getToPeersInLastTwoBuckets() {
		List<Peer> result = new ArrayList<Peer>();
		for(Peer peer: getPeersInLastTwoBuckets()) {
			if(PeerType.TO.equals(peer.getType())) {
				result.add(peer);
			}
		}
		return result;
	}
	
	public List<Peer> getPeersInLastTwoBuckets() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			List<Peer> result = new ArrayList<>();
			int power = getPower();
			result.addAll(getAllPeersInBucket(power));
			result.addAll(getAllPeersInBucket(power - 1));
			return result;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public List<Peer> getMyBucketToPeers() {
		List<Peer> result = new ArrayList<Peer>();
		for(Peer peer: getMyBucketPeers()) {
			if(PeerType.TO.equals(peer.getType())) {
				result.add(peer);
			}
		}
		return result;
	}

	public void printBuckets() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			Network network = Network.getInstance();
			
			logger.info("Thin peers");
			for (Peer peer : Peer.getThinPeers()) {
				if(peer.getTAddress() == null) {
					continue;
				}
				logger.info("\t{} {}", peer.getTAddress().getBinary(), peer);
			}
			logger.info("\n");
			
			
			
			List<Peer> peers = Peer.getPeers();
			peers.removeAll(network.getAllPeers());
			//peers.removeIf(p -> p.getTAddress() == null);
			
			logger.info("Non bucket peers #peers={}", peers.size());
			
			for (Peer peer : peers) {
				logger.info("\t{} {}", (peer.getTAddress() == null? "No TAddress                     ": peer.getTAddress().getBinary()), peer);
			}
			
			logger.info("\n");
			
			for (Bucket bucket : buckets) {
				logger.info("bucket index={} key={} #peer={}", bucket.getIndex(), bucket.getBucketKey(), bucket.getPeers().size());
				
				List<Peer> bucketPeers = bucket.getPeers();
				Collections.sort(bucketPeers, new Comparator<Peer>() {
					@Override
					public int compare(Peer p1, Peer p2) {
						return TAddress.getMyTAddress().compareDistance(p2.getTAddress(), p1.getTAddress());
					}
				});
				for (Peer peer : bucketPeers) {
					TAddress peerTAddress = peer.getTAddress();
					logger.info("\t{} \t{} {}", TAddress.getMyTAddress().xor(peerTAddress), peerTAddress.getBinary(), peer);
				}
			}
			logger.info("\n");
			logger.info("my\t{} {}\n", DhcAddress.getMyDhcAddress().getBinary(), DhcAddress.getMyDhcAddress());
			logger.info("Print buckets # peers {} bucketPeers {} myPeers {} pp {}\n", Peer.getTotalPeerCount(), getAllPeers().size(), getMyBucketPeers().size(), getPossiblePower());

		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public List<Peer> getAllPeers() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			List<Peer> result = new ArrayList<Peer>();
			for (Bucket bucket : buckets) {
				result.addAll(bucket.getPeers());
			}
			//logger.trace("getAllPeers()={}", result);
			return result;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public List<Peer> getMyBucketPeers() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return getAllPeersInBucket(getPower());
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public List<Peer> getAllPeersInBucket(int index) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			if (buckets.isEmpty() || buckets.size() <= index || index < 0) {
				return new ArrayList<Peer>();
			}
			return buckets.get(index).getPeers();
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	private Bucket find(DhcAddress dhcAddress) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			DhcAddress myDhcAddress = DhcAddress.getMyDhcAddress();
			int index = myDhcAddress.getBucketIndex(dhcAddress, getPower());
			if (index < 0) {
				logger.error("index={} dhcAddress={} peer.getDhcAddress({} power={}", index, myDhcAddress, dhcAddress, getPower());
			}
			Bucket bucket = buckets.get(index);
			return bucket;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	private Bucket find(TAddress tAddress) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			if(buckets == null || buckets.isEmpty()) {
				return null;
			}
			TAddress myTAddress = TAddress.getMyTAddress();
			int index = myTAddress.getBucketIndex(tAddress, getPower());
			if (index < 0) {
				logger.error("index={} dhcAddress={} peer.getDhcAddress({} power={}", index, myTAddress, tAddress, getPower());
			}
			Bucket bucket = buckets.get(index);
			return bucket;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public int indexOf(Bucket bucket) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			return buckets.indexOf(bucket);
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	public String getBucketKey(int index) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			if((index < 0 || index >= buckets.size())) {
				return null;
			}
			return buckets.get(index).getBucketKey();
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}
	
	public String getBucketKey(DhcAddress dhcAddress) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			return find(dhcAddress).getBucketKey();
		} finally {
			readLock.unlock();
			//logger.trace("unlock");
		}
	}
	
	public String getBucketKey(TAddress tAddress) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			return find(tAddress).getBucketKey();
		} finally {
			readLock.unlock();
			//logger.trace("unlock");
		}
	}
	
	public int getNumberOfBuckets() {
		return buckets.size();
	}

	public boolean isEmpty() {
		return buckets.isEmpty();
	}
	
	public List<String> getBucketKeys() {
		List<String> result = new ArrayList<>();
		if(buckets == null || buckets.isEmpty()) {
			return result;
		}
		for(Bucket bucket: buckets) {
			result.add(bucket.getBucketKey());
		}
		return result;
	}

	public int getPossiblePower() {
		if(getMyBucket() != null && getMyBucket().canSplit()) {
			return getPower() + 1;
		}
		return getPower();
	}

	public void removePeer(Peer peer) {
		if(peer.getTAddress() == null) {
			return;
		}
		
		Bucket bucket;
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			bucket = find(peer.getTAddress());
			if(bucket == null) {
				bucket = new Bucket(this);
				add(bucket);
				return;
			}
			bucket.removePeer(peer);
			if(bucket.getPeers().size() < Constants.k) {
				while(true) {
					Bucket b = buckets.remove(buckets.size() - 1);
					if(bucket == b) {
						break;
					}
					bucket.addAll(b.getPeers());
				}
				if(buckets.isEmpty() || bucket.getPeers().size() >= Constants.k) {
					buckets.add(bucket);
					return;
				}
				buckets.get(buckets.size() - 1).addAll(bucket.getPeers());
			}
		} finally {
			writeLock.unlock();
			if(getPower() < Blockchain.getInstance().getLastAveragePower()) {
				PeerSync.getInstance().executeNow();
			}
		}
	}

	public void addPeer(Peer peer) {
		if(peer.isThin()) {
			return;
		}
		Bucket bucket;
		int averagePower = Blockchain.getInstance().getAveragePower();
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			bucket = find(peer.getTAddress());
			if(bucket == null) {
				bucket = new Bucket(this);
				add(bucket);
				bucket.addPeer(peer);
				return;
			}
			if(bucket.isMyBucket()) {
				bucket.addPeer(peer);
				if(bucket.getPeers().size() < Constants.k * 2) {// my bucket has to have a minimum k * 2 in order to be able to split into two buckets with k number of peers each
					return;
				}
				bucket.trySplit(averagePower);
				return;
			}
			if(bucket.getPeers().size() < Constants.k + 3) {
				bucket.addPeer(peer);
			} else if(!peer.getInUse()) {
				peer.close("Buckets.addPeer() Close peer because it is not in use");
			}
		} finally {
			writeLock.unlock();
			checkWithMyPeers();
		}
	}

	public boolean shouldAddPeer(TAddress tAddress) {
		Bucket bucket = find(tAddress);
		if(bucket == null) {
			return true;
		}
		if(bucket.isMyBucket()) {
			return true;
		}
		if(bucket.getPeers().size() < Constants.k + 3) {
			return true;
		}
		
		return false;
	}
	
	public void remove(int index) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			buckets.remove(index);
		} finally {
			writeLock.unlock();
		}
	}
	
	public void add(Bucket bucket) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			buckets.add(bucket);
		} finally {
			writeLock.unlock();
		}
	}
	
	public void trySplit() {
		int averagePower = Blockchain.getInstance().getAveragePower();
		int power = getPower();
		int possiblePower = getPossiblePower();
		long time = System.currentTimeMillis();
		if(possiblePower < Blockchain.getInstance().getLastAveragePower() && time - navigateTime > Constants.MINUTE * 10) {
			navigateTime = time;
			ThreadExecutor.getInstance().execute(new DhcRunnable("navigate") {
				public void doRun() {
					Bootstrap.getInstance().navigate(getAllPeers(), TAddress.getMyTAddress());
					logger.info("Will navigate");
				}
			});
		}
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			if(power < possiblePower && power < averagePower) {
				getMyBucket().trySplit(averagePower);
				return;
			}
			
		} finally {
			writeLock.unlock();
		}
		collapseToPower();
	}
	

}
