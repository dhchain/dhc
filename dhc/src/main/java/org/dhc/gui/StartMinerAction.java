package org.dhc.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;

import org.dhc.gui.util.SwingUtil;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;

class StartMinerAction extends AbstractAction {

	private static final long serialVersionUID = -5348678702516608164L;
	private Main main;
	
	public StartMinerAction(Main main) {
		putValue(NAME, "Start Miner");
		putValue(SHORT_DESCRIPTION, "Start Miner");
		this.main = main;
	}
	public void actionPerformed(ActionEvent e) {
		final JLabel label = SwingUtil.showWait(main);
		ThreadExecutor.getInstance().execute(new DhcRunnable("Stop Miner") {
			public void doRun() {
				Registry.getInstance().getMiner().start();
				label.setText("Miner started");
			}
		});
	}
}