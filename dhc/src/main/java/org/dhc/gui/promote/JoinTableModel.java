package org.dhc.gui.promote;

import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.dhc.util.Coin;


public class JoinTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1012645969055258357L;
	
	private Set<JoinLine> set;
	
	public JoinTableModel(Set<JoinLine> set) {
		this.set = set;
	}

	public int getRowCount() {
		return set.size();
	}

	public int getColumnCount() {
		return 3;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		JoinLine line = set.toArray(new JoinLine[0])[rowIndex];
        Object value = null;
        switch (columnIndex) {
            case 0:
                value = line.getPosition();
                break;
            case 1:
                value = line.getCount();
                break;
            case 2:
                value = new Coin(Long.parseLong(line.getAmount())).toNumberOfCoins() + " coins";
                break;
        }
        return value;
	}

	public String getColumnName(int columnIndex) {
		String value = null;
		switch (columnIndex) {
		case 0:
			value = "Position";
			break;
		case 1:
			value = "Count";
			break;
		case 2:
			value = "Amount";
			break;
		}
		return value;
	}

	

}
