package org.dhc.lite;

import java.security.PublicKey;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class GetSenderPublicKeyAsyncReply extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress from;
	private DhcAddress to;
	private PublicKey publicKey;

	public GetSenderPublicKeyAsyncReply(DhcAddress from, DhcAddress to, PublicKey publicKey) {
		this.from = from;
		this.to = to;
		this.publicKey = publicKey;
		
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.info("process {}", this);

		if(DhcAddress.getMyDhcAddress().equals(from)) {
			GetSenderPublicKeyEvent event = new GetSenderPublicKeyEvent(publicKey, getCorrelationId());
			Listeners.getInstance().sendEvent(event);
		}
		Network network = Network.getInstance();
		network.sendToAddress(from, this);
		logger.info("sent to {} message {}", from, this);
		
	}
	
	@Override
	public String toString() {
		String str = String.format("GetSenderPublicKeyAsyncReply %s-%s-%s", from, to, getCorrelationId());
		return str;
	}

}
