package org.dhc.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.jcajce.provider.digest.SHA3.Digest256;

public class CryptoUtil {
	
	private static final SecureRandom random = new SecureRandom();
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	public static String getHashBase58Encoded(String input) {
		return Base58.encode(getHash(input.getBytes(StandardCharsets.UTF_8)));
	}
	
	public static byte[] getHash(String input) {
		return getHash(input.getBytes(StandardCharsets.UTF_8));
	}
	
	public static byte[] getHash(byte[] input) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(new Digest256().digest(input));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public static DhcAddress getDhcAddressFromKey(Key key) {
		byte[] publicKey = key.getEncoded();
		return getDhcAddressFromBytes(publicKey);
	}
	
	public static DhcAddress getDhcAddressFromBytes(byte[] bytes) {
		RIPEMD160Digest ripemd160 = new RIPEMD160Digest();
		MessageDigest sha256;
		MessageDigest sha3 = new Digest256();
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		byte[] sha256Hash = sha256.digest(sha3.digest(bytes));

		byte[] ripemdHash = new byte[ripemd160.getDigestSize() + 2];
		ripemd160.update(sha256Hash, 0, sha256Hash.length);
		ripemd160.doFinal(ripemdHash, 0);

		// Set version bytes
		ripemdHash[ripemdHash.length-2] = Constants.NETWORK_TYPE[0];
		ripemdHash[ripemdHash.length-1] = Constants.NETWORK_TYPE[1];

		sha256Hash = sha3.digest(ripemdHash);
		sha256Hash = sha256.digest(sha256Hash);

		byte[] addressBytes = new byte[ripemdHash.length + 4];

		System.arraycopy(ripemdHash, 0, addressBytes, 0, ripemdHash.length);
		System.arraycopy(sha256Hash, 0, addressBytes, (ripemdHash.length), 4);
		
		String address = Base58.encode(addressBytes);
		return new DhcAddress(address);
	}
	
	public static boolean isDhcAddressValid(String dhcAddress) {
		dhcAddress = StringUtil.trim(dhcAddress);
		if("".equals(dhcAddress) || dhcAddress == null) {
			return false;
		}
		try {
			byte[] addressBytes = Base58.decode(dhcAddress);
			byte[] ripemdHash = new byte[addressBytes.length - 4];
			System.arraycopy(addressBytes, 0, ripemdHash, 0, ripemdHash.length);
			
			MessageDigest sha3 = new Digest256();
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

			byte[] sha256Hash = sha256.digest(sha3.digest(ripemdHash));

			return ripemdHash[ripemdHash.length - 2] == Constants.NETWORK_TYPE[0] 
					&& ripemdHash[ripemdHash.length - 1] == Constants.NETWORK_TYPE[1] 
					&& sha256Hash[0] == addressBytes[ripemdHash.length]
					&& sha256Hash[1] == addressBytes[ripemdHash.length + 1] 
					&& sha256Hash[2] == addressBytes[ripemdHash.length + 2] 
					&& sha256Hash[3] == addressBytes[ripemdHash.length + 3];
		} catch (NegativeArraySizeException | ArrayIndexOutOfBoundsException e) {
			logger.trace("dhcAddress = {}", dhcAddress);
			logger.trace(e.getMessage(), e);
		} catch (Exception e) {
			logger.trace(e.getMessage(), e);
		}
		return false;
	}
	
	public static PublicKey loadPublicKey(String key58) throws GeneralSecurityException {
		if(key58 == null) {
			return null;
		}
	    byte[] clear = Base58.decode(key58);
	    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(clear);
	    KeyFactory fact = KeyFactory.getInstance("ECDSA");
	    PublicKey key = fact.generatePublic(keySpec);
	    Arrays.fill(clear, (byte) 0);
	    return key;
	}
	
	public static PrivateKey loadPrivateKey(byte[] clear) throws GeneralSecurityException {
		try {
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
			KeyFactory fact = KeyFactory.getInstance("ECDSA");
			PrivateKey key = fact.generatePrivate(keySpec);
			Arrays.fill(clear, (byte) 0);
			return key;
		} catch (InvalidKeySpecException e) {
			
		}
		return null;
	}
	
	public static String getBinaryRepresentation(String base58Str) {
		byte[] bytes = Base58.decode(base58Str);
		String str = "";
		for(byte b: bytes) {
			str = str + Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
		}
		return str;
	}
	
	public static String fromBinaryString(String binary) {
		byte[] bytes = new byte[binary.length() / 8];
		for (int i = 0; i < binary.length() / 8; i++) {
			String str = binary.substring(i * 8, i * 8 + 8);
			bytes[i] = (byte) Integer.parseInt(str, 2);
		}
		return Base58.encode(bytes);
	}

	
	public static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
			keyGen.initialize(ecSpec, random); // 256
			KeyPair keyPair = keyGen.generateKeyPair();
			return keyPair;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getRandomString(int length) {
		byte[] bytes = new byte[length];
		random.nextBytes(bytes);
		return Base58.encode(bytes);
	}
	
	public static byte[] getRandomBytes(int length) {
		byte[] bytes = new byte[length];
		random.nextBytes(bytes);
		return bytes;
	}
	
	public static String getMerkleTreeRoot(List<String> strings) {
		if(strings.size() == 0 ) {
			return "";
		}
		if(strings.size() == 1 ) {
			return strings.iterator().next();
		}
		Collections.sort(strings);
		int count = strings.size();
		
		List<String> previousTreeLayer = new ArrayList<String>(strings);
		List<String> treeLayer = previousTreeLayer;
		
		while(count > 1) {
			treeLayer = new ArrayList<String>();
			for(int left=0; left < previousTreeLayer.size(); left += 2) {
				int right = Math.min(left + 1, previousTreeLayer.size() - 1);
				treeLayer.add(getHashBase58Encoded(previousTreeLayer.get(left) + previousTreeLayer.get(right)));
			}
			count = treeLayer.size();
			previousTreeLayer = treeLayer;
		}
		
		String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : "";
		return merkleRoot;
	}

	public static List<SimpleEntry<String, String>> getMerklePath(String key, List<String> strings, String string) {
		List<SimpleEntry<String, String>> result = new ArrayList<>();
		if(strings == null || strings.isEmpty()) {
			return result;
		}

		if(!strings.contains(string)) {
			return result;
		}

		if(strings.size() == 1) {
			result.add(new SimpleEntry<String, String>(key, string));
			return result;
		}
		
		Collections.sort(strings);
		int count = strings.size();
		
		List<String> previousTreeLayer = new ArrayList<String>(strings);
		List<String> treeLayer = previousTreeLayer;
		
		List<String> keys = new ArrayList<>();
		List<String> values  = new ArrayList<>();
		String str = string;
		values.add(0, str);
		while(count > 1) {
			treeLayer = new ArrayList<String>();
			for(int left=0; left < previousTreeLayer.size(); left += 2) {
				int right = Math.min(left + 1, previousTreeLayer.size() - 1);
				String hash = getHashBase58Encoded(previousTreeLayer.get(left) + previousTreeLayer.get(right));
				if(str.equals(previousTreeLayer.get(left)) || str.equals(previousTreeLayer.get(right))) {
					if(str.equals(previousTreeLayer.get(left))) {
						keys.add(0, "1");
						values.add(0, previousTreeLayer.get(right));
					} else {
						keys.add(0, "0");
						values.add(0, previousTreeLayer.get(left));
					}
					str = hash;
				}
				treeLayer.add(hash);
			}
			count = treeLayer.size();
			previousTreeLayer = treeLayer;
		}

		if(keys.get(keys.size() - 1).equals("0")) {
			keys.add("1");
		} else {
			keys.add("0");
		}
		
		String strKey = flipLast(key);
		for(int i=0; i< keys.size() - 2; i++) {
			strKey = flipLast(strKey) + keys.get(i);
			result.add(new SimpleEntry<String, String>(strKey, values.get(i)));
		}
		strKey = flipLast(strKey);
		result.add(new SimpleEntry<String, String>(strKey + keys.get(result.size()), values.get(result.size())));
		result.add(new SimpleEntry<String, String>(strKey + keys.get(result.size()), values.get(result.size())));
		
		return result;
	}
	
	private static String flipLast(String key) {
		if("".equals(key)) {
			return "";
		}
		char lastChar = key.charAt(key.length() - 1);
		return key.substring(0, key.length() - 1) + (lastChar == '0'? "1": "0");
	}
	
	public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
		Signature dsa;
		byte[] output = new byte[0];
		try {
			dsa = Signature.getInstance("ECDSA", "BC");
			dsa.initSign(privateKey);
			byte[] strByte = input.getBytes(StandardCharsets.UTF_8);
			dsa.update(strByte);
			byte[] realSig = dsa.sign();
			output = realSig;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return output;
	}
	
	public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
		try {
			Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
			ecdsaVerify.initVerify(publicKey);
			ecdsaVerify.update(data.getBytes(StandardCharsets.UTF_8));
			boolean result = ecdsaVerify.verify(signature);
			return result;
		} catch (Exception e) {
			logger.error("publicKey={}, data={}, signature={}", publicKey, data, signature);
			logger.error(e.getMessage(), e);
			return false;
		}
	}
	
	public static Signature getSignature(PrivateKey privateKey) {
		Signature dsa;
		try {
			dsa = Signature.getInstance("ECDSA", "BC");
			dsa.initSign(privateKey);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return dsa;
	}

}
