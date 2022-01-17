package org.dhc.lite.post;

import org.dhc.blockchain.Keywords;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class GetKeywordsAsyncReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myAddress;
	private Keywords keywords;

	public GetKeywordsAsyncReply(DhcAddress myAddress, Keywords keywords) {
		this.myAddress = myAddress;
		this.keywords = keywords;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);

		if(DhcAddress.getMyDhcAddress().equals(myAddress)) {
			GetKeywordsEvent event = new GetKeywordsEvent(keywords, getCorrelationId());
			Listeners.getInstance().sendEvent(event);
		}
		Network network = Network.getInstance();
		network.sendToAddress(myAddress, this);
		logger.trace("sent to {} message {}", myAddress, this);
		
	}
	
	@Override
	public String toString() {
		String str = String.format("GetKeywordsAsyncReply %s-%s", myAddress, getCorrelationId());
		return str;
	}

}
