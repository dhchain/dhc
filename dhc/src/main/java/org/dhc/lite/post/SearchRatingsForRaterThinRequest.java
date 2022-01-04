package org.dhc.lite.post;

import org.dhc.network.Peer;
import org.dhc.util.Message;

public class SearchRatingsForRaterThinRequest extends Message {

	private String rater;

	public SearchRatingsForRaterThinRequest(String rater) {
		this.rater = rater;

	}

	@Override
	public void process(Peer peer) {


	}

	public String getRater() {
		return rater;
	}

}
