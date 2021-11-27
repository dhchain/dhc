package org.dhc.util;

import java.util.HashMap;
import java.util.Map;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.MyAddresses;
import org.dhc.network.Network;

public class TotalBalance {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Map<String, Coin> balances = new HashMap<>();
	private MyAddresses myAddresses = new MyAddresses();
	private String blockhash;
	private String correlationId;
	
	public TotalBalance() {
		Listeners.getInstance().addEventListener(GetBalanceEvent.class, new GetTotalBalanceEventListener());
	}

	public void process() {
		balances = new HashMap<>();
		Network network = Network.getInstance();
		Blockchain blockchain = Blockchain.getInstance();
		Block block = blockchain.getLastBlocks().get(0);
		blockhash = block.getPreviousHash();
		DhcAddress replyTo = DhcAddress.getMyDhcAddress();
		correlationId = CryptoUtil.getRandomString(16);
		
		for(String address: myAddresses.getList()) {
			DhcAddress dhcAddress = new DhcAddress(address);
			Message message = new GetBalanceAsyncRequest(dhcAddress, replyTo, blockhash);
			message.setCorrelationId(correlationId);
			network.sendToAddress(dhcAddress, message);
		}
		
	}
	
	public synchronized void updateBalance(String address, Coin balance, String blockhash, String correlationId) {
		logger.trace("TotalBalance.updateBalance() address={}, balance={}, blockhash={}, correlationId={}", address, balance, blockhash);
		
		if(!blockhash.equals(this.blockhash)) {
			return;
		}
		if(!correlationId.equals(this.correlationId)) {
			return;
		}
		balances.put(address, balance);
		if(balances.size() != myAddresses.getList().size()) {
			return;
		}
		
		Coin sum = Coin.ZERO;
		for(Coin bal : balances.values()) {
			sum = sum.add(bal);
		}
		logger.info("Total balance: {}", sum.toNumberOfCoins());
	}

}
