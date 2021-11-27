package org.dhc.gui.util;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.dhc.util.CryptoUtil;

public class ValidatorDhcAddress implements DocumentListener {
	
	private JTextField tf;
	
	public ValidatorDhcAddress(JTextField tf) {
		this.tf = tf;
	}

	public void insertUpdate(DocumentEvent e) {
		validate();
	}

	public void removeUpdate(DocumentEvent e) {
		validate();
		
	}

	public void changedUpdate(DocumentEvent e) {
		validate();
		
	}
	
	private void validate() {
		if("".equals(tf.getText())) {
			tf.setBackground(Color.white);
			return;
		}
		if(CryptoUtil.isDhcAddressValid(tf.getText())) {
			tf.setBackground(Color.white);
			return;
		}
		tf.setBackground(Constants.VERY_LIGHT_RED);
	}

}
