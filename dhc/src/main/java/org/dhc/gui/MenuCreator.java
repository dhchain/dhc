package org.dhc.gui;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.dhc.gui.promote.Join;
import org.dhc.gui.promote.ViewJoin;
import org.dhc.gui.transaction.Mempool;
import org.dhc.gui.transaction.SendTransaction;
import org.dhc.gui.transaction.ShowOutputs;
import org.dhc.gui.transaction.ShowTransactions;

public class MenuCreator {
	
	private Main main;
	
	public MenuCreator(Main main) {
		this.main = main;
	}

	public void addMenu() {
		JMenuBar menuBar = new JMenuBar();
		main.getFrame().setJMenuBar(menuBar);
		
		addMenuFile(menuBar);
		addMenuTools(menuBar);
		addMenuTransactions(menuBar);
		addMenuPromote(menuBar);
		menuBar.updateUI();
	}
	
	private void addMenuPromote(JMenuBar menuBar) {
		JMenu jmenu = new JMenu("Promote");
		menuBar.add(jmenu);
		
		JMenuItem menuItem = new JMenuItem("Join Team");
		menuItem.setAction(new Join(main));
		jmenu.add(menuItem);
		
		menuItem = new JMenuItem("View Team");
		menuItem.setAction(new ViewJoin(main));
		jmenu.add(menuItem);
		
	}

	private void addMenuTransactions(JMenuBar menuBar) {
		JMenu jmenu = new JMenu("Transactions");
		menuBar.add(jmenu);
		
		JMenuItem menuItem = new JMenuItem("Send Transaction");
		menuItem.setAction(new SendTransaction(main));
		jmenu.add(menuItem);
		
		menuItem = new JMenuItem("Show Transactions");
		menuItem.setAction(new ShowTransactions(main));
		jmenu.add(menuItem);
		
		menuItem = new JMenuItem("Mempool");
		menuItem.setAction(new Mempool(main));
		jmenu.add(menuItem);
        
		menuItem = new JMenuItem("Show Outputs");
		menuItem.setAction(new ShowOutputs(main));
		jmenu.add(menuItem);
	}

	private void addMenuFile(JMenuBar menuBar) {
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setAction(new ExitAction());
		mnFile.add(mntmExit);
		
		JMenuItem mntmStopMiner = new JMenuItem("Stop Miner");
		mntmStopMiner.setAction(new StopMinerAction(main));
		mnFile.add(mntmStopMiner);
		
		JMenuItem mntmStartMiner = new JMenuItem("Start Miner");
		mntmStartMiner.setAction(new StartMinerAction(main));
		mnFile.add(mntmStartMiner);
		
		JMenuItem mntmStopNetwork = new JMenuItem("Stop Network");
		mntmStopNetwork.setAction(new StopNetworkAction(main));
		mnFile.add(mntmStopNetwork);
		
		JMenuItem mntmStartNetwork = new JMenuItem("Start Network");
		mntmStartNetwork.setAction(new StartNetworkAction(main));
		mnFile.add(mntmStartNetwork);
		
	}
	
	private void addMenuTools(JMenuBar menuBar) {
		JMenu jmenu = new JMenu("Tools");
		menuBar.add(jmenu);
		
		JMenuItem menuItem = new JMenuItem("Change Password");
		menuItem.setAction(new ChangePassword(main));
		jmenu.add(menuItem);
		
		menuItem = new JMenuItem("Get Balance");
		menuItem.setAction(new GetBalance(main));
		jmenu.add(menuItem);

		menuItem = new JMenuItem("Show Address");
		menuItem.setAction(new ShowAddress(main));
		jmenu.add(menuItem);
		
		menuItem = new JMenuItem("Print Peers");
		menuItem.setAction(new PrintPeers(main));
		jmenu.add(menuItem);
	}

}
