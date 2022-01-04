package org.dhc.lite.post;

import java.util.Set;

import org.dhc.network.Peer;
import org.dhc.util.Message;

public class SearchRateesThinRequest extends Message {

	private String post;
	private Set<String> words;

	public SearchRateesThinRequest(String post, Set<String> words) {
		this.post = post;
		this.words = words;

	}

	@Override
	public void process(Peer peer) {


	}

	public String getPost() {
		return post;
	}

	public Set<String> getWords() {
		return words;
	}

}
