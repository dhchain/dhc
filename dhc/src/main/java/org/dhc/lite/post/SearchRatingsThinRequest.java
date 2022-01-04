package org.dhc.lite.post;

import org.dhc.network.Peer;
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
		

	}

	public String getRateeName() {
		return rateeName;
	}

	public String getTransactionId() {
		return transactionId;
	}

}
