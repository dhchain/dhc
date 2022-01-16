package org.dhc.lite.post;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class GetKeywordsThinRequest extends Message {

	private DhcAddress dhcAddress;
	private String transactionId;

	public GetKeywordsThinRequest(DhcAddress dhcAddress, String transactionId) {
		this.dhcAddress = dhcAddress;
		this.transactionId = transactionId;
	}

	@Override
	public void process(Peer peer) {
		Network network = Network.getInstance();
		DhcAddress myDhcAddress = DhcAddress.getMyDhcAddress();
		String correlationId = getCorrelationId();
		Message message = new GetKeywordsAsyncRequest(myDhcAddress, dhcAddress, transactionId);
		message.setCorrelationId(correlationId);
		Listeners.getInstance().addEventListener(GetKeywordsEvent.class, new GetKeywordsListener(peer, correlationId));
		network.sendToAddress(dhcAddress, message);
	}

}
