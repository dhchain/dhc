package org.dhc.network.consensus;

public class BlockchainIndexStaleException extends ResetMiningException {

	private static final long serialVersionUID = 4489519773614311561L;

	public BlockchainIndexStaleException(String message) {
		super(message);
	}

}
