package org.dhc.network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;

public class SendMyToPeersMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private List<Peer> myToPeers;

	public SendMyToPeersMessage(List<Peer> myToPeers) {
		this.myToPeers = myToPeers;
	}

	@Override
	public void process(Peer peer) {
		String str = "SendMyToPeersMessage";
		
		try {
			ThreadExecutor.getInstance().execute(new DhcRunnable(str) {
				public void doRun() {
					doIt();
				}
			});
		} catch (RejectedExecutionException e) {
			logger.error(e.getMessage(), e);
			return;
		}
	}
	
	private void doIt() {
		Bootstrap bootstrap = Bootstrap.getInstance();
		List<Peer> list = Peer.getPeers();
		Set<Peer> foundPeers = new HashSet<Peer>(myToPeers);
		foundPeers.removeIf(p -> p.getInetSocketAddress().toString().contains("127.0.0.1"));
		foundPeers.removeAll(list);

		for (Peer p : foundPeers) {
			String networkIdentifier = p.getNetworkIdentifier();
			if(networkIdentifier == null) {
				continue;
			}
			if(!Peer.getPeersByNetworkIdentifier(networkIdentifier).isEmpty()) {
				continue;
			}
			if(Network.getInstance().getNetworkIdentifier().equals(networkIdentifier)) {
				logger.trace("SendMyToPeersMessage.doIt() - Cannot connect to yourself");
				continue;
			}

			bootstrap.connectIfShouldAdd(p);
		}
	}

}
