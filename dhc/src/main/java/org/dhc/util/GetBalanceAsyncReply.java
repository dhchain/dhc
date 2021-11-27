package org.dhc.util;

import org.dhc.network.Network;
import org.dhc.network.Peer;

public class GetBalanceAsyncReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress address;
	private Coin balance;
	private DhcAddress replyTo;
	private String blockhash;

	public GetBalanceAsyncReply(DhcAddress address, Coin balance, DhcAddress replyTo, String blockhash) {
		this.address = address;
		this.balance = balance;
		this.replyTo = replyTo;
		this.blockhash = blockhash;
		logger.trace("init {}", this);
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);

		if(DhcAddress.getMyDhcAddress().equals(replyTo)) {
			GetBalanceEvent event = new GetBalanceEvent(address, balance, blockhash, getCorrelationId());
			Listeners.getInstance().sendEvent(event);
		}
		Network network = Network.getInstance();
		network.sendToAddress(replyTo, this);
		logger.trace("sent to {} message {}", replyTo, this);
	}
	
	@Override
	public String toString() {
		String str = String.format("GetBalanceAsyncReply %s-%s-%s-%s", address, replyTo, blockhash, getCorrelationId());
		return str;
	}

	public DhcAddress getAddress() {
		return address;
	}

	public Coin getBalance() {
		return balance;
	}

	public DhcAddress getReplyTo() {
		return replyTo;
	}

	public String getBlockhash() {
		return blockhash;
	}


}
