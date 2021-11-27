package org.dhc.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.gui.util.SwingUtil;
import org.dhc.network.Network;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.GetBalanceAsyncRequest;
import org.dhc.util.GetBalanceEvent;
import org.dhc.util.Listeners;
import org.dhc.util.Message;
import org.dhc.util.StringUtil;
import org.dhc.util.ThreadExecutor;

public class GetBalanceAction extends AbstractAction implements Caller {

	private static final long serialVersionUID = 4798442956508802794L;
	private static final DhcLogger logger = DhcLogger.getLogger();

	private JTextField jAddress;
	private String address;
	private Main main;

	public GetBalanceAction(JTextField address, Main main) {
		putValue(NAME, "Get Balance");
		putValue(SHORT_DESCRIPTION, "Get Balance Action");
		this.jAddress = address;
		this.main = main;
	}
	
	private void getBalance() {
		
		Network network = Network.getInstance();
		Blockchain blockchain = Blockchain.getInstance();
		Block block = blockchain.getLastBlocks().get(0);
		String blockhash = block.getPreviousHash();
		DhcAddress myAddress = DhcAddress.getMyDhcAddress();
		DhcAddress toAddress = new DhcAddress(address);
		Message message = new GetBalanceAsyncRequest(toAddress, myAddress, blockhash);
		Listeners.getInstance().addEventListener(GetBalanceEvent.class, new GetBalanceEventListener(main, message.getCorrelationId()));
		network.sendToAddress(toAddress, message);
		logger.trace("sent to {} message {}", toAddress, message);
	}

	public void actionPerformed(ActionEvent e) {
		address = StringUtil.trim(jAddress.getText());
		if(!CryptoUtil.isDhcAddressValid(address)) {
			JOptionPane.showMessageDialog(main.getFrame(), "DHC Address is not valid");
			return;
		}
		
		SwingUtil.showWait(main);
		
		ThreadExecutor.getInstance().execute(new DhcRunnable("GetBalanceAction") {
			public void doRun() {
				
				getBalance();
				
			}
		});
		
	}

	public void log(String message) {
		JOptionPane.showMessageDialog(main.getFrame(), message);
	}



}
