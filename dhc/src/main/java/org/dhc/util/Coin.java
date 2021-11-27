package org.dhc.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class Coin {

	public static Coin ONE = new Coin(new BigDecimal("1000000000").longValue() );
	public static Coin ZERO = new Coin(0);
	public static Coin SATOSHI = new Coin(1);
	
	private long value;
	
	public Coin() {

	}

	public Coin(long value) {
		this.value = value;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}
	
	public Coin multiply(double amount) {

		return new Coin(   BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(value)).longValue()   );
	}

	public String toString() {
		return Long.toString(value);
	}

	public Coin subtract(Coin amount) {
		return new Coin(value - amount.getValue());
	}

	public Coin add(Coin amount) {
		return new Coin(value + amount.getValue());
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return this.value == ((Coin) o).value;
	}
	
	public boolean less(Coin coin) {
		return value < coin.getValue();
	}
	
	public boolean greaterOrEqual(Coin coin) {
		return value >= coin.getValue();
	}
	
	public String toNumberOfCoins() {
		DecimalFormat df = new DecimalFormat("#,##0.000000000");
		String str = df.format(BigDecimal.valueOf(value).divide(BigDecimal.valueOf(ONE.getValue())));
		return str;
	}

}
