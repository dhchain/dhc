package org.dhc;

import java.util.HashSet;
import java.util.Set;

import org.dhc.blockchain.Transaction;
import org.dhc.blockchain.TransactionInput;
import org.dhc.blockchain.TransactionOutput;
import org.dhc.persistence.TransactionDependencySorter;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class TransactionDependencySorterTest extends TestCase {

	private static final DhcLogger logger = DhcLogger.getLogger();

	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public TransactionDependencySorterTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(TransactionDependencySorterTest.class);
	}

	/**
	 * Rigorous Test :-)
	 */
	public void testApp() {
		Set<Transaction> transactions = new HashSet<>();
		DhcAddress address = new DhcAddress("4TTvUxkHiaFaxqwiYHwARYb4WLCsaAHKnri3");
		
		Transaction transaction = new Transaction();
		transaction.setTransactionId("transaction1");
		TransactionInput input = new TransactionInput();
		input.setOutputId("input1");
		transaction.getInputs().add(input);
		TransactionOutput output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("outputId11");
		transaction.getOutputs().add(output);
		output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("outputId12");
		transaction.getOutputs().add(output);
		transactions.add(transaction);
		
		transaction = new Transaction();
		transaction.setTransactionId("transaction2");
		input = new TransactionInput();
		input.setOutputId("outputId11");
		transaction.getInputs().add(input);
		output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("outputId2");
		transaction.getOutputs().add(output);
		transactions.add(transaction);
		
		transaction = new Transaction();
		transaction.setTransactionId("transaction3");
		input = new TransactionInput();
		input.setOutputId("outputId12");
		transaction.getInputs().add(input);
		output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("outputId3");
		transaction.getOutputs().add(output);
		transactions.add(transaction);
		
		transaction = new Transaction();
		transaction.setTransactionId("transaction4");
		input = new TransactionInput();
		input.setOutputId("outputId4");
		transaction.getInputs().add(input);
		output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("input1");
		transaction.getOutputs().add(output);
		transactions.add(transaction);
		
		transaction = new Transaction();
		transaction.setTransactionId("transaction5");
		input = new TransactionInput();
		input.setOutputId("outputId2");
		transaction.getInputs().add(input);
		input = new TransactionInput();
		input.setOutputId("outputId3");
		transaction.getInputs().add(input);
		output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("outputId5");
		transaction.getOutputs().add(output);
		transactions.add(transaction);
		
		transaction = new Transaction();
		transaction.setTransactionId("transaction6");
		input = new TransactionInput();
		input.setOutputId("input61");
		transaction.getInputs().add(input);
		input = new TransactionInput();
		input.setOutputId("input62");
		transaction.getInputs().add(input);
		output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("outputId6");
		transaction.getOutputs().add(output);
		transactions.add(transaction);
		
		transaction = new Transaction();
		transaction.setTransactionId("transaction7");
		input = new TransactionInput();
		input.setOutputId("outputId6");
		transaction.getInputs().add(input);
		output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("outputId7");
		transaction.getOutputs().add(output);
		transactions.add(transaction);
		
		long start = System.currentTimeMillis();
		Set<Transaction> set = new TransactionDependencySorter(transactions).sortByInputsOutputs();
		logger.info("Sort took {}ms", System.currentTimeMillis() - start);
		
		for(Transaction t: set) {
			logger.info("{}", t.getTransactionId());
			for(TransactionInput i: t.getInputs()) {
				logger.info("\tinput id = {}", i.getOutputId());
			}
			for(TransactionOutput o: t.getOutputs()) {
				logger.info("\toutput id = {}", o.getOutputId());
			}
		}
		
		
		//assertTrue(dhcAddress.getBinary().startsWith(tAddress.getBinary()));
	}
	
	public void testSingleTransaction() {
		Set<Transaction> transactions = new HashSet<>();
		DhcAddress address = new DhcAddress("4TTvUxkHiaFaxqwiYHwARYb4WLCsaAHKnri3");
		
		Transaction transaction = new Transaction();
		transaction.setTransactionId("transaction1");
		TransactionInput input = new TransactionInput();
		input.setOutputId("input1");
		transaction.getInputs().add(input);
		TransactionOutput output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("outputId11");
		transaction.getOutputs().add(output);
		output = new TransactionOutput(address, Coin.ONE);
		output.setOutputId("outputId12");
		transaction.getOutputs().add(output);
		transactions.add(transaction);
		
		long start = System.currentTimeMillis();
		Set<Transaction> set = new TransactionDependencySorter(transactions).sortByInputsOutputs();
		logger.info("Sort took {}ms", System.currentTimeMillis() - start);
		
		assertTrue(set.equals(transactions));
		
	}

	
}
