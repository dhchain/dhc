package org.dhc.lite.post;

import java.util.List;

import org.dhc.util.Event;

public class SearchRatingsEvent implements Event {

	private List<Rating> ratings;
	private String correlationId;

	public SearchRatingsEvent(List<Rating> ratings, String correlationId) {
		this.ratings = ratings;
		this.correlationId = correlationId;
	}

	public List<Rating> getRatings() {
		return ratings;
	}

	public String getCorrelationId() {
		return correlationId;
	}

}
