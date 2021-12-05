package org.dhc.util;

import java.math.BigInteger;

public class Difficulty {
	
	public static BigInteger getTarget(long bits) {
		String hex = Long.toString(bits, 16);
		int size = Integer.parseInt(hex.substring(0, 2), 16);
		BigInteger word = new BigInteger(hex.substring(2), 16);
		BigInteger result = word.multiply(new BigInteger("2").pow(8 * (size-3)));
		return result;
	}
	
	public static long convertDifficultyToBits(double difficulty) { 
		long word;
		int shiftBytes;
	    for (shiftBytes = 1; true; shiftBytes++) {
	        word = (long) ((0x00ffff * Math.pow(0x100, shiftBytes)) / difficulty);
	        if (word >= 0xffff) break;
	    }

	    word &= 0xffffff;
	    int size = 0x1d - shiftBytes;

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
		int exponent_diff  = (int)(8 * (0x1D - ((bits >> 24) & 0xFF)));
		double significand = bits & 0xFFFFFF; 
		return Math.scalb(0x00FFFF / significand, exponent_diff);
	}

}
