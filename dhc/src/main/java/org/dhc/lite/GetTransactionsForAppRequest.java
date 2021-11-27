package org.dhc.lite;

import java.util.Set;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Transaction;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.Message;

public class GetTransactionsForAppRequest extends Message {

	private DhcAddress myDhcAddress;
	private String app;

	public GetTransactionsForAppRequest(String app, DhcAddress myDhcAddress) {
		this.setMyDhcAddress(myDhcAddress);
		this.app = app;
	}

	@Override
	public void process(Peer peer) {
		Set<Transaction> transactions = null;
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(myDhcAddress, Blockchain.getInstance().getPower())) {
			transactions = Blockchain.getInstance().getTransactionsForApp(app, myDhcAddress);
		}
		Message message  = new GetTransactionsForAppReply(transactions);
		message.setCorrelationId(getCorrelationId());
		peer.send(message);
	}

	public DhcAddress getMyDhcAddress() {
		return myDhcAddress;
	}

	public void setMyDhcAddress(DhcAddress myDhcAddress) {
		this.myDhcAddress = myDhcAddress;
	}

	public String getApp() {
		return app;
	}

}
