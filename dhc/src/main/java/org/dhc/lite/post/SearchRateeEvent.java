package org.dhc.lite.post;

import org.dhc.util.Event;

public class SearchRateeEvent implements Event {
	
	private Ratee ratee;
	private String correlationId;
	
	public SearchRateeEvent(Ratee ratee, String correlationId) {
		super();
		this.ratee = ratee;
		this.correlationId = correlationId;
	}

	public Ratee getRatee() {
		return ratee;
	}

	public String getCorrelationId() {
		return correlationId;
	}

}
