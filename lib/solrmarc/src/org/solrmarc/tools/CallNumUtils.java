package org.solrmarc.tools;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import schema.UnicodeCharUtil;

/**
 * Call number utility functions for solrmarc
 * 
 * @author Naomi Dushay, Stanford University
 */

public final class CallNumUtils {
	
	/**
	 * Default Constructor: private, so it can't be instantiated by other objects
	 */	
	private CallNumUtils(){ }
	
    public final static Pattern DEWEY_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d+)?.*");
	/** LC call numbers can't begin with I, O, W, X, or Y */
    public final static Pattern LC_PATTERN = Pattern.compile("^[A-Za-z&&[^IOWXYiowxy]]{1}[A-Za-z]{0,2} *\\d+(\\.\\d+)?.*");

	/**
	 * regular expression string for the required portion of the LC classification
	 *  LC classification is 
	 *    1-3 capital letters followed by  float number (may be an integer)
	 *    optionally followed by a space and then a year or other number, 
	 *      e.g. "1987" "15th"
	 */
	public static String LC_CLASS_REQ_REGEX = "[A-Z]{1,3}\\d+(\\.\\d+)?";

	/**
	 * non-cutter text that can appear before or after cutters
	 */
	public static String NOT_CUTTER = "([\\da-z]\\w*)|([A-Z]\\D+[\\w]*)";
	
	/**
	 * the full LC classification string
	 */
	public static String LC_CLASS = "(" + LC_CLASS_REQ_REGEX + "( +" + NOT_CUTTER + ")?)";
	
	/**
	 * regular expression string for the cutter, without preceding characters 
	 * (such as the "required" period, which is sometimes missing, or spaces).
	 * A Cutter is a single letter followed by digits.  
	 */
	public static String CUTTER_REGEX = "[A-Z]\\d+";
	
	/**
	 * the full LC classification string, followed by the first cutter
	 */
	public static String LC_CLASS_N_CUTTER = LC_CLASS + " *\\.?" + CUTTER_REGEX;
		
	/**
	 * regular expression for Dewey classification.
	 *  Dewey classification is a three digit number (possibly missing leading
	 *   zeros) with an optional fraction portion.
	 */
	public static String DEWEY_CLASS_REGEX = "\\d{1,3}(\\.\\d+)?";
	
	/**
	 * Dewey cutters can have following letters, preceeded by space or not
	 */
	public static String DEWEY_CUTTER_REGEX = CUTTER_REGEX + " *[A-Z]*";
	

	private static Map<Character, Character> alphanumReverseMap = new HashMap<Character, Character>();
	static {
		alphanumReverseMap.put('0', 'Z');
		alphanumReverseMap.put('1', 'Y');
		alphanumReverseMap.put('2', 'X');
		alphanumReverseMap.put('3', 'W');
		alphanumReverseMap.put('4', 'V');
		alphanumReverseMap.put('5', 'U');
		alphanumReverseMap.put('6', 'T');
		alphanumReverseMap.put('7', 'S');
		alphanumReverseMap.put('8', 'R');
		alphanumReverseMap.put('9', 'Q');
		alphanumReverseMap.put('A', 'P');
		alphanumReverseMap.put('B', 'O');
		alphanumReverseMap.put('C', 'N');
		alphanumReverseMap.put('D', 'M');
		alphanumReverseMap.put('E', 'L');
		alphanumReverseMap.put('F', 'K');
		alphanumReverseMap.put('G', 'J');
		alphanumReverseMap.put('H', 'I');
		alphanumReverseMap.put('I', 'H');
		alphanumReverseMap.put('J', 'G');
		alphanumReverseMap.put('K', 'F');
		alphanumReverseMap.put('L', 'E');
		alphanumReverseMap.put('M', 'D');
		alphanumReverseMap.put('N', 'C');
		alphanumReverseMap.put('O', 'B');
		alphanumReverseMap.put('P', 'A');
		alphanumReverseMap.put('Q', '9');
		alphanumReverseMap.put('R', '8');
		alphanumReverseMap.put('S', '7');
		alphanumReverseMap.put('T', '6');
		alphanumReverseMap.put('U', '5');
		alphanumReverseMap.put('V', '4');
		alphanumReverseMap.put('W', '3');
		alphanumReverseMap.put('X', '2');
		alphanumReverseMap.put('Y', '1');
		alphanumReverseMap.put('Z', '0');
	}
	
    
    /** this character will sort first */
    public static char SORT_FIRST_CHAR = Character.MIN_VALUE;
	private static StringBuffer reverseDefault = new StringBuffer(75);
	static {
		for (int i = 0; i < 75; i++) 
			reverseDefault.append(Character.toChars(Character.MAX_CODE_POINT));
	}

//------ public methods --------	
    
	/**
	 * given a possible Library of Congress call number value, determine if it
	 *  matches the pattern of an LC call number
	 */
	public static final boolean isValidLC(String possLCval)
	{
		if (possLCval != null && LC_PATTERN.matcher(possLCval.trim()).matches())
	    	return true;
		return false;
	}

	/**
	 * given a possible Dewey call number value, determine if it
	 *  matches the pattern of an Dewey call number
	 */
	public static final boolean isValidDewey(String possDeweyVal)
	{
	    if (possDeweyVal != null && DEWEY_PATTERN.matcher(possDeweyVal.trim()).matches())
	    	return true;
		return false;
	}

	/**
	 * return the portion of the call number string that occurs before the 
	 *  Cutter, NOT including any class suffixes occuring before the cutter
	 */
	public static final String getPortionBeforeCutter(String callnum) {
	
		// cutter is a single letter followed by digits.
		// there may be a space before a cutter
		// there should be a period, which is followed by a single letter
		//   the period is sometimes missing
		// For Dewey callnumber, there may be a slash instead of a cutter, 
		//  or there might be NO cutter
		String beginCutterRegex = "( +|(\\.[A-Z])| */)";  
		
		String[] pieces = callnum.split(beginCutterRegex);
		if (pieces.length == 0 || pieces[0] == null || pieces[0].length() == 0)
			return null;
		else
			return pieces[0].trim();
	}

	/**
	 * return the portion of the LC call number string that occurs before the 
	 *  Cutter.
	 */
	public static final String getLCB4FirstCutter(String callnum) {
		String result = null;
	    
	    String cutter = getFirstLCcutter(callnum);
	    if (cutter != null) {
	    	// lc class can start with same chars as first cutter: (G384 G3)
	    	int ix = callnum.indexOf(cutter);
	    	String lets = getLCstartLetters(callnum);
	    	if (ix < lets.length())
	    		ix = callnum.indexOf(cutter, lets.length());
	
	    	if (ix > 0) {
	        	result = callnum.substring(0, ix).trim();            	
	            if (result.endsWith("."))
	            	result = result.substring(0, result.length() - 1).trim();
	    	}
	    	else
	    		result = callnum;
	    }
	    else // no cutter 
	    	result = callnum;
	
	    return result;
	}

	/** 
	 * Given a raw LC call number, return the initial letters (before any
	 *  numbers)
	 */
	public static String getLCstartLetters(String rawLCcallnum) {
		String result = null;
		if (rawLCcallnum != null) {
		    String [] lcClass = rawLCcallnum.split("[^A-Z]+");
		    if (lcClass.length > 0)
		    	result = lcClass[0];
		}
		return result;
	}

	/**
	 * return the numeric portion of the required portion of the LC classification.
	 *  LC classification requires
	 *    1-3 capital letters followed by  float number (may be an integer)
	 * @param rawLCcallnum
	 */
	public static String getLCClassDigits(String rawLCcallnum) {
		String result = null;
	
		String rawClass = getLCB4FirstCutter(rawLCcallnum);
		if (rawClass != null) {
		    String [] pieces = rawClass.split("[A-Z ]+");
		    if (pieces.length > 1)
		    	result = pieces[1].trim();
		}
		return result;
	}

	/**
	 * return the string between the LC class number and the cutter, if it
	 *  starts with a digit, null otherwise
	 * @param rawLCcallnum - the entire LC call number, as a string
	 */
	public static String getLCClassSuffix(String rawLCcallnum) {
		String result = null;
		
		String b4cutter = getLCB4FirstCutter(rawLCcallnum);
		if (b4cutter == null)
			return null;
		
	    String classDigits = getLCClassDigits(rawLCcallnum);
	
	    if (classDigits != null) {
	        int reqClassLen = b4cutter.indexOf(classDigits) + classDigits.length();
	
	        if (b4cutter.length() > reqClassLen)
	        	result = b4cutter.substring(reqClassLen).trim();
	    }
	    
	    return result;
	}

	/**
	 * return the first cutter in the LC call number, without the preceding 
	 * characters (such as the "required" period, which is sometimes missing, 
	 * or spaces), or any suffixes
	 * @param rawCallnum - the entire call number, as a string
	 */
	public static String getFirstLCcutter(String rawCallnum) {
		String result = null;
	
		String regex = LC_CLASS + " *\\.?(" + CUTTER_REGEX + ")";
	    Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(rawCallnum);
	
	    if (matcher.find())
	    	result = matcher.group(6).trim();
	
	    // if no well formed cutter, take the chunk after last period or space 
	    //  if it begins with a letter
	    if (result == null) {
	    	int i = rawCallnum.trim().lastIndexOf('.');  // period
	    	if (i == -1)
	    		i = rawCallnum.trim().lastIndexOf(' ');  // space
	    	if (rawCallnum.trim().length() > i+1) {
	        	String possible = rawCallnum.trim().substring(i+1).trim();
	        	if (Character.isLetter(possible.charAt(0)))
	        		result = possible;
	    	}
	    }
	    
		return result;
	}

	/**
	 * return the suffix after the first cutter, if there is one.  This occurs
	 *  before the second cutter, if there is one.
	 * @param rawLCcallnum - the entire LC call number, as a string
	 */
	public static String getFirstLCcutterSuffix(String rawLCcallnum) {
		String result = null;
	
		String regex = LC_CLASS_N_CUTTER + " *(" + NOT_CUTTER + ")*"; 
	    Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(rawLCcallnum);
	
	    // non cutter string optionally followed by cutter preceded by a period
	    if (matcher.find() && matcher.groupCount() > 5 
	    		&& matcher.group(6) != null && matcher.group(6).length() > 0) {
	
	    	// this only grabs the FIRST non-cutter string it encounters after
	    	//   the first cutter
	    	result = matcher.group(6).trim();
	    	
	    	// this is to cope with additional non-cutter strings after the
	    	//  first cutter  (e.g. M211 .M93 K.240 1988)
	    	int endLastIx = matcher.end(6); // end of previous match
	    	if (endLastIx < rawLCcallnum.length()) {
	    		// if there is a suffix, there must be a period before second cutter
	        	Pattern cutterPat = Pattern.compile(" *\\." + CUTTER_REGEX);
	        	matcher.usePattern(cutterPat);
	        	if (matcher.find(endLastIx)) {
					if (endLastIx < matcher.start())
						result = result.trim() + " " + rawLCcallnum.substring(endLastIx, matcher.start()).trim();
	        	}
	        	else
	        		result = result.trim() + " " + rawLCcallnum.substring(endLastIx).trim();
	    	}
	    }
	    else {
	    	// string after first cutter looks like a second cutter, but is
	    	//  not because further on there is a second cutter preceded by
	    	//  a period.
	    	// look for period before second cutter
	    	regex = LC_CLASS_N_CUTTER + " *((" + NOT_CUTTER + ")*.*)\\." + CUTTER_REGEX; 
	        pattern = Pattern.compile(regex);
	        matcher = pattern.matcher(rawLCcallnum);
	
	        if (matcher.find() && matcher.groupCount() > 5 
	        		&& matcher.group(6) != null && matcher.group(6).length() > 0)
	        	// there is a second cutter preceded by a period
	        	result = matcher.group(6).trim();
	    }
		return result;
	}

	/**
	 * return the second cutter in the call number, without the preceding 
	 * characters (such as the "required" period, which is sometimes missing, 
	 * or spaces), or any suffixes
	 * @param rawLCcallnum - the entire call number, as a string
	 */
	public static String getSecondLCcutter(String rawLCcallnum) {
		String result = null;
		
		String firstCutSuffix = getFirstLCcutterSuffix(rawLCcallnum);
		if (firstCutSuffix == null) {
	    	// look for second cutter 
	       	String regex = LC_CLASS_N_CUTTER + " *\\.?(" + CUTTER_REGEX + ")";  
			Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(rawLCcallnum);
	        if (matcher.find() && matcher.groupCount() > 5 
	        		&& matcher.group(6) != null && matcher.group(6).length() > 0) {
	        	result = matcher.group(6).trim();
	        }
		}
		else {
			// get the call number after the first cutter suffix, then parse out
			//   the cutter from any potential following text.
			int ix = rawLCcallnum.indexOf(firstCutSuffix) + firstCutSuffix.length();
			if (ix < rawLCcallnum.length()) {
				String remaining = rawLCcallnum.substring(ix).trim();
				String regex = "(" + CUTTER_REGEX + ")";
	    		Pattern pattern = Pattern.compile(regex);
	            Matcher matcher = pattern.matcher(remaining);
	            if (matcher.find() && matcher.group(1) != null && matcher.group(1).length() > 0) {
	            	result = matcher.group(1).trim();
	            }
			}
		}
	    
		return result;
	}

	/**
	 * return the suffix after the first cutter, if there is one.  This occurs
	 *  before the second cutter, if there is one.
	 * @param rawLCcallnum - the entire LC call number, as a string
	 */
	public static String getSecondLCcutterSuffix(String rawLCcallnum) {
		String result = null;
		
		String secondCutter = getSecondLCcutter(rawLCcallnum);
		if (secondCutter != null) {
			// get the call number after the 2nd cutter
			int ix = rawLCcallnum.indexOf(secondCutter) + secondCutter.length();
			if (ix < rawLCcallnum.length())
				result = rawLCcallnum.substring(ix).trim();
		}
	
		return result;
	}

	/**
	     * return the suffix after the first cutter, if there is one.  This occurs
	     *  before the second cutter, if there is one.
	     * @param rawLCcallnum - the entire LC call number, as a string
	     * @deprecated
	     */
	// do we want to separate out year suffixes?  for all or just here? - unused
	    public static String getSecondLCcutterYearSuffix(String rawLCcallnum) {
	    	String result = null;
	    	
	    	String regex = LC_CLASS_N_CUTTER + " *(" + NOT_CUTTER + ")*"; 
	        Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(rawLCcallnum);
	
	        if (matcher.find() && matcher.groupCount() > 5 
	        		&& matcher.group(6) != null && matcher.group(6).length() > 0) {
	
	        	// this only grabs the FIRST non-cutter string it encounters after
	        	//   the first cutter
	        	result = matcher.group(6);
	        	
	        	// this is to cope with additional non-cutter strings after the
	        	//  first cutter  (e.g. M211 .M93 K.240 1988)
	        	int endLastIx = matcher.end(6); // end of previous match
	        	if (endLastIx < rawLCcallnum.length()) {
	            	Pattern cutterPat = Pattern.compile(" *\\.?" + CUTTER_REGEX + ".*");
	            	matcher.usePattern(cutterPat);
	            	if (matcher.find(endLastIx)) {
						if (endLastIx < matcher.start())
							result = result.trim() + " " + rawLCcallnum.substring(endLastIx, matcher.start()).trim();
	            	}
	            	else
	            		result = result.trim() + rawLCcallnum.substring(endLastIx);
	        	}
	        }
	
	        return result;
	    }

	// DEWEY    
	/**
	 * return the portion of the Dewey call number string that occurs before the 
	 *  Cutter.
	 */
	public static final String getDeweyB4Cutter(String callnum) {
		String result = null;
		
		String entireCallNumRegex = "(" + DEWEY_CLASS_REGEX + ").*";
	    Pattern pattern = Pattern.compile(entireCallNumRegex);
	    Matcher matcher = pattern.matcher(callnum);
	    if (matcher.find())
	    	result = matcher.group(1).trim();
	    
	    return result;
	}

	/**
	     * return the first cutter in the call number, without the preceding 
		 * characters (such as the "required" period, which is sometimes missing, 
		 * or spaces).
	     * @param rawCallnum - the entire call number, as a string
	     */
	// TODO:  need to allow weird suffixes for Dewey, and not see next thing as cutter
	    public static String getDeweyCutter(String rawCallnum) {
	    	String result = null;
	
			String regex = DEWEY_CLASS_REGEX +  " *\\.?(" + DEWEY_CUTTER_REGEX + ").*";
	        Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(rawCallnum);
	
	        if (matcher.find())
	        	result = matcher.group(2).trim();
	        
			return result;
	    }

	/**
	     * return the first cutter in the call number, without the preceding 
		 * characters (such as the "required" period, which is sometimes missing, 
		 * or spaces).
	     * @param rawCallnum - the entire call number, as a string
	     */
	// TODO:  need to allow weird suffixes for Dewey, and not see next thing as cutter
	// TODO: need to normalize this suffix
	    public static String getDeweyCutterSuffix(String rawCallnum) {
	    	String result = null;
	
			String regex = DEWEY_CLASS_REGEX +  " *\\.?(" + DEWEY_CUTTER_REGEX + ")(.*)";
	        Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(rawCallnum);
	
	        if (matcher.find())
	        	result = matcher.group(3).trim();
	        
			return result;
	    }

	// TODO:  method to normalize year and immediate following chars (no space)?   <-- stupid?
	    
	    /**
	     * given a raw LC call number, return the shelf key - a sortable version
	     *  of the call number
	     */
	    public static String getLCShelfkey(String rawLCcallnum, String recid) {
	    	StringBuffer resultBuf = new StringBuffer();
	    	String upcaseLCcallnum = rawLCcallnum.toUpperCase();
	    	
	// TODO: don't repeat same parsing -- some of these methods could take the
	//   portion of the callnumber before the cutter as the input string.    	
	    	
	    	// pad initial letters with trailing blanks to be 4 chars long
	    	StringBuffer initLetBuf = new StringBuffer("    ");
	    	String lets = getLCstartLetters(upcaseLCcallnum);
	    	initLetBuf.replace(0, lets.length(), lets);
	   		resultBuf.append(initLetBuf);
	    	
	   		try {
	   	    	// normalize first numeric portion to a constant length:
	   	    	//  four digits before decimal, 6 digits after
	   	   		String digitStr = getLCClassDigits(upcaseLCcallnum);
	   	   		if (digitStr != null) 
	   	   			resultBuf.append(normalizeFloat(digitStr, 4, 6));
	   	   	    else
	   	   	    	resultBuf.append(normalizeFloat("0", 4, 6));
	   	    	
	   	    	// optional string b/t class and first cutter
	   	    	String classSuffix = getLCClassSuffix(upcaseLCcallnum);
	   	    	if (classSuffix != null)
	   	    		resultBuf.append(" " + normalizeSuffix(classSuffix));
	   	    	
	   	    	// normalize first cutter  - treat number as a fraction
	   	    	String firstCutter = getFirstLCcutter(upcaseLCcallnum);
	   	    	if (firstCutter != null) {
	   	    		resultBuf.append(" " + normalizeCutter(firstCutter, 6));
	   	    	
	   		    	// normalize optional first cutter suffix
	   		    	String firstCutterSuffix = getFirstLCcutterSuffix(upcaseLCcallnum);
	   		    	if (firstCutterSuffix != null)
	   		    		resultBuf.append(" " + normalizeSuffix(firstCutterSuffix));
	   		    	
	   		    	// optional second cutter - normalize
	   		       	String secondCutter = getSecondLCcutter(upcaseLCcallnum);
	   		    	if (secondCutter != null) {
	   		    		resultBuf.append(" " + normalizeCutter(secondCutter, 6));
	   		    		
	   			    	String secondCutterSuffix = getSecondLCcutterSuffix(upcaseLCcallnum);
	   			    	if (secondCutterSuffix != null)
	   			    		resultBuf.append(" " + normalizeSuffix(secondCutterSuffix));
	   		    	}
	   	    	}
	   		} catch (NumberFormatException e) {
	   			System.err.println("Problem creating shelfkey for record " + recid + "; call number: " + rawLCcallnum);
	   			e.printStackTrace();
	   		}
	    	
	    	if (resultBuf.length() == 0)
	    		resultBuf.append(upcaseLCcallnum);
	
	    	return resultBuf.toString().trim();
	    }

	/**
	 * normalize the cutter string for shelf list sorting - make number into  
	 *  decimal of the number of digits indicated by param
	 */
	private static String normalizeCutter(String cutter, int numDigits) {
		String result = null;
		if (cutter != null) {
			String cutLets = getLCstartLetters(cutter);
			String cutDigs = cutter.substring(cutLets.length());
			String norm = null;
			if (cutDigs != null && cutDigs.length() > 0) {
				try {
					// make sure part after letters is an integer
					Integer.parseInt(cutDigs);
	    			norm = normalizeFloat("." + cutDigs, 1, numDigits); 
				} catch (NumberFormatException e) {
					norm = cutDigs;
				}
			} 
			else if (cutDigs.length() == 0 && cutLets.length() == 1)
				// if no digits in cutter, want it to sort first
				norm = normalizeFloat("0", 1, numDigits);
	
			result = cutLets + norm;    	
		}
		return result;
	}

	/**
	 * normalize a suffix for shelf list sorting by changing all digit 
	 *  substrings to a constant length (left padding with zeros).
	 */
	private static String normalizeSuffix(String suffix) {
		if (suffix != null && suffix.length() > 0) {
			StringBuffer resultBuf = new StringBuffer(suffix.length());
			// get digit substrings
			String[] digitStrs = suffix.split("[\\D]+");
			int len = digitStrs.length;
	    	if (digitStrs != null && len != 0) {
	    		int s = 0;
	        	for (int d = 0; d < len; d++) {
	        		String digitStr = digitStrs[d];
	        		int ix = suffix.indexOf(digitStr, s);
	        		// add the non-digit chars before, if they exist
	        		if (s < ix) {
	        			String text = suffix.substring(s, ix);
	        			resultBuf.append(text);
	        		}
	        		if (digitStr != null && digitStr.length() != 0) {
	            		// add the normalized digit chars, if they exist
	        			resultBuf.append(normalizeFloat(digitStr, 6, 0));
	            		s = ix + digitStr.length();
	        		}
	        			
	        	}
	        	// add any chars after the last digStr
	    		resultBuf.append(suffix.substring(s));
	        	return resultBuf.toString();
	    	}
		}
		
		return suffix;
	}

	/**
	 * given a shelfkey (a lexicaly sortable call number), return the reverse 
	 * shelf key - a sortable version of the call number that will give the 
	 * reverse order (for getting "previous" call numbers in a list)
	 */
	public static String getReverseShelfKey(String shelfkey) {
		StringBuffer resultBuf = new StringBuffer(reverseDefault);
		resultBuf.replace(0, shelfkey.length(), reverseAlphanum(shelfkey));
		return resultBuf.toString();
	}

	/**
	     * return the reverse String value, mapping A --> 9, B --> 8, ...
	     *   9 --> A
	     */
	    private static String reverseAlphanum(String orig) {
	
	/*    	
	    	char[] origArray = orig.toCharArray();
	
	    	char[] reverse = new char[origArray.length];
	    	for (int i = 0; i < origArray.length; i++) {
	    		Character ch = origArray[i];
	    		if (ch != null) {
	            	if (Character.isLetterOrDigit(ch))
	            		reverse[i] = alphanumReverseMap.get(ch);
	            	else 
	            		reverse[i] = reverseNonAlphanum(ch);
	    		}
	    	}
	*/    	    
	    	StringBuilder reverse = new StringBuilder();
	    	for (int ix = 0; ix < orig.length(); ) {
	    		int codePoint = Character.toUpperCase(orig.codePointAt(ix));
				char[] chs = Character.toChars(codePoint);
				
	    		if (Character.isLetterOrDigit(codePoint)) {
	    			if (chs.length == 1) {
						char c = chs[0];
	    				if (alphanumReverseMap.containsKey(c))
	        				reverse.append(alphanumReverseMap.get(c));
	    				else {
	    					// not an ASCII letter or digit
	    					
	    					// map latin chars with diacritic to char without
	        				char foldC;
	        				if (!UnicodeCharUtil.isCombiningCharacter(c) &&  
	        					 !UnicodeCharUtil.isSpacingModifier(c) &&
	        					 (foldC = Utils.foldDiacriticLatinChar(c)) != 0x00)
	        					// we mapped a latin char w diacritic to plain ascii 
	        					reverse.append(alphanumReverseMap.get(foldC));
	        				else
	        					// single char, but non-latin, non-digit
	            				// ... view it as after Z in regular alphabet, for now
	            				reverse.append(SORT_FIRST_CHAR);
	    				}
	    			}
	    			else  {
	    				// multiple 16 bit character unicode letter
	    				// ... view it as after Z in regular alphabet, for now
	    				reverse.append(SORT_FIRST_CHAR);
	    			}
	    		}
	           	else // not a letter or a digit
	      			reverse.append(reverseNonAlphanum(chs[0]));
	
	    		ix += chs.length;
	    	}
	
	    	return new String(reverse);    	
	    }

	/**
	 * for non alpha numeric characters, return a character that will sort
	 *  first or last, whichever is the opposite of the original character. 
	 */
	public static char[] reverseNonAlphanum(char ch) {
		// use punctuation before or after alphanum as appropriate
		switch (ch) {
			case '{':
			case '|':
			case '}':
			case '~':
				return Character.toChars(Character.MIN_CODE_POINT);
			default:
				return Character.toChars(Character.MAX_CODE_POINT);
		}  	
	}

	/**
	     * given a raw Dewey call number, return the shelf key - a sortable 
	     *  version of the call number
	     */
	    public static String getDeweyShelfKey(String rawDeweyCallnum) {
	    	StringBuffer resultBuf = new StringBuffer();
	
	    	// class 
	    	// float number, normalized to have 3 leading zeros
	    	//   and trailing zeros if blank doesn't sort before digits
	    	String classNum = normalizeFloat(getDeweyB4Cutter(rawDeweyCallnum), 3, 8);
	    	resultBuf.append(classNum);
	    	
	    	// cutter   1-3 digits
	    	// optional cutter letters suffix
	    	//   letters preceded by space or not.
	
	    	// normalize cutter  - treat number as a fraction.
	//   TODO:  normalize dewey cutter 
	    	String cutter = getDeweyCutter(rawDeweyCallnum);
	    	if (cutter != null)
	    		resultBuf.append(" " + cutter);
	
	    	// optional suffix (year, part, volume, edition) ...
	    	String cutterSuffix = getDeweyCutterSuffix(rawDeweyCallnum);
	    	if (cutterSuffix != null)
	    		resultBuf.append(" " + cutterSuffix);
	    	
	    	
	    	if (resultBuf.length() == 0)
	    		resultBuf.append(rawDeweyCallnum);
	
	    	return resultBuf.toString().trim();
	    }   

	    
	/**
	 * normalizes numbers (can have decimal portion) to (digitsB4) before
	 *  the decimal (adding leading zeroes as necessary) and (digitsAfter 
	 *  after the decimal.  In the case of a whole number, there will be no
	 *  decimal point.
	 * @param floatStr, the number, as a String
	 * @param digitsB4 - the number of characters the result should have before the
	 *   decimal point (leading zeroes will be added as necessary). A negative 
	 *   number means leave whatever digits encountered as is; don't pad with leading zeroes.
	 * @param digitsAfter - the number of characters the result should have after
	 *   the decimal point.  A negative number means leave whatever fraction
	 *   encountered as is; don't pad with trailing zeroes (trailing zeroes in
	 *   this case will be removed)
	 * @throws NumberFormatException if string can't be parsed as a number
	 */
	public static String normalizeFloat(String floatStr, int digitsB4, int digitsAfter)
	{
		double value = Double.valueOf(floatStr).doubleValue();
		
		String formatStr = getFormatString(digitsB4) + '.' + getFormatString(digitsAfter);
		
		DecimalFormat normFormat = new DecimalFormat(formatStr);
		String norm = normFormat.format(value);
		if (norm.endsWith("."))
			norm = norm.substring(0, norm.length() - 1);
		return norm;
	}
			

	/**
	 * return a format string corresponding to the number of digits specified
	 * @param numDigits - the number of characters the result should have (to be padded
	 *  with zeroes as necessary). A negative number means leave whatever digits
	 *   encountered as is; don't pad with zeroes -- up to 12 characters.
	 */
	private static String getFormatString(int numDigits) {
		StringBuffer b4 = new StringBuffer();
		if (numDigits < 0)
			b4.append("############");
		else if (numDigits > 0) {
			for (int i = 0; i < numDigits; i++) {
				b4.append('0');
			}
		}
		return b4.toString();	
	}
			    
}
