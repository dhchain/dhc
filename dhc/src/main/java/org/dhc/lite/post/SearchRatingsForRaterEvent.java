package org.dhc.lite.post;

import java.util.List;

import org.dhc.util.Event;

public class SearchRatingsForRaterEvent implements Event {
	
	private List<Rating> ratings;
	private String correlationId;

	public SearchRatingsForRaterEvent(List<Rating> ratings, String correlationId) {
		this.ratings = ratings;
		this.correlationId = correlationId;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public List<Rating> getRatings() {
		return ratings;
	}

}
