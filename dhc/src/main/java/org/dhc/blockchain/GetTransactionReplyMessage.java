package org.dhc.blockchain;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Constants;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class GetTransactionReplyMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Transaction transaction;
	private DhcAddress address;

	public GetTransactionReplyMessage(Transaction transaction, DhcAddress address) {
		this.transaction = transaction;
		this.address = address;
		logger.trace("Sent transaction {} to address {}", transaction, address);
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString(), Constants.MINUTE)) {
			return;
		}
		logger.trace("Received transaction {} to address {}", transaction, address);
		
		if(!DhcAddress.getMyDhcAddress().equals(address)) {
			send();
			return;
		}
		
		Registry.getInstance().getGetTransaction().put(transaction);//put will also process the transaction

	}
	
	@Override
	public String toString() {
		String str = String.format("GetTransactionReplyMessage %s", getCorrelationId());
		return str;
	}

	public void send() {
		Network network = Network.getInstance();
		network.sendToAddress(address, this);
	}

}
