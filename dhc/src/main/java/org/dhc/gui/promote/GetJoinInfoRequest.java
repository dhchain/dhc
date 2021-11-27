package org.dhc.gui.promote;

import java.util.Set;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Transaction;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;

public class GetJoinInfoRequest extends Message {

	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private DhcAddress from;
	private DhcAddress to;
	private Coin amount;
	
	
	public GetJoinInfoRequest(DhcAddress from, DhcAddress to, Coin amount) {
		this.from = from;
		this.to = to;
		this.amount = amount;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString())) {
			return;
		}
		
		logger.info("process {}", this);
		Network network = Network.getInstance();
		Blockchain blockchain = Blockchain.getInstance();
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(to, blockchain.getPower())) {
			Set<Transaction> transactions =  Blockchain.getInstance().getTransactionsForApp("JOIN", to);
			JoinInfo joinInfo = new JoinInfo();
			joinInfo.setFrom(from);
			joinInfo.setTo(to);
			joinInfo.load(transactions);
			GetJoinInfoReply message = new GetJoinInfoReply(joinInfo, amount);
			message.setCorrelationId(getCorrelationId());
			network.sendToAddress(from, message);
			return;
		}
		
		network.sendToAddress(to, this);
		
	}

	@Override
	public String toString() {
		String str = String.format("GetJoinInfoRequest %s-%s-%s", from, to, getCorrelationId());
		return str;
	}

}
