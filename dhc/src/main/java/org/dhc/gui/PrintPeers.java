package org.dhc.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;

import org.dhc.gui.util.SwingUtil;
import org.dhc.network.Network;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ThreadExecutor;

class PrintPeers extends AbstractAction {

	private static final long serialVersionUID = -5348678702516608164L;
	private Main main;
	
	public PrintPeers(Main main) {
		this.main = main;
		putValue(NAME, "Print Peers");
		putValue(SHORT_DESCRIPTION, "Print Peers");
	}
	public void actionPerformed(ActionEvent e) {
		final JLabel label = SwingUtil.showWait(main);
		ThreadExecutor.getInstance().execute(new DhcRunnable("Print Peers") {
			public void doRun() {
				process();
				label.setText("Please check the log for peers");
			}
		});
		
	}
	
	private void process() {
		Network network = Network.getInstance();
		network.printBuckets();
	}
}