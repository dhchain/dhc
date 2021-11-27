package org.dhc.util;

import java.math.BigInteger;

public class DhcAddress {
	
	private String address;

	public DhcAddress(String address) {
		
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

	@Override
	public String toString() {
		return address;
	}
	
	public int getBucketIndex(DhcAddress dhcAddress, int power) {
		String binary1 = getBinary();
		String binary2 = dhcAddress.getBinary();
		for(int i = 0; i < power; i++) {
			if(binary1.charAt(i) != binary2.charAt(i)) {
				return i;
			}
		}
		return power;
	}
	
	public boolean isFromTheSameShard(DhcAddress dhcAddress, int power) {
		String binary1 = getBinary().substring(0, power);
		String binary2 = dhcAddress.getBinary().substring(0, power);
		return binary1.equals(binary2);
	}
	
	@Override
	public int hashCode() {
		return address.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof DhcAddress)) {
			return false;
		}
		DhcAddress other = (DhcAddress) obj;
		return address.equals(other.address);
	}

	public BigInteger toBigInteger() {
		byte[] bytes = Base58.decode(address);
		return new BigInteger(bytes);
	}
	
	private BigInteger xor(DhcAddress other) {
		return toBigInteger().xor(other.toBigInteger());
	}
	
	public int compareDistance(DhcAddress dhcAddress1, DhcAddress dhcAddress2) {
		return xor(dhcAddress1).compareTo(xor(dhcAddress2));
	}
	
	public DhcAddress xor(int index) {
		String binary = getBinary();
		if(binary.length() < index) {
			return this;
		}
		String xorBinary = binary.substring(0, index);
		for(int i = index; i< binary.length(); i++) {
			xorBinary = xorBinary + (binary.charAt(i) == '0'? "1": "0");
		}
		return new DhcAddress(CryptoUtil.fromBinaryString(xorBinary));
	}
	
	public boolean isMyKey(String key) {
		if(key == null) {
			return false;
		}
		return getBinary().startsWith(key);
	}
	
	public static DhcAddress getMyDhcAddress() {
		return Wallet.getInstance().getDhcAddress();
	}
	
	public boolean isDhcAddressValid() {
		return CryptoUtil.isDhcAddressValid(address);
	}

}
