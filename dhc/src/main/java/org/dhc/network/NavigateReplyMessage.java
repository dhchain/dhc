package org.dhc.network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class NavigateReplyMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private List<Peer> peers;
	private DhcAddress hash;
	private int index;

	public NavigateReplyMessage(List<Peer> peers, DhcAddress hash, int index) {
		this.peers = peers;
		this.hash = hash;
		this.index = index;
		logger.trace("NavigateReplyMessage init {} index {} peers {}", hash, index, peers);
	}

	@Override
	public void process(Peer peer) {
		logger.trace("START");
		if(alreadySent(hash + "-" + index + "-" + peer.getNetworkIdentifier())) {
			return;
		}
		
		String str = String.format("NavigateReplyMessage.doIt() index={} peer {}", index, peer);
		ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
			public void doRun() {
				doIt(peer);
			}
		});

	}
	
	private void doIt(Peer peer) {
		Bootstrap bootstrap = Bootstrap.getInstance();
		List<Peer> list = Peer.getPeers();
		Set<Peer> foundPeers = new HashSet<Peer>(peers);
		foundPeers.removeAll(list);
		
		int count = 0;
		for (Peer p : foundPeers) {
			String networkIdentifier = p.getNetworkIdentifier();
			if(networkIdentifier == null) {
				continue;
			}
			if(!Peer.getPeersByNetworkIdentifier(networkIdentifier).isEmpty()) {
				continue;
			}
			Peer foundPeer = Peer.getInstance(p.getInetSocketAddress());
			foundPeer.setType(PeerType.TO);
			if(bootstrap.connect(foundPeer)) {
				foundPeer.send(new NavigateMessage(hash, index));
				count++;
			}
		}
		if(count > 0) {
			peer.send(new NavigateMessage(hash, index + 1));
		}
	}


}
