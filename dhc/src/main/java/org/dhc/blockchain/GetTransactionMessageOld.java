package org.dhc.blockchain;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.DhcLogger;

public class GetTransactionMessageOld extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private String outputBlockHash;
	private String outputTransactionId;

	public GetTransactionMessageOld(String outputBlockHash, String outputTransactionId) {
		this.outputBlockHash = outputBlockHash;
		this.outputTransactionId = outputTransactionId;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("Received GetTransactionMessageOld blockHash={}, transactionId={}", outputBlockHash, outputTransactionId);
		Transaction transaction = Blockchain.getInstance().getTransaction(outputTransactionId, outputBlockHash);
		if(transaction == null) {
			return;
		}
		
		peer.send(new GetTransactionReplyMessageOld(transaction));
	}
	
	public void send() {
		GetTransaction getTransaction = Registry.getInstance().getGetTransaction();
		Transaction transaction = getTransaction.get(outputBlockHash, outputTransactionId);
		if(transaction != null) {
			logger.info("GetTransactionMessageOld found in cache transaction {}", transaction);
			getTransaction.process(transaction);
			return;
		}
		Network.getInstance().sendToAllMyPeers(this);
		logger.info("Sent GetTransactionMessageOld blockHash={}, transactionId={}", outputBlockHash, outputTransactionId);
		getTransaction.addCount(outputBlockHash, outputTransactionId);
	}


}
