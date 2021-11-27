package org.dhc.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

class ExitAction extends AbstractAction {

	private static final long serialVersionUID = -5348678702516608164L;
	
	public ExitAction() {
		putValue(NAME, "Exit");
		putValue(SHORT_DESCRIPTION, "Exit");
	}
	public void actionPerformed(ActionEvent e) {
		System.exit(0);
	}
}