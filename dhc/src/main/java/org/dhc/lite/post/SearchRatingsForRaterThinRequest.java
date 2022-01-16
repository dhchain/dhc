package org.dhc.lite.post;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class SearchRatingsForRaterThinRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private String rater;

	public SearchRatingsForRaterThinRequest(String rater) {
		this.rater = rater;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("SearchRatingsForRaterThinRequest.process() START");
		Network network = Network.getInstance();
		DhcAddress myAddress = DhcAddress.getMyDhcAddress();
		DhcAddress dhcAddress = new DhcAddress(rater);
		Message message = new SearchPostsAsyncRequest(myAddress, dhcAddress);
		message.setCorrelationId(getCorrelationId());
		Listeners.getInstance().addEventListener(SearchPostsEvent.class, new SearchPostsEventThinListener(peer, getCorrelationId()));
		network.sendToAddress(dhcAddress, message);
		logger.trace("SearchRatingsForRaterThinRequest.process() END");
	}


}
