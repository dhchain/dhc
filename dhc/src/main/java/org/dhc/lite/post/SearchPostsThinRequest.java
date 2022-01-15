package org.dhc.lite.post;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class SearchPostsThinRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress dhcAddress;

	public SearchPostsThinRequest(DhcAddress dhcAddress) {
		this.dhcAddress = dhcAddress;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("SearchPostsThinRequest.process() START");
		Network network = Network.getInstance();
		DhcAddress myAddress = DhcAddress.getMyDhcAddress();
		Message message = new SearchPostsAsyncRequest(myAddress, dhcAddress);
		message.setCorrelationId(getCorrelationId());
		Listeners.getInstance().addEventListener(SearchPostsEvent.class, new SearchPostsEventThinListener(peer, getCorrelationId()));
		network.sendToAddress(dhcAddress, message);
		logger.trace("SearchRateeRequest.process() END");
	}

}
