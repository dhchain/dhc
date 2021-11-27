package org.dhc.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;

import org.dhc.gui.util.SwingUtil;
import org.dhc.network.Network;
import org.dhc.util.DhcRunnable;
import org.dhc.util.ThreadExecutor;

class StopNetworkAction extends AbstractAction {

	private static final long serialVersionUID = -5348678702516608164L;
	private Main main;
	
	public StopNetworkAction(Main main) {
		putValue(NAME, "Stop Network");
		putValue(SHORT_DESCRIPTION, "Stop Network");
		this.main = main;
	}
	
	public void actionPerformed(ActionEvent e) {
		final JLabel label = SwingUtil.showWait(main);
		ThreadExecutor.getInstance().execute(new DhcRunnable("Start Network") {
			public void doRun() {
				Network.getInstance().stop();
				label.setText("Network stopped");
			}
		});
	}
}