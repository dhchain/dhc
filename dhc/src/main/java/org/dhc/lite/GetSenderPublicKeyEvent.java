package org.dhc.lite;

import java.security.PublicKey;

import org.dhc.util.Event;

public class GetSenderPublicKeyEvent implements Event {
	
	private PublicKey publicKey;
	private String correlationId;

	public GetSenderPublicKeyEvent(PublicKey publicKey, String correlationId) {
		this.publicKey = publicKey;
		this.correlationId = correlationId;
	}

	
	public PublicKey getPublicKey() {
		return publicKey;
	}


	public String getCorrelationId() {
		return correlationId;
	}

}
