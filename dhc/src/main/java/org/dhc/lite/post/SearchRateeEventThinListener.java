package org.dhc.lite.post;

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

public class SearchRateeEventThinListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Peer peer;
	private String correlationId;
	private ScheduledFuture<?> future;

	public SearchRateeEventThinListener(Peer peer, String correlationId) {
		this.peer = peer;
		this.correlationId = correlationId;
		schedule();
	}

	private void schedule() {

		future = ThreadExecutor.getInstance().schedule(new DhcRunnable("SearchRateeEventThinListener") {

			@Override
			public void doRun() {
				Listeners.getInstance().removeEventListener(SearchRateeEvent.class, SearchRateeEventThinListener.this);
			}
		}, Constants.MINUTE * 10);
	}

	@Override
	public void onEvent(Event event) {
		SearchRateeEvent searchRateeEvent = (SearchRateeEvent)event;
		if(!correlationId.equals(searchRateeEvent.getCorrelationId())) {
			logger.info("correlation ids are not equal correlationId={}, getBalanceEvent.getCorrelationId()={}", correlationId, searchRateeEvent.getCorrelationId());
			return;
		}
		Listeners.getInstance().removeEventListener(SearchRateeEvent.class, this);
		future.cancel(true);
		Message message  = new SearchRateeThinResponse(searchRateeEvent.getRatee());
		message.setCorrelationId(correlationId);
		peer.send(message);

	}

}
