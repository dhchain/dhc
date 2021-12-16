package org.dhc.util;

import org.dhc.network.Peer;

public abstract class Message {
	
	
	private static final ExpiringMap<String, String> sentMessages =  new ExpiringMap<String, String>(Constants.MINUTE * 5);
	public static final String NETWORK_IDENTIFIER = CryptoUtil.getRandomString(32);

	private long timestamp = System.currentTimeMillis();
	private String networkIdentifier;
	private TAddress tAddress;
	private String correlationId = CryptoUtil.getRandomString(16);
	private byte[] networkType = Constants.NETWORK_TYPE;
	private String callbackId;
	private int power;
	private boolean inUse;
	private int possiblePower;
	private boolean thin = false;
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public abstract void process(Peer peer);

	public String getNetworkIdentifier() {
		return networkIdentifier;
	}

	public TAddress getDhcAddress() {
		return tAddress;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public byte[] getNetworkType() {
		return networkType;
	}

	public void setNetworkType(byte[] networkType) {
		this.networkType = networkType;
	}

	public String getCallbackId() {
		return callbackId;
	}

	public void setCallbackId(String callbackId) {
		this.callbackId = callbackId;
	}

	public ExpiringMap<String, String> getSentMessages() {
		return sentMessages;
	}

	public int getPower() {
		return power;
	}

	public void setPower(int power) {
		this.power = power;
	}
	
	public boolean alreadySent(String key) {
		ExpiringMap<String, String> sentMessages = getSentMessages();
		synchronized(sentMessages) {
			if(sentMessages.containsKey(key)) {
				return true;
			}
			sentMessages.put(key, key);
			return false;
		}
	}
	
	public boolean alreadySent(String key, long timeout) {
		ExpiringMap<String, String> sentMessages = getSentMessages();
		synchronized(sentMessages) {
			if(sentMessages.containsKey(key)) {
				return true;
			}
			sentMessages.put(key, key, timeout);
			return false;
		}
	}

	public int getPossiblePower() {
		return possiblePower;
	}

	public void setPossiblePower(int possiblePower) {
		this.possiblePower = possiblePower;
	}

	public void setNetworkIdentifier(String networkIdentifier) {
		this.networkIdentifier = networkIdentifier;
	}

	public void setTAddress(TAddress tAddress) {
		this.tAddress = tAddress;
	}

	public void failedToSend(@SuppressWarnings("unused") Peer peer, @SuppressWarnings("unused") Exception e) {
		
	}

	public boolean isInUse() {
		return inUse;
	}

	public void setInUse(boolean inUse) {
		this.inUse = inUse;
	}

	public boolean isThin() {
		return thin;
	}

	public void successfullySent(@SuppressWarnings("unused") Peer peer) {
		
		
	}

}
