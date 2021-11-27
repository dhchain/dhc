package org.dhc.gui.promote;

import org.dhc.util.Coin;
import org.dhc.util.Event;

public class JoinInfoEvent implements Event {
	
	private JoinInfo joinInfo;
	private Coin amount;
	private String correlationId;
	
	public JoinInfoEvent(JoinInfo joinInfo, Coin amount, String correlationId) {
		this.joinInfo = joinInfo;
		this.amount = amount;
		this.correlationId = correlationId;
	}

	public JoinInfo getJoinInfo() {
		return joinInfo;
	}

	public Coin getAmount() {
		return amount;
	}

	public String getCorrelationId() {
		return correlationId;
	}


}
