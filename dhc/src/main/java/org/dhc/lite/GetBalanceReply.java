package org.dhc.lite;

import org.dhc.network.Peer;
import org.dhc.util.Coin;
import org.dhc.util.Message;

public class GetBalanceReply extends Message {
	
	private Coin balance;

	public GetBalanceReply(Coin balance) {
		super();
		this.setBalance(balance);
	}

	@Override
	public void process(Peer peer) {

	}

	public Coin getBalance() {
		return balance;
	}

	public void setBalance(Coin balance) {
		this.balance = balance;
	}

}
