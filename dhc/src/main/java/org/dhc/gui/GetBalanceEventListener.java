package org.dhc.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.dhc.gui.util.JTextFieldRegularPopupMenu;
import org.dhc.network.Network;
import org.dhc.util.DhcLogger;
import org.dhc.util.Event;
import org.dhc.util.EventListener;
import org.dhc.util.GetBalanceEvent;
import org.dhc.util.Listeners;

public class GetBalanceEventListener implements EventListener {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Main main;
	private String correlationId;

	public GetBalanceEventListener(Main main, String correlationId) {
		this.main = main;
		this.correlationId = correlationId;
	}

	@Override
	public void onEvent(Event event) {
		GetBalanceEvent getBalanceEvent = (GetBalanceEvent)event;
		if(!correlationId.equals(getBalanceEvent.getCorrelationId())) {
			return;
		}
		
		String balance = getBalanceEvent.getBalance().toNumberOfCoins();
		logger.info("Address {} has balance {}", getBalanceEvent.getAddress(),  balance);

		JPanel form = new JPanel(new BorderLayout());

		JPanel labelPanel = new JPanel(new GridLayout(2, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(2, 1));
		form.add(labelPanel, BorderLayout.WEST);
		form.add(fieldPanel, BorderLayout.CENTER);

		JLabel label = new JLabel(
				"DHC Address on shard " + getBalanceEvent.getAddress().getBinary(Network.getInstance().getPower()) + ":",
				JLabel.RIGHT);
		label.setBorder(new EmptyBorder(5, 5, 5, 5));
		labelPanel.add(label);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField textField = new JTextField(30);
		textField.setText(getBalanceEvent.getAddress().toString());
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
		
		Listeners.getInstance().removeEventListener(GetBalanceEvent.class, this);

	}

	

}
