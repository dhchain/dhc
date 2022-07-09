package org.dhc.network;

import java.util.List;

import org.dhc.util.DhcRunnable;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;

public class SendMyToPeersMessage extends Message {

	private List<Peer> myToPeers;

	public SendMyToPeersMessage(List<Peer> myToPeers) {
		this.myToPeers = myToPeers;
	}

	@Override
	public void process(Peer peer) {
		ThreadExecutor.getInstance().execute(new DhcRunnable("SendMyToPeersMessage") {
			public void doRun() {
				Bootstrap bootstrap = Bootstrap.getInstance();
				bootstrap.addPeers(myToPeers);
			}
		});
	}

}
