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
import org.dhc.util.StringUtil;

public class SubmitPasswordAction extends AbstractAction {
	
	private static final long serialVersionUID = -5348678702516608164L;
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private JLabel label;
	private Main main;
	
	public SubmitPasswordAction(Main main) {
		putValue(NAME, "Submit");
		putValue(SHORT_DESCRIPTION, "Submit Passphrase");
		this.main = main;
	}

	public void actionPerformed(ActionEvent actionEvent) {
		String passphrase = StringUtil.trim(new String(main.getPasswordField().getPassword()));
		try {
			PasswordHelper passwordHelper = new PasswordHelper(main.getKey());
			if(!passwordHelper.verifyPassphrase(passphrase)) {
				String message = "Passphrase entered was not correct. Try again.";
				JOptionPane.showMessageDialog(main.getFrame(), message);
				return;
			}
			
			JPanel form = new JPanel(new BorderLayout());
			label = new JLabel("Passphrase accepted, starting DHC");
			label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
			label.setBorder(new EmptyBorder(5,5,5,5));
			form.add(label);
			main.setForm(form);
			logger.info("SubmitPasswordAction.actionPerformed() before main.start()");
			main.start();
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
