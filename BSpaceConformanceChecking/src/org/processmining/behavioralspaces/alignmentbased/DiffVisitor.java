package org.processmining.behavioralspaces.alignmentbased;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.diff.CommandVisitor;


public class DiffVisitor implements CommandVisitor {

	private ArrayList<String> add;
	private ArrayList<String> del;
	private Map<Character, String> charMap;
	
  public DiffVisitor(Map<Character, String> charMap) {
    add = new ArrayList<String>();
    del = new ArrayList<String>();
    this.charMap = charMap;
  }

  public void visitInsertCommand(Object object) {
	  add.add(charMap.get((char) object));
  }

  public void visitKeepCommand(Object object) {
	  
  }

  public void visitDeleteCommand(Object object) {
	  del.add(charMap.get((char) object));
  }

  private void display(String commandName, Object object) {
    System.out.println(commandName + " " + object + ": " + this);
  }

  public Set<String> affectedActivities() {
	  Set<String> res = new HashSet<String>(add);
	  res.addAll(del);
	  return res;
  }

  public void printResult() {
	  System.out.println("rem from s1: " + del.toString());
	  System.out.println("added to s1: " + add.toString());
  }

}
