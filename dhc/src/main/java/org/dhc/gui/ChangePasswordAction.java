package org.dhc.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.EmptyBorder;

import org.dhc.PasswordHelper;
import org.dhc.util.DhcLogger;
import org.dhc.util.StringUtil;
import org.dhc.util.Wallet;

public class ChangePasswordAction extends AbstractAction implements Caller {
	
	private static final long serialVersionUID = -5348678702516608164L;
	private static final DhcLogger logger = DhcLogger.getLogger();

	private JPasswordField oldPasswordField;
	private JPasswordField newPasswordField;
	private JPasswordField confirmPasswordField;
	private Main main;
	
	public ChangePasswordAction(Main main, JPasswordField oldPasswordField, JPasswordField newPasswordField, JPasswordField confirmPasswordField) {
		putValue(NAME, "Submit");
		putValue(SHORT_DESCRIPTION, "Submit Passphrase");
		this.main = main;
		this.oldPasswordField = oldPasswordField;
		this.newPasswordField = newPasswordField;
		this.confirmPasswordField = confirmPasswordField;
	}
	
	public void actionPerformed(ActionEvent actionEvent) {
		String oldPassword = StringUtil.trim(new String(oldPasswordField.getPassword()));
		String newPassword = StringUtil.trim(new String(newPasswordField.getPassword()));
		String confirmPassword = StringUtil.trim(new String(confirmPasswordField.getPassword()));
		if(!newPassword.equals(confirmPassword)) {
			log("New and reentered new passwords do not match");
			return;
		}
		
		try {
			PasswordHelper passwordHelper = new PasswordHelper(main.getKey());
			if(!passwordHelper.verifyPassphrase(oldPassword)) {
				log("Passphrase entered was not correct. Try again.");
				return;
			}
			passwordHelper.saveKey(Wallet.getInstance(), newPassword);

			JPanel form = new JPanel(new BorderLayout());
			JLabel label = new JLabel("Passphrase Changed");
			label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
			label.setBorder(new EmptyBorder(5,5,5,5));
			form.add(label);
			main.setForm(form);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void log(String message) {
		JFrame frame = main.getFrame();
		JOptionPane.showMessageDialog(frame, message);
	}
}
