package org.dhc.network;

import java.util.List;

import org.dhc.util.DhcRunnable;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;

public class GetPeersReplyMessage extends Message {

	private List<Peer> allPeers;

	public GetPeersReplyMessage(List<Peer> allPeers) {
		this.allPeers = allPeers;
	}

	@Override
	public void process(Peer peer) {
		
		
		ThreadExecutor.getInstance().execute(new DhcRunnable("GetPeersReplyMessage") {
			public void doRun() {
				Bootstrap bootstrap = Bootstrap.getInstance();
				bootstrap.addPeers(allPeers);
				PeersFinder.getInstance().peerComplete(peer);
			}
		});
		
	}
	
	

}
