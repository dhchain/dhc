package org.dhc.blockchain;

import org.dhc.util.Constants;
import org.dhc.util.ExpiringMap;

public class MissingOutputs {
	
	private final ExpiringMap<TransactionKey, Block> map =  new ExpiringMap<>(Constants.MINUTE * 5);
	
	public void put(TransactionKey key, Block block) {
		map.put(key, block);
	}

	public Block get(TransactionKey transactionKey) {
		return map.remove(transactionKey);
	}

}
