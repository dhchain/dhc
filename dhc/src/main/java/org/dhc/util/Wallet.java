package org.dhc.util;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.dhc.blockchain.Blockchain;

public class Wallet {
	
	private static Wallet instance =  new Wallet();
	
	public static Wallet getInstance() {
		return instance;
	}
	
	private Wallet() {
		
	}

	private PrivateKey privateKey;
	private PublicKey publicKey;
		
	public void generateKeyPair() {
		Blockchain blockchain = Blockchain.getInstance();
		String bucketKey = blockchain.getLastBlocks().get(0).getBucketKey();
		while (true) {
			KeyPair keyPair = CryptoUtil.generateKeyPair();
			privateKey = keyPair.getPrivate();
			publicKey = keyPair.getPublic();
			if(blockchain.getIndex() == 0) {
				break;
			}
			DhcAddress myAddress = DhcAddress.getMyDhcAddress();
			if(myAddress.getBinary().startsWith(bucketKey)) {
				break;
			}
		}
		
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}
	
	public DhcAddress getDhcAddress() {
		return CryptoUtil.getDhcAddressFromKey(publicKey);
	}
	
}


