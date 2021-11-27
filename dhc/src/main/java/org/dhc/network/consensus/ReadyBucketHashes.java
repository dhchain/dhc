package org.dhc.network.consensus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dhc.util.DhcLogger;

public class ReadyBucketHashes {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Map<Long, List<BucketHash>> map = new HashMap<>();
	
	public void add(BucketHash consensus, long index) {
		if(consensus == null) {
			return;
		}
		List<BucketHash> list = map.get(index);
		if(list == null) {
			list = new ArrayList<>();
			map.put(index, list);
		}
		list.add(consensus);
		logger.trace("ReadyBucketHashes add {} {}", index, consensus.toStringFull());
	}
	
	public void process(long index) {

		for (Iterator<Long> iterator = map.keySet().iterator(); iterator.hasNext();) {
			if (iterator.next() < index) {
				iterator.remove();
			}
		}
		
		
		List<BucketHash> list = map.remove(index);
		if(list == null) {
			return;
		}
		logger.trace("ReadyBucketHashes process index {}", index);
		for(BucketHash consensus: list) {
			Consensus.getInstance().processReadyBucketHashes(consensus, index);
		}
	}

}
