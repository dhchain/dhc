package org.dhc.network;

import java.math.BigInteger;

/**
 * key to represent bucket identification. Key could be "" - the whole address space, "0" - half of addresses starting with 0. Opposite of "0" bucket is "1" bucket. 
 * Bucket "" is the opposite of itself. 
 *
 */
public class BucketKey {
	
	private String key;
	
	public BucketKey() {
		
	}
	
	public BucketKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public boolean less(BucketKey other) {
		if("".equals(key)) {
			return false;
		}
		return new BigInteger(key, 2).compareTo(new BigInteger(other.key, 2)) == -1;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof BucketKey)) {
			return false;
		}
		BucketKey other = (BucketKey) obj;
		return key.equals(other.key);
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	public BucketKey getOtherBucketKey() {
		if("".equals(key)) {
			return this;
		}
		StringBuilder b = new StringBuilder(key);
		b.setCharAt(key.length() - 1, key.charAt(key.length() - 1) == '0'? '1': '0');
		String otherKey = b.toString();
		BucketKey other = new BucketKey();
		other.setKey(otherKey);
		return other;
	}
	
	public BucketKey getParentKey() {
		if("".equals(key)) {
			return this;
		}
		BucketKey parent = new BucketKey();
		String parentKey = key.substring(0, key.length() - 1);
		parent.setKey(parentKey);
		return parent;
	}
	
	public BucketKey getLeftKey() {
		BucketKey left = new BucketKey();
		String leftKey = key + "0";
		left.setKey(leftKey);
		return left;
	}
	
	public BucketKey getRightKey() {
		BucketKey right = new BucketKey();
		String rightKey = key + "1";
		right.setKey(rightKey);
		return right;
	}

	@Override
	public String toString() {
		return key;
	}

}
