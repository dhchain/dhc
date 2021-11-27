package org.dhc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.dhc.util.Base58;
import org.dhc.util.CryptoUtil;
import org.dhc.util.Encryptor;
import org.dhc.util.DhcLogger;
import org.dhc.util.Wallet;

public class PasswordHelper {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static Encryptor encryptor = new Encryptor();
	
	private String key;
	
	public PasswordHelper(String key) {
		this.key = key;
	}
	
	public void enterPassphrase() {
		String passphrase;
		do {
			passphrase = getPassPhrase("Enter passphrase");
		} while (!verifyPassphrase(passphrase));
	}
	
	public void createNewPassphrase() {
		String newPassPhrase;
		String reenterNewPassPhrase;
		do {
			newPassPhrase = getPassPhrase("Enter new passphrase");
			reenterNewPassPhrase = getPassPhrase("Reenter new passphrase");
		} while(!newPassPhrase.equals(reenterNewPassPhrase));
		generateKey(newPassPhrase);
	}
	
	public void generateKey(String passPhrase) {
		Wallet wallet = Wallet.getInstance();
		wallet.generateKeyPair();
		saveKey(wallet, passPhrase);
	}
	
	public void saveKey(Wallet wallet, String passphrase) {
		File targetFile = new File(key);
		File parent = targetFile.getParentFile();
		if (!parent.exists() && !parent.mkdirs()) {
		    throw new IllegalStateException("Couldn't create dir: " + parent);
		}
		try {
			try (
					OutputStream os = new FileOutputStream(key);
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));
			) {
				String publicKey = Base58.encode(wallet.getPublicKey().getEncoded());
				String privateKey = Base58.encode(encryptor.encrypt(passphrase, wallet.getPrivateKey().getEncoded()));
				out.write(publicKey + "," + privateKey);
				out.newLine();
				out.flush();
				System.out.println("Encrypted key were saved to " + new File(key).getAbsolutePath());
				System.out.println("Backup " + new File(key).getAbsolutePath() + " to a safe location");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private String getPassPhrase(String prompt) {
		System.out.println(prompt);
		String passPhrase = "";
		Console console = System.console();
		if(console == null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				passPhrase = br.readLine();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			passPhrase = new String(console.readPassword());
		}
		return passPhrase.trim();
	}
	
	public boolean verifyPassphrase(String passphrase) {
		try {
			try (
					InputStream is = new FileInputStream(key);
					BufferedReader in = new BufferedReader(new InputStreamReader(is));
			) {
				String line;
				while ((line = in.readLine()) != null) {
					String[] strs = line.split(",");
					if(strs.length != 2) {
						continue;
					}
					Wallet wallet = Wallet.getInstance();
					PublicKey publicKey = CryptoUtil.loadPublicKey(strs[0]);
					PrivateKey privateKey = CryptoUtil.loadPrivateKey(encryptor.decrypt(passphrase, Base58.decode(strs[1])));
					if(privateKey == null) {
						System.out.println("Passphrase entered was not correct. Try again.");
						return false;
					}
					wallet.setPrivateKey(privateKey);
					wallet.setPublicKey(publicKey);
				}
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return true;
	}

}
