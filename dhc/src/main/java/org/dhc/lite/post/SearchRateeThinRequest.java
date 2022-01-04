package org.dhc.lite.post;

import org.dhc.blockchain.Blockchain;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
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
		Ratee ratee = null;
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(dhcAddress, Blockchain.getInstance().getPower())) {
			ratee = null;
		}
		Message message  = new SearchRateeThinResponse(ratee);
		message.setCorrelationId(getCorrelationId());
		peer.send(message);
		logger.trace("SearchRateeRequest.process() END ratee={}", ratee);

	}

	public String getTransactionId() {
		return transactionId;
	}

}
