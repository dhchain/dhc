package org.dhc.gui.promote;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
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
import org.dhc.gui.Caller;
import org.dhc.gui.Main;
import org.dhc.gui.util.SwingUtil;
import org.dhc.gui.util.TableColumnAdjuster;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ThreadExecutor;

public class ViewJoin extends AbstractAction implements Caller {

	private static final long serialVersionUID = 4036313657721664495L;
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Main main;
	
	public ViewJoin(Main main) {
		putValue(NAME, "View Team");
		putValue(SHORT_DESCRIPTION, "View Team");
		this.main = main;
	}
	
	
	public void log(String message) {
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}

	public void actionPerformed(ActionEvent actionEvent) {
		
		final JLabel label = SwingUtil.showWait(main);

		ThreadExecutor.getInstance().execute(new DhcRunnable("View Join") {
			public void doRun() {
				
				Set<JoinLine> set = Blockchain.getInstance().getJoinLines(DhcAddress.getMyDhcAddress());
				
				if(set == null) {
					label.setText("Failed to retrieve joins. Please try again");
					return;
				}
				
				String dhcAddress = DhcAddress.getMyDhcAddress().toString();
				logger.debug("found # of join lines: {} for {}", set.size(), dhcAddress);
				
				int totalCount = 0;
				Coin totalAmount = Coin.ZERO;
				for(JoinLine line: set) {
					totalCount = totalCount + Integer.parseInt(line.getCount());
					totalAmount = totalAmount.add(new Coin(Long.parseLong(line.getAmount())));
				}
				JoinLine totalLine = new JoinLine("Total", Integer.toString(totalCount), Long.toString(totalAmount.getValue()));
				set.add(totalLine);
				
				JPanel form = new JPanel();
				form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
				
				JLabel label = new JLabel("Your team");
				label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
				label.setBorder(new EmptyBorder(5,5,5,5));
				form.add(label);
				
				if(set.size() != 0) {
					JoinTableModel model = new JoinTableModel(set);
					JTable table = new JTable(model);

					table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
					TableColumnAdjuster tca = new TableColumnAdjuster(table);
					tca.adjustColumns();

					JScrollPane scroll = new JScrollPane (table);
					scroll.setBorder(null);
					
					JPanel jp = new JPanel(new FlowLayout());
					jp.add(scroll);
					//jp.setAlignmentX(JLabel.LEFT_ALIGNMENT);
					
					form.add(jp);
				} else {
					label.setText("No joins found for " + dhcAddress);
				}
				
				
				main.setForm(form);
			}
		});
	}
	

}
