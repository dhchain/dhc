package org.dhc.blockchain;

import org.dhc.util.BoundedMap;

public class BannedBlockhashes {
	
	private final BoundedMap<String, String> boundedMap = new BoundedMap<>(100);
	
	public void add(String blockhash) {
		boundedMap.put(blockhash, blockhash);
	}
	
	public boolean contains(String blockhash) {
		return boundedMap.get(blockhash) != null;
	}

}
