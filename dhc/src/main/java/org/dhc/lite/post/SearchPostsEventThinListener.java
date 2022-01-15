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

public class SearchPostsEventThinListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Peer peer;
	private String correlationId;
	private ScheduledFuture<?> future;

	public SearchPostsEventThinListener(Peer peer, String correlationId) {
		this.peer = peer;
		this.correlationId = correlationId;
		schedule();
	}

	private void schedule() {

		future = ThreadExecutor.getInstance().schedule(new DhcRunnable("SearchPostsEventThinListener") {

			@Override
			public void doRun() {
				Listeners.getInstance().removeEventListener(SearchPostsEvent.class, SearchPostsEventThinListener.this);
			}
		}, Constants.MINUTE * 10);
	}

	@Override
	public void onEvent(Event event) {
		SearchPostsEvent searchPostsEvent = (SearchPostsEvent)event;
		if(!correlationId.equals(searchPostsEvent.getCorrelationId())) {
			logger.info("correlation ids are not equal correlationId={}, searchPostsEvent.getCorrelationId()={}", correlationId, searchPostsEvent.getCorrelationId());
			return;
		}
		Listeners.getInstance().removeEventListener(SearchPostsEvent.class, this);
		future.cancel(true);
		Message message  = new SearchPostsThinResponse(searchPostsEvent.getRatees());
		message.setCorrelationId(correlationId);
		peer.send(message);

	}

}
