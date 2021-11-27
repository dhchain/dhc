package org.dhc.util;

import org.dhc.blockchain.Blockchain;
import org.dhc.network.Network;
import org.dhc.network.Peer;

public class HelloMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress address;

	public HelloMessage(String address) {
		this.address = new DhcAddress(address);
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.info(toString());
		send();
		
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(address, Blockchain.getInstance().getPower())) {
			return;
		}

	}
	
	@Override
	public String toString() {
		String str = String.format("HelloMessage %s", address);
		return str;
	}
	
	public void send() {
		
		String bucketKey = Network.getInstance().getBucketKey(address);
		logger.info("HelloMessage send to key='{}' address='{}'", bucketKey, address);
		Network.getInstance().sendToKey(bucketKey, this);

	}

}
