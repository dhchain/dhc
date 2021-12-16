package org.dhc.util;

import java.util.List;

import org.dhc.blockchain.Transaction;
import org.dhc.lite.NotifyTransactionMessage;
import org.dhc.network.Network;
import org.dhc.network.Peer;

public class ThinPeerNotifier {
	
	public void notifyTransaction(Transaction transaction) {
		ThreadExecutor.getInstance().execute(new DhcRunnable("notifyTransaction") {
			public void doRun() {
				doNotifyTransaction(transaction);
			}
		});
	}

	private void doNotifyTransaction(Transaction transaction) {
		List<Peer> thinPeers = Network.getInstance().getThinPeers();
		for(Peer peer: thinPeers) {
			if(transaction.getSenderDhcAddress().startsWith(peer.getTAddress()) || transaction.getReceiver().startsWith(peer.getTAddress())) {
				peer.send(new NotifyTransactionMessage(transaction));
			}
		}
	}

}
