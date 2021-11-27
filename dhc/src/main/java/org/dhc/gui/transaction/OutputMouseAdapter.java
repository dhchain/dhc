package org.dhc.gui.transaction;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.dhc.blockchain.TransactionOutput;
import org.dhc.gui.Main;
import org.dhc.util.DhcLogger;

public class OutputMouseAdapter extends MouseAdapter {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private JTable table;
	private List<TransactionOutput> list;
	private Main main;
	
	public void mouseClicked(MouseEvent evt) {
        int row = table.rowAtPoint(evt.getPoint());
        int col = table.columnAtPoint(evt.getPoint());
        if (row >= 0 && col >= 0) {
            doit(list.get(row));
        }
    }
	
	private JPanel showBackButton() {
		JFrame frame = main.getFrame();
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JButton btnSubmit = new JButton();
		btnSubmit.setAction(new ShowOutputs(main));
		btnSubmit.setText("Back");
		
		frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "backButton");

		frame.getRootPane().getActionMap().put("backButton", new AbstractAction() {
			private static final long serialVersionUID = 4946947535624344910L;

			public void actionPerformed(ActionEvent actionEvent) {
				btnSubmit.doClick();
				frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
				frame.getRootPane().getActionMap().clear();
			}
		});
		
		p.add(btnSubmit);
		return p;
	}

	private void doit(TransactionOutput output) {
		
		logger.info("output {}", output);
		
		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		
		JPanel p = showBackButton();
		
		form.add(p);
		
		JPanel subform = new JPanel(new BorderLayout());
		form.add(subform);
		
		
		JPanel labelPanel = new JPanel(new GridLayout(9, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(9, 1));
		subform.add(labelPanel, BorderLayout.WEST);
		subform.add(fieldPanel, BorderLayout.CENTER);
		
		JLabel label = new JLabel("Output ID:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField identifier = new JTextField(36);
		identifier.setText(output.getOutputId());
		identifier.setBorder( null );
		identifier.setOpaque( false );
		identifier.setEditable( false );
		p.add(identifier);
		fieldPanel.add(p);
		
		label = new JLabel("Value:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField blockhash = new JTextField(36);
		blockhash.setText(output.getValue().toNumberOfCoins());
		blockhash.setBorder( null );
		blockhash.setOpaque( false );
		blockhash.setEditable( false );
		p.add(blockhash);
		fieldPanel.add(p);
		
		label = new JLabel("Output Transaction Id:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField blockIndex = new JTextField(36);
		blockIndex.setText(output.getOutputTransactionId());
		blockIndex.setBorder( null );
		blockIndex.setOpaque( false );
		blockIndex.setEditable( false );
		p.add(blockIndex);
		fieldPanel.add(p);
		
		label = new JLabel("Output block index:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField timeStamp = new JTextField(36);
		timeStamp.setText(Long.toString(output.getOutputBlockIndex()));
		timeStamp.setBorder( null );
		timeStamp.setOpaque( false );
		timeStamp.setEditable( false );
		p.add(timeStamp);
		fieldPanel.add(p);
		
		label = new JLabel("Output block hash:", JLabel.RIGHT);
		labelPanel.add(label);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField address = new JTextField(36);
		address.setText(output.getOutputBlockHash());
		address.setBorder( null );
		address.setOpaque( false );
		address.setEditable( false );
		p.add(address);
		fieldPanel.add(p);
		
		main.setForm(form);
	}

	public OutputMouseAdapter(JTable table, List<TransactionOutput> list, Main main) {
		this.table = table;
		this.list = list;
		this.main = main;
	}

}
