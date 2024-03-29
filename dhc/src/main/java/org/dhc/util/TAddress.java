package org.dhc.util;

import java.math.BigInteger;

/**
 * Truncated DhcAddress. Contains first 4 bytes of DhcAddress (first 32 bits)
 * 
 * 
 * @author dhc
 *
 */
public class TAddress {
	
	private String address;

	public TAddress(String address) {
		
		this.address = address;
	}
	
	public String getBinary() {
		return CryptoUtil.getBinaryRepresentation(address);
	}
	
	public String getBinary(int size) {
		return getBinary().substring(0, size);
	}
	
	public long toLong(int size) {
		return new BigInteger(getBinary(size), 2).longValue();
	}
	
	public int compareDistance(TAddress address1, TAddress address2) {
		return xor(address1).compareTo(xor(address2));
	}
	
	public Long xor(TAddress other) {
		String binary = getBinary();
		String binaryOther = other.getBinary();
		String result = "";
		for(int i = 0; i < binary.length(); i++) {
			char ch1 = binary.charAt(i);
			char ch2 = binaryOther.charAt(i);
			result = result + (ch1 == ch2? '0': '1');
		}
		return Long.parseLong(result, 2);
	}
	
	public TAddress xor(int index) {
		String binary = getBinary();
		if(binary.length() < index) {
			return this;
		}
		String xorBinary = binary.substring(0, index);
		for(int i = index; i < binary.length(); i++) {
			xorBinary = xorBinary + (binary.charAt(i) == '0'? "1": "0");
		}
		return new TAddress(CryptoUtil.fromBinaryString(xorBinary));
	}

	public String getAddress() {
		return address;
	}
	
	public static TAddress getMyTAddress() {
		return Wallet.getInstance().getDhcAddress().getTAddress();
	}
	
	public int getBucketIndex(TAddress tAddress, int power) {
		String binary1 = getBinary();
		String binary2 = tAddress.getBinary();
		for(int i = 0; i < power; i++) {
			if(binary1.charAt(i) != binary2.charAt(i)) {
				return i;
			}
		}
		return power;
	}
	
	public boolean isMyKey(String key) {
		if(key == null) {
			return false;
		}
		return getBinary().startsWith(key);
	}
	
	@Override
	public int hashCode() {
		return address.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof TAddress)) {
			return false;
		}
		TAddress other = (TAddress) obj;
		return address.equals(other.address);
	}
	
	@Override
	public String toString() {
		return address;
	}

}
