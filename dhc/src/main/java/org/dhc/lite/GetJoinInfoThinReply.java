package org.dhc.lite;

import org.dhc.gui.promote.JoinInfo;
import org.dhc.network.Peer;
import org.dhc.util.Message;

public class GetJoinInfoThinReply extends Message {
	
	private JoinInfo joinInfo;

	public GetJoinInfoThinReply(JoinInfo joinInfo) {
		this.joinInfo = joinInfo;
	}

	@Override
	public void process(Peer peer) {
		

	}

	public JoinInfo getJoinInfo() {
		return joinInfo;
	}


}
