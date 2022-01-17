package org.dhc.blockchain;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.dhc.network.ChainSync;
import org.dhc.network.consensus.BucketHash;
import org.dhc.persistence.BlockStore;
import org.dhc.util.Base58;
import org.dhc.util.Coin;
import org.dhc.util.Constants;
import org.dhc.util.CryptoUtil;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.Registry;
import org.dhc.util.StringUtil;
import org.dhc.util.Wallet;

public class Transaction {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static int APP_CODE_LIMIT = 5;
	
	private String transactionId;
	private PublicKey sender;
	private DhcAddress senderAddress;
	private DhcAddress receiver;
	private String blockHash;
	private List<SimpleEntry<String, String>> merklePath;
	private Set<TransactionInput> inputs = new LinkedHashSet<TransactionInput>();
	private Set<TransactionOutput> outputs = new LinkedHashSet<TransactionOutput>();
	private Coin value;
	private Coin fee;
	private String signature;
	private TransactionData expiringData;
	private String app;
	private Keywords keywords;
	private TransactionType type = TransactionType.STANDARD;
	private long blockIndex;
	private long timeStamp;
	
	public Transaction clone() {
		Transaction clone = new Transaction();
		clone.transactionId = transactionId;
		clone.sender = sender;
		clone.senderAddress = senderAddress;
		clone.receiver = receiver;
		clone.blockHash = blockHash;
		clone.blockIndex = blockIndex;
		clone.signature = signature;
		
		clone.merklePath = cloneMerklePath();
		clone.inputs = cloneInputs();
		clone.outputs = cloneOutputs();
		clone.value = value;
		clone.fee = fee;
		if(expiringData != null) {
			clone.expiringData = expiringData.clone();
		}
		
		clone.app = app;
		if(keywords != null) {
			clone.keywords = keywords.clone();
		}
		
		clone.type = type;
		clone.timeStamp = timeStamp;
		
		return clone;
	}
	
	private Set<TransactionOutput> cloneOutputs() {
		Set<TransactionOutput> result = new LinkedHashSet<>();
		for(TransactionOutput output: outputs) {
			result.add(output.clone());
		}
		return result;
	}

	private Set<TransactionInput> cloneInputs() {
		Set<TransactionInput> result = new LinkedHashSet<>();
		for(TransactionInput input: inputs) {
			result.add(input.clone());
		}
		return result;
	}
	
	private List<SimpleEntry<String, String>> cloneMerklePath() {
		if(merklePath == null) {
			return null;
		}
		List<SimpleEntry<String, String>> result = new ArrayList<>();
		for(SimpleEntry<String, String> entry: merklePath) {
			result.add(new SimpleEntry<String, String>(entry.getKey(), entry.getValue()));
		}
		return result;
	}
	
	public Transaction() {
		setTimeStamp(System.currentTimeMillis());
	}
	
	public Coin getOutputsValue() {
		Coin total = Coin.ZERO;
		for(TransactionOutput o : outputs) {
			total = total.add(o.getValue());
		}
		return total;
	}
	
	public boolean verifySignature() {
		if(isPruning()) {
			return true;
		}
		String preHash = getPreHash();
		//logger.info("verifySignature() preHash={}", preHash);
		boolean result = CryptoUtil.verifyECDSASig(sender, preHash, Base58.decode(signature));
		if(result) {
			return true;
		}
		return false;
	}
	
	public void signTransaction(PrivateKey privateKey) {
		String preHash = getPreHash();
		//logger.info("signTransaction() preHash={}", preHash);
		byte[] signature = CryptoUtil.applyECDSASig(privateKey, preHash);
		this.signature = Base58.encode(signature);
		//logger.info("signTransaction() signature={}", this.signature);
	}
	
	private String getPreHash() {
		return getSenderDhcAddress().toString() + receiver + value + fee + getInputsMerkleRoot() + getOutputsMerkleRoot()
				+ (expiringData == null? "": expiringData.getHash()) + (keywords == null? "": keywords.getHash());
	}
	
	private String calculateHash() {
		return CryptoUtil.getHashBase58Encoded(getPreHash() + signature);
	}
	
	public String getInputsMerkleRoot() {
		if(inputs.isEmpty()) {
			return "";
		}
		List<String> strings =  new ArrayList<String>();
		for(TransactionInput input: inputs) {
			strings.add(input.getOutputId());
		}
		return CryptoUtil.getMerkleTreeRoot(strings);
	}
	
	public String getOutputsMerkleRoot() {
		if(outputs.isEmpty()) {
			return "";
		}
		List<String> strings =  new ArrayList<String>();
		for(TransactionOutput output: outputs) {
			strings.add(output.getOutputId());
		}
		return CryptoUtil.getMerkleTreeRoot(strings);
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setSender(PublicKey sender) {
		this.sender = sender;
		if(sender != null) {
			senderAddress = CryptoUtil.getDhcAddressFromKey(sender);
		}
	}
	
	public void setSender(String sender) throws GeneralSecurityException {
		if(sender != null) {
			setSender(CryptoUtil.loadPublicKey(sender));
		}
	}
	
	public DhcAddress getSenderDhcAddress() {
		return senderAddress;
	}
	
	public int hashCode() {
		return transactionId.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Transaction)) {
			return false;
		}
		Transaction other = (Transaction) obj;
		return transactionId.equals(other.transactionId);
	}
	
	public static String computeHash(String key, Set<Transaction> transactions) {
		//long start = System.currentTimeMillis();
		if(transactions == null || transactions.isEmpty()) {
			return "";
		}
		transactions = filter(key, transactions);
		
		if(transactions.size() == 1) {
			return transactions.iterator().next().getTransactionId();
		}
		if(senderTheSame(transactions)) {
			return getMerkleRoot(new ArrayList<Transaction>(transactions));
		}
		
		String leftKey = key + "0";
		Set<Transaction> leftSet = filter(leftKey, transactions);
		String leftHash = computeHash(leftKey, leftSet);
		String rightKey = key + "1";
		Set<Transaction> rightSet = filter(rightKey, transactions);
		String rightHash = computeHash(rightKey, rightSet);
		if("".equals(leftHash)) {
			return rightHash;
		}
		if("".equals(rightHash)) {
			return leftHash;
		}
		//long start = System.currentTimeMillis();
		String result = CryptoUtil.getHashBase58Encoded(leftHash + rightHash);
		//logger.trace("computeHash for key={} number of transactions {} took {} ms.", key, transactions.size(), System.currentTimeMillis() - start);
		return result;
	}
	
	public static List<SimpleEntry<String, String>> computeMerklePath(String key, Set<Transaction> transactions, Transaction transaction) {
		List<SimpleEntry<String, String>> result = new ArrayList<>();
		if(transactions == null || transactions.isEmpty()) {
			return result;
		}
		transactions = filter(key, transactions);
		if(!transactions.contains(transaction)) {
			return result;
		}

		if(transactions.size() == 1) {
			result.add(new SimpleEntry<String, String>(key, transaction.getTransactionId()));
			return result;
		}
		if(senderTheSame(transactions)) {
			return getMerklePath(key, new ArrayList<Transaction>(transactions), transaction);
		}
		
		String leftKey = key + "0";
		String rightKey = key + "1";
		if(transaction.getSenderDhcAddress().getBinary().startsWith(leftKey)) {
			result.addAll(computeMerklePath(leftKey, transactions, transaction));
			Set<Transaction> rightSet = filter(rightKey, transactions);
			String rightHash = computeHash(rightKey, rightSet);
			result.add(0, new SimpleEntry<String, String>(rightKey, rightHash));
		} else {
			result.addAll(computeMerklePath(rightKey, transactions, transaction));
			Set<Transaction> leftSet = filter(leftKey, transactions);
			String leftHash = computeHash(leftKey, leftSet);
			result.add(0, new SimpleEntry<String, String>(leftKey, leftHash));
		}
		return result;
	}
	
	private static List<SimpleEntry<String, String>> getMerklePath(String key, List<Transaction> transactions, Transaction transaction) {
		List<String> strings = new ArrayList<String>();
		for(Transaction tx: transactions) {
			strings.add(tx.getTransactionId());
		}
		List<SimpleEntry<String, String>> result = CryptoUtil.getMerklePath(key, strings, transaction.getTransactionId());
		return result;
	}

	private static boolean senderTheSame(Set<Transaction> transactions) {
		String senderAddress = null;
		for(Transaction transaction: transactions) {
			if(senderAddress == null) {
				senderAddress = transaction.getSenderDhcAddress().getBinary();
			}
			if(!senderAddress.equals(transaction.getSenderDhcAddress().getBinary())) {
				return false;
			}
		}
		
		return true;
	}
	
	public static Set<Transaction> filter(String key, Set<Transaction> transactions) {
		Set<Transaction> result = new HashSet<>();
		if(transactions == null) {
			return result;
		}
		for(Transaction transaction: transactions) {
			if(transaction.getSenderDhcAddress().getBinary().startsWith(key)) {
				result.add(transaction);
			}
		}
		return result;
	}

	public DhcAddress getReceiver() {
		return receiver;
	}

	public void setReceiver(DhcAddress receiver) {
		this.receiver = receiver;
	}

	@Override
	public String toString() {
		Blockchain blockchain = Blockchain.getInstance();
		int power = blockchain == null? 0: Blockchain.getInstance().getPower();
		return "sender=" + getSenderDhcAddress().getBinary(power) + " receiver=" + getReceiver().getBinary(power)
				+ " transactionId=" + transactionId + (blockHash == null? "": " blockHash=" + getBlockIndex() + "-" + blockHash)
				+ (expiringData == null? "": ", expiringData=" + expiringData) + (app == null? "": ", app=" + app)
				+ (keywords == null? "": ", keywords=" + keywords) + (value == null? "": " value=" + value.toNumberOfCoins()) + (fee == null? "": " fee=" + fee.toNumberOfCoins())
				+ " inputsValue=" + getInputsValue().toNumberOfCoins() + " outputsValue=" + getOutputsValue().toNumberOfCoins()
				+ " type=" + type + " #inputs=" + inputs.size() + " #outputs=" + outputs.size() + " signatureValid=" + verifySignature()
				+ " sender=" + getSenderDhcAddress() + " receiver=" + getReceiver();
	}
	
	public boolean isCrossShard() {
		if(getSenderDhcAddress().equals(receiver)) {
			return false;
		}
		return true;
	}

	public void setBlockHash(String blockHash, long blockIndex) {
		this.blockHash = blockHash;
		this.blockIndex = blockIndex;
		for(TransactionOutput output: outputs) {
			output.setOutputBlockHash(blockHash);
			output.setOutputBlockIndex(blockIndex);
		}
		for(TransactionInput input: inputs) {
			input.setInputBlockHash(blockHash);
			input.setInputBlockIndex(blockIndex);
		}
		if(expiringData != null) {
			expiringData.setBlockHash(blockHash);
		}
		if(keywords != null) {
			keywords.setBlockHash(blockHash);
		}
	}

	public String getBlockHash() {
		return blockHash;
	}
	
	public static String getMerkleRoot(List<Transaction> transactions) {
		List<String> strings = new ArrayList<String>();
		for(Transaction transaction: transactions) {
			strings.add(transaction.getTransactionId());
		}
		String merkleRoot = CryptoUtil.getMerkleTreeRoot(strings);
		return merkleRoot;
	}

	public void setMerklePath(List<SimpleEntry<String, String>> merklePath) {
		this.merklePath = merklePath;
	}

	public List<SimpleEntry<String, String>> getMerklePath() {
		return merklePath;
	}

	public boolean isMerklePathValid() {

		if(getMerklePath() == null) {
			return true;
		}
		
		List<SimpleEntry<String, String>> merklePath = new LinkedList<>(getMerklePath());
		
		SimpleEntry<String, String> first = merklePath.remove(0);

		SimpleEntry<String, String> entry = merklePath.isEmpty()? first: computeHash(merklePath);
		
		if(!first.equals(entry)) {
			logger.info("First {} != entry {}", first, entry);
			return false;
		}
		
		if(!merklePath.get(merklePath.size() - 1).getValue().equals(getTransactionId())) {
			logger.info("Last entry is not the same {} {}", merklePath.get(merklePath.size() - 1).getValue(), getTransactionId());
			return false;
		}

		return true;
	}
	
	public boolean isValid(Set<TransactionOutput> pendingOutputs) {
		
		if(getValue().less(Coin.ZERO)) {
			logger.info("Value {} is less than zero", getValue());
			return false;
		}
		
		if(Coin.ZERO.equals(getValue()) && !isCoinbase()) {
			logger.info("Value {} is zero and not coinbase", getValue());
			return false;
		}
		
		
		
		if(!isMerklePathValid()) {
			logger.info("Merkle path is not valid {}", getMerklePath());
			return false;
		}

		if(!verifySignature()) {
			logger.info("signature is not valid {}", this);
			return false;
		}
		
		Blockchain blockchain = Blockchain.getInstance();
		int power = blockchain == null? 0: blockchain.getPower();
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(getSenderDhcAddress(), power) || isPruning()) {
			for(TransactionInput input: inputs) {
				
				long lastIndex = Math.max(blockchain.getIndex(), ChainSync.getInstance().getLastBlockchainIndex());
				if(lastIndex - input.getOutputBlockIndex() > Constants.MAX_NUMBER_OF_BLOCKS) {
					continue;
				}
				
				long prunedIndex = Registry.getInstance().getCompactor().getPrunedIndex();
				if(input.getOutputBlockIndex() <= prunedIndex) {
					continue;
				}
				
				
				TransactionOutput output = TransactionOutputFinder.getByOutputId(input.getOutputId(), pendingOutputs);
				if(output == null) {
					Block block = BlockStore.getInstance().getByBlockhash(input.getOutputBlockHash());
					if (block != null && block.isPruned()) {
						logger.info("Output block is pruned, returning true because can not verify outputs in it: {}", block);
						continue;
					}
					logger.info("Transaction is no valid power={} {}", power, this);
					logger.info("No output found for input power={} {}", power, input);
					Registry.getInstance().getMissingOutputsForTransaction().put(new TransactionKey(input.getOutputTransactionId(), input.getOutputBlockHash()), this);
					
					input.findMissingOutput();
					
					return false;
				} else if(!output.getRecipient().equals(input.getSender()) && !isPruning()) {
					logger.info("output recipient is not the same as input sender");
					logger.info("Output recipient: {}", output.getRecipient());
					logger.info("Input sender:     {}", input.getSender());
					logger.info("\n");
					return false;
				}
			}
		}
		
		if(!isCoinbase() && !isGenesis()) {
			if(!getInputsValue().equals(getOutputsValue().add(fee))) {
				logger.info("Not valid transaction {}", this);
				return false;
			}
		}
		
		
		if(!isPruningTransactionValid()) {
			return false;
		}
		
		if(isGenesis() && getBlockIndex() != 0) {
			return false;
		}

		return true;
	}
	
	public boolean isPruning() {
		return TransactionType.PRUNING.equals(type);
	}
	
	/**
	 * this function is making sure that pruning transaction is sending coins from the same address to itself only
	 * @return
	 */
	public boolean isPruningTransactionValid() {
		if(!isPruning()) {
			return true; // this function validates only pruning transactions
		}

		if(outputs.size() != 1) {
			logger.error("Number of outputs is not 1 for pruning transaction {}", this);
			return false;
		}
		
		if(!receiver.equals(getSenderDhcAddress())) {
			logger.error("transaction receiver is different from transaction sender");
			return false;
		}
		
		for(TransactionOutput output: outputs) {
			if(!receiver.equals(output.getRecipient())) {
				logger.error("transaction receiver is different from output recipient");
				return false;
			}
		}
		
		for(TransactionInput input: inputs) {
			if(!input.getSender().equals(receiver)) {
				logger.error("Input sender {} != recipient from output {} for pruning transaction {}", input.getSender(), receiver, this);
				return false;
			}
		}
		return true;
	}
	
	public boolean inputsOutputsValid() {

		for(TransactionInput input: inputs) {
			if(!getTransactionId().equals(input.getInputTransactionId())) {
				logger.info("transactionId !=input.getInputTransactionId() for transaction {}, input {}", this, input);
				return false;
			}
			if(!getBlockHash().equals(input.getInputBlockHash())) {
				logger.info("blockhash !=input.getInputBlockHash() for transaction {}, input {}", this, input);
				return false;
			}
			if(!TransactionType.PRUNING.equals(type) && !input.getSender().equals(getSenderDhcAddress())) {
				logger.info("Input sender is not the same as transaction sender");
				return false;
			}
		}
		
		for(TransactionOutput output: outputs) {
			if(!getTransactionId().equals(output.getOutputTransactionId())) {
				logger.info("transactionId !=output.getOutputTransactionId() for transaction {}, output {}", this, output);
				return false;
			}
			if(!getBlockHash().equals(output.getOutputBlockHash())) {
				logger.info("blockhash !=output.getOutputBlockHash() for transaction {}, input {}", this, output);
				return false;
			}
			
			if(!output.getRecipient().equals(getReceiver()) && !output.getRecipient().equals(getSenderDhcAddress())) {
				logger.info("Output recipient is not the same as transaction recipient");
				logger.info("Output recipient:      {}", output.getRecipient());
				logger.info("transaction recipient: {}", getReceiver());
				logger.info("transaction sender: {}", getSenderDhcAddress());
				logger.info("\n");
				return false;
			}
		}
		
		if(!isPruningTransactionValid()) {
			logger.error("Pruning transaction is not valid {}", this);
			return false;
		}
		
		return true;
	}
	
	public boolean hasOutputsForAllInputs(Block block) {
		
		if(!outputBlockHashExist(block.getOutputs())) {
			return false;
		}
		
		int power = block.getPower();
		if (!DhcAddress.getMyDhcAddress().isFromTheSameShard(getSenderDhcAddress(), power) && !isPruning()) {//if different shard we can not determine if it has output or not
			return true;
		}

		for(TransactionInput input: inputs) {
			if(!input.hasOutput(block.getOutputs())) {
				Registry.getInstance().getMissingOutputs().put(new TransactionKey(input.getOutputTransactionId(), input.getOutputBlockHash()), block);
				
				input.findMissingOutput();

				return false;
			}
			TransactionOutput output = TransactionOutputFinder.getByOutputId(input.getOutputId(), block.getOutputs());
			if(output != null && !output.getRecipient().equals(input.getSender()) && !isPruning()) {
				logger.info("output recipient is not the same as input sender");
				logger.info("Output recipient:      {}", output.getRecipient());
				logger.info("input sender:          {}", input.getSender());
				logger.info("\n");
				return false;
			}
		}
		return true;
	}
	
	public boolean hasOutputsForAllInputs(Set<TransactionOutput> pendingOutputs) {
		if(!outputBlockHashExist(pendingOutputs)) {
			return false;
		}
		int power = Blockchain.getInstance().getPower();
		if (!DhcAddress.getMyDhcAddress().isFromTheSameShard(getSenderDhcAddress(), power) && !isPruning()) {//if different shard we can not determine if it has output or not
			return true;
		}
		for(TransactionInput input: inputs) {
			if(!input.hasOutput(pendingOutputs)) {
				
				Registry.getInstance().getMissingOutputsForTransaction().put(new TransactionKey(input.getOutputTransactionId(), input.getOutputBlockHash()), this);
				
				input.findMissingOutput();

				return false;
			}
			TransactionOutput output = TransactionOutputFinder.getByOutputId(input.getOutputId(), pendingOutputs);
			if(output != null && !output.getRecipient().equals(input.getSender()) && !isPruning()) {
				logger.info("output recipient is not the same as input sender");
				logger.info("Output recipient:      {}", output.getRecipient());
				logger.info("input sender:          {}", input.getSender());
				logger.info("\n");
				return false;
			}
		}
		return true;
	}
	
	public boolean outputBlockHashExist(Set<TransactionOutput> pendingOutputs) {
		for(TransactionInput input: inputs) {
			TransactionOutput output = TransactionOutputFinder.getByOutputId(input.getOutputId(), pendingOutputs);
			if(output == null) {
				if(!input.outputBlockHashExists()) {
					logger.info("Output blockhash does not exists for input: {}", input);
					logger.info("Output blockhash does not exists for input for transaction: {}", this);
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean inputAlreadySpent(Set<TransactionOutput> pendingOutputs) {
		if(DhcAddress.getMyDhcAddress().isFromTheSameShard(getSenderDhcAddress(), Blockchain.getInstance().getPower())) {
			for(TransactionInput input: inputs) {
				TransactionOutput output = TransactionOutputFinder.getByOutputId(input.getOutputId(), pendingOutputs);
				if(output == null) {
					logger.trace("Transaction is no valid {}", this);
					logger.trace("No unspent output found for input {}", input);
					return false;
				}
			}
		}
		return true;
	}
	
    public SimpleEntry<String, String> computeHash(List<SimpleEntry<String, String>> path) {
    	int index = path.size() - 1;
    	String hash;
    	SimpleEntry<String, String> entry = path.get(index);
    	while(true) {
    		if(index == 0) {
    			break;
    		}
    		SimpleEntry<String, String> previousEntry = path.get(index - 1);
    		hash = getHash(entry, previousEntry);
    		entry = new SimpleEntry<String, String>(entry.getKey().substring(0, entry.getKey().length() - 1), hash);
    		
    		index--;
    	}
    	//logger.trace("entry: {}", entry);
    	return entry;
    }
    
    private String getHash(SimpleEntry<String, String> entry1, SimpleEntry<String, String> entry2) {
    	if("".equals(entry1.getValue())) {
    		return entry2.getValue();
    	}
    	if("".equals(entry2.getValue())) {
    		return entry1.getValue();
    	}
    	if(entry1.getKey().charAt(entry1.getKey().length() - 1) < entry2.getKey().charAt(entry2.getKey().length() - 1)) {
			return CryptoUtil.getHashBase58Encoded(entry1.getValue() + entry2.getValue());
		} else {
			return CryptoUtil.getHashBase58Encoded(entry2.getValue() + entry1.getValue());
		}
    }

	public void computeFullMerklePath(Block block) {
		BucketHashes bucketHashes = block.getBucketHashes();
		List<SimpleEntry<String, String>> list = bucketHashes.getMerklePath();
		BucketHash last = bucketHashes.getLastBucketHash();
		list.addAll(computeMerklePath(last.getBinaryStringKey(), last.getTransactions(), this));
		setMerklePath(list);
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public void setTransactionId() {
		setTransactionId(calculateHash());
		for (TransactionInput input : inputs) {
			input.setInputTransactionId(transactionId);
		}
		for (TransactionOutput output : outputs) {
			output.setOutputTransactionId(transactionId);
		}

		if(expiringData != null) {
			expiringData.setTransactionId(transactionId);
		}
		if(keywords != null) {
			keywords.setTransactionId(transactionId);
		}
	}

	public PublicKey getSender() {
		return sender;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}
	
	public String getApp() {
		return StringUtil.truncate(app, APP_CODE_LIMIT);
	}

	public void setApp(String app) {
		this.app = StringUtil.truncate(app, APP_CODE_LIMIT);
	}

	public Keywords getKeywords() {
		return keywords;
	}

	public void setKeywords(Keywords keywords) {
		if(keywords != null && keywords.isEmpty()) {
			return;
		}
		if(keywords != null && !keywords.isValid()) {
			throw new RuntimeException("Keywords are not valid");
		}
		this.keywords = keywords;
	}

	public Set<TransactionInput> getInputs() {
		return inputs;
	}

	public Set<TransactionOutput> getOutputs() {
		return outputs;
	}

	public TransactionData getExpiringData() {
		return expiringData;
	}

	public void setExpiringData(TransactionData expiringData) {
		this.expiringData = expiringData;
	}

	public void create(DhcAddress to, Coin value,  Coin fee, TransactionData expiringData, Keywords keywords, Block block) {
		if(fee.less(Coin.ZERO)) {
			throw new CreateTransactionException("fee has to be positive");
		}
		if(value.lessOrEqual(Coin.ZERO)) {
			throw new CreateTransactionException("amount has to be positive");
		}
		Wallet wallet = Wallet.getInstance();
		setSender(wallet.getPublicKey());
		receiver = to;
		this.value = value;
		this.fee = fee;
		
		inputs = Blockchain.getInstance().getInputs(value.add(fee), block);

		TransactionOutput output =  new TransactionOutput(to, value);
		outputs.add(output);
		Coin change = getInputsValue().subtract(value.add(fee));
		if (!Coin.ZERO.equals(change)) {
			output = new TransactionOutput(DhcAddress.getMyDhcAddress(), change);
			outputs.add(output);
		}

		this.expiringData = expiringData;
		this.keywords = keywords;
		
		signTransaction(wallet.getPrivateKey());
		setTransactionId();
		logger.trace("Created transaction {}", this);
		for(TransactionInput input: this.inputs) {
			logger.trace("Input {}", input);
		}
	}
	
	public void create(DhcAddress to, Set<TransactionOutput> outputs, String app, TransactionData expiringData) {
		Wallet wallet = Wallet.getInstance();
		setSender(wallet.getPublicKey());
		receiver = to;
		this.fee = Coin.ZERO;
		setApp(app);
		for(TransactionOutput output: outputs) {
			TransactionInput input = output.toInput();
			inputs.add(input);
		}
		
		value = this.getInputsValue();
		TransactionOutput output =  new TransactionOutput(to, value);
		this.outputs.add(output);
		
		this.expiringData = expiringData;
		
		signTransaction(wallet.getPrivateKey());
		setTransactionId();
		logger.info("Created transaction {}", this);
		for(TransactionInput input: this.inputs) {
			logger.trace("Input  {}", input);
		}
		for(TransactionOutput o: this.outputs) {
			logger.trace("Output {}", o);
		}
	}
	
	public void createSplitOutputsTransaction(DhcAddress to, Coin fee, Block block, Coin... value) {
		
		if(fee.less(Coin.ZERO)) {
			throw new CreateTransactionException("fee has to be positive");
		}
		
		for(Coin coin: value) {
			if(coin.lessOrEqual(Coin.ZERO)) {
				throw new CreateTransactionException("amount has to be positive");
			}
		}
		
		Wallet wallet = Wallet.getInstance();
		setSender(wallet.getPublicKey());
		receiver = to;
		
		Coin sum = Coin.ZERO;
		for(Coin coin: value) {
			sum = sum.add(coin);
		}
		
		this.value = sum;
		this.fee = fee;
		
		inputs = Blockchain.getInstance().getInputs(sum.add(fee), block);
		
		for(Coin coin: value) {
			TransactionOutput output =  new TransactionOutput(to, coin);
			outputs.add(output);
		}
		
		Coin change = getInputsValue().subtract(sum.add(fee));
		if (!Coin.ZERO.equals(change)) {
			TransactionOutput output = new TransactionOutput(DhcAddress.getMyDhcAddress(), change);
			outputs.add(output);
		}
		
		signTransaction(wallet.getPrivateKey());
		setTransactionId();
		logger.trace("Created transaction {}", this);
		for(TransactionInput input: this.inputs) {
			logger.trace("Input {}", input);
		}
		for(TransactionOutput o: this.outputs) {
			logger.trace("Output {}", o);
		}
	}
	
	private Coin getInputsValue() {
		Coin sum = Coin.ZERO;
		for(TransactionInput input: inputs) {
			sum = sum.add(input.getValue());
		}
		return sum;
	}
	
	public long getInputOutputBlockIndex() {
		long result = 0;
		for(TransactionInput input: inputs) {
			result = result < input.getOutputBlockIndex()? input.getOutputBlockIndex(): result;
		}
		return result;
	}

	public boolean isCoinbase() {
		return TransactionType.COINBASE.equals(type);
	}
	
	public boolean isGenesis() {
		return TransactionType.GENESIS.equals(type);
	}

	public static Coin collectFees(Set<Transaction> transactions) {
		Coin fees = Coin.ZERO;
		if(transactions == null) {
			return fees;
		}
		for(Transaction transaction: transactions) {
			fees = fees.add(transaction.getFee());
		}
		return fees;
	}
	
	public static Transaction createCoinbase(Coin amount) {
		Transaction transaction = new Transaction();
		transaction.setType(TransactionType.COINBASE);
		Wallet wallet = Wallet.getInstance();
		transaction.setSender(wallet.getPublicKey());
		transaction.setReceiver(wallet.getDhcAddress());
		transaction.setValue(amount);
		transaction.setFee(Coin.ZERO);
		
		if(!Coin.ZERO.equals(transaction.getValue())) {
			TransactionOutput output =  new TransactionOutput(transaction.getReceiver(), transaction.getValue());
			transaction.getOutputs().add(output);
		}

		transaction.signTransaction(wallet.getPrivateKey());
		transaction.setTransactionId();
		return transaction;
	}
	
	public static Transaction createPruning(String recipient, Block block) {
		Transaction transaction = new Transaction();
		transaction.setType(TransactionType.PRUNING);
		transaction.setSenderDhcAddress(new DhcAddress(recipient));
		transaction.setReceiver(new DhcAddress(recipient));
		transaction.inputs = Blockchain.getInstance().getInputs(block, recipient);
		transaction.setValue(transaction.getInputsValue());
		transaction.setFee(Coin.ZERO);
		
		TransactionOutput output =  new TransactionOutput(transaction.getReceiver(), transaction.getValue());
		transaction.getOutputs().add(output);

		transaction.setSignature("");
		transaction.setTransactionId();
		logger.trace("Created pruning transaction {}", transaction);
		for(TransactionInput input: transaction.inputs) {
			logger.trace("Pruning input {}", input);
		}
		return transaction;
	}

	public Coin getFee() {
		return fee;
	}

	public void setFee(Coin fee) {
		this.fee = fee;
	}

	public Coin getValue() {
		return value;
	}

	public void setValue(Coin value) {
		this.value = value;
	}

	public boolean containsInput(TransactionInput input) {
		if(inputs.contains(input)) {
			return true;
		}
		return false;
	}

	public TransactionType getType() {
		return type;
	}

	public void setType(TransactionType type) {
		this.type = type;
	}

	public void setSenderDhcAddress(DhcAddress senderAddress) {
		this.senderAddress = senderAddress;
	}
	
	
	public long getBlockIndex() {
		return blockIndex;
	}

	public void setBlockIndex(long blockIndex) {
		this.blockIndex = blockIndex;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public TransactionOutput getChange() {
		for(TransactionOutput output: outputs) {
			if(output.getValue().equals(getOutputsValue().subtract(getValue()).subtract(getFee())) && output.getRecipient().equals(getSenderDhcAddress())) {
				return output;
			}
		}
		return null;
	}

}
