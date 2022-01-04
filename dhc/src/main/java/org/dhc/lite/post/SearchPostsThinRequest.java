package org.dhc.lite.post;

import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.Message;

public class SearchPostsThinRequest extends Message {

	private DhcAddress dhcAddress;

	public SearchPostsThinRequest(DhcAddress dhcAddress) {
		this.dhcAddress = dhcAddress;
	}

	@Override
	public void process(Peer peer) {

	}

	public DhcAddress getDhcAddress() {
		return dhcAddress;
	}


}
