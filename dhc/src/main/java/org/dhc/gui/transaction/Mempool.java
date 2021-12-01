package org.dhc.gui.transaction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;

import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionMemoryPool;
import org.dhc.gui.Caller;
import org.dhc.gui.Main;
import org.dhc.gui.util.SwingUtil;
import org.dhc.gui.util.TableColumnAdjuster;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ThreadExecutor;

public class Mempool extends AbstractAction implements Caller {

	private static final long serialVersionUID = 4036313657721664495L;
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Main main;
	
	public Mempool(Main main) {
		putValue(NAME, "Mempool");
		putValue(SHORT_DESCRIPTION, "Mempool");
		this.main = main;
	}
	
	
	public void log(String message) {
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}

	public void actionPerformed(ActionEvent actionEvent) {
		
		SwingUtil.showWait(main);

		ThreadExecutor.getInstance().execute(new DhcRunnable("Mempool") {
			public void doRun() {
				
				Set<Transaction> set = TransactionMemoryPool.getInstance().getCopyOfTransactions();
				List<Transaction> list = new ArrayList<Transaction>(set);
				Collections.sort(list, new TransactionTimestampComparator());
				set = new LinkedHashSet<Transaction>(list);

				logger.info("found # of transactions: {} in memory pool", set.size());
				
				JPanel form = new JPanel();
				form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
				
				JLabel label = new JLabel("Transactions in memory pool");
				label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
				label.setBorder(new EmptyBorder(5,5,5,5));
				form.add(label);
				
				if(set.size() != 0) {
					TransactionTableModel model = new TransactionTableModel(set);
					JTable table = new JTable(model);

					table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
					TableColumnAdjuster tca = new TableColumnAdjuster(table);
					tca.adjustColumns();
					table.addMouseListener(new MempoolMouseAdapter(table, list, main));

					JScrollPane scroll = new JScrollPane (table);
					scroll.setBorder(null);
					form.add(scroll);
				} else {
					label.setText("No transactions found in memory pool");
				}
				
				
				main.setForm(form);
			}
		});
	}

	

}
