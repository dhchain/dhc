package org.dhc.lite.post;

import java.io.Serializable;

public class Rating implements Serializable {

	private static final long serialVersionUID = -8591771215382205115L;
	
	private String rater;
	private String ratee;
	private String comment;
	private long timeStamp;
	private String rate;
	private String transactionId;
	
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	public String getRater() {
		return rater;
	}
	public void setRater(String rater) {
		this.rater = rater;
	}
	public String getRatee() {
		return ratee;
	}
	public void setRatee(String ratee) {
		this.ratee = ratee;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	public String getRate() {
		return rate;
	}
	public void setRate(String rate) {
		this.rate = rate;
	}
	
	public String getDisplayRate() {
		return "Yes".equals(rate)? "Positive": "Negative" ;
	}
}
