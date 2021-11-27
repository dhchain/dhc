package org.dhc.blockchain;

import java.nio.charset.StandardCharsets;
import java.security.Signature;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Base58;
import org.dhc.util.Constants;
import org.dhc.util.CryptoUtil;
import org.dhc.util.Message;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Wallet;

public class RollbackMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private long index;
	private String signature;
	private String bucketKey;

	public RollbackMessage(long index, String bucketKey) {
		this.index = index;
		this.bucketKey = bucketKey;
		sign();
	}

	@Override
	public void process(Peer peer) {
		if (!isValid()) {
			return;
		}
		Network.getInstance().sendToAllPeers(this);
		if(bucketKey != null && !DhcAddress.getMyDhcAddress().isMyKey(bucketKey)) {
			return;
		}
		Blockchain.getInstance().removeByIndex(index);
	}
	
	private boolean isValid() {
		long timeDifference = Math.abs(System.currentTimeMillis() - getTimestamp());
		if(timeDifference > Constants.MINUTE) {
			return false;
		}
		if (alreadySent(toString())) {
			return false;
		}
		if (!verifySignature()) {
			return false;
		}
		return true;
	}
	
	private void sign() {
		Signature dsa = CryptoUtil.getSignature(Wallet.getInstance().getPrivateKey());
		String data = getDataToSign();
		
		try {
			dsa.update(data.getBytes(StandardCharsets.UTF_8));
			byte[] signature = dsa.sign();
			this.signature = Base58.encode(signature);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private boolean verifySignature() {
		String data = getDataToSign();
		boolean result = CryptoUtil.verifyECDSASig(Constants.PUBLIC_KEY, data, Base58.decode(signature));
		if(result) {
			return true;
		}
		return false;
	}
	
	private String getDataToSign() {
		return index + (bucketKey == null? "": bucketKey) + getTimestamp();
	}
	
	public String toString() {
		return String.format("RollbackMessage %s-%s", index, getTimestamp());
	}

}
