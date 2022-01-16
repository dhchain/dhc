package org.dhc.lite.post;

import java.util.List;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class SearchRatingsAsyncReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress from;
	private DhcAddress to;
	private List<Rating> ratings;

	public SearchRatingsAsyncReply(DhcAddress from, DhcAddress to, List<Rating> ratings) {
		this.from = from;
		this.to = to;
		this.ratings = ratings;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);

		if(DhcAddress.getMyDhcAddress().equals(from)) {
			SearchRatingsEvent event = new SearchRatingsEvent(ratings, getCorrelationId());
			Listeners.getInstance().sendEvent(event);
		}
		Network network = Network.getInstance();
		network.sendToAddress(from, this);
		logger.trace("sent to {} message {}", from, this);
		
	}
	
	@Override
	public String toString() {
		String str = String.format("SearchRatingsAsyncReply %s-%s-%s", from, to, getCorrelationId());
		return str;
	}

}
