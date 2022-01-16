package org.dhc.lite.post;

import org.dhc.blockchain.Keywords;
import org.dhc.util.Event;

public class GetKeywordsEvent implements Event {
	
	private Keywords keywords;
	private String correlationId;
	
	public GetKeywordsEvent(Keywords keywords, String correlationId) {
		super();
		this.keywords = keywords;
		this.correlationId = correlationId;
	}

	public Keywords getKeywords() {
		return keywords;
	}

	public String getCorrelationId() {
		return correlationId;
	}

}
