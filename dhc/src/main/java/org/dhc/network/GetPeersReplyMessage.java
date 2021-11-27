package org.dhc.network;

import java.util.List;

import org.dhc.util.Message;

public class GetPeersReplyMessage extends Message {

	private List<Peer> allPeers;

	public GetPeersReplyMessage(List<Peer> allPeers) {
		this.allPeers = allPeers;
	}

	@Override
	public void process(Peer peer) {
		Bootstrap bootstrap = Bootstrap.getInstance();
		bootstrap.addPeers(allPeers);
		PeersFinder.getInstance().peerComplete(peer);
	}

}
