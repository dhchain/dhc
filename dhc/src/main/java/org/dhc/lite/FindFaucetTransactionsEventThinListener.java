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

public class FindFaucetTransactionsEventThinListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Peer peer;
	private String correlationId;
	private ScheduledFuture<?> future;

	public FindFaucetTransactionsEventThinListener(Peer peer, String correlationId) {
		this.peer = peer;
		this.correlationId = correlationId;
		schedule();
	}
		
	private void schedule() {

		future = ThreadExecutor.getInstance().schedule(new DhcRunnable("FindFaucetTransactionsEventThinListener") {

			@Override
			public void doRun() {
				Listeners.getInstance().removeEventListener(FindFaucetTransactionsEvent.class, FindFaucetTransactionsEventThinListener.this);
			}
		}, Constants.MINUTE * 10);
	}

	@Override
	public void onEvent(Event event) {
		FindFaucetTransactionsEvent findFaucetTransactionsEvent = (FindFaucetTransactionsEvent)event;
		if(!correlationId.equals(findFaucetTransactionsEvent.getCorrelationId())) {
			logger.info("correlation ids are not equal correlationId={}, findFaucetTransactionsEvent.getCorrelationId()={}", correlationId, findFaucetTransactionsEvent.getCorrelationId());
			return;
		}
		Listeners.getInstance().removeEventListener(FindFaucetTransactionsEvent.class, this);
		future.cancel(true);
		Message message  = new FindFaucetTransactionsReply(findFaucetTransactionsEvent.getTransactions());
		message.setCorrelationId(correlationId);
		peer.send(message);

	}

}
