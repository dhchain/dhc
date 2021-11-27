package org.dhc.network;

import java.util.List;

import org.dhc.util.Message;

public class GetPeersMessage extends Message {
	
	private boolean dontSkip;

	@Override
	public void process(Peer peer) {
		if(!dontSkip && alreadySent(peer.getNetworkIdentifier())) {
			return;
		}
		List<Peer> allPeers = Peer.getAllToPeers();
		peer.send(new GetPeersReplyMessage(allPeers));
	}

	public void setDontSkip(boolean dontSkip) {
		this.dontSkip = dontSkip;
	}

}
