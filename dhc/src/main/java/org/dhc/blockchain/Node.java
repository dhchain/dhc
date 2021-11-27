package org.dhc.blockchain;

import java.util.HashSet;
import java.util.Set;

public class Node {
	
	private Block block;
	private Node parent;
	private Set<Node> children = new HashSet<>();
	
	public Block getBlock() {
		return block;
	}
	public void setBlock(Block block) {
		this.block = block;
	}
	public Node getParent() {
		return parent;
	}
	public void setParent(Node parent) {
		this.parent = parent;
	}
	public Set<Node> getChildren() {
		return children;
	}
	public void setChildren(Set<Node> children) {
		this.children = children;
	}
	
	public boolean isGenesis() {
		return block.getIndex() == 0;
	}

}
