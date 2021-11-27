package org.dhc.lite;

import org.dhc.gui.promote.JoinInfo;
import org.dhc.gui.promote.JoinInfoEvent;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Event;
import org.dhc.util.EventListener;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class JoinInfoThinEventListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myDhcAddress;
	private String correlationId;
	private Peer peer;

	public JoinInfoThinEventListener(DhcAddress myDhcAddress, String correlationId, Peer peer) {
		this.myDhcAddress = myDhcAddress;
		this.correlationId = correlationId;
		this.peer = peer;
	}

	@Override
	public void onEvent(Event event) {
		JoinInfoEvent joinInfoEvent = (JoinInfoEvent)event;
		if(!correlationId.equals(joinInfoEvent.getCorrelationId())) {
			logger.info("correlation ids are not equal");
			return;
		}
		JoinInfo joinInfo = joinInfoEvent.getJoinInfo();
		if(!joinInfo.isValid()) {
			logger.info("joinInfo.isValid() returned false");
			return;
		}
		reply(joinInfoEvent);
		Listeners.getInstance().removeEventListener(JoinInfoEvent.class, this);
	}

	private void reply(JoinInfoEvent joinInfoEvent) {
		JoinInfo joinInfo = joinInfoEvent.getJoinInfo();
		joinInfo.setFrom(myDhcAddress);
		Message message  = new GetJoinInfoThinReply(joinInfo);
		message.setCorrelationId(correlationId);
		peer.send(message);
	}

}
