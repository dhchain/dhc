package org.dhc.gui.transaction;

import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.dhc.blockchain.TransactionOutput;

public class TransactionOutputTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1012645969055258357L;
	
	private Set<TransactionOutput> set;
	
	public TransactionOutputTableModel(Set<TransactionOutput> set) {
		this.set = set;
	}

	public int getRowCount() {
		return set.size();
	}

	public int getColumnCount() {
		return 5;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		TransactionOutput output = set.toArray(new TransactionOutput[0])[rowIndex];
        Object value = null;
        switch (columnIndex) {
            case 0:
                value = output.getOutputId();
                break;
            case 1:
                value = output.getValue().toNumberOfCoins();
                break;
            case 2:
                value = output.getOutputTransactionId();
                break;
            case 3:
                value = output.getOutputBlockIndex();
                break;
            case 4:
                value = output.getOutputBlockHash();
                break;
        }
        return value;
	}

	public String getColumnName(int columnIndex) {
		String value = null;
		switch (columnIndex) {
		case 0:
			value = "Output Id";
			break;
		case 1:
			value = "Value";
			break;
		case 2:
			value = "Output Transaction Id";
			break;
		case 3:
			value = "Output Block Index";
			break;
		case 4:
			value = "Output BlockHash";
			break;
		}
		return value;
	}

	

}
