package org.dhc.lite.post;

import java.util.List;

import org.dhc.network.Peer;
import org.dhc.util.Message;

public class SearchPostsThinResponse extends Message {
	
	private List<Ratee> ratees;

	public SearchPostsThinResponse(List<Ratee> ratees) {
		super();
		this.ratees = ratees;
	}

	@Override
	public void process(Peer peer) {


	}

	public List<Ratee> getRatees() {

		return ratees;
	}

}
