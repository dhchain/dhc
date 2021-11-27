package org.dhc.network;

import java.util.List;

import org.dhc.util.Message;

public class SendMyToPeersMessage extends Message {

	private List<Peer> myToPeers;

	public SendMyToPeersMessage(List<Peer> myToPeers) {
		this.myToPeers = myToPeers;
	}

	@Override
	public void process(Peer peer) {
		Bootstrap bootstrap = Bootstrap.getInstance();
		bootstrap.addPeers(myToPeers);
	}

}
