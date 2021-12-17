package org.dhc.network;

import java.util.ArrayList;
import java.util.Collections;
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
	private int possiblePower;
	private int allPeersCount;
	private int currentPower;

	public int getPower() {
		List<Bucket> buckets = this.buckets;
		int size = buckets == null ? 0 : buckets.size();
		return size == 0 ? 0 : size - 1;
	}

	public void reload() {
		
		Lock writeLock = readWriteLock.writeLock();
		if(!writeLock.tryLock()) {
			return;
		}
		long start = System.currentTimeMillis();
		try {
			
			allPeersCount = Peer.getTotalPeerCount();
			TAddress tAddress = TAddress.getMyTAddress();

			buckets.clear();

			int i = 0;
			while (true) {// this first loop builds all buckets starting with index 0 key length 1 that have at least Constants.k peers
				List<Peer> list = getBucketPeers(i++);
				if (list.size() >= Constants.k) {
					Bucket bucket = new Bucket(this);
					bucket.addAll(list.subList(0, Math.min(Constants.k, list.size())));
					buckets.add(bucket);
				} else {
					i--;
					break;
				}
			}
			List<Peer> peers = new ArrayList<>();

			while (true) {// the rest of peers, which are closer to my peer go to peers variable
				List<Peer> list = getBucketPeers(i++);
				if (!list.isEmpty()) {
					peers.addAll(list);
				} else {
					i--;
					break;
				}
			}

			peers.addAll(getMyBucketPeers(i));
			
			Collections.sort(peers, new TimeAddedPeerComparator());

			if (peers.size() >= Constants.k) {//reverting to k instead of k/2
				Bucket bucket = new Bucket(this);
				bucket.getPeers().addAll(peers);
				buckets.add(bucket);
			} else if (!buckets.isEmpty()) {
				Bucket bucket = buckets.get(buckets.size() - 1);
				bucket.getPeers().addAll(peers);
			}

			if(possiblePower > getPower() || possiblePower < Blockchain.getInstance().getLastAveragePower()) {
				ThreadExecutor.getInstance().execute(new DhcRunnable("navigate") {
					public void doRun() {
						Bootstrap.getInstance().navigate(getAllPeers(), tAddress);
					}
				});
				
				//expiringMap.clear();
			}
			
			possiblePower = getPower();
			logger.trace("Real Power={}", possiblePower);
			
			checkWithMyPeers();
			
			if(!buckets.isEmpty()) {
				Bucket myBucket = buckets.get(buckets.size() - 1);
				myBucket.fill();
			}
			
			if(allPeersCount != Peer.getTotalPeerCount()) {
				ThreadExecutor.getInstance().schedule(new DhcRunnable("Buckets reload") {
					
					@Override
					public void doRun() {
						reload();
					}
				}, Constants.SECOND * 1);
			}

			collapseToPower();

			Bucket myBucket = getMyBucket();
			if(myBucket != null) {
				myBucket.trim();
			}

			if(currentPower > getPower()) {
				ChainRest.getInstance().execute();
			}
			currentPower = getPower();

		} finally {
			writeLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
				logger.info("", new RuntimeException());
			}
			//logger.trace("unlock");
			logger.trace("END Reload buckets power={}, took {} ms", getPower(), System.currentTimeMillis() - start);
		}

	}
	
	private Bucket getMyBucket() {
		if(!buckets.isEmpty()) {
			return buckets.get(buckets.size() - 1);
		}
		return null;
	}
	
	private void collapseToPower() {
		
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		long start = System.currentTimeMillis();
		try {
			int power = Blockchain.getInstance().getAveragePower();
			if (getPower() <= power) {
				return;
			}

			Bucket bucket = buckets.get(power);
			while (power < getPower()) {
				Bucket b = buckets.get(power + 1);
				bucket.addAll(b.getPeers());
				buckets.remove(power + 1);
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
		for(Peer peer: myPeers) {
			if(peer.getPower() > getPower()) {
				if(expiringMap.get(peer.getNetworkIdentifier()) != null) {
					continue;
				}
				expiringMap.put(peer.getNetworkIdentifier(), peer.getNetworkIdentifier());
				peer.send(new GetPeersMessage());
			}
		}
		List<Peer> myToPeers = Network.getInstance().getMyBucketToPeers();
		for(Peer peer: myPeers) {
			String key = "mypeers-" + peer.getNetworkIdentifier();
			if(expiringMap.get(key) != null) {
				continue;
			}
			expiringMap.put(key, key);
			peer.send(new SendMyToPeersMessage(myToPeers));
		}
		
	}

	public void printBuckets() {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			Network network = Network.getInstance();
			logger.info("Non bucket peers");
			List<Peer> peers = Peer.getPeers();
			peers.removeAll(network.getAllPeers());
			
			for (Peer peer : peers) {
				if(peer.getTAddress() == null) {
					continue;
				}
				logger.info("\t{} {}", peer.getTAddress().getBinary(), peer);
			}
			logger.info("\n");
			
			for (Bucket bucket : buckets) {
				logger.info("bucket index={} key={}", bucket.getIndex(), bucket.getBucketKey());
				for (Peer peer : bucket.getPeers()) {
					logger.info("\t{} {}", peer.getTAddress().getBinary(), peer);
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

	private List<Peer> getMyBucketPeers(int i) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			BucketKey bucketKey = new BucketKey(DhcAddress.getMyDhcAddress().getBinary(i + 1));
			String key = bucketKey.getKey();
			List<Peer> list = new ArrayList<>();
			for (Peer peer : Peer.getPeers()) {
				if (peer.getTAddress() != null && peer.getTAddress().getBinary().startsWith(key)) {
					list.add(peer);
				}
			}
			Collections.sort(list, new TimeAddedPeerComparator());
			return list;
		} finally {
			readLock.unlock();
			long duration = System.currentTimeMillis() - start;
			if(duration > Constants.SECOND * 10) {
				logger.info("took {} ms", duration);
			}
			//logger.trace("unlock");
		}
	}

	private List<Peer> getBucketPeers(int i) {
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		long start = System.currentTimeMillis();
		try {
			BucketKey bucketKey = new BucketKey(DhcAddress.getMyDhcAddress().getBinary(i + 1));
			String key = bucketKey.getOtherBucketKey().getKey();
			List<Peer> list = new ArrayList<>();
			for (Peer peer : Peer.getPeers()) {
				if (peer.getTAddress() != null && peer.getTAddress().getBinary().startsWith(key)) {
					list.add(peer);
				}
			}
			Collections.sort(list, new TimeAddedPeerComparator());
			return list;
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
			if (buckets.isEmpty() || buckets.size() <= index) {
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
		return possiblePower;
	}

}
