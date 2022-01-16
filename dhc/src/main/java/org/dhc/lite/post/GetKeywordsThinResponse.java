package org.dhc.lite.post;

import org.dhc.blockchain.Keywords;
import org.dhc.network.Peer;
import org.dhc.util.Message;

public class GetKeywordsThinResponse extends Message {
	
	private Keywords keywords;

	public GetKeywordsThinResponse(Keywords keywords) {
		super();
		this.keywords = keywords;
	}

	@Override
	public void process(Peer peer) {
		

	}

	public Keywords getKeywords() {
		return keywords;
	}

}
