package org.dhc.network;

import java.util.List;

import org.dhc.util.Message;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class NavigateMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private DhcAddress hash;
	private int index;

	public NavigateMessage(DhcAddress hash, int index) {
		this.hash = hash;
		this.index = index;
		logger.trace("NavigateMessage init {} index {}", hash, index);
	}

	@Override
	public void process(Peer peer) {
		if(alreadySent(hash + "-" + index + "-" + peer.getNetworkIdentifier())) {
			return;
		}
		List<Peer> peers  = Peer.getClosestKPeers(hash.xor(index));
		Message message = new NavigateReplyMessage(peers, hash, index);
		peer.send(message);
		
	}

}
