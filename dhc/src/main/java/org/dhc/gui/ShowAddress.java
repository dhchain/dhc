package org.dhc.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.gui.util.JTextFieldRegularPopupMenu;
import org.dhc.gui.util.SwingUtil;
import org.dhc.network.Network;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ThreadExecutor;

public class ShowAddress extends AbstractAction implements Caller {

	private static final long serialVersionUID = 4036313657721664495L;

	private Main main;
	
	public ShowAddress(Main main) {
		putValue(NAME, "Show Address");
		putValue(SHORT_DESCRIPTION, "Show Address");
		this.main = main;
	}
	
	
	public void log(String message) {
		JFrame frame = main.getFrame();
		JOptionPane.showMessageDialog(frame, message);
	}
	
	private String getBalance() {

		Blockchain blockchain = Blockchain.getInstance();
		Block block = blockchain.getLastBlocks().get(0);
		Coin balance = blockchain.sumByRecipient(DhcAddress.getMyDhcAddress().toString(), block);
		return balance.toNumberOfCoins();
	}
	
	private void showBalance(String balance) {

		JPanel form = new JPanel(new BorderLayout());

		JPanel labelPanel = new JPanel(new GridLayout(2, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(2, 1));
		form.add(labelPanel, BorderLayout.WEST);
		form.add(fieldPanel, BorderLayout.CENTER);

		JLabel label = new JLabel(
				"Your DHC Address on shard " + Network.getInstance().getBucketKey() + ":",
				JLabel.RIGHT);
		label.setBorder(new EmptyBorder(5, 5, 5, 5));
		labelPanel.add(label);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField textField = new JTextField(30);
		textField.setText(DhcAddress.getMyDhcAddress().toString());
		JTextFieldRegularPopupMenu.addTo(textField);
		p.add(textField);
		fieldPanel.add(p);

		

		label = new JLabel("Balance:", JLabel.RIGHT);
		label.setBorder(new EmptyBorder(5, 5, 5, 5));
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		textField = new JTextField(30);
		textField.setText(balance + " coins");
		JTextFieldRegularPopupMenu.addTo(textField);
		p.add(textField);
		fieldPanel.add(p);

		main.setForm(form);
		
	}

	public void actionPerformed(ActionEvent actionEvent) {
		SwingUtil.showWait(main);
		
		ThreadExecutor.getInstance().execute(new DhcRunnable("ShowAddress") {
			public void doRun() {
				
				String balance = getBalance();
				showBalance(balance);

			}
		});
	}

	

}
