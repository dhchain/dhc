package org.dhc.lite;

import java.security.PublicKey;

import org.dhc.blockchain.Blockchain;
import org.dhc.network.ChainSync;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;

public class GetSenderPublicKeyAsyncRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress from;
	private DhcAddress to;

	public GetSenderPublicKeyAsyncRequest(DhcAddress from, DhcAddress to) {
		this.from = from;
		this.to = to;
		
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);
		Network network = Network.getInstance();
		
		Blockchain blockchain = Blockchain.getInstance();
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(to, blockchain.getPower()) && !ChainSync.getInstance().isRunning()) {
			PublicKey publicKey = blockchain.getPublicKey(to);
			GetSenderPublicKeyAsyncReply message  = new GetSenderPublicKeyAsyncReply(from, to, publicKey);
			message.setCorrelationId(getCorrelationId());
			network.sendToAddress(from, message);
			logger.trace("sent to {} message {}", from, message);
			return;
		}
		
		network.sendToAddress(to, this);
	}
	
	@Override
	public String toString() {
		String str = String.format("GetSenderPublicKeyAsyncRequest %s-%s-%s", from, to, getCorrelationId());
		return str;
	}

}
