package org.dhc.lite.post;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class SearchRateeAsyncReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myAddress;
	private DhcAddress dhcAddress;
	private Ratee ratee;

	public SearchRateeAsyncReply(DhcAddress myAddress, DhcAddress dhcAddress, Ratee ratee) {
		this.myAddress = myAddress;
		this.dhcAddress = dhcAddress;
		this.ratee = ratee;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);

		if(DhcAddress.getMyDhcAddress().equals(myAddress)) {
			SearchRateeEvent event = new SearchRateeEvent(ratee, getCorrelationId());
			Listeners.getInstance().sendEvent(event);
		}
		Network network = Network.getInstance();
		network.sendToAddress(myAddress, this);
		logger.trace("sent to {} message {}", myAddress, this);
		
	}
	
	@Override
	public String toString() {
		String str = String.format("SearchRateeAsyncReply %s-%s-%s", myAddress, dhcAddress, getCorrelationId());
		return str;
	}

}
