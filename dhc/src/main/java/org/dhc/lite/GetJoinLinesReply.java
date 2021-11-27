package org.dhc.lite;

import java.util.Set;

import org.dhc.gui.promote.JoinLine;
import org.dhc.network.Peer;
import org.dhc.util.Message;

public class GetJoinLinesReply extends Message {
	
	private Set<JoinLine> joinLines;

	public GetJoinLinesReply(Set<JoinLine> joinLines) {
		this.joinLines = joinLines;
	}

	@Override
	public void process(Peer peer) {
		

	}

	public Set<JoinLine> getJoinLines() {
		return joinLines;
	}

}
