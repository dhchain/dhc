package org.dhc.network;

import java.util.List;

import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;
import org.dhc.util.TAddress;

public class NavigateMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private TAddress hash;
	private int index;

	public NavigateMessage(TAddress hash, int index) {
		this.hash = hash;
		this.index = index;
	}

	@Override
	public void process(Peer peer) {
		String str = String.format("NavigateMessage.process %s-%s-%s", hash, index, peer.getInetSocketAddress());
		if(alreadySent(str)) {
			return;
		}
		List<Peer> peers  = Peer.getClosestKPeers(hash.xor(index));
		Message message = new NavigateReplyMessage(peers, hash, index);
		peer.send(message);
		
	}
	
	public void send(Peer peer) {
		String str = String.format("NavigateMessage.send %s-%s-%s", hash, index, peer.getInetSocketAddress());
		if(alreadySent(str, Constants.MINUTE)) {
			return;
		}
		logger.trace(str);
		peer.send(this);
	}

}
