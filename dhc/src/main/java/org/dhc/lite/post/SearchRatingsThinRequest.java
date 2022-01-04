package org.dhc.lite.post;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class SearchRatingsThinRequest extends Message {

	private String rateeName;
	private String transactionId;

	public SearchRatingsThinRequest(String rateeName, String transactionId) {
		this.rateeName = rateeName;
		this.transactionId = transactionId;
		
	}

	@Override
	public void process(Peer peer) {
		
		Network network = Network.getInstance();
		DhcAddress from = DhcAddress.getMyDhcAddress();
		DhcAddress to = CryptoUtil.getDhcAddressFromString(rateeName);
		String correlationId = getCorrelationId();
		Message message = new SearchRatingsAsyncRequest(from, to, transactionId);
		message.setCorrelationId(correlationId);
		Listeners.getInstance().addEventListener(SearchRatingsEvent.class, new SearchRatingsListener(peer, correlationId));
		network.sendToAddress(to, message);

	}

	public String getRateeName() {
		return rateeName;
	}

	public String getTransactionId() {
		return transactionId;
	}

}
