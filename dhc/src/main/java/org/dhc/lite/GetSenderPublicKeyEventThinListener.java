package org.dhc.lite;

import java.util.concurrent.ScheduledFuture;

import org.dhc.network.Peer;
import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Event;
import org.dhc.util.EventListener;
import org.dhc.util.Listeners;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;

public class GetSenderPublicKeyEventThinListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Peer peer;
	private String correlationId;
	private ScheduledFuture<?> future;

	public GetSenderPublicKeyEventThinListener(Peer peer, String correlationId) {
		this.peer = peer;
		this.correlationId = correlationId;
		schedule();
	}
		
	private void schedule() {

		future = ThreadExecutor.getInstance().schedule(new DhcRunnable("GetSenderPublicKeyEventThinListener") {

			@Override
			public void doRun() {
				Listeners.getInstance().removeEventListener(GetSenderPublicKeyEvent.class, GetSenderPublicKeyEventThinListener.this);
			}
		}, Constants.MINUTE * 10);
	}

	@Override
	public void onEvent(Event event) {
		GetSenderPublicKeyEvent getSenderPublicKeyEvent = (GetSenderPublicKeyEvent)event;
		if(!correlationId.equals(getSenderPublicKeyEvent.getCorrelationId())) {
			logger.info("correlation ids are not equal correlationId={}, getSenderPublicKeyEvent.getCorrelationId()={}", correlationId, getSenderPublicKeyEvent.getCorrelationId());
			return;
		}
		Listeners.getInstance().removeEventListener(GetSenderPublicKeyEvent.class, this);
		future.cancel(true);
		Message message  = new GetSenderPublicKeyReply(getSenderPublicKeyEvent.getPublicKey());
		message.setCorrelationId(correlationId);
		peer.send(message);

	}

}
