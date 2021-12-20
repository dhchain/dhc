package org.dhc.blockchain;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;
import org.dhc.util.Registry;

public class SendSMTransactionMessage extends Message {
	
private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Transaction transaction;

	public SendSMTransactionMessage(Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public void process(Peer peer) {
		if(alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);
		Network network = Network.getInstance();
		
		Blockchain blockchain = Blockchain.getInstance();
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(transaction.getReceiver(), blockchain.getPower())) {
			Registry.getInstance().getThinPeerNotifier().notifySecureMessage(transaction);
			return;
		}
		
		network.sendToAddress(transaction.getReceiver(), this);
		logger.trace("sent to {} message {}", transaction.getReceiver(), this);
	}
	
	@Override
	public String toString() {
		String str = String.format("SendSMTransactionMessage %s", transaction.getTransactionId());
		return str;
	}

}
