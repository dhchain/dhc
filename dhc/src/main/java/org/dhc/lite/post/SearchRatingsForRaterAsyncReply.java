package org.dhc.lite.post;

import java.util.List;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class SearchRatingsForRaterAsyncReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myAddress;
	private DhcAddress dhcAddress;
	private List<Rating> ratings;

	public SearchRatingsForRaterAsyncReply(DhcAddress myAddress, DhcAddress dhcAddress, List<Rating> ratings) {
		this.myAddress = myAddress;
		this.dhcAddress = dhcAddress;
		this.ratings = ratings;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);

		if(DhcAddress.getMyDhcAddress().equals(myAddress)) {
			SearchRatingsForRaterEvent event = new SearchRatingsForRaterEvent(ratings, getCorrelationId());
			Listeners.getInstance().sendEvent(event);
		}
		Network network = Network.getInstance();
		network.sendToAddress(myAddress, this);
		logger.trace("sent to {} message {}", myAddress, this);
		
	}
	
	
	@Override
	public String toString() {
		String str = String.format("SearchRatingsForRaterAsyncReply %s-%s-%s", myAddress, dhcAddress, getCorrelationId());
		return str;
	}

}
