package org.dhc.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import net.miginfocom.swing.MigLayout;

public class ChangePassword extends AbstractAction {

	private static final long serialVersionUID = -5588285023715553613L;
	
	private Main main;
	private JPasswordField oldPasswordField;
	private JPasswordField newPasswordField;
	private JPasswordField confirmPasswordField;

	public ChangePassword(Main main) {
		this.main = main;
		putValue(NAME, "Change Password");
		putValue(SHORT_DESCRIPTION, "Change Password");
	}

	public void actionPerformed(ActionEvent actionEvent) {
		
		JPanel form = new JPanel(new MigLayout("wrap 2", "[right][fill]"));
		
		JLabel label = new JLabel("Enter Old Passphrase:");
		form.add(label);
		
		oldPasswordField = new JPasswordField(45);
		form.add(oldPasswordField);
		
		label = new JLabel("Enter New Passphrase:");
		form.add(label);
		
		newPasswordField = new JPasswordField(45);
        form.add(newPasswordField);
		
		label = new JLabel("Reenter New Passphrase:");
		form.add(label);

		confirmPasswordField = new JPasswordField(45);
		form.add(confirmPasswordField);
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.setAction(new ChangePasswordAction(main, oldPasswordField, newPasswordField, confirmPasswordField));
		p.add(btnSubmit);
		form.add(p);

		main.getFrame().getRootPane().setDefaultButton(btnSubmit);
		main.setForm(form);

	}

}
