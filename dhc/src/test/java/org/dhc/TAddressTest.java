package org.dhc;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dhc.util.Constants;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.ExpiringMap;
import org.dhc.util.TAddress;
import org.dhc.util.ThreadExecutor;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class TAddressTest extends TestCase {

	private static final DhcLogger logger = DhcLogger.getLogger();

	static {
		Security.setProperty("crypto.policy", "unlimited");
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public TAddressTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(TAddressTest.class);
	}

	/**
	 * Rigorous Test :-)
	 */
	public void testApp() {
		DhcAddress dhcAddress = new DhcAddress("8aTbk8t8Mc23x2mHRskTfESGbWd9gFwCz5oo");
		TAddress tAddress = dhcAddress.getTAddress();
		logger.info("dhcAddress={}", dhcAddress);
		logger.info("tAddress  ={}", tAddress);
		logger.info("dhcAddress.getBinary()={}", dhcAddress.getBinary());
		logger.info("tAddress.getBinary()  ={}", tAddress.getBinary());
		assertTrue(dhcAddress.getBinary().startsWith(tAddress.getBinary()));
	}
	
	public void testExpiringMap() {
		final ExpiringMap<String, String> expiringMap =  new ExpiringMap<>(Constants.MINUTE);
		expiringMap.put("test", "test1");
		expiringMap.put("test", "test2");
		ThreadExecutor.sleep(Constants.MINUTE * 2);
	}
	
	public void testXorInt() {
		DhcAddress dhcAddress = new DhcAddress("8aTbk8t8Mc23x2mHRskTfESGbWd9gFwCz5oo");
		TAddress tAddress = dhcAddress.getTAddress();
		logger.info("tAddress.getBinary()  ={}", tAddress.getBinary());
		for(int i = 0; i < 50; i++) {
			TAddress xorAddress = tAddress.xor(i);
			Long distance = tAddress.xor(xorAddress);
			logger.info("i={}\t distance={}\t\t\t\t xorAddress.getBinary() = {}", i, distance, xorAddress.getBinary());
		}
			
	}
	
}
