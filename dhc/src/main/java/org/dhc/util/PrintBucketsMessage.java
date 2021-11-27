package org.dhc.util;

import org.dhc.network.Network;
import org.dhc.network.Peer;

public class PrintBucketsMessage extends Message {

	@Override
	public void process(Peer peer) {
		Network network = Network.getInstance();
		network.printBuckets();
		network.reloadBuckets();
		network.printBuckets();

	}

}
