package org.dhc.gui.promote;

import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Transaction;
import org.dhc.gui.Caller;
import org.dhc.gui.Main;
import org.dhc.gui.util.SwingUtil;
import org.dhc.network.Network;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Listeners;
import org.dhc.util.Message;
import org.dhc.util.StringUtil;
import org.dhc.util.ThreadExecutor;

public class JoinAction extends AbstractAction implements Caller {

	private static final long serialVersionUID = -5614773311159436840L;
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Main main;
	private JTextField addressField;
	private JTextField amountField;

	public JoinAction(JTextField addressField, JTextField amountField, Main main) {
		putValue(NAME, "Join");
		putValue(SHORT_DESCRIPTION, "Join Action");
		this.main = main;
		this.addressField = addressField;
		this.amountField = amountField;
		
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		ThreadExecutor.getInstance().execute(new DhcRunnable("JoinAction") {
			public void doRun() {
				process();
			}
		});
	}
	
	private void process() {
		
		Set<Transaction> transactions =  Blockchain.getInstance().getTransactionsForApp("JOIN", DhcAddress.getMyDhcAddress());
		if(transactions != null && !transactions.isEmpty()) {
			log("You already joined a team");
			return;
		}
		
		
		DhcAddress address = new DhcAddress(StringUtil.trim(addressField.getText()));
		if(!address.isDhcAddressValid()) {
			log("Please enter valid DHC Address");
			return;
		}
		if(address.equals(DhcAddress.getMyDhcAddress())) {
			log("You cannot join to your own DHC Address");
			return;
		}
		Coin amount;
		try {
			amount = Coin.ONE.multiply(Double.parseDouble(StringUtil.trim(amountField.getText())));
			if(amount.lessOrEqual(Coin.ZERO)) {
				log("Amount cannot be negative");
				return;
			}
		} catch (Exception e) {
			log("Please enter valid amount");
			return;
		}
		
		Blockchain blockchain = Blockchain.getInstance();
		Block block = blockchain.getLastBlocks().get(0);
		Coin balance = blockchain.sumByRecipient(DhcAddress.getMyDhcAddress().toString(), block);
		if(balance.less(amount)) {
			log("Your balance is less than entered amount");
			return;
		}
		
		final JLabel label = SwingUtil.showWait(main);

		getJoinInfo(amount);
		label.setText("<html>You joined promotion with peer " + address + " <br>for amount " + amount.toNumberOfCoins() + "</html>");
		logger.info("You joined promotion with peer " + address);
	}
	
	private void getJoinInfo(Coin amount) {
		DhcAddress address = new DhcAddress(StringUtil.trim(addressField.getText()));
		Network network = Network.getInstance();
		Message message = new GetJoinInfoRequest(DhcAddress.getMyDhcAddress(), address, amount);
		Listeners.getInstance().addEventListener(JoinInfoEvent.class, new JoinInfoEventListener(message.getCorrelationId()));
		network.sendToAddress(address, message);
	}
	
	public void log(String message) {
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}

}
