package org.dhc.lite;

import java.util.Set;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.TransactionOutput;
import org.dhc.network.Peer;
import org.dhc.util.DhcAddress;
import org.dhc.util.Message;

public class GetTransactionOutputsRequest extends Message {

	private DhcAddress myDhcAddress;

	public GetTransactionOutputsRequest(DhcAddress myDhcAddress) {
		this.setMyDhcAddress(myDhcAddress);
	}

	@Override
	public void process(Peer peer) {
		Set<TransactionOutput> transactionOutputs = null;
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(myDhcAddress, Blockchain.getInstance().getPower())) {
			transactionOutputs = Blockchain.getInstance().getTransactionOutputs(myDhcAddress);
		}
		Message message  = new GetTransactionOutputsReply(transactionOutputs);
		message.setCorrelationId(getCorrelationId());
		peer.send(message);
	}

	public DhcAddress getMyDhcAddress() {
		return myDhcAddress;
	}

	public void setMyDhcAddress(DhcAddress myDhcAddress) {
		this.myDhcAddress = myDhcAddress;
	}

}
