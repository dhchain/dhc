package org.dhc.network;

import java.util.List;

import org.dhc.util.Message;
import org.dhc.util.DhcLogger;

public class FindNodeResponseMessage extends Message {
	
	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private List<Peer> peers;

	@Override
	public void process(Peer peer) {
		Bootstrap bootstrap = Bootstrap.getInstance();
		bootstrap.addPeers(peers);
		PeersFinder.getInstance().peerComplete(peer);
	}

	public FindNodeResponseMessage(List<Peer> peers) {
		this.peers = peers;
	}

	public List<Peer> getPeers() {
		return peers;
	}

}
