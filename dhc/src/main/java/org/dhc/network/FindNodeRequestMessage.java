package org.dhc.network;

import java.util.List;

import org.dhc.util.DhcLogger;
import org.dhc.util.Message;
import org.dhc.util.TAddress;

/**
 * The recipient peer will return the k nodes in its own buckets that are the closest ones to the passed hash.
 *
 */
public class FindNodeRequestMessage extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private TAddress hash;

	@Override
	public void process(Peer peer) {
		logger.trace("START FindNodeRequestMessage from {}", peer);
		List<Peer> peers  = Peer.getClosestKPeers(getHash());
		FindNodeResponseMessage message = new FindNodeResponseMessage(peers);
		peer.send(message);
		logger.trace("END FindNodeRequestMessage from {}", peer);
	}

	public TAddress getHash() {
		return hash;
	}

	public FindNodeRequestMessage(TAddress hash) {
		this.hash = hash;
	}

}
