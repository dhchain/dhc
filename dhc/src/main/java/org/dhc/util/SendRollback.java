package org.dhc.util;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dhc.PasswordHelper;
import org.dhc.blockchain.RollbackMessage;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.spi.IntOptionHandler;
import org.kohsuke.args4j.spi.LongOptionHandler;

import com.google.gson.stream.JsonWriter;

public class SendRollback {
	
	static {
		Security.setProperty("crypto.policy", "unlimited");
		Security.addProvider(new BouncyCastleProvider());
		System.setProperty("jvm.name", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	}

	private static final GsonUtil gsonUtil = GsonUtil.getInstance();
	@SuppressWarnings("unused")
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	@Option(name="-p", handler=IntOptionHandler.class, aliases="--port", usage="Port number", required=true, metaVar="<port number>")
	private int port;
	
	@Option(name="-i", handler=LongOptionHandler.class, aliases="--index", usage="Blockchain index", required=true, metaVar="<Blockchain index>")
	private long index;
	
	@Option(name="-pw", aliases="--password", usage="Optional password to avoid entering manually, not recommended to be used for security reason", metaVar="<password>")
	private String password;
	
	@Option(name="-k", aliases="--key", usage="Path to key file, containing encrypted private key", required=true, metaVar="<key file path>")
	private String key;
	
	@Option(name="-bk", aliases="--bucketKey", usage="Bucket Key", required=false, metaVar="<bucket key>")
	private String bucketKey;
	
	private PasswordHelper passwordHelper;


	public static void main(String[] args) throws Exception {
		SendRollback main = new SendRollback();
		main.parseArgs(args);
		main.process();
	}
	
	private void process() throws Exception {
		
		passwordHelper = new PasswordHelper(key);
		if(password == null) {
			passwordHelper.enterPassphrase();
		} else if(password != null && !passwordHelper.verifyPassphrase(password)) {
			passwordHelper.enterPassphrase();
		}
		
		try (
				//Socket socket = new Socket(InetAddress.getLocalHost(), port);
				Socket socket = new Socket("5.135.137.215", port);
				JsonWriter writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)));
		) {
			Message message = new RollbackMessage(index, bucketKey);
			gsonUtil.write(message, writer);
		}
	}
	
	private void parseArgs(final String[] arguments) {
		final CmdLineParser parser = new CmdLineParser(this);
		if (arguments.length < 1) {
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
		System.err.println("Usage: java " + getClass().getName() + " [options...]");
		parser.printUsage(System.err);
		System.err.println("Example: java " + getClass().getName() + " " + parser.printExample(OptionHandlerFilter.REQUIRED));
		System.exit(-1);
	}

}
