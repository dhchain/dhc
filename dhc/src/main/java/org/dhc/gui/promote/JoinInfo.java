package org.dhc.gui.promote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.dhc.blockchain.Transaction;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class JoinInfo {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private DhcAddress from;
	private DhcAddress to;
	private Coin amount;
	private Map<Integer, JoinInfoItem> items = new HashMap<>();
	
	public boolean isValid() {
		if(items.isEmpty() && !to.equals(new DhcAddress("42pKbvTuPq8XYNQpxZmHe5NVMEiUnv43FTHG"))) {
			logger.info("items are empty and to is not a genesis address");
			return false;
		}
		return true;
	}
	
	public int getPosition() {
		int result = 0;
		for(JoinInfoItem item: items.values()) {
			result = result < item.getPosition()? item.getPosition(): result;
		}
		return result + 1;
	}
	
	public int getMyPosition() {
		return getPosition() + 1;
	}
	
	public DhcAddress getFrom() {
		return from;
	}
	public void setFrom(DhcAddress from) {
		this.from = from;
	}
	public Coin getAmount() {
		return amount;
	}

	public Map<Integer, JoinInfoItem> getItems() {
		return items;
	}
	
	public void load(Set<Transaction> transactions) {
		Coin sum = Coin.ZERO;
		int minimum = Integer.MAX_VALUE;
		for(Transaction transaction: transactions) {
			JoinInfoItem item = new JoinInfoItem(Integer.parseInt(transaction.getKeywords().get("position")), transaction.getReceiver(), new Coin(Long.parseLong(transaction.getKeywords().get("amount")))  );
			sum = sum.add(transaction.getValue());
			items.put(item.getPosition(), item);
			minimum = minimum > item.getPosition()? item.getPosition(): minimum;
		}
		if(items.size() >= 10) {
			items.remove(minimum);
		}
		amount = sum;
	}
	
	private Coin getTotal() {
		Coin sum = amount;
		for(JoinInfoItem item: items.values()) {
			sum = sum.add(item.getAmount());
		}
		return sum;
	}
	
	public Coin getFraction(Coin coin, Coin itemAmount) {
		Coin total = getTotal();
		if(Coin.ZERO.equals(total)) {
			return coin;
		}
		logger.info("coin.getValue()={}, itemAmount.getValue()={}, total.getValue()={}", coin.getValue(), itemAmount.getValue(), total.getValue());
		return new Coin(BigDecimal.valueOf(coin.getValue()).multiply(BigDecimal.valueOf(itemAmount.getValue())).divide(BigDecimal.valueOf(total.getValue()), RoundingMode.DOWN).longValue());
	}
	
	@Override
	public String toString() {
		String str = String.format("JoinInfo from=%s to=%s amount=%s items=%s", getFrom(), getTo(), getAmount(), items);
		return str;
	}

	public DhcAddress getTo() {
		return to;
	}

	public void setTo(DhcAddress to) {
		this.to = to;
	}

}
