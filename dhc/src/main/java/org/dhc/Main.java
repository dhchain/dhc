package org.dhc;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;
import org.dhc.blockchain.Trimmer;
import org.dhc.network.Network;
import org.dhc.persistence.DBExecutor;
import org.dhc.util.Coin;
import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Registry;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.spi.IntOptionHandler;

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
		System.setProperty("jvm.name", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	}
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	@Option(name="-d", aliases="--dataDir", usage="Database path", required=true, metaVar="<database path>")
	private String dataDir;
	
	@Option(name="-p", handler=IntOptionHandler.class, aliases="--port", usage="Port number, for thin network port should be set to 0", required=true, metaVar="<port number>")
	private int port;
	
	@Option(name="-pw", aliases="--password", usage="Optional password to avoid entering manually, not recommended to be used for security reason", metaVar="<password>")
	private String password;
	
	@Option(name="-k", aliases="--key", usage="Path to key file, containing encrypted private key", required=true, metaVar="<key file path>")
	private String key;
	
	private PasswordHelper passwordHelper;
	
	public static void main(String[] args) {
		logger.info("START");
		Main main = new Main();
		main.parseArgs(args);
		main.start();
	}
	
	private void start() {
		Constants.DATABASE = dataDir;
		passwordHelper = new PasswordHelper(key);
		File file = new File(key);
		if(!file.exists()) {
			if(password == null) {
				passwordHelper.createNewPassphrase();
			} else if(password != null) {
				passwordHelper.generateKey(password);
			}
		} else {
			if(password == null) {
				passwordHelper.enterPassphrase();
			} else if(password != null && !passwordHelper.verifyPassphrase(password)) {
				passwordHelper.enterPassphrase();
			}
		}
		
		Blockchain blockchain = Blockchain.getInstance();
		DhcAddress dhcAddress = DhcAddress.getMyDhcAddress();
		logger.info("My DHC Address: {} \n{}", dhcAddress, dhcAddress.getBinary() + "\n");
		
		Trimmer.getInstance().runImmediately();

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

		network.printBuckets();
		Registry.getInstance().getMiner().start();
		
	}

	private void parseArgs(final String[] arguments) {
		final CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(arguments);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printUsage(parser);
		}
	}
	
	private void printUsage(CmdLineParser parser) {
		System.err.println("Usage: org.dhc.Main [options...]");
		parser.printUsage(System.err);
		System.err.println("Example: java org.dhc.Main" + parser.printExample(OptionHandlerFilter.REQUIRED));
		System.exit(-1);
	}
	

}
