package org.dhc.lite.post;

import org.dhc.network.Peer;
import org.dhc.util.Message;

public class SearchRateeThinResponse extends Message {
	
	private Ratee ratee;

	public SearchRateeThinResponse(Ratee ratee) {
		super();
		this.ratee = ratee;
	}

	@Override
	public void process(Peer peer) {
		

	}

	public Ratee getRatee() {
		return ratee;
	}

}
