package org.dhc.lite;

import java.util.concurrent.ScheduledFuture;

import org.dhc.gui.promote.JoinInfo;
import org.dhc.gui.promote.JoinInfoEvent;
import org.dhc.network.Peer;
import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Event;
import org.dhc.util.EventListener;
import org.dhc.util.Listeners;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;

public class JoinInfoThinEventListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myDhcAddress;
	private String correlationId;
	private Peer peer;
	private ScheduledFuture<?> future;

	public JoinInfoThinEventListener(DhcAddress myDhcAddress, String correlationId, Peer peer) {
		logger.info("JoinInfoThinEventListener.init() myDhcAddress={}, correlationId={}, peer={}", myDhcAddress, correlationId, peer);
		this.myDhcAddress = myDhcAddress;
		this.correlationId = correlationId;
		this.peer = peer;
		schedule();
	}
	
	private void schedule() {
    	
		future = ThreadExecutor.getInstance().schedule(new DhcRunnable("JoinInfoThinEventListener") {
			
			@Override
			public void doRun() {
				Listeners.getInstance().removeEventListener(JoinInfoEvent.class, JoinInfoThinEventListener.this);
			}
		}, Constants.MINUTE * 10);
	}

	@Override
	public void onEvent(Event event) {
		JoinInfoEvent joinInfoEvent = (JoinInfoEvent)event;
		if(!correlationId.equals(joinInfoEvent.getCorrelationId())) {
			logger.info("correlation ids are not equal correlationId={}, joinInfoEvent.getCorrelationId()={}", correlationId, joinInfoEvent.getCorrelationId());
			return;
		}
		Listeners.getInstance().removeEventListener(JoinInfoEvent.class, this);
		future.cancel(true);
		JoinInfo joinInfo = joinInfoEvent.getJoinInfo();
		if(!joinInfo.isValid()) {
			logger.info("joinInfo.isValid() returned false");
			//return; we will do validation in client side
		}
		reply(joinInfoEvent);
		
	}

	private void reply(JoinInfoEvent joinInfoEvent) {
		
		JoinInfo joinInfo = joinInfoEvent.getJoinInfo();
		logger.info("JoinInfoThinEventListener.reply() joinInfo={}", joinInfo);
		joinInfo.setFrom(myDhcAddress);
		Message message  = new GetJoinInfoThinReply(joinInfo);
		message.setCorrelationId(correlationId);
		peer.send(message);
	}

}
