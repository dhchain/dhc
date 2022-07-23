package org.dhc.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;

import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.SharedLock;
import org.dhc.util.TAddress;

public class Bucket {

	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();
	private final SharedLock readWriteLock = SharedLock.getInstance();
	
	private List<Peer> peers = new CopyOnWriteArrayList<Peer>();
	private Buckets buckets;

	public Bucket(Buckets buckets) {
		this.buckets = buckets;
	}
	
	public List<Peer> getPeers() {
		List<Peer> result = new ArrayList<>();
		for(Peer peer: peers) {
			if(!peer.isClosed()) {
				result.add(peer);
			} else {
				peers.remove(peer);
			}
		}
		return result;
	}

	public String getBucketKey() {
		String dhcAddressBinary = DhcAddress.getMyDhcAddress().getBinary();
		int power = buckets.getPower();
		int index  = getIndex();
		if(index == power) {
			return dhcAddressBinary.substring(0, power);
		}
		BucketKey bucketKey = new BucketKey(dhcAddressBinary.substring(0, index + 1));
		String result = bucketKey.getOtherBucketKey().getKey();
		return result;
	}
	
	public int getIndex() {
		return buckets.indexOf(this);
	}
	
	public boolean isMyBucket() {
		return this == buckets.getMyBucket();
	}

	public void addAll(List<Peer> peers) {
		this.peers.addAll(peers);
	}

	public boolean isEmpty() {
		return peers.isEmpty();
	}
	
	private void trim() {
		Collections.sort(peers, new TimeAddedPeerComparator());
		peers = peers.subList(0, Math.min(Constants.k + 3, peers.size()));
	}

	public void removePeer(Peer peer) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			peers.remove(peer);
		} finally {
			writeLock.unlock();
		}
	}

	public void addPeer(Peer peer) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			peers.add(peer);
		} finally {
			writeLock.unlock();
		}
	}

	public void trySplit(int averagePower) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			if(!canSplit()) {
				return;
			}
			if(buckets.getPower() >= averagePower) {
				return;
			}
			BucketKey bucketKey = new BucketKey(getBucketKey());
			BucketKey left = bucketKey.getLeftKey();
			List<Peer> leftPeers = new ArrayList<>();
			List<Peer> rightPeers = new ArrayList<>();
			for (Peer peer : getPeers()) {
				TAddress tAddress = peer.getTAddress();
				if (tAddress != null && tAddress.isMyKey(left.getKey())) {
					leftPeers.add(peer);
				} else {
					rightPeers.add(peer);
				}
			}			
			
			Bucket myBucket = new Bucket(buckets);
			Bucket otherBucket = new Bucket(buckets);
			if (TAddress.getMyTAddress().isMyKey(left.getKey())) {
				myBucket.addAll(leftPeers);
				otherBucket.addAll(rightPeers);
			} else {
				otherBucket.addAll(leftPeers);
				myBucket.addAll(rightPeers);
			}
			buckets.remove(getIndex());
			otherBucket.trim();
			buckets.add(otherBucket);
			buckets.add(myBucket);
		} finally {
			writeLock.unlock();
		}
	}
	
	public boolean canSplit() {
		BucketKey bucketKey = new BucketKey(getBucketKey());
		BucketKey left = bucketKey.getLeftKey();
		List<Peer> leftPeers = new ArrayList<>();
		List<Peer> rightPeers = new ArrayList<>();
		for (Peer peer : getPeers()) {
			TAddress tAddress = peer.getTAddress();
			if(tAddress == null) {
				continue;
			}
			if (tAddress.isMyKey(left.getKey())) {
				leftPeers.add(peer);
			} else {
				rightPeers.add(peer);
			}
		}
		if (leftPeers.size() >= Constants.k && rightPeers.size() >= Constants.k) {
			return true;
		}
		return false;
	}

}
