package org.dhc.lite;

import org.dhc.network.Peer;
import org.dhc.util.Event;
import org.dhc.util.EventListener;
import org.dhc.util.GetBalanceEvent;
import org.dhc.util.Message;

public class GetBalanceEventThinListener implements EventListener {

	private Peer peer;
	private String correlationId;

	public GetBalanceEventThinListener(Peer peer, String correlationId) {
		this.peer = peer;
		this.correlationId = correlationId;
	}

	@Override
	public void onEvent(Event event) {
		
		GetBalanceEvent getBalanceEvent = (GetBalanceEvent)event;
		if(!correlationId.equals(getBalanceEvent.getCorrelationId())) {
			return;
		}
		
		Message message  = new GetBalanceReply(getBalanceEvent.getBalance());
		message.setCorrelationId(correlationId);
		peer.send(message);
	}

}
