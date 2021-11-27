package org.dhc.util;

public class GetBalanceEvent implements Event {
	
	private DhcAddress address;
	private Coin balance;
	private String blockhash;
	private String correlationId;
	
	public GetBalanceEvent(DhcAddress address, Coin balance, String blockhash, String correlationId) {
		this.address = address;
		this.balance = balance;
		this.blockhash = blockhash;
		this.correlationId = correlationId;
	}

	public DhcAddress getAddress() {
		return address;
	}

	public Coin getBalance() {
		return balance;
	}

	public String getBlockhash() {
		return blockhash;
	}

	public String getCorrelationId() {
		return correlationId;
	}
	
	

}
