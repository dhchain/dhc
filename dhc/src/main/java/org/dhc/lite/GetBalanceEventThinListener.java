package org.dhc.lite;

import java.util.concurrent.ScheduledFuture;

import org.dhc.network.Peer;
import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Event;
import org.dhc.util.EventListener;
import org.dhc.util.GetBalanceEvent;
import org.dhc.util.Listeners;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;

public class GetBalanceEventThinListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Peer peer;
	private String correlationId;
	private ScheduledFuture<?> future;

	public GetBalanceEventThinListener(Peer peer, String correlationId) {
		this.peer = peer;
		this.correlationId = correlationId;
		schedule();
	}
		
	private void schedule() {

		future = ThreadExecutor.getInstance().schedule(new DhcRunnable("GetBalanceEventThinListener") {

			@Override
			public void doRun() {
				remove();
			}
		}, Constants.MINUTE * 10);
	}
	
	private void remove() {
		Listeners.getInstance().removeEventListener(GetBalanceEvent.class, this);
	}

	@Override
	public void onEvent(Event event) {
		
		GetBalanceEvent getBalanceEvent = (GetBalanceEvent)event;
		if(!correlationId.equals(getBalanceEvent.getCorrelationId())) {
			logger.info("correlation ids are not equal correlationId={}, getBalanceEvent.getCorrelationId()={}", correlationId, getBalanceEvent.getCorrelationId());
			return;
		}
		remove();
		future.cancel(true);
		Message message  = new GetBalanceReply(getBalanceEvent.getBalance());
		message.setCorrelationId(correlationId);
		peer.send(message);
	}

}
