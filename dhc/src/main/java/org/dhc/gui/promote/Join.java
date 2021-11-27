package org.dhc.gui.promote;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.dhc.PasswordHelper;
import org.dhc.gui.Caller;
import org.dhc.gui.Main;
import org.dhc.gui.util.JTextFieldRegularPopupMenu;
import org.dhc.gui.util.ValidatorDhcAddress;
import org.dhc.util.StringUtil;

import net.miginfocom.swing.MigLayout;

public class Join extends AbstractAction implements Caller {

	private static final long serialVersionUID = 4036313657721664495L;

	private Main main;
	private JPasswordField passwordField;
	
	public Join(Main main) {
		putValue(NAME, "Join Team");
		putValue(SHORT_DESCRIPTION, "Join Team");
		this.main = main;
	}
	
	
	public void log(String message) {
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}
	
	private void enterPassphrase() {
		
		JPanel form = new JPanel(new MigLayout("wrap 2", "[right][fill]"));
		
		JLabel label = new JLabel("Enter Passphrase:");
		form.add(label);
		
		passwordField = new JPasswordField(45);
		form.add(passwordField);
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.setAction(this);
		btnSubmit.setText("Submit");
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
		PasswordHelper passwordHelper = new PasswordHelper(main.getKey());
		if(!passwordHelper.verifyPassphrase(passphrase)) {
			JOptionPane.showMessageDialog(main.getFrame(), "Passphrase entered was not correct. Try again.");
			return;
		}
		passwordField = null;
		showJoinForm();
	}

	public void showJoinForm() {
		
		JPanel form = new JPanel(new BorderLayout());
		
		JPanel labelPanel = new JPanel(new GridLayout(3, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(3, 1));
		form.add(labelPanel, BorderLayout.WEST);
		form.add(fieldPanel, BorderLayout.CENTER);
		
		JLabel label = new JLabel("DHC Address:", JLabel.RIGHT);
		label.setBorder(new EmptyBorder(5,5,5,5));
		labelPanel.add(label);
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField address = new JTextField(30);
		address.getDocument().addDocumentListener(new ValidatorDhcAddress(address));
		JTextFieldRegularPopupMenu.addTo(address);
		address.setToolTipText("DHC Address of a peer to join");
		p.add(address);
		fieldPanel.add(p);
		
		label = new JLabel("Amount in coins:", JLabel.RIGHT);
		label.setBorder(new EmptyBorder(5, 5, 5, 5));
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField amount = new JTextField(30);
		amount.setText("1");
		JTextFieldRegularPopupMenu.addTo(amount);
		amount.setToolTipText("<html>Amount in coins. Defaults to one coin. <br>"
				+ "Higher or lower amount will be proportionate to expected promotion.</html>");
		p.add(amount);
		fieldPanel.add(p);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.setAction(new JoinAction(address, amount, main));
		p.add(btnSubmit);
		fieldPanel.add(p);

		main.getFrame().getRootPane().setDefaultButton(btnSubmit);
		main.setForm(form);
		address.grabFocus();
	}

	

}
