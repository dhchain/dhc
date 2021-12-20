package org.dhc.lite;

import java.util.List;

import org.dhc.network.Peer;
import org.dhc.util.Message;

public class GetMessagesReply extends Message {

	private List<SecureMessage> messages;

	public GetMessagesReply(List<SecureMessage> messages) {
		super();
		this.messages = messages;
	}

	@Override
	public void process(Peer peer) {
		

	}

	public List<SecureMessage> getMessages() {
		return messages;
	}

}
