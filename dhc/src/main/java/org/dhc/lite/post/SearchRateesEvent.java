package org.dhc.lite.post;

import java.util.List;

import org.dhc.util.Event;

public class SearchRateesEvent implements Event {
	
	private List<Ratee> ratees;
	private String correlationId;

	public SearchRateesEvent(List<Ratee> ratees, String correlationId) {
		this.ratees = ratees;
		this.correlationId = correlationId;
	}

	public List<Ratee> getRatees() {
		return ratees;
	}

	public String getCorrelationId() {
		return correlationId;
	}

}
