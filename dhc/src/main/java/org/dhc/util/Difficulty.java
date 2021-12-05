package org.dhc.util;

import java.math.BigInteger;

public class Difficulty {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	public static final long INITIAL_BITS = 0x6300ffff;
	public static final int SIZE = 0x70; //Satoshi has 0x1D
	
	public static BigInteger getTarget(long bits) {
		String hex = Long.toString(bits, 16);
		int size = Integer.parseInt(hex.substring(0, 2), 16);
		BigInteger word = new BigInteger(hex.substring(2), 16);
		BigInteger result = word.multiply(new BigInteger("2").pow(8 * (size-3)));
		return result;
	}
	
	public static void main(String[] args) {
		long bits = Difficulty.convertDifficultyToBits(Difficulty.getDifficulty(Difficulty.INITIAL_BITS) / Math.pow(2, 30));
		logger.info("bits={}", Long.toString(bits, 16));
	}
	
	public static long convertDifficultyToBits(double difficulty) { 
		long word;
		int shiftBytes;
	    for (shiftBytes = 1; true; shiftBytes++) {
	        word = (long) ((0x00ffff * Math.pow(0x100, shiftBytes)) / difficulty);
	        if (word >= 0xffff) break;
	    }

	    word &= 0xffffff;
	    int size = SIZE - shiftBytes; // originally was 0x1d

	    if ((word & 0x800000) != 0) {
	        word >>= 8;
	        size++;
	    }
	    long bits = (size << 24) | word;
	    return bits;
	}
	
	public static boolean checkProofOfWork(long bits, String hashStr) {
		BigInteger hash = new BigInteger(CryptoUtil.getBinaryRepresentation(hashStr));
		return getTarget(bits).compareTo(hash) >= 0;
	}
	
	public static double getDifficulty(long bits) {
		int exponent_diff  = (int)(8 * (SIZE - ((bits >> 24) & 0xFF)));
		double significand = bits & 0xFFFFFF; 
		return Math.scalb(0x00FFFF / significand, exponent_diff);
	}

}
