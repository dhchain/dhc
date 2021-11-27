package org.dhc.util;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.network.Network;
import org.dhc.network.Peer;

public class GetBalanceAsyncRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress address;
	private DhcAddress replyTo;
	private String blockhash;

	public GetBalanceAsyncRequest(DhcAddress address, DhcAddress replyTo, String blockhash) {
		this.address = address;
		this.replyTo = replyTo;
		this.blockhash = blockhash;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.trace("process {}", this);
		Network network = Network.getInstance();
		
		Blockchain blockchain = Blockchain.getInstance();
		Block block = blockchain.getByHash(blockhash);
		if(block != null && DhcAddress.getMyDhcAddress().isFromTheSameShard(address, blockchain.getPower())) {
			Coin balance = blockchain.sumByRecipient(address.toString(), block);
			GetBalanceAsyncReply message  = new GetBalanceAsyncReply(address, balance, replyTo, blockhash);
			message.setCorrelationId(getCorrelationId());
			network.sendToAddress(replyTo, message);
			logger.trace("sent to {} message {}", replyTo, message);
			return;
		}
		
		network.sendToAddress(address, this);
	}

	@Override
	public String toString() {
		String str = String.format("GetBalanceAsyncRequest %s-%s-%s-%s", address, replyTo, blockhash, getCorrelationId());
		return str;
	}
}
