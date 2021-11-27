package org.dhc.gui.promote;

import java.util.HashSet;
import java.util.Set;

import org.dhc.blockchain.Keywords;
import org.dhc.blockchain.SendTransactionMessage;
import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionMemoryPool;
import org.dhc.blockchain.TransactionOutput;
import org.dhc.network.Network;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Event;
import org.dhc.util.EventListener;
import org.dhc.util.Listeners;

public class JoinTransactionEventListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Coin amount;
	private Transaction transaction;
	private JoinInfo joinInfo;

	
	

	public JoinTransactionEventListener(Coin amount, Transaction transaction, JoinInfo joinInfo) {
		this.amount = amount;
		this.transaction = transaction;
		this.joinInfo = joinInfo;
	}

	@Override
	public void onEvent(Event event) {
		JoinTransactionEvent joinTransactionEvent = (JoinTransactionEvent)event;
		if(!transaction.equals(joinTransactionEvent.getTransaction())) {
			return;
		}
		logger.info("Received join transaction {}", transaction);

		Set<TransactionOutput> set = joinTransactionEvent.getTransaction().getOutputs();
		Set<TransactionOutput> outputs = new HashSet<>();
		for(TransactionOutput output: set) {
			if(output.equals(transaction.getChange())) {
				continue;
			}
			outputs.add(output);
		}
		
		Coin value = joinInfo.getFraction(amount, joinInfo.getAmount());
		int myPosition = joinInfo.getMyPosition();
		sendTransaction(joinInfo.getTo(), removeOutput(value, outputs), joinInfo.getPosition(), joinInfo.getAmount(), myPosition);
		for(JoinInfoItem item: joinInfo.getItems().values()) {
			value = joinInfo.getFraction(amount, item.getAmount());
			sendTransaction(item.getTo(), removeOutput(value, outputs), item.getPosition(), item.getAmount(), myPosition);
		}
		
		
		Listeners.getInstance().removeEventListener(JoinTransactionEvent.class, this);

	}
	
	private TransactionOutput removeOutput(Coin value, Set<TransactionOutput> outputs) {
		logger.info("Will look for output with value {}", value);
		TransactionOutput result = null;
		for(TransactionOutput output: outputs) {
			logger.info("output {}", output);
			if(output.getValue().equals(value)) {
				result = output;
			}
		}
		outputs.remove(result);
		logger.info("result {}", result);
		return result;
	}
	
	private void sendTransaction(DhcAddress address, TransactionOutput output, int position, Coin amount, int myPosition) {
		logger.info("sendTransaction() address={}, output={}, position={}, amount={}, myPosition={}", address, output, position, amount, myPosition);
		if(Coin.ZERO.equals(output.getValue())) {
			return;
		}
		Network network = Network.getInstance();
		Transaction transaction = new Transaction();
		Set<TransactionOutput> outputs = new HashSet<>();
		outputs.add(output);
		Keywords keywords = new Keywords();
		keywords.put("position", Integer.toString(position));
		keywords.put("amount", Long.toString(amount.getValue()));
		keywords.put("myPosition", Integer.toString(myPosition));
		transaction.setKeywords(keywords);
		transaction.create(address, outputs, "JOIN", null);
		if (TransactionMemoryPool.getInstance().add(transaction)) {
			network.sendToAllMyPeers(new SendTransactionMessage(transaction));
		}
	}

	

}
