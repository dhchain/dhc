package org.dhc.lite.post;

import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.Message;

public class SearchRatingsAsyncRequest extends Message {

	private DhcAddress from;
	private DhcAddress to;
	private String transactionId;

	public SearchRatingsAsyncRequest(DhcAddress from, DhcAddress to, String transactionId) {
		this.from = from;
		this.to = to;
		this.transactionId = transactionId;
	}

	@Override
	public void process(Peer peer) {
		// TODO Auto-generated method stub

	}

	public DhcAddress getFrom() {
		return from;
	}

	public DhcAddress getTo() {
		return to;
	}

	public String getTransactionId() {
		return transactionId;
	}

}
