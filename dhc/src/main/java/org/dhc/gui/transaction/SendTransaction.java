package org.dhc.gui.transaction;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.dhc.PasswordHelper;
import org.dhc.gui.Caller;
import org.dhc.gui.Main;
import org.dhc.gui.util.JTextFieldRegularPopupMenu;
import org.dhc.gui.util.ValidatorDhcAddress;
import org.dhc.gui.util.ValidatorDouble;
import org.dhc.gui.util.ValidatorLong;
import org.dhc.util.StringUtil;

import net.miginfocom.swing.MigLayout;

public class SendTransaction extends AbstractAction implements Caller {

	private static final long serialVersionUID = 4036313657721664495L;
	
	private Main main;
	private JPasswordField passwordField;
	
	public SendTransaction(Main main) {
		putValue(NAME, "Send Transaction");
		putValue(SHORT_DESCRIPTION, "Send Transaction");
		this.main = main;
	}
	
	
	public void log(String message) {
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}
	
	private void enterPassphrase() {
		
		JPanel form = new JPanel(new MigLayout("wrap 2", "[right][fill]"));
		
		JLabel label = new JLabel("Enter Passphrase:");
		form.add(label);
		
		JPasswordField passwordFieldLocal = new JPasswordField(45);
		form.add(passwordFieldLocal);
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.setAction(this);
		btnSubmit.setText("Submit");
		
		btnSubmit.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		    	passwordField = passwordFieldLocal;
		    }
		});
		
		p.add(btnSubmit);
		form.add(p);

		main.getFrame().getRootPane().setDefaultButton(btnSubmit);
		main.setForm(form);
	}

	public void actionPerformed(ActionEvent actionEvent) {
		if(passwordField == null) {
			enterPassphrase();
			return;
		}
		String passphrase = StringUtil.trim(new String(passwordField.getPassword()));
		passwordField = null;
		PasswordHelper passwordHelper = new PasswordHelper(main.getKey());
		if(!passwordHelper.verifyPassphrase(passphrase)) {
			JOptionPane.showMessageDialog(main.getFrame(), "Passphrase entered was not correct. Try again.");
			return;
		}
		
		showSendTransactionForm();
	}
	
	private void showSendTransactionForm() {
		JPanel mainForm = new JPanel(new BorderLayout());
		mainForm.setPreferredSize(new Dimension(main.getFrame().getWidth(), main.getStatusBar().getY() - main.getForm().getY()));
		
		JPanel form = new JPanel(new BorderLayout());
		mainForm.add(form, BorderLayout.NORTH);
		
		JPanel labelPanel = new JPanel(new GridLayout(5, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(5, 1));
		form.add(labelPanel, BorderLayout.WEST);
		form.add(fieldPanel, BorderLayout.CENTER);
		
		JLabel label = new JLabel("Recipient DHC Address:", JLabel.RIGHT);
		label.setBorder(new EmptyBorder(0,5,0,0));
		labelPanel.add(label);
		
		label = new JLabel("Amount in coins:", JLabel.RIGHT);
		labelPanel.add(label);
		
		label = new JLabel("Fee in satoshis:", JLabel.RIGHT);
		labelPanel.add(label);
		
		label = new JLabel("Expire after # blocks:", JLabel.RIGHT);
		labelPanel.add(label);
		
		label = new JLabel("Expiring Data:", JLabel.RIGHT);
		labelPanel.add(label);
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField address = new JTextField(36);
		address.getDocument().addDocumentListener(new ValidatorDhcAddress(address));
		JTextFieldRegularPopupMenu.addTo(address);
		p.add(address);
		fieldPanel.add(p);
		
		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField amount = new JTextField(36);
		amount.getDocument().addDocumentListener(new ValidatorDouble(amount));
		JTextFieldRegularPopupMenu.addTo(amount);
		p.add(amount);
		fieldPanel.add(p);
		
		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField fee = new JTextField(36);
		fee.setText("1");
		fee.getDocument().addDocumentListener(new ValidatorLong(fee));
		JTextFieldRegularPopupMenu.addTo(fee);
		p.add(fee);
		fieldPanel.add(p);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField expire = new JTextField(36);
		expire.getDocument().addDocumentListener(new ValidatorLong(expire));
		JTextFieldRegularPopupMenu.addTo(expire);
		p.add(expire);
		fieldPanel.add(p);
		
		p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		
		JTextArea expiringData = new JTextArea();
		expiringData.setToolTipText("Limited to 32672 chars");
		JTextFieldRegularPopupMenu.addTo(expiringData);
		JScrollPane scroll = new JScrollPane (expiringData);
		p.add(scroll);
		
		p.add(Box.createRigidArea(new Dimension(0, 10)));

		JButton btnSubmit = new JButton("Submit");
		btnSubmit.setAction(new SendTransactionAction(main, address, amount, fee, expire, expiringData));
		p.add(btnSubmit);
		
		p.add(Box.createRigidArea(new Dimension(0, 10)));
		
		mainForm.add(p);

		main.getFrame().getRootPane().setDefaultButton(btnSubmit);
		main.setForm(mainForm);
		
		address.grabFocus();
	}

}
