package org.dhc.lite;

import org.dhc.blockchain.Blockchain;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcRunnable;
import org.dhc.util.GetBalanceAsyncRequest;
import org.dhc.util.GetBalanceEvent;
import org.dhc.util.Listeners;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;

public class GetBalanceRequest extends Message {

	private DhcAddress address;

	public GetBalanceRequest(DhcAddress address) {
		this.address = address;
	}

	@Override
	public void process(Peer peer) {
		ThreadExecutor.getInstance().execute(new DhcRunnable("GetBalanceRequest") {
			public void doRun() {
				getBalance(peer, getCorrelationId());
			}
		});
	}
	
	private void getBalance(Peer peer, String correlationId) {
		Network network = Network.getInstance();
		Blockchain blockchain = Blockchain.getInstance();
		String blockhash = blockchain.getLastBlockHashes().get(0);
		DhcAddress myAddress = DhcAddress.getMyDhcAddress();
		DhcAddress toAddress = address;
		Message message = new GetBalanceAsyncRequest(toAddress, myAddress, blockhash);
		message.setCorrelationId(correlationId);
		Listeners.getInstance().addEventListener(GetBalanceEvent.class, new GetBalanceEventThinListener(peer, correlationId));
		network.sendToAddress(toAddress, message);
	}

}
