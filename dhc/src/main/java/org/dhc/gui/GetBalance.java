package org.dhc.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.dhc.gui.util.JTextFieldRegularPopupMenu;
import org.dhc.gui.util.ValidatorDhcAddress;

public class GetBalance extends AbstractAction implements Caller {

	private static final long serialVersionUID = 4036313657721664495L;

	private Main main;
	
	public GetBalance(Main main) {
		putValue(NAME, "Get Balance");
		putValue(SHORT_DESCRIPTION, "Get Balance");
		this.main = main;
	}
	
	
	public void log(String message) {
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}

	public void actionPerformed(ActionEvent actionEvent) {
		
		JPanel form = new JPanel(new BorderLayout());
		
		JPanel labelPanel = new JPanel(new GridLayout(2, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(2, 1));
		form.add(labelPanel, BorderLayout.WEST);
		form.add(fieldPanel, BorderLayout.CENTER);
		
		JLabel label = new JLabel("DHC Address:", JLabel.RIGHT);
		label.setBorder(new EmptyBorder(5,5,5,5));
		labelPanel.add(label);
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField address = new JTextField(30);
		address.getDocument().addDocumentListener(new ValidatorDhcAddress(address));
		JTextFieldRegularPopupMenu.addTo(address);
		p.add(address);
		fieldPanel.add(p);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.setAction(new GetBalanceAction(address, main));
		p.add(btnSubmit);
		fieldPanel.add(p);

		main.getFrame().getRootPane().setDefaultButton(btnSubmit);
		main.setForm(form);
		address.grabFocus();
	}

	

}
