package org.dhc.gui.promote;

public class JoinLine {
	
	private String position;
	private String count;
	private String amount;
	
	public JoinLine(String position, String count, String amount) {
		this.position = position;
		this.count = count;
		this.amount = amount;
	}

	public String getPosition() {
		return position;
	}

	public String getCount() {
		return count;
	}

	public String getAmount() {
		return amount;
	}

}
