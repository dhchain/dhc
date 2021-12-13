package org.dhc.util;

import java.math.BigInteger;

import org.dhc.blockchain.Block;
import org.dhc.blockchain.Blockchain;

public class Difficulty {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	public static final long INITIAL_BITS = 0x1f00ffff;
	public static final int SIZE = 0x30; //Satoshi has 0x1D
	
	public static BigInteger getTarget(long bits) {
		String hex = Long.toString(bits, 16);
		int size = Integer.parseInt(hex.substring(0, 2), 16);
		BigInteger word = new BigInteger(hex.substring(2), 16);
		BigInteger result = word.multiply(new BigInteger("2").pow(8 * (size-3)));
		return result;
	}
	
	public static void main(String[] args) {
		long bits = Difficulty.convertDifficultyToBits(Difficulty.getDifficulty(0x1e137853) / Math.pow(2, 2));
		logger.info("bits={}", Long.toString(bits, 16));
		logger.info("target.length()={}", getTarget(bits).toString(2).length());
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
		BigInteger hash = new BigInteger(CryptoUtil.getBinaryRepresentation(hashStr), 2);
		return getTarget(bits).compareTo(hash) >= 0;
	}
	
	public static boolean checkProofOfWork(String hashStr, BigInteger target) {
		BigInteger hash = new BigInteger(CryptoUtil.getBinaryRepresentation(hashStr), 2);
		return target.compareTo(hash) >= 0;
	}
	
	public static double getDifficulty(long bits) {
		if(bits == 0) {
			bits = INITIAL_BITS;
		}
		int exponent_diff  = (int)(8 * (SIZE - ((bits >> 24) & 0xFF)));
		double significand = bits & 0xFFFFFF; 
		return Math.scalb(0x00FFFF / significand, exponent_diff);
	}
	
	public static long getBits(Block block) {
		Blockchain blockchain = Blockchain.getInstance();
		if(blockchain == null || blockchain.getIndex() == 0) {
			return INITIAL_BITS;
		}
		long time = blockchain.getAverageMiningTime(block);
		long averageBits = blockchain.getAverageBits(block);
		double averageDificulty = getDifficulty(averageBits);
		double newDifficulty = averageDificulty / time * Constants.MINUTE;
		long bits = convertDifficultyToBits(newDifficulty);
		bits = Math.min(INITIAL_BITS, bits); // for block bits cannot be bigger that INITIAL_BITS, 
		// which implies there is a minimum difficulty for blocks. For bucketHashes difficulty is fraction of the containing block
		return bits;
	}

}
