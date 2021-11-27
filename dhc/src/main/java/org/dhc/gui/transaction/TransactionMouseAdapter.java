package org.dhc.gui.transaction;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.dhc.blockchain.Transaction;
import org.dhc.gui.Main;
import org.dhc.util.DhcLogger;

public class TransactionMouseAdapter extends MouseAdapter {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private JTable table;
	private List<Transaction> list;
	private Main main;
	
	public void mouseClicked(MouseEvent evt) {
        int row = table.rowAtPoint(evt.getPoint());
        int col = table.columnAtPoint(evt.getPoint());
        if (row >= 0 && col >= 0) {
            doit(list.get(row));
        }
    }
	
	private JPanel showBackButton() {
		JFrame frame = main.getFrame();
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JButton btnSubmit = new JButton();
		btnSubmit.setAction(new ShowTransactions(main));
		btnSubmit.setText("Back");
		
		frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "backButton");

		frame.getRootPane().getActionMap().put("backButton", new AbstractAction() {
			private static final long serialVersionUID = 4946947535624344910L;

			public void actionPerformed(ActionEvent actionEvent) {
				btnSubmit.doClick();
				frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
				frame.getRootPane().getActionMap().clear();
			}
		});
		
		p.add(btnSubmit);
		return p;
	}

	private void doit(Transaction transaction) {
		
		logger.info("transaction {}", transaction);
		
		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		
		JPanel p = showBackButton();
		
		form.add(p);
		
		JPanel subform = new JPanel(new BorderLayout());
		form.add(subform);
		
		
		JPanel labelPanel = new JPanel(new GridLayout(10, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(10, 1));
		subform.add(labelPanel, BorderLayout.WEST);
		subform.add(fieldPanel, BorderLayout.CENTER);
		
		JLabel label = new JLabel("Transaction ID:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField identifier = new JTextField(36);
		identifier.setText(transaction.getTransactionId());
		identifier.setBorder( null );
		identifier.setOpaque( false );
		identifier.setEditable( false );
		p.add(identifier);
		fieldPanel.add(p);
		
		label = new JLabel("Blockhash:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField blockhash = new JTextField(36);
		blockhash.setText(transaction.getBlockHash());
		blockhash.setBorder( null );
		blockhash.setOpaque( false );
		blockhash.setEditable( false );
		p.add(blockhash);
		fieldPanel.add(p);
		
		label = new JLabel("Block Index:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField blockIndex = new JTextField(36);
		blockIndex.setText(Long.toString(transaction.getBlockIndex()));
		blockIndex.setBorder( null );
		blockIndex.setOpaque( false );
		blockIndex.setEditable( false );
		p.add(blockIndex);
		fieldPanel.add(p);
		
		label = new JLabel("Time Stamp:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField timeStamp = new JTextField(36);
		timeStamp.setText(new Date(transaction.getTimeStamp()).toString());
		timeStamp.setBorder( null );
		timeStamp.setOpaque( false );
		timeStamp.setEditable( false );
		p.add(timeStamp);
		fieldPanel.add(p);
		
		label = new JLabel("Sender:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField address = new JTextField(36);
		address.setText(transaction.getSenderDhcAddress().toString());
		address.setBorder( null );
		address.setOpaque( false );
		address.setEditable( false );
		p.add(address);
		fieldPanel.add(p);
		
		label = new JLabel("Recipient:", JLabel.RIGHT);
		labelPanel.add(label);
		
		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField recipient = new JTextField(36);
		recipient.setText(transaction.getReceiver().toString());
		recipient.setBorder( null );
		recipient.setOpaque( false );
		recipient.setEditable( false );
		p.add(recipient);
		fieldPanel.add(p);
		
		
		label = new JLabel("Value:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField amount = new JTextField(36);
		amount.setText(transaction.getValue().toNumberOfCoins());
		amount.setBorder( null );
		amount.setOpaque( false );
		amount.setEditable( false );
		p.add(amount);
		fieldPanel.add(p);
		
		label = new JLabel("Fee:", JLabel.RIGHT);
		labelPanel.add(label);
				
		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField fee = new JTextField(36);
		fee.setText(transaction.getFee().toNumberOfCoins());
		fee.setBorder( null );
		fee.setOpaque( false );
		fee.setEditable( false );
		p.add(fee);
		fieldPanel.add(p);
		
		label = new JLabel("Application:", JLabel.RIGHT);
		labelPanel.add(label);
				
		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField app = new JTextField(36);
		app.setText(transaction.getApp());
		app.setBorder( null );
		app.setOpaque( false );
		app.setEditable( false );
		p.add(app);
		fieldPanel.add(p);
		
		String data = transaction.getExpiringData() == null ? "": transaction.getExpiringData().getData();
		
		if(!"".equals(data)) {
		
			label = new JLabel("Data:", JLabel.RIGHT);
			labelPanel.add(label);
			
			p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
			form.add(p);
	
			JTextArea expiringData = new JTextArea();
			expiringData.setLineWrap(true);
			expiringData.setWrapStyleWord(true);
			expiringData.setOpaque( false );
			expiringData.setEditable( false );
			
			
			JScrollPane scroll = new JScrollPane (expiringData);
			scroll.setBorder(null);
			p.add(scroll);
			
			
			expiringData.setText(data);
		}
		
		main.setForm(form);
	}

	public TransactionMouseAdapter(JTable table, List<Transaction> list, Main main) {
		this.table = table;
		this.list = list;
		this.main = main;
	}

}
