package org.dhc.gui.util;

import java.awt.BorderLayout;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.dhc.gui.Main;

public class SwingUtil {

	public static JLabel showWait(Main main) {
		JPanel form = new JPanel(new BorderLayout());
		JLabel label = new JLabel("Please wait, processing.");
		label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		label.setBorder(new EmptyBorder(5, 5, 5, 5));
		form.add(label);
		main.setForm(form);
		return label;
	}

	public static String getSelectedButtonText(ButtonGroup buttonGroup) {
		for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();

			if (button.isSelected()) {
				return button.getText();
			}
		}

		return null;
	}

}
