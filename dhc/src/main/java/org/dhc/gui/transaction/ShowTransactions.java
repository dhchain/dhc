package org.dhc.gui.transaction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;

import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Transaction;
import org.dhc.gui.Caller;
import org.dhc.gui.Main;
import org.dhc.gui.util.SwingUtil;
import org.dhc.gui.util.TableColumnAdjuster;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ThreadExecutor;

public class ShowTransactions extends AbstractAction implements Caller {

	private static final long serialVersionUID = 4036313657721664495L;
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Main main;
	
	public ShowTransactions(Main main) {
		putValue(NAME, "Show Transactions");
		putValue(SHORT_DESCRIPTION, "Show Transactions");
		this.main = main;
	}
	
	
	public void log(String message) {
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}

	public void actionPerformed(ActionEvent actionEvent) {
		
		final JLabel label = SwingUtil.showWait(main);

		ThreadExecutor.getInstance().execute(new DhcRunnable("Show Transactions") {
			public void doRun() {
				
				Set<Transaction> set = Blockchain.getInstance().getTransactions(DhcAddress.getMyDhcAddress());
				
				if(set == null) {
					label.setText("Failed to retrieve transactions. Please try again");
					return;
				}
				
				String dhcAddress = DhcAddress.getMyDhcAddress().toString();
				logger.debug("found # of transactions: {} for {}", set.size(), dhcAddress);
				
				JPanel form = new JPanel();
				form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
				
				JLabel label = new JLabel("Transactions for " + dhcAddress);
				label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
				label.setBorder(new EmptyBorder(5,5,5,5));
				form.add(label);
				
				if(set.size() != 0) {
					TransactionTableModel model = new TransactionTableModel(set);
					JTable table = new JTable(model);

					table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
					TableColumnAdjuster tca = new TableColumnAdjuster(table);
					tca.adjustColumns();
					table.addMouseListener(new TransactionMouseAdapter(table, new ArrayList<Transaction>(set), main));

					JScrollPane scroll = new JScrollPane (table);
					scroll.setBorder(null);
					form.add(scroll);
				} else {
					label.setText("No transactions found for " + dhcAddress);
				}
				
				
				main.setForm(form);
			}
		});
	}

	

}
