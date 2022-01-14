package org.dhc.lite.post;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class SearchRateeThinRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private DhcAddress dhcAddress;
	private String transactionId;

	public SearchRateeThinRequest(DhcAddress dhcAddress, String transactionId) {
		super();
		this.dhcAddress = dhcAddress;
		this.transactionId = transactionId;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("SearchRateeRequest.process() START");
		Network network = Network.getInstance();
		DhcAddress myAddress = DhcAddress.getMyDhcAddress();
		Message message = new SearchRateeAsyncRequest(myAddress, dhcAddress, transactionId);
		message.setCorrelationId(getCorrelationId());
		Listeners.getInstance().addEventListener(SearchRateeEvent.class, new SearchRateeEventThinListener(peer, getCorrelationId()));
		network.sendToAddress(dhcAddress, message);
		logger.trace("SearchRateeRequest.process() END");
	}



}
