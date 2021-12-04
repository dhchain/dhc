package org.dhc.util;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

public class Constants {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final Configurator configurator = Configurator.getInstance();
	public static final int k = configurator.getIntProperty("org.dhc.util.Constants.k", 8);
	public static final int maxNumberOfPeersToSend = configurator.getIntProperty("org.dhc.util.Constants.maxNumberOfPeersToSend", 5);
	public static final int MAX_CONN = configurator.getIntProperty("org.dhc.util.Constants.max.number.of.connections", 500);
	public static final long SECOND = 1000;
	public static final long MINUTE = SECOND * 60;
	public static final long HOUR = MINUTE * 60;
	public static final byte[] MAINNET = new byte[]{0, 0};
	public static final byte[] NETWORK_TYPE = MAINNET;
	public static final int MAX_NUMBER_OF_BLOCKS = 525600;
	public static String DATABASE;
	public static PublicKey PUBLIC_KEY = getPublicKey();
	public static final boolean showSum = configurator.getBooleanProperty("org.dhc.util.Constants.showSum");
	public static final long INITIAL_BITS = 0x6400ffff;
	
	private static PublicKey getPublicKey() {
		try {
			return CryptoUtil.loadPublicKey("PZ8Tyr4Nx8MHsRAGMpZmZ6TWY63dXWSCwGxR46Y82jX2JcdxKAhmEfci7itk9fVU7VDJDxiJ9gtu8iBd3FYSRiTmyHNeuhiNWisg5npLgVYb99XSg9iocLvf");
		} catch (GeneralSecurityException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
}
