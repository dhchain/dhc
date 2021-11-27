package org.dhc;

import java.security.Security;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dhc.blockchain.Block;
import org.dhc.blockchain.Transaction;
import org.dhc.network.consensus.BucketHash;
import org.dhc.util.Coin;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Wallet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class BucketConsensusesTest extends TestCase {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	static {
		Security.setProperty("crypto.policy", "unlimited");
		Security.addProvider(new BouncyCastleProvider());
	}
	
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public BucketConsensusesTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( BucketConsensusesTest.class );
    }

    /**
     * Rigorous Test :-)
     */
	public void testApp() {
		Block block = new Block();
		block.setBlockHash("");
		block.setIndex(0);
		Wallet wallet = Wallet.getInstance();
		wallet.generateKeyPair();
		DhcAddress dhcAddress = wallet.getDhcAddress();
		Set<Transaction> transactions = new HashSet<>();
		for (int i = 0; i < 3; i++) {
			Transaction transaction = new Transaction();
			transaction.create(CryptoUtil.getDhcAddressFromBytes(CryptoUtil.getRandomBytes(32)), Coin.ONE, Coin.SATOSHI, null, null, block);
		}
		String key = dhcAddress.getBinary(4);
		BucketHash bucketHash = new BucketHash(key, transactions, "");
		recover(bucketHash, dhcAddress);
		assertTrue(bucketHash.isValid());
	}
    
    public void testMerklePath() {
    	Block block = new Block();
		block.setBlockHash("");
		block.setIndex(0);
    	
    	Wallet wallet = Wallet.getInstance();

		Set<Transaction> transactions = new HashSet<>();
		for (int i = 0; i < 129; i++) {
			wallet.generateKeyPair();
			Transaction transaction = new Transaction();
			transaction.create(CryptoUtil.getDhcAddressFromBytes(CryptoUtil.getRandomBytes(32)), Coin.ONE, Coin.SATOSHI, null, null, block);
		}
		Transaction transaction = transactions.iterator().next();
		String key = transaction.getSenderDhcAddress().getBinary(1);
		logger.info("key={}", key);
		List<SimpleEntry<String, String>> path = Transaction.computeMerklePath(key, transactions, transaction);
		logger.info("path={}", path);
		String hash = Transaction.computeHash(key, transactions);
		logger.info("hash={}", hash);
		SimpleEntry<String, String> entry = transaction.computeHash(path);
		assertTrue(key.equals(entry.getKey()));
		assertTrue(hash.equals(entry.getValue()));
    }
    
	private void recover(BucketHash bucketHash, DhcAddress dhcAddress) {
		Set<Transaction> transactions = bucketHash.getTransactionsIncludingCoinbase();
		if(transactions == null) {
			return;
		}

		String key = bucketHash.getBinaryStringKey();

		String myKey = dhcAddress.getBinary(5);
		if(key.equals(myKey) || !myKey.startsWith(key)) {
			return; //bucket for key does not strictly contain bucket for myKey
		}

		String otherKey = null;
		BucketHash otherBucketHash =null;
		BucketHash parent = null;
		while(!key.equals(myKey)) {
			BucketHash myBucketHash = new BucketHash(myKey, transactions, "");
			otherKey = myBucketHash.getKey().getOtherBucketKey().getKey();
			otherBucketHash = new BucketHash(otherKey, transactions, "");
			parent = new BucketHash(myBucketHash, otherBucketHash);
			myKey = parent.getBinaryStringKey();
		}
		if(parent == null) {
			return;
		}
		bucketHash.setLeftRight(parent.getLeft(), parent.getRight());
	}
}







