# Distributed Hash Chain
Distributed hash chain or DHC is an extension of blockchain structure first implemented in bitcoin. The goal of DHC is to improve scalability of a standard blockchain.

One approach to improve scalability is to use sharding where instead of a single blockchain there are multiple blockchains that store different sets of transactions and they are synchronized between each other in some way.

DHC has a single blockchain, but each node only stores a subset of transactions in a given block. Imagine there are two nodes A and B. Address of node A in binary form starts with 0 and address of node B in binary form starts with 1. Then node A will start collecting all pending transactions from senders which address start with 0 and node B will start collecting all pending transactions from senders which address start with 1. Node A will not even receive transactions from senders which address start with 1 thus reducing network traffic. Similarly for node B. How A and B can work together and create a distributed block? Here are the steps.

 1. Node A creates hash of transactions it collected and sends that hash to node B
 2. Node B creates hash of transactions it collected and sends that hash to node A
 3. Both nodes produce combined hash in the same order. First hash from A then hash from B, since address of A starts with 0 and address of B starts with 1.
 4. Both nodes start mining using the same combined hash

When there are more nodes the same approach applies. Any node with address starting with 0 needs to combine hash of transaction it collected with a hash it receives from a node with address starting with 1. It needs to broadcast that information to other nodes on the same partition so they would be able to recover that information once they receive mined block even if that node goes down. To reduce the number of these broadcasts and consequently the number of combinations with other partitions, nodes are required to include small proof of work.

How would we deal when there are $ 2^2 = 4 $ partitions?