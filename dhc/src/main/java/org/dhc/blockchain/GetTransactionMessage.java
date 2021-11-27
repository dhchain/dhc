package org.dhc.blockchain;

import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Constants;
import org.dhc.util.Message;
import org.dhc.util.Registry;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

public class GetTransactionMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private String outputBlockHash;
	private String outputTransactionId;
	private DhcAddress from;
	private DhcAddress to;

	public GetTransactionMessage(String outputBlockHash, String outputTransactionId, DhcAddress from, DhcAddress to) {
		this.outputBlockHash = outputBlockHash;
		this.outputTransactionId = outputTransactionId;
		this.from = from;
		this.to = to;
	}

	@Override
	public void process(Peer peer) {
		if (alreadySent(toString(), Constants.MINUTE)) {
			return;
		}
		logger.trace("Received GetTransactionMessage blockHash={}, transactionId={}, from={}, to={}", outputBlockHash, outputTransactionId, from, to);
		
		Transaction transaction = Blockchain.getInstance().getTransaction(outputTransactionId, outputBlockHash);
		if(transaction == null) {
			send();
			return;
		}
		
		GetTransactionReplyMessage reply = new GetTransactionReplyMessage(transaction, from);
		reply.setCorrelationId(getCorrelationId());// this way all replies have the same identifier to avoid flooding network 
		reply.send();
	}
	
	public void send() {
		Network network = Network.getInstance();
		if(DhcAddress.getMyDhcAddress().equals(from)) {
			GetTransaction getTransaction = Registry.getInstance().getGetTransaction();
			Transaction transaction = getTransaction.get(outputBlockHash, outputTransactionId);
			if(transaction != null) {
				logger.info("GetTransactionMessage found in cache transaction {}", transaction);
				getTransaction.process(transaction);
				return;
			}
		}
		String str = String.format("GetTransactionMessage %s-%s", outputBlockHash, outputTransactionId);
		if (alreadySent(str, Constants.MINUTE)) {
			return;
		}
		network.sendToAddress(to, this);
		logger.info("Sent GetTransactionMessage blockHash={}, transactionId={}, from={}, to={}", outputBlockHash, outputTransactionId, from, to);
	}
	
	@Override
	public String toString() {
		String str = String.format("GetTransactionMessage %s", getCorrelationId());
		return str;
	}


}
