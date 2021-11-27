package org.dhc.network;

import java.util.Comparator;

public class TimeAddedPeerComparator implements Comparator<Peer> {

	@Override
	public int compare(Peer p1, Peer p2) {
		int compareInUse = compareInUse(p1, p2);
		if(compareInUse != 0) {
			return compareInUse;
		}
		long difference = p1.getTimeAdded() - p2.getTimeAdded();
		if(difference > 0) {
			return -1;
		} else if(difference == 0) {
			return 0;
		} else {
			return 1;
		}
	}
	
	public int compareInUse(Peer p1, Peer p2) {
		if(p1.getInUse() && !p2.getInUse()) {
			return -1;
		}
		if(!p1.getInUse() && p2.getInUse()) {
			return 1;
		}
		return 0;
	}

}
