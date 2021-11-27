package org.dhc.gui.transaction;

import java.util.Date;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.dhc.blockchain.Transaction;

public class TransactionTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1012645969055258357L;
	
	private Set<Transaction> set;
	
	public TransactionTableModel(Set<Transaction> set) {
		this.set = set;
	}

	public int getRowCount() {
		return set.size();
	}

	public int getColumnCount() {
		return 8;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		Transaction transaction = set.toArray(new Transaction[0])[rowIndex];
        Object value = null;
        switch (columnIndex) {
            case 0:
                value = transaction.getTransactionId();
                break;
            case 1:
                value = transaction.getSenderDhcAddress();
                break;
            case 2:
                value = transaction.getReceiver();
                break;
            case 3:
                value = transaction.getValue().toNumberOfCoins();
                break;
            case 4:
                value = transaction.getFee().toNumberOfCoins();
                break;
            case 5:
                value = transaction.getBlockHash();
                break;
            case 6:
                value = transaction.getBlockIndex();
                break;
            case 7:
                value = new Date(transaction.getTimeStamp()).toString();
                break;
        }
        return value;
	}

	public String getColumnName(int columnIndex) {
		String value = null;
        switch (columnIndex) {
        case 0:
            value = "Transaction Id";
            break;
        case 1:
            value = "Sender";
            break;
        case 2:
            value = "Recipient";
            break;
        case 3:
            value = "Value";
            break;
        case 4:
            value = "Fee";
            break;
        case 5:
            value = "Block Hash";
            break;
        case 6:
            value = "Block Index";
            break;
        case 7:
            value = "Time Stamp";
            break;
    }
    return value;
	}

	

}
