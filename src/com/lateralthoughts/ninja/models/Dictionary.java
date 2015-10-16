package com.lateralthoughts.ninja.models;

import java.util.ArrayList;
import java.util.HashMap;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class Dictionary {
	public enum Languages{
		EN,
	};
	
	private static final char[] englishCharSet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	private static HashMap<String, Integer> sEnglishWordsMap = new HashMap<String, Integer>();
	
	private static final String DB_URL = "jdbc:mysql://localhost:3306/ninjadb";
	private static final String USER = "ninja";
	private static final String PASSWORD = "ninja_password";
	static final Logger logger = Logger.getLogger(Dictionary.class);
	   
	public static void build () {
		Connection connection = null;
		Statement statement = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(DB_URL,USER,PASSWORD);
			statement = connection.createStatement();
			String selectWordsQuery;
			selectWordsQuery = "SELECT word, weight FROM english_words";
			ResultSet rs = statement.executeQuery(selectWordsQuery);
			
			while(rs.next()){
				sEnglishWordsMap.put(rs.getString("word"), rs.getInt("weight"));
			}
			rs.close();
		    statement.close();
		    connection.close();
		      
		} catch (SQLException se) {
			logger.debug("Encountered SQLException with DB. Error = " + se.toString());
			
		} catch (ClassNotFoundException e) {
			logger.debug("Encountered ClassNotFoundException with DB");
		}
		
	}
	
	/**
	 * Adds the word to the dictionary identified by dictID
	 * @param inputString - word to be added into the dictionary
	 */
	public static void add (String inputString) {
		Connection connection = null;
		if (inputString == null || sEnglishWordsMap.containsKey(inputString))
			return;
		
		try{
			//STEP 2: Register JDBC driver
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(DB_URL,USER,PASSWORD);
			String insertWordQuery = "INSERT INTO english_words"
					+ " (word, weight) VALUES"
					+ " (?, ?)";
			
			logger.debug("query to execute = " + insertWordQuery);
			PreparedStatement preparedStatement = connection.prepareStatement(insertWordQuery);
			preparedStatement.setString(1, inputString);
			preparedStatement.setInt(2, 0);
			logger.debug("Prepared statement = " + preparedStatement.toString());
			
			preparedStatement.execute();
		    connection.close();
		    logger.debug("Word " + inputString + " has been added to dictionary");
		      
		} catch (SQLException se) {
			logger.debug("Encountered SQLException with DB. Error = " + se.toString());
			
		} catch (ClassNotFoundException e) {
			logger.debug("Encountered ClassNotFoundException with DB");
		}
	}
	
	public static boolean has(String inputString) {
		if (null != inputString) {
			if (sEnglishWordsMap.containsKey(inputString)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This function will take an input string and apply a morphing algorithm which essentially calculates
	 * variants of the inputString. It does this by doing the following:
	 * a) Calculate possible words by removing 1 character at a time from each index in the input
	 * b) Calculate possible words by adding each of the alphabets in the character set to each of the
	 * 	  indices in the inputString
	 * c) Calculate possible words by replacing the character at each index by each of the alphabets
	 * 	  in the character set
	 * d) Calculate the possible words by swapping adjacent characters in the input string
	 * Using this approach, I can find a valid word if there is just one mistake made in the spelling of
	 * the input word. I can apply this morph to the list of generated words as well and that should give
	 * me valid words that are 2 characters away form the input string and so on.
	 * @param inputString
	 * @return list of strings that are created by morphing. Notice that the list isn't essentially valid
	 * 		words
	 */
    private static ArrayList<String> morph(String inputString) {
    	
    	ArrayList<String> morphedWords = new ArrayList<String>();
    	morphedWords.addAll(wordsByRemovingAlphabet(inputString));
    	morphedWords.addAll(wordsByAddingAlphabet(inputString));
    	morphedWords.addAll(wordsBySwappingAdjacentCharacters(inputString));
    	morphedWords.addAll(wordsByRelacingCharWithAlphabet(inputString));
    	return morphedWords;    	
    }
    
    private static ArrayList<String> wordsByRemovingAlphabet(String inputString) {
    	ArrayList<String> result = new ArrayList<String>();
    	
        for(int i=0; i < inputString.length(); i++) {
        	result.add(inputString.substring(0, i) + inputString.substring(i+1));
        }
        return result;
    }
    
    private static ArrayList<String> wordsBySwappingAdjacentCharacters(String inputString) {
    	ArrayList<String> result = new ArrayList<String>();    	
        for (int i = 0; i < inputString.length() - 1; i++) {
            String temp = inputString.substring(0, i);
            temp = temp + inputString.charAt(i + 1);
            temp = temp + inputString.charAt(i);
            temp = temp.concat(inputString.substring((i + 2)));
        }
        
        return result;
    }
    
    private static ArrayList<String> wordsByAddingAlphabet(String inputString) {
    	ArrayList<String> result = new ArrayList<String>();
    	
        for(int i=0; i < inputString.length(); i++) {
        	for (char c : englishCharSet) {
        		result.add(inputString.substring(0, i) + String.valueOf(c) + inputString.substring(i));
        	}
        }
        return result;
    }
    
    private static ArrayList<String> wordsByRelacingCharWithAlphabet(String inputString) {
    	ArrayList<String> result = new ArrayList<String>();
    	
        for(int i=0; i < inputString.length(); i++) {
        	for (char c : englishCharSet) {
        		result.add(inputString.substring(0, i) + String.valueOf(c) + inputString.substring(i+1));
        	}
        }
        return result;
    }
    
    public static ArrayList<String> getMatches(String inputString) {
    	ArrayList<String> matches = new ArrayList<String>();
    	ArrayList<String> potentialMatches = morph(inputString);
    	Dictionary.build();
    	for (String s : potentialMatches) {
    		if (Dictionary.has(s) && !matches.contains(s)) {
    			matches.add(s);
    		}
    	}
    	
    	//if there are no matches - lets go one more layer and see if maybe 2 letters
    	//were misspelled
    	if (!matches.isEmpty()) return matches;
    	
    	for (String s : potentialMatches) {
    		for (String s2 : morph(s)) {
    			if (Dictionary.has(s2) && !matches.contains(s2)) {
    				
        			matches.add(s2);
        		}
    		}
    	}
    	return matches;
    }
    
    /**
     * This function is used to see if a given inputString is made up of multiple words
     * NOTE: this doesn't work very well right now. As soon as it finds a substring that
     * is a meaningful word the algorithm divides the input into two strings but thats
     * premature.
     * For ex: input = thequickbrownfoxjumpedoverthelazydog
     * Desired Output = the quick brown fox jumped over the lazy dog
     * 
     * But this algorithm produces: the quick brow
     * 
     * When it encountered "brow" - it decided it has a full word and splits the string
     * and the rest of the string looks like "nfoxjumpedoverthelazydog" and there is no
     * legit substring here starting at 0 so it fails. Ughhh!
     */
	@SuppressWarnings(value = { "unused" })
    private static ArrayList<String> splitIntoWords(String inputString) {
    	Dictionary.build();
    	ArrayList<String> subStringsToParse = new ArrayList<String>();
    	ArrayList<String> phrase = new ArrayList<String>();
    	subStringsToParse.add(inputString);
    	while (!subStringsToParse.isEmpty()) {
    		String currentUnparsedString = subStringsToParse.remove(0);
        	for (int i=0; i<currentUnparsedString.length(); i++) {
        		String possibleWord = currentUnparsedString.substring(0,i+1);
        		if (Dictionary.has(possibleWord) && (possibleWord.length() > 2)) {
        			String remainingString = currentUnparsedString.substring(i+1);
        			subStringsToParse.add(remainingString);
        			phrase.add(possibleWord);
        			break;
        		}
        	}    		    		
    	}
    	return phrase;
    }
}
