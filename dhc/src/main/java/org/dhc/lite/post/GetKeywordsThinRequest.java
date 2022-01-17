package org.dhc.lite.post;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class GetKeywordsThinRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private String rateeName;
	private String transactionId;

	public GetKeywordsThinRequest(String rateeName, String transactionId) {
		this.rateeName = rateeName;
		this.transactionId = transactionId;
	}

	@Override
	public void process(Peer peer) {
		logger.info("dhcAddress={}, transactionId={}", rateeName, transactionId);
		Network network = Network.getInstance();
		DhcAddress myDhcAddress = DhcAddress.getMyDhcAddress();
		String correlationId = getCorrelationId();
		Message message = new GetKeywordsAsyncRequest(myDhcAddress, rateeName, transactionId);
		message.setCorrelationId(correlationId);
		Listeners.getInstance().addEventListener(GetKeywordsEvent.class, new GetKeywordsListener(peer, correlationId));
		network.sendToAddress(CryptoUtil.getDhcAddressFromString(rateeName), message);
	}

}
