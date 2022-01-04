package org.dhc.lite.post;

import org.dhc.network.Peer;
import org.dhc.util.Event;
import org.dhc.util.EventListener;

public class SearchRatingsListener implements EventListener {

	private Peer peer;
	private String correlationId;

	public SearchRatingsListener(Peer peer, String correlationId) {
		this.peer = peer;
		this.correlationId = correlationId;

	}

	@Override
	public void onEvent(Event event) {
		

	}

	public Peer getPeer() {
		return peer;
	}

	public String getCorrelationId() {
		return correlationId;
	}

}
