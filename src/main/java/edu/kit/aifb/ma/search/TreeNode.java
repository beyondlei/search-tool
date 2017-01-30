package edu.kit.aifb.ma.search;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
  private List<TreeNode> children = new ArrayList<>();

  private TreeNode parent;

  private Object data;

  public TreeNode(TreeNode parent, Object data) {
    this.parent = parent;
    this.data = data;
  }

  public List<TreeNode> getChildren() {
    return children;
  }

  public void setChildren(List<TreeNode> children) {
    this.children = children;
  }

  public void addChild(TreeNode tn) {
    children.add(tn);
  }

  public TreeNode getParent() {
    return parent;
  }

  public Object getData() {
    return data;
  }

}
