package org.dhc.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.dhc.util.Message;

public class GetPeersMessage extends Message {
	
	private boolean dontSkip;

	@Override
	public void process(Peer peer) {
		if(!dontSkip && alreadySent(peer.getNetworkIdentifier())) {
			return;
		}
		Set<Peer> peers = new HashSet<>(Bootstrap.getInstance().getCandidatePeers());
		peers.addAll(Peer.getAllToPeers());
		peer.send(new GetPeersReplyMessage(new ArrayList<Peer>(peers)));
	}

	public void setDontSkip(boolean dontSkip) {
		this.dontSkip = dontSkip;
	}

}
