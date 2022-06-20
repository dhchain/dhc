package org.dhc.lite.post;

import java.io.Serializable;

public class Ratee implements Serializable {

	private static final long serialVersionUID = -1993236778248003479L;
	
	private String name;
	private String description;
	private long timeStamp;
	private String transactionId;
	private long totalRating;
	private String creatorDhcAddress;
	
	public String getCreatorDhcAddress() {
		return creatorDhcAddress;
	}
	public void setCreatorDhcAddress(String creatorDhcAddress) {
		this.creatorDhcAddress = creatorDhcAddress;
	}
	
	public long getTotalRating() {
		return totalRating;
	}
	public void setTotalRating(long totalRating) {
		this.totalRating = totalRating;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

}
