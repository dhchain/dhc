package org.dhc.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.dhc.PasswordHelper;
import org.dhc.util.DhcLogger;

public class CreateKeyAction extends AbstractAction implements Caller {
	
	private static final long serialVersionUID = -5348678702516608164L;
	private static final DhcLogger logger = DhcLogger.getLogger();

	private JLabel label;
	private Main main;
	
	public CreateKeyAction(Main main) {
		putValue(NAME, "Submit");
		putValue(SHORT_DESCRIPTION, "Submit Passphrase");
		this.main = main;
	}

	public void actionPerformed(ActionEvent actionEvent) {
		String passphrase = new String(main.getPasswordField().getPassword());
		String confirmPassword = new String(main.getConfirmPasswordField().getPassword());
		if(!passphrase.equals(confirmPassword)) {
			log("Passphrase and reentered passphrase do not match");
			return;
		}
		try {
			PasswordHelper passwordHelper = new PasswordHelper(main.getKey());
			passwordHelper.generateKey(passphrase);

			JPanel form = new JPanel(new BorderLayout());
			label = new JLabel("New key generated, starting network");
			label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
			label.setBorder(new EmptyBorder(5,5,5,5));
			form.add(label);
			main.setForm(form);
			
			main.start();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void log(String message) {
		if(label != null) {
			label.setText(message);
			new MenuCreator(main).addMenu();
			return;
		}
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}

}
