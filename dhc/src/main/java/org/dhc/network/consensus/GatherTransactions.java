package org.dhc.network.consensus;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Transaction;
import org.dhc.network.Network;
import org.dhc.util.Callback;
import org.dhc.util.Constants;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class GatherTransactions {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static GatherTransactions instance = new GatherTransactions();

	public static GatherTransactions getInstance() {
		return instance;
	}

	private Map<String, GatherTransactionsEntry> map = new ConcurrentHashMap<>();

	private GatherTransactions() {

	}

	public void run(BucketHash bucketHash, long blockchainIndex, Callback callback) {
		
		if (bucketHash.isHashForTransactionsValid()) {
			callback.callBack(bucketHash);
			return;
		}

		GatherTransactionsEntry entry = new GatherTransactionsEntry(bucketHash, blockchainIndex, callback);
		
		synchronized (map) {
			if (map.containsKey(entry.getKey())) {
				return;
			}
			map.put(entry.getKey(), entry);
		}
		
		//logger.trace("GatherTransactions.run() blockchainIndex={}, bucketHash={}", blockchainIndex, bucketHash.getKeyHash());

		Network.getInstance().sendToKey(bucketHash.getBinaryStringKey(), new GatherTransactionsMessage(bucketHash, blockchainIndex, bucketHash));
		
		expire(entry, callback);
	}
	
	private void expire(GatherTransactionsEntry entry, Callback callback) {
		logger.trace("Expire GatherTransactionsEntry blockchain index {} backetHash {}", entry.getBlockchainIndex(), entry.getBucketHash().toStringFull());
		long realBlockchainIndex = Blockchain.getInstance().getIndex();
		ThreadExecutor.getInstance().schedule(new DhcRunnable("GatherTransactions") {
			
			@Override
			public void doRun() {
				synchronized (map) {
					if (!map.containsKey(entry.getKey())) {
						return;
					}
					map.remove(entry.getKey());
				}
				if(Blockchain.getInstance().getIndex() == realBlockchainIndex) {
					Blockchain.getInstance().syncAsync();
				}
				callback.expire();
			}
		}, Constants.SECOND * 60);
	}

	private boolean validate(BucketHash bucketHash, long blockchainIndex, BucketHash subBucketHash) {
		//logger.trace("GatherTransactions subBucketHash={}", subBucketHash == null? "null": subBucketHash.toStringFull());
		if (subBucketHash == null) {
			return false;
		}

		GatherTransactionsEntry entry;
		
		synchronized (map) {
			entry = map.get(GatherTransactionsEntry.constructKey(bucketHash, blockchainIndex));
	
			if (entry == null) {
				return false;
			}
		}
		
		synchronized (entry) {
			if (entry.getMap().containsKey(subBucketHash.getBinaryStringKey())) {
				return false;
			}
			entry.getMap().put(subBucketHash.getBinaryStringKey(), subBucketHash);
		}

		if (!subBucketHash.isHashForTransactionsValid()) {
			Network.getInstance().sendToKey(subBucketHash.getLeft().getBinaryStringKey(),
					new GatherTransactionsMessage(bucketHash, blockchainIndex, subBucketHash.getLeft()));
			Network.getInstance().sendToKey(subBucketHash.getRight().getBinaryStringKey(),
					new GatherTransactionsMessage(bucketHash, blockchainIndex, subBucketHash.getRight()));
			return false;
		}
		return true;
	}

	private void doProcess(BucketHash bucketHash, long blockchainIndex, BucketHash subBucketHash) {
		GatherTransactionsEntry entry;

		synchronized (map) {
			entry = map.get(GatherTransactionsEntry.constructKey(bucketHash, blockchainIndex));
	
			if (entry == null) {
				return;
			}
		}
		
		if(subBucketHash.getBinaryStringKey().equals(bucketHash.getBinaryStringKey())) {
			Callback callback = entry.getCallback();
			bucketHash = entry.getBucketHash();
			synchronized (map) {
				if(map.remove(entry.getKey()) == null) {
					return;
				}
			}
			bucketHash.setTransactions(subBucketHash.getTransactionsIncludingCoinbase());
			logger.info("GatherTransactions bucketHash={}", bucketHash.toStringFull());
			callback.callBack(subBucketHash);
		}

		BucketHash other;
		
		synchronized (entry) {
			other = entry.getMap().get(subBucketHash.getKey().getOtherBucketKey().getKey());
			if (other == null || !other.isHashForTransactionsValid()) {
				//logger.trace("GatherTransactions other=null or not complete for other key={}", subBucketHash.getKey().getOtherBucketKey().getKey());
				return;
			}
		}

		Set<Transaction> set = new HashSet<>();

		if (subBucketHash.getTransactionsIncludingCoinbase() != null) {
			set.addAll(subBucketHash.getTransactionsIncludingCoinbase());
		}
		if (other.getTransactionsIncludingCoinbase() != null) {
			set.addAll(other.getTransactionsIncludingCoinbase());
		}

		//logger.trace("GatherTransactions set.size()={}", set.size());
		
		if(!"".equals(subBucketHash.getBinaryStringKey())) {
			BucketHash parent = entry.getMap().get(subBucketHash.getKey().getParentKey().getKey());
			if (parent != null && !parent.getBinaryStringKey().equals(bucketHash.getBinaryStringKey())) {
				parent.setTransactions(set);
				//logger.trace("GatherTransactions parent={}", parent.toStringFull());
				doProcess(bucketHash, blockchainIndex, parent);
				return;
			}
		}
		
		
		synchronized (map) {
			if(map.remove(entry.getKey()) == null) {
				return;
			}
		}

		bucketHash = entry.getBucketHash();
		bucketHash.setTransactions(set);

		if (bucketHash.isHashForTransactionsValid()) {
			Callback callback = entry.getCallback();
			//logger.trace("GatherTransactions bucketHash={}", bucketHash.toStringFull());
			callback.callBack(bucketHash);
		} else {
			logger.info("GatherTransactions bucketHash.isHashForTransactionsValid()={}", bucketHash.isHashForTransactionsValid());
		}

	}

	public void process(BucketHash bucketHash, long blockchainIndex, BucketHash subBucketHash) {
		if (!validate(bucketHash, blockchainIndex, subBucketHash)) {
			return;
		}

		doProcess(bucketHash, blockchainIndex, subBucketHash);

	}

}
