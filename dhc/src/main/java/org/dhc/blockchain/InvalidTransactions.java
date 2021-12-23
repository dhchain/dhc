package org.dhc.blockchain;

import java.util.HashSet;
import java.util.Set;

import org.dhc.network.Network;
import org.dhc.persistence.BlockStore;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class InvalidTransactions {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private Block block;

	public InvalidTransactions(Block block) {
		this.block = block;
	}

	public void process() {
		ThreadExecutor.getInstance().execute(new DhcRunnable("processInvalidTransactions") {
			public void doRun() {
				doProcess();
			}
		});
	}
	private void doProcess() {
		for(Transaction transaction: block.getAllTransactions()) {
			processTransaction(transaction);
		}
		
	}

	private void processTransaction(Transaction transaction) {

		Block previousBlock = BlockStore.getInstance().getByBlockhash(block.getPreviousHash());
		if(previousBlock == null) {
			return;
		}
		
		transaction.computeFullMerklePath(block);
		
		Set<String> blockhashes = Blockchain.getInstance().getBranchBlockhashes(previousBlock);
		long minCompeting = BlockStore.getInstance().getMinCompeting();
		long branchIndex = minCompeting == 0 || minCompeting > previousBlock.getIndex() ? previousBlock.getIndex() : minCompeting;
		
		Set<String> outputIds = new HashSet<>();
		for (Transaction t : block.getAllTransactions()) {
			for(TransactionOutput output: t.getOutputs()) {
				outputIds.add(output.getOutputId());
			}
		}
		
		for (TransactionInput input : transaction.getInputs()) {
			//skip if dependency is on another transaction in this set of transactions
			if(outputIds.contains(input.getOutputId())) {
				continue;
			}
			
			// this checks that all transactions inputs come from the branch, no other
			// branches
			// but there is a possibility that some inputs already used in the branch
			if (input.getOutputBlockIndex() >= branchIndex && !blockhashes.contains(input.getOutputBlockHash())) {
				logger.info("Input does not come from the branch, input{} transaction {} this block {}", input, transaction, previousBlock);
				logger.info("branchIndex = {}, blockhashes = {}", branchIndex, blockhashes);
				logger.info("Output for the input comes from block {}-{}, it is not from the same branch as this block {}-{}", 
						input.getOutputBlockIndex(), input.getOutputBlockHash(), previousBlock.getIndex(), previousBlock.getBlockHash());
				
				Network.getInstance().sendToAllPeers(new InputNotFromBranchMessage(transaction));
				break;
			}
			if (input.wasUsedInBranch(branchIndex, blockhashes)) {
				logger.trace("Input was already used in the branch {}, transaction {}, this block {}", input, transaction, previousBlock);
				
				Set<TransactionInput> set = Blockchain.getInstance().getByOutputId(input.getOutputId());
				for(TransactionInput inputWitness: set) {
					if(inputWitness.getInputBlockIndex() < branchIndex || blockhashes.contains(inputWitness.getInputBlockHash())) {
						Set<Transaction> transactions = Blockchain.getInstance().getTransaction(inputWitness.getInputTransactionId());
						logger.trace("Found {} transaction for input transaction id {}", transactions.size(), inputWitness.getInputTransactionId());
						for (Transaction transactionWitness : transactions) {
							logger.trace("Transaction {}", transactionWitness);
							transactionWitness.computeFullMerklePath(BlockStore.getInstance().getByBlockhash(transactionWitness.getBlockHash()));
							Network.getInstance().sendToAllPeers(new InputWasUsedMessage(transaction, input, transactionWitness, inputWitness));

							break;
						}
						logger.trace("Duplicate input                      {}", inputWitness);
						break;
					}
				}
				break;
			}
		}
	}
}
