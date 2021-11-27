package org.dhc.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class Bucket {

	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();
	
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

	public void addAll(List<Peer> peers) {
		this.peers.addAll(peers);
	}

	public boolean isEmpty() {
		return peers.isEmpty();
	}

	public void fill() {
		List<Peer> allPeers = Peer.getPeers();
		String myKey = buckets.getBucketKey(getIndex());//this can happen if the number of buckets changed and this bucket was removed
		if(myKey == null) {
			return;
		}
		for(Peer peer: allPeers) {
			DhcAddress dhcAddress = peer.getDhcAddress();
			if(dhcAddress != null && dhcAddress.getBinary().startsWith(myKey) && !peers.contains(peer)) {
				peers.add(peer);
			}
		}
		
	}
	
	public void trim() {
/*		Collections.sort(peers, new TimeAddedPeerComparator());
		peers = peers.subList(0, Math.min(Constants.k * 2, peers.size()));*/
	}

}
