package org.dhc.lite;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.dhc.util.Base58;
import org.dhc.util.Coin;
import org.dhc.util.CryptoUtil;
import org.dhc.util.Encryptor;
import org.dhc.util.StringUtil;
import org.dhc.util.DhcLogger;

public class SecureMessage implements Serializable {

	private static final long serialVersionUID = 3594175706393924245L;
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final Encryptor encryptor = new Encryptor();
	
	private PublicKey sender;
	private String text;
	private String recipient;
	private Coin value;
	private Coin fee;
	private long timeStamp;
	private long expire;
	private String transactionId;
	
	public SecureMessage() {
		
	}
	
	public SecureMessage(PublicKey sender, String text) {
		this.sender = sender;
		this.text = text;
	}

	public PublicKey getSender() {
		return sender;
	}
	
	public void setSender(String sender) {
		try {
			this.sender = CryptoUtil.loadPublicKey(sender);
		} catch (GeneralSecurityException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Coin getValue() {
		return value;
	}

	public void setValue(Coin value) {
		this.value = value;
	}

	public Coin getFee() {
		return fee;
	}

	public void setFee(Coin fee) {
		this.fee = fee;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public long getExpire() {
		return expire;
	}

	public void setExpire(long expire) {
		this.expire = expire;
	}
	
	public String getSubject(PrivateKey privateKey) {
        try {
            String str = StringUtil.trimToNull(getText());
            if(str != null) {
                str = new String(encryptor.decryptAsymm(Base58.decode(str), privateKey), StandardCharsets.UTF_8);
                int index = str.indexOf("\n");
                index = index == -1? str.length(): index;
                return str.substring(0, index);
            }
        } catch (GeneralSecurityException e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    public String getBody(PrivateKey privateKey) {
        try {
            String str = StringUtil.trimToNull(getText());
            if(str != null) {
                str = new String(encryptor.decryptAsymm(Base58.decode(str), privateKey), StandardCharsets.UTF_8);
                int index = str.indexOf("\n");
                index = index == -1? str.length(): index;
                return  str.substring(index + 1);
            }
        } catch (GeneralSecurityException e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public String getSenderDhcAddress() {
		
		return CryptoUtil.getDhcAddressFromKey(getSender()).toString();
	}

	public void setSenderPublicKey(PublicKey sender) {
		this.sender = sender;
	}

	
	
}
