package org.dhc.lite;

import java.util.Set;

import org.dhc.blockchain.Blockchain;
import org.dhc.gui.promote.JoinLine;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.Message;

public class GetJoinLinesRequest extends Message {

	private DhcAddress myDhcAddress;

	public GetJoinLinesRequest(DhcAddress myDhcAddress) {
		this.setMyDhcAddress(myDhcAddress);
	}

	@Override
	public void process(Peer peer) {
		Set<JoinLine> joinLines = null;
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(myDhcAddress, Blockchain.getInstance().getPower())) {
			joinLines = Blockchain.getInstance().getJoinLines(myDhcAddress);
		}
		Message message  = new GetJoinLinesReply(joinLines);
		message.setCorrelationId(getCorrelationId());
		peer.send(message);
	}

	public DhcAddress getMyDhcAddress() {
		return myDhcAddress;
	}

	public void setMyDhcAddress(DhcAddress myDhcAddress) {
		this.myDhcAddress = myDhcAddress;
	}

}
