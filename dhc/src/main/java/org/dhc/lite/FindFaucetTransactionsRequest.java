package org.dhc.lite;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class FindFaucetTransactionsRequest extends Message {

	private DhcAddress address;
	private String ip;

	public FindFaucetTransactionsRequest(DhcAddress address, String ip) {
		this.address = address;
		this.ip = ip;
	}

	@Override
	public void process(Peer peer) {
		Network network = Network.getInstance();
		DhcAddress myAddress = DhcAddress.getMyDhcAddress();
		Message message = new FindFaucetTransactionsAsyncRequest(myAddress, address, ip);
		message.setCorrelationId(getCorrelationId());
		Listeners.getInstance().addEventListener(FindFaucetTransactionsEvent.class, new FindFaucetTransactionsEventThinListener(peer, getCorrelationId()));
		network.sendToAddress(address, message);

	}

	public DhcAddress getAddress() {
		return address;
	}

	public String getIp() {
		return ip;
	}


}
