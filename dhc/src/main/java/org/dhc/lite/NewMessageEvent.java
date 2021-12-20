package org.dhc.lite;

import org.dhc.util.Event;

public class NewMessageEvent implements Event {
	
	private SecureMessage secureMessage;

	public NewMessageEvent(SecureMessage secureMessage) {
		this.secureMessage = secureMessage;
	}

	public SecureMessage getSecureMessage() {
		return secureMessage;
	}

}
