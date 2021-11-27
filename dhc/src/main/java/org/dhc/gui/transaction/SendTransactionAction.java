package org.dhc.gui.transaction;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.SendTransactionMessage;
import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionData;
import org.dhc.blockchain.TransactionMemoryPool;
import org.dhc.gui.Caller;
import org.dhc.gui.Main;
import org.dhc.gui.util.SwingUtil;
import org.dhc.network.Network;
import org.dhc.util.Coin;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.StringUtil;
import org.dhc.util.ThreadExecutor;

public class SendTransactionAction extends AbstractAction implements Caller {

	private static final long serialVersionUID = 4798442956508802794L;
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Main main;
	
	private JTextField jaddress;
	private JTextField jamount;
	private JTextField jfee;
	private JTextField jexpire;
	private JTextArea jexpiringData;
	
	
	private String recipient;
	private String amount; 
	private String fee; 
	private String expire; 
	private String expiringData;

	public SendTransactionAction(Main main, JTextField address, JTextField amount, JTextField fee, JTextField expire, JTextArea expiringData) {
		putValue(NAME, "Send Transaction");
		putValue(SHORT_DESCRIPTION, "Send Transaction Action");
		this.main = main;
		
		jaddress = address;
		jamount = amount;
		jfee = fee;
		jexpire = expire;
		jexpiringData = expiringData;
	}
	
	private boolean validate() {
		if(!CryptoUtil.isDhcAddressValid(recipient)) {
			return false;
		}
		try {
			Double.parseDouble(amount);
			Long.parseLong(fee);
			if(expiringData != null) {
				Long.parseLong(expire);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	private void load() {
		recipient = StringUtil.trim(jaddress.getText());
		amount = StringUtil.trim(jamount.getText());
		fee = StringUtil.trim(jfee.getText());
		expire = StringUtil.trim(jexpire.getText());
		expiringData = StringUtil.trimToNull(jexpiringData.getText());
	}

	public void actionPerformed(ActionEvent e) {
		ThreadExecutor.getInstance().execute(new DhcRunnable("SendTransactionAction") {
			public void doRun() {
				process();
			}
		});
	}
	
	private void process() {
		load();
		
		if(!validate()) {
			log("Please verify inputs");
			return;
		}

		final Coin amount = Coin.ONE.multiply(Double.parseDouble(this.amount));
		final TransactionData expiringData = this.expiringData == null? null: new TransactionData(this.expiringData, Long.parseLong(expire));
		
		Network network = Network.getInstance();
		Block block = Blockchain.getInstance().getLastBlocks().iterator().next();
		Transaction transaction = new Transaction();
		transaction.create(new DhcAddress(recipient), amount, new Coin(Long.parseLong(fee)), expiringData, null, block);
		if (TransactionMemoryPool.getInstance().add(transaction)) {
			final JLabel label = SwingUtil.showWait(main);
			network.sendToAllMyPeers(new SendTransactionMessage(transaction));
			label.setText("Transaction was sent");
			logger.info("Transaction was sent {}", transaction);
			return;
		}

		log("Could not sent transaction");
		logger.info("Could not sent transaction {}", transaction);
	}

	public void log(String message) {
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}



}
