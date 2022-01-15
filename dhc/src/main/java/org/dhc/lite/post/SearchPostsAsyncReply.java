package org.dhc.lite.post;

import java.util.List;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class SearchPostsAsyncReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myAddress;
	private DhcAddress dhcAddress;
	private List<Ratee> ratees;

	public SearchPostsAsyncReply(DhcAddress myAddress, DhcAddress dhcAddress, List<Ratee> ratees) {
		this.myAddress = myAddress;
		this.dhcAddress = dhcAddress;
		this.ratees = ratees;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);

		if(DhcAddress.getMyDhcAddress().equals(myAddress)) {
			SearchPostsEvent event = new SearchPostsEvent(ratees, getCorrelationId());
			Listeners.getInstance().sendEvent(event);
		}
		Network network = Network.getInstance();
		network.sendToAddress(myAddress, this);
		logger.trace("sent to {} message {}", myAddress, this);
		
	}
	
	@Override
	public String toString() {
		String str = String.format("SearchPostsAsyncReply %s-%s-%s", myAddress, dhcAddress, getCorrelationId());
		return str;
	}

}
