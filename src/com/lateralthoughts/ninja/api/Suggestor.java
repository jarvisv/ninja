package com.lateralthoughts.ninja.api;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.lateralthoughts.ninja.models.Dictionary;


/** Overview of the spell checker implementation:
 * @author explorer
 * Assumptions, caveats and design notes:
 * ---------------------------------------
 * 1) ninja uses a mysql db and uses one table oer language. However, English is currently the only language
 * supported.
 * 2) Algorithm: There is 2 parts to this algorithm and I can talk more about how I came to this conclusion in more detail:
 * 		a) For the spell checking, I am effectively going to create 3 list - the 1st list is obtained by modifying
 * 		   input 1 character at a time. The 2nd list is obtained by modifying each word in the 1st list by 1 character
 * 		   and the 3rd list is obtained by modifying each word in the 2nd list by character. Modifying means add 1
 * 		   character, remove 1 character, swap adjacent characters. The idea here is that I am counting on users usually
 *         being no farther than 3 wrongly typed characters in a single word. According to quora, the average English word
 *         is 5.1 characters long so the heuristics seems reasonable.Also, in my testing with my phone
 *         I seemed to be making 1 or 2 errors so I think 3 should cover most.
 *       b) In addition to checking for spelling errors, I am also checking to see if the given input is made of
 *       multiple words - but this is incomplete.
 *    I think, between 3 layers of spelling error detection and word detection in phrases, we should cover a lot of
 *    ground.
 *    
 * 4) Code organization: I have split this service into 2 segments: api & model. Model consists of the business logic and also
 * the interaction with the DB. For a more complex data model, I would create domain objects and have a separate
 * persistence layer fill them up but this did not seem necessary for this implementation. But the separation of concern is
 * something I thought about and would like to see what feedback others might have :-)
 * 
 * 5) In order to support multiple languages the suggestion API is going to include language identifier in the request
 * (query params) although English is the only supported language at this time. But this should be expandable
 * relatively straight forward.
 * 
 * The 2 APIs exposed by this service are as follows:
 * http://localhost:8080/ninja/api/suggestions/<inputString>?lang=EN - this is used to get suggestions and the closest matches
 * are returned as an array
 * http://localhost:8080/ninja/api/suggestions/include/<inputString>?lang=EN - this is used to add a word to the dictionary.
 * 
 * Areas for improvement:
 * ----------------------
 * 1) The dictionary just stores words in a hashmap which is good enough to get boolean answers on whether a match is found but
 * this isn't great for approximation or seeing really how close we are to a possible valid word.
 * 
 * 2) In the DB table, I have included a column called weight but I am not using it yet. The algorithm currently provides a list
 * of closest matches but not all matches are equally close. Weight could be either based on past usage of words by user or be
 * assigned some values by the developer based on how often a particular word is found in the wild.
 * 
 * 3) Speaking of user, the DB is shared across all users - it would be cool to keep this DB untouched and instead have a per user
 * table so we can personalize.
 * 
 */

@Path("/suggestions")
public class Suggestor {
	
	@Path("{input}")
	@GET
	@Produces("application/json")
	public Response findMatches(@PathParam("input") String inputString, @QueryParam("lang") String languageIdentifier) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		//find the best matches by morphing characters and see if we can find a match
		//in the dictionary
		ArrayList<String> closestWords;
		if (inputString == null) {
			jsonObject.put("closestWords","");
		} else if (inputString.length() < 2) {
			jsonObject.put("closestWords",inputString);
		} else {
			closestWords = Dictionary.getMatches(inputString);
			jsonObject.put("closestWords",closestWords);
		}
		
		//Lets see if we can find any words by splitting
		
		String result = jsonObject.toString();
		return Response.status(200).entity(result).build();
	}
	
	@Path("/include/{input}")
	@POST
	@Produces("application/json")
	public Response includeInDictionary(@PathParam("input") String inputString, @QueryParam("lang") String languageIdentifier) throws JSONException {
		Dictionary.add(inputString);
		return Response.status(201).entity("OK").build();
	}    
}
