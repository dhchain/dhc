package org.dhc.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Security;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.BevelBorder;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.network.ChainRest;
import org.dhc.network.Network;
import org.dhc.network.PeerSync;
import org.dhc.persistence.DBExecutor;
import org.dhc.util.BlockEvent;
import org.dhc.util.Coin;
import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Listeners;
import org.dhc.util.Registry;
import org.dhc.util.ThreadExecutor;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.spi.IntOptionHandler;

import net.miginfocom.swing.MigLayout;

public class Main {

	static {
		Security.setProperty("crypto.policy", "unlimited");
		Security.addProvider(new BouncyCastleProvider());
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				logger.debug("Shutdown Hook is running !");
				DBExecutor.shutdownDerby();
			}
		});
		System.setProperty("jvm.name", ManagementFactory.getRuntimeMXBean().getName());
	}
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private JFrame frame;
	private JPanel form;
	private JPanel statusBar;
	private JLabel status;
	private JPasswordField passwordField;
	private JPasswordField confirmPasswordField;
	
	@Option(name="-d", aliases="--dataDir", usage="Database path", required=true, metaVar="<database path>")
	private String dataDir;
	
	@Option(name="-p", handler=IntOptionHandler.class, aliases="--port", usage="Port number, for thin network port should be set to 0", required=true, metaVar="<port number>")
	private int port;
	
	@Option(name="-k", aliases="--key", usage="Path to key file, containing encrypted private key", required=true, metaVar="<key file path>")
	private String key;

	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main main = new Main();
					main.parseArgs(args);
					main.initialize();
					main.frame.setVisible(true);
					logger.debug("Distributed Hash Chain Started");
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
	}

	public Main() {
		Listeners.getInstance().addEventListener(BlockEvent.class, new BlockEventListener(this));
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("DHC");

		File file = new File(key);
		if(!file.exists() && !copyExisting()) {
			createNewPassphrase();
		} else {
			enterPassphrase();
		}
		frame.setSize(640, 480);
		setIcon();
		
		statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.setPreferredSize(new Dimension(frame.getWidth(), 24));
	    
	    status = new JLabel();
	    statusBar.add(status);
	    frame.add(statusBar, BorderLayout.SOUTH);
		
		logger.info("Main.initialize() completed");
	}
	
	private boolean copyExisting() {
		int input = JOptionPane.showConfirmDialog(frame, 
                "Do you want to import existing key file?", "Select existing kye file", 
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

		if(input != JOptionPane.YES_OPTION) {
			return false;
		}
		JFileChooser jfc = new JFileChooser();
		int returnValue = jfc.showOpenDialog(null);

		if (returnValue != JFileChooser.APPROVE_OPTION) {
			return false;
		}
		File selectedFile = jfc.getSelectedFile();
		File file = new File(key);
		try {
			if (file.exists()) {
				Files.copy(file.toPath(), new File(key + ".backupOriginalFile").toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			}
			Files.copy(selectedFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	private void createNewPassphrase() {
		
		form = new JPanel(new MigLayout(
		        "wrap 2",
		        "[right][fill]"
		        ));
		
		JLabel label = new JLabel("Enter Passphrase:");
		form.add(label);
		
		passwordField = new JPasswordField(45);
		form.add(passwordField);
		
		label = new JLabel("Reenter New Passphrase:");
		form.add(label);

		confirmPasswordField = new JPasswordField(45);
		form.add(confirmPasswordField);
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.setAction(new CreateKeyAction(this));
		p.add(btnSubmit);
		form.add(p);

		frame.getContentPane().add(form, BorderLayout.NORTH);
		frame.getRootPane().setDefaultButton(btnSubmit);
	}
	
	private void enterPassphrase() {
		
		form = new JPanel(new MigLayout("wrap 2", "[right][fill]"));
		
		JLabel label = new JLabel("Enter Passphrase:");
		form.add(label);
		
		passwordField = new JPasswordField(45);
		form.add(passwordField);
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.setAction(new SubmitPasswordAction(this));
		p.add(btnSubmit);
		form.add(p);

		frame.getContentPane().add(form);
		
		frame.getRootPane().setDefaultButton(btnSubmit);
	}
	
	private void setIcon() {
		try {
            URL resource = ClassLoader.getSystemResource("dhcLogo.png");
            BufferedImage image = ImageIO.read(resource);
            frame.setIconImage(image);
        } catch (IOException e) {
        	logger.error(e.getMessage(), e);
        }
	}
	
	private void parseArgs(final String[] arguments) {
		final CmdLineParser parser = new CmdLineParser(this);
		if (arguments.length != 6) {
			printUsage(parser);
		}
		try {
			parser.parseArgument(arguments);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printUsage(parser);
		}
	}
	
	private void printUsage(CmdLineParser parser) {
		System.err.println("Usage: org.dhc.gui.Main [options...]");
		parser.printUsage(System.err);
		System.err.println("Example: java org.dhc.gui.Main" + parser.printExample(OptionHandlerFilter.REQUIRED));
		System.exit(-1);
	}

	public String getKey() {
		return key;
	}

	public JFrame getFrame() {
		return frame;
	}

	public JPasswordField getPasswordField() {
		return passwordField;
	}

	public JPasswordField getConfirmPasswordField() {
		return confirmPasswordField;
	}
	
	public void start(Caller caller) {
		ThreadExecutor.getInstance().execute(new DhcRunnable("START") {
			public void doRun() {
				doStart(caller);
			}
		});
	}
	
	private void doStart(Caller caller) {
		Constants.DATABASE = dataDir;
		
		Blockchain blockchain = Blockchain.getInstance();
		DhcAddress dhcAddress = DhcAddress.getMyDhcAddress();
		logger.info("My DHC Address: {} \n{}", dhcAddress, dhcAddress.getBinary() + "\n");

		for(Block block: blockchain.getLastBlocks()) {
			logger.info("Last block {}", block);
			Coin balance = blockchain.sumByRecipient(DhcAddress.getMyDhcAddress().toString(), block);
			logger.info("My balance {} for block {}", balance.toNumberOfCoins(), block);
		}
		
		
		
		//blockchain.removeByIndex(4280);

		Network network = Network.getInstance();
		network.setPort(port);
		
		network.start();

		blockchain.isValid();
		
		Registry.getInstance().getCompactor().pruneBlockchain();
		
		blockchain.start();

		PeerSync.getInstance().start();
		Registry.getInstance().getMiner().start();
		network.printBuckets();
		ChainRest.getInstance().execute();
		logger.info("Main.start() completed");
		caller.log("Started Distributed Hash Chain");
	}

	public JPanel getForm() {
		return form;
	}

	public void setForm(JPanel form) {
		frame.getContentPane().remove(this.form);
		
		frame.getContentPane().add(form, BorderLayout.NORTH);
		frame.revalidate();
		frame.getContentPane().repaint();
		
		this.form = form;
	}

	public JPanel getStatusBar() {
		return statusBar;
	}

	public JLabel getStatus() {
		return status;
	}
	
	
}
