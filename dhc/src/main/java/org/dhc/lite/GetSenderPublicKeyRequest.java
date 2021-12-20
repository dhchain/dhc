package org.dhc.lite;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class GetSenderPublicKeyRequest extends Message {

	private String address;

	public GetSenderPublicKeyRequest(String address) {
		this.address = address;
	}

	@Override
	public void process(Peer peer) {
		
		Network network = Network.getInstance();
		DhcAddress myAddress = DhcAddress.getMyDhcAddress();
		DhcAddress toAddress = new DhcAddress(address);
		Message message = new GetSenderPublicKeyAsyncRequest(myAddress, toAddress);
		message.setCorrelationId(getCorrelationId());
		Listeners.getInstance().addEventListener(GetSenderPublicKeyEvent.class, new GetSenderPublicKeyEventThinListener(peer, getCorrelationId()));
		network.sendToAddress(toAddress, message);

	}


}
