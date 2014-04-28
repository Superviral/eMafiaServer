/* eMafiaServer - TeamForum.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2014  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.GameObjects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.inverseinnovations.eMafiaServer.includes.Constants;
import com.inverseinnovations.eMafiaServer.includes.classes.ERS.TeamForumERS;

public class TeamForum extends GameObject{
	private MatchForum match;
	private TeamForumERS teamERS = null;;
	private boolean victory;
	private boolean mayGameEnd;
	private Map<String, String> ersScript = new LinkedHashMap<String, String>();
	private ArrayList<Integer> teammates = new ArrayList<Integer>();
	private Map<String, Integer> customVarInt = new LinkedHashMap<String, Integer>();
	private Map<String, String> customVarString = new LinkedHashMap<String, String>();
	private Map<String, Boolean> customVarBoolean = new LinkedHashMap<String, Boolean>();
	private Map<String, LinkedHashMap<Integer, Integer>> poll = new LinkedHashMap<String, LinkedHashMap<Integer, Integer>>();

	/**
	 * Creates a Team for use in a Match
	 * @param match Match reference
	 * @param name name of Team to make
	 */
	public TeamForum(final MatchForum match,String name){
		super(0, name, Constants.TYPE_GAMEOB_TEAM);
		if(this.match == null){this.match = match;}
	}
	/**
	 * Returns the Match reference
	 */
	public MatchForum getMatch(){
		return this.match;
	}
	/**Returns the requested script for the specified event. Returns null if no existing event or script.
	 * @param eventCall name of script
	 * @return script to process
	 */
	public String getScript(String eventCall){
		String theReturn = null;
		if(StringUtils.isNotEmpty(eventCall)){
			if(this.ersScript.containsKey(eventCall)){
				theReturn = this.ersScript.get(eventCall);
			}
		}
		return theReturn;
	}
	/**Saves an ERS Script for the specified event
	 * @param eventCall name of script
	 * @param script script to save
	 */
	public void setScript(String eventCall, String script){
		if(StringUtils.isNotEmpty(eventCall)){
			this.ersScript.put(eventCall, script);
		}
	}
	/**Is game allowed to end?*/
	public boolean getMayGameEnd(){
		return mayGameEnd;
	}
	/**Set if game is allowed to end using these conditions*/
	public void setMayGameEnd(boolean end){
		mayGameEnd = end;
	}
	/**Has the Team won?*/
	public boolean getVictory(){
		return victory;
	}
	/**Set if Team won or not*/
	public void setVictory(boolean vict){
		victory = vict;
	}
	/**
	 * Returns a List of all player numbers assigned to this Team
	 */
	public ArrayList<Integer> getTeammates(){
		return teammates;
	}
	/**
	 * Returns a List of all living player numbers assigned to this Team
	 */
	public ArrayList<Integer> getAliveTeammates(){
		ArrayList<Integer> list = new ArrayList<Integer>();
		for(int num :getTeammates()){
			if(match.getPlayerRole(num).isAlive()){
				list.add(num);
			}
		}
		return list;
	}
	/**
	 * Assigns a player num to this Team
	 */
	public void addTeammate(int playerNum){
		if(!teammates.contains(playerNum)){
			teammates.add(playerNum);
		}
	}
	/**
	 * Removes a player num from this Team
	 */
	public void removeTeammate(int playerNum){
		if(teammates.contains(playerNum)){
			teammates.remove(playerNum);
		}
	}
	/**
	 * Checks if a member of this team possesses a certain Role
	 * @param roleId database id of role searching for
	 */
	public boolean isRoleExist(int roleId){
		boolean theReturn = false;
		for(Integer i:getTeammates()){
			if(match.getPlayerRole(i) != null){
				if(match.getPlayerRole(i).getEID() == roleId){
					theReturn = true;
					break;
				}
			}
		}
		return theReturn;
	}
	/**
	 * Returns number of Roles in this Team
	 * @param roleId database id of role searching for
	 */
	public int numRoleAlive(int roleId){
		int theReturn = 0;
		for(Integer i:getTeammates()){
			RoleForum role = match.getPlayerRole(i);
			if(role != null){
				if(role.getEID() == roleId){
					if(role.isAlive()){
						theReturn++;
					}
				}
			}
		}
		return theReturn;
	}
	/**
	 * Checks if a member of this team possesses a certain Role and is alive
	 * @param roleId database id of role searching for
	 */
	public boolean isRoleAlive(int roleId){
		boolean theReturn = false;
		for(Integer i:getTeammates()){
			RoleForum role = match.getPlayerRole(i);
			if(role != null){
				if(role.getEID() == roleId){
					if(role.isAlive()){
						theReturn = true;
						break;
					}
				}
			}
		}
		return theReturn;
	}
	/**
	 * Saves a Boolean in this Team for later retrieval
	 * @param var name of boolean to save
	 */
	public void setCustomVarBoolean(String var,boolean value){
		customVarBoolean.put(var, value);
	}
	/**
	 * Saves an Int in this Team for later retrieval
	 * @param var name of Tnt to save
	 */
	public void setCustomVarInt(String var,int value){
		customVarInt.put(var, value);
	}
	/**
	 * Saves a String in this Team for later retrieval
	 * @param var name of String to save
	 */
	public void setCustomVarString(String var,String value){
		customVarString.put(var, value);
	}
	/**
	 * Returns a previously saved Boolean
	 * @param var name of Boolean
	 * @return false is no variable by that name found
	 */
	public boolean getCustomVarBoolean(String var){
		boolean theReturn = false;
		if(customVarBoolean.containsKey(var)){
			theReturn = customVarBoolean.get(var);
		}
		return theReturn;
	}
	/**
 	 * Returns a previously saved Int
	 * @param var name of Int
	 * @return 0 is no variable by that name found
 	 */
 	public int getCustomVarInt(String var){
		int theReturn = 0;
		if(customVarInt.containsKey(var)){
			theReturn = customVarInt.get(var);
		}
		return theReturn;
	}
 	/**
	 * Returns a previously saved String
	 * @param var name of String
	 * @return null is no variable by that name found
	 */
 	public String getCustomVarString(String var){
		String theReturn = null;
		if(customVarString.containsKey(var)){
			theReturn = customVarString.get(var);
		}
		return theReturn;
	}
	/**
	 * Resets a Poll if it exists
	 */
	public void pollClear(String pollName){
		if(poll.containsKey(pollName)){
			poll.get(pollName).clear();
		}
	}
	/**
	 * Adds a vote to an option of a specified Poll
	 * @param pollName name of Poll
	 * @param option poll option to add to
	 */
	public void pollVoteAdd(String pollName, int option){
		if(!poll.containsKey(pollName)){
			poll.put(pollName, new LinkedHashMap<Integer, Integer>());
		}
		LinkedHashMap<Integer, Integer> thePoll = poll.get(pollName);
		if(thePoll.containsKey(option)){
			thePoll.put(option,thePoll.get(option) + 1);
		}
		else{
			thePoll.put(option,1);
		}
	}
	/**
	 * Removes a vote from an option of a specified Poll
	 * @param pollName name of Poll
	 * @param option poll option to remove from
	 */
	public void pollVoteRemove(String pollName, int option){
		if(!poll.containsKey(pollName)){
			poll.put(pollName, new LinkedHashMap<Integer, Integer>());
		}
		LinkedHashMap<Integer, Integer> thePoll = poll.get(pollName);
		if(thePoll.containsKey(option)){
			if(thePoll.get(option) >= 1){
				thePoll.put(option,thePoll.get(option) - 1);
			}
		}
		else{
			thePoll.put(option,0);
		}
	}
	/**
	 * Returns the highest option choosen within a specified Poll
	 * @param pollName name of Poll
	 * @return 0 if nonexistant poll
	 */
	public int pollHighestVote(String pollName){
		int highestOp = 0;
		int value = 0;
		if(poll.containsKey(pollName)){
			LinkedHashMap<Integer, Integer> thePoll = poll.get(pollName);
			for(int key : thePoll.keySet()){
				if(thePoll.get(key) > value){
					value = thePoll.get(key);
					highestOp = key;
				}
			}
		}
		return highestOp;
	}
	/**
	 * Returns the ERSClass for scripting purposes
	 */
 	public TeamForumERS getERSClass(){
		if(this.teamERS == null){
			this.teamERS = new TeamForumERS(this);
		}
		return this.teamERS;
	}
}