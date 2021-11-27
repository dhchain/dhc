package org.dhc.gui.transaction;

import java.util.Comparator;

import org.dhc.blockchain.Transaction;

public class TransactionTimestampComparator implements Comparator<Transaction> {

	@Override
	public int compare(Transaction p1, Transaction p2) {
		long difference = p1.getTimeStamp() - p2.getTimeStamp();
		if(difference > 0) {
			return -1;
		} else if(difference == 0) {
			return 0;
		} else {
			return 1;
		}
	}


}
