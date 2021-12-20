package org.dhc.lite;

import org.dhc.network.Peer;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class NewMessagesNotification extends Message {
	
	private static final Listeners listeners = Listeners.getInstance();
	
	private SecureMessage secureMessage;

	public NewMessagesNotification(SecureMessage secureMessage) {
		this.secureMessage = secureMessage;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		listeners.sendEvent(new NewMessageEvent(secureMessage));
	}

	public SecureMessage getSecureMessage() {
		return secureMessage;
	}
	
	@Override
	public String toString() {
		String str = String.format("NewMessagesNotification %s", secureMessage.getTransactionId());
		return str;
	}

}
