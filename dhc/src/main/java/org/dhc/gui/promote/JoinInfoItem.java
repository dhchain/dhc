package org.dhc.gui.promote;

import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;

public class JoinInfoItem {
	
	private int position;
	private DhcAddress to;
	private Coin amount;
	
	public JoinInfoItem(int position, DhcAddress to, Coin amount) {
		this.position = position;
		this.to = to;
		this.amount = amount;
	}

	public int getPosition() {
		return position;
	}

	public DhcAddress getTo() {
		return to;
	}

	public Coin getAmount() {
		return amount;
	}
	
	@Override
	public String toString() {
		String str = String.format("JoinInfoItem position=%s to=%s value=%s", getPosition(), getTo(), getAmount());
		return str;
	}

}
