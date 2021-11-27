package org.dhc.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Encryptor {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private static final int iterations = 2000;
	private static final int keyLength = 256;
	private static final int SALT_LENGTH = 16;
	private static final Provider provider = new BouncyCastleProvider();
	private static final String TRANSFORMATION = "AES/CTR/NOPADDING";

	public byte[] encryptAsymm(byte[] input, PublicKey key) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("ECIES");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(input);
	}

	public byte[] decryptAsymm(byte[] input, PrivateKey key) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("ECIES");
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(input);
	}
	
	public static void main(String[] args) throws Exception {
		Security.setProperty("crypto.policy", "unlimited");
		Security.addProvider(new BouncyCastleProvider());
		Wallet wallet = Wallet.getInstance();
		wallet.generateKeyPair();
		PublicKey key = wallet.getPublicKey();
		Cipher cipher = Cipher.getInstance("ECIES");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] bytes = cipher.doFinal("How to use ECDH keypairs algorithm for encryption and decryption same like RSA keypairs without key agreement?".getBytes());
		logger.debug((new String(bytes, StandardCharsets.UTF_8)));
		
		cipher = Cipher.getInstance("ECIES");
		cipher.init(Cipher.DECRYPT_MODE, wallet.getPrivateKey());
		bytes = cipher.doFinal(bytes);
		logger.debug(new String(bytes, StandardCharsets.UTF_8));
	}

	public byte[] encrypt(String passphrase, byte[] toEncrypt) throws Exception {
		byte[] salt = CryptoUtil.getRandomBytes(SALT_LENGTH);
		SecretKey key = generateKey(passphrase, salt);
		Cipher cipher = Cipher.getInstance(TRANSFORMATION, provider);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] encrypted = cipher.doFinal(toEncrypt);
		byte[] result = new byte[encrypted.length + salt.length];
		System.arraycopy(encrypted, 0, result, 0, encrypted.length);
		System.arraycopy(salt, 0, result, encrypted.length, salt.length);
		return result;
	}

	public byte[] decrypt(String passphrase, byte[] ciphertext) throws Exception {
		byte[] salt = new byte[SALT_LENGTH];
		System.arraycopy(ciphertext, ciphertext.length - salt.length, salt, 0, salt.length);
		SecretKey key = generateKey(passphrase, salt);
		byte[] encrypted = new byte[ciphertext.length - salt.length];
		System.arraycopy(ciphertext, 0, encrypted, 0, ciphertext.length - salt.length);
		Cipher cipher = Cipher.getInstance(TRANSFORMATION, provider);
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(encrypted);
	}

	private SecretKey generateKey(String passphrase, byte[] salt) throws Exception {
		PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray(), salt, iterations, keyLength);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWITHSHA256AND256BITAES-CBC-BC");
		return keyFactory.generateSecret(keySpec);
	}
	
	public static void verifyCryptography() throws NoSuchAlgorithmException {
		if(Cipher.getMaxAllowedKeyLength(Encryptor.TRANSFORMATION) != Integer.MAX_VALUE) {
			String error = "Unlimited strength cryptography is not enabled. Please install unlimited strength cryptography or update Java to the latest version.";
			logger.debug(error);
			System.out.println(error);
			System.exit(0);
		}
	}

}
