package org.dhc.gui.util;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.dhc.util.StringUtil;

public class ValidatorDouble implements DocumentListener {
	
	private JTextField tf;
	
	public ValidatorDouble(JTextField tf) {
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
		String value = StringUtil.trim(tf.getText());
		try {
			Double.parseDouble(value);
			tf.setBackground(Color.white);
		} catch (Exception e) {
			tf.setBackground(Constants.VERY_LIGHT_RED);
		}
	}

}
