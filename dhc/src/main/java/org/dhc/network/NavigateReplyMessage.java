package org.dhc.network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Message;
import org.dhc.util.TAddress;
import org.dhc.util.ThreadExecutor;

public class NavigateReplyMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private List<Peer> peers;
	private TAddress hash;
	private int index;

	public NavigateReplyMessage(List<Peer> peers, TAddress hash, int index) {
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
		
		String str = String.format("NavigateReplyMessage@%s.doIt() %s %s", this.hashCode(), index, peer.socketToString());
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
		foundPeers.removeIf(p -> p.getInetSocketAddress().toString().contains("127.0.0.1"));
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
			if(Network.getInstance().getNetworkIdentifier().equals(networkIdentifier)) {
				logger.trace("NavigateReplyMessage.doIt() - Cannot connect to yourself");
				continue;
			}

			if(bootstrap.connectIfShouldAdd(p)) {
				Peer foundPeer = Peer.getInstance(p.getInetSocketAddress());
				new NavigateMessage(hash, index).send(foundPeer);
				count++;
			}
		}
		if(count > 0 ) {
			new NavigateMessage(hash, index + 1).send(peer);
		}
	}


}
