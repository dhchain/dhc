package org.dhc.gui.promote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.SendTransactionMessage;
import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionMemoryPool;
import org.dhc.network.Network;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Event;
import org.dhc.util.EventListener;
import org.dhc.util.Listeners;

public class JoinInfoEventListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private String correlationId;

	public JoinInfoEventListener(String correlationId) {
		this.correlationId = correlationId;
	}

	@Override
	public void onEvent(Event event) {
		JoinInfoEvent joinInfoEvent = (JoinInfoEvent)event;
		if(!correlationId.equals(joinInfoEvent.getCorrelationId())) {
			logger.info("correlation ids are not equal");
			return;
		}
		Listeners.getInstance().removeEventListener(JoinInfoEvent.class, this);
		JoinInfo joinInfo = joinInfoEvent.getJoinInfo();
		if(!joinInfo.isValid()) {
			logger.info("joinInfo.isValid() returned false");
			return;
		}
		split(joinInfoEvent);
	}
	
	private void split(JoinInfoEvent joinInfoEvent) {
		JoinInfo joinInfo = joinInfoEvent.getJoinInfo();
		Coin amount = joinInfoEvent.getAmount();
		try {
			Blockchain blockchain = Blockchain.getInstance();
			Block block = blockchain.getLastBlocks().get(0);
			Map<Integer, JoinInfoItem> items = joinInfo.getItems();
			List<Coin> list = new ArrayList<>();
			list.add(joinInfo.getFraction(amount, joinInfo.getAmount()));
			for(JoinInfoItem item: items.values()) {
				list.add(joinInfo.getFraction(amount, item.getAmount()));
			}
			Transaction transaction = new Transaction();
			transaction.createSplitOutputsTransaction(DhcAddress.getMyDhcAddress(), Coin.ZERO, block, list.toArray(new Coin[0]));
			if (TransactionMemoryPool.getInstance().add(transaction)) {
				Listeners.getInstance().addEventListener(JoinTransactionEvent.class, new JoinTransactionEventListener(amount, transaction, joinInfo));
				Network.getInstance().sendToAllMyPeers(new SendTransactionMessage(transaction));
				return;
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
