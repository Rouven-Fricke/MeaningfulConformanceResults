package org.processmining.behavioralspaces.alignmentbased;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityToCharMap {

	Map<String, Character> charMap;
	Map<Character, String> inverseMap;
	
	public ActivityToCharMap(List<String> activities) {
		charMap = new HashMap<String, Character>();
		inverseMap = new HashMap<Character, String>();
		for (int i = 0; i < activities.size(); i++) {
			String act = activities.get(i);
			char c = (char) (i + 48);
			charMap.put(act, c);
			inverseMap.put(c, act);
		}
	}
	
	public String getCorrespondingString(char c) {
		return inverseMap.get(c);
	}
	
	public char getCorrespondingChar(String s) {
		return charMap.get(s);
	}
	
	public Map<String, Character> getStringToCharMap() {
		return charMap;
	}
	
	public Map<Character, String> getCharToStringMap() {
		return inverseMap;
	}

	
}
