package org.dhc.util;

import java.util.List;

import org.dhc.blockchain.Transaction;
import org.dhc.lite.NewMessagesNotification;
import org.dhc.lite.NotifyTransactionMessage;
import org.dhc.lite.SecureMessage;
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
			if(transaction.getReceiver().startsWith(peer.getTAddress()) && Applications.MESSAGING.equals(transaction.getApp())) {
				SecureMessage message = new SecureMessage();
				message.setExpire(transaction.getExpiringData().getValidForNumberOfBlocks());
				message.setFee(transaction.getFee());
				message.setRecipient(transaction.getReceiver().toString());
				message.setSenderPublicKey(transaction.getSender());
				message.setText(transaction.getExpiringData().getData());
				message.setTimeStamp(transaction.getTimeStamp());
				message.setTransactionId(transaction.getTransactionId());
				message.setValue(transaction.getValue());
				peer.send(new NewMessagesNotification(message));
			}
		}
	}

}
