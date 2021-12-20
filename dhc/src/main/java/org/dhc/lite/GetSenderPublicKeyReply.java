package org.dhc.lite;

import java.security.PublicKey;

import org.dhc.network.Peer;
import org.dhc.util.Message;

public class GetSenderPublicKeyReply extends Message {

	private PublicKey publicKey;

	public GetSenderPublicKeyReply(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	@Override
	public void process(Peer peer) {
		

	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

}
