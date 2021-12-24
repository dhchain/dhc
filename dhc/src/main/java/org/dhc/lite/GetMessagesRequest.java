package org.dhc.lite;

import java.util.List;

import org.dhc.blockchain.Blockchain;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;

public class GetMessagesRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress dhcAddress;

	public GetMessagesRequest(DhcAddress dhcAddress) {
		this.dhcAddress = dhcAddress;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("GetMessagesRequest.process() START");
		List<SecureMessage> messages = null;
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(dhcAddress, Blockchain.getInstance().getPower())) {
			messages = Blockchain.getInstance().getSecureMessages(dhcAddress);
		}
		Message message  = new GetMessagesReply(messages);
		message.setCorrelationId(getCorrelationId());
		peer.send(message);
		logger.trace("GetMessagesRequest.process() END messages={}", messages.size());

	}

}
