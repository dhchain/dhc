package org.dhc.gui.promote;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class GetJoinInfoReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private JoinInfo joinInfo;
	private Coin amount;

	public GetJoinInfoReply(JoinInfo joinInfo, Coin amount) {
		this.joinInfo = joinInfo;
		this.amount = amount;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);

		if(DhcAddress.getMyDhcAddress().equals(joinInfo.getFrom())) {
			logger.info("GetJoinInfoReply.process() {}", this);
			logger.info("GetJoinInfoReply.process() joinInfo={}", joinInfo);
			Listeners.getInstance().sendEvent(new JoinInfoEvent(joinInfo, amount, getCorrelationId()));
		}
		Network network = Network.getInstance();
		network.sendToAddress(joinInfo.getFrom(), this);
		logger.trace("sent to {} message {}", joinInfo.getFrom(), this);
	}
	
	@Override
	public String toString() {
		String str = String.format("GetJoinInfoReply %s-%s", joinInfo.getFrom(), getCorrelationId());
		return str;
	}

}
