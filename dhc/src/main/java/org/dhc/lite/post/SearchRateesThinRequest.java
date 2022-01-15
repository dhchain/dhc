package org.dhc.lite.post;

import java.util.Set;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Listeners;
import org.dhc.util.Message;

public class SearchRateesThinRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private String account;
	private Set<String> words;

	public SearchRateesThinRequest(String account, Set<String> words) {
		this.account = account;
		this.words = words;
	}

	@Override
	public void process(Peer peer) {
		String driver = null;
		if(!"".equals(account.trim())) {
			driver = account.trim();
		} else if(words != null && !words.isEmpty()) {
			driver = words.iterator().next();
		}
		if(driver == null) {
			return;
		}
		DhcAddress dhcAddress = CryptoUtil.getDhcAddressFromString(driver);
		
		
		logger.trace("SearchRateesThinRequest.process() START");
		Network network = Network.getInstance();
		DhcAddress myAddress = DhcAddress.getMyDhcAddress();
		Message message = new SearchRateesAsyncRequest(myAddress, dhcAddress, account, words);
		message.setCorrelationId(getCorrelationId());
		Listeners.getInstance().addEventListener(SearchRateesEvent.class, new SearchRateesEventThinListener(peer, getCorrelationId()));
		network.sendToAddress(dhcAddress, message);
		logger.trace("SearchRateesThinRequest.process() END");
	}


}
