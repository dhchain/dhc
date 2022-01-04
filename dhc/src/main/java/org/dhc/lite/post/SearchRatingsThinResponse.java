package org.dhc.lite.post;

import java.util.List;

import org.dhc.network.Peer;
import org.dhc.util.Message;

public class SearchRatingsThinResponse extends Message {
	
	private List<Rating> ratings;

	public SearchRatingsThinResponse(List<Rating> ratings) {
		super();
		this.ratings = ratings;
	}

	@Override
	public void process(Peer peer) {
		

	}

	public List<Rating> getRatings() {
		return ratings;
	}

}
