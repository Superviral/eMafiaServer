/* eMafiaServer - RoleForum.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2014  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.GameObjects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import com.inverseinnovations.eMafiaServer.includes.scriptProcess;
import com.inverseinnovations.eMafiaServer.includes.classes.ERS.RoleForumERS;

public class RoleForum extends GameObject{
	public MatchForum Match;
	//Static Variables from Database
	private int version;
	private String affiliation;
	private String[] category;// = array();
	private String actionCat;
	//private int numPossibleNightTargets;//targets 0-2 for nightaction(how many buttons should appear if any)
	//private int numPossibleDayTargets;//targets 0-2 for dayaction(how many buttons should appear if any)
	/** Who you are able to target with button 1
	 0 = No one
	 1 = Everyone
	 2 = Everyone except self
	 3 = Only self
	 */
	public int targetablesNight1;
	public int targetablesNight2;
	public int targetablesDay1;
	public int targetablesDay2;
	//Dynamic Variables
	private int playernum;
	private RoleForumERS roleERS = null;
	private boolean isAlive = true;
	private boolean mayGameEnd = false;
	private boolean victoryMet = false;
	private int hp = 1;
	private Map<String, Flag> flags = new LinkedHashMap<String, Flag>();
	public List<String> deathDesc = new ArrayList<String>();//each death desc is added
	public List<String> deathTypes = new ArrayList<String>();//records every reason for death. Lynched, Shot, Burned, ???
	private List<Integer> visitedBy = new ArrayList<Integer>();//each player the role was visited by that last night
	private int target1 = 0;//day/night action targets
	private int target2 = 0;//day/night action targets
	private boolean onTeam = false;
	private String teamName;
	private boolean teamWin = false;
	private boolean teamVisible = false;
	private boolean chatAtNight = false;
	private Map<String, Integer> customVarInt = new LinkedHashMap<String, Integer>();
	private Map<String, String> customVarString = new LinkedHashMap<String, String>();
	private Map<String, Boolean> customVarBoolean = new LinkedHashMap<String, Boolean>();
	private Map<String, String> ersScript = new LinkedHashMap<String, String>();

 	/**
 	 * Creates Role for use in a match
 	 * @param match Match reference that role is partcipating in
 	 * @param eid
 	 * @param name Name o fRole
 	 * @param type Default of Custom Role
 	 * @param affiliation
 	 * @param category 2 possible categories
 	 */
 	public RoleForum(final MatchForum match, int eid, String name, int type, String affiliation, String[] category){
		super(eid, name, type);
		this.Match = match;
		this.affiliation = affiliation;
		this.category = category;
	}
	/**
	 * Set the Match reference at a later point
	 */
	public void setMatch(final MatchForum match){
		this.Match = match;
	}
	/**
	 * Returns the Match reference
	 */
	public MatchForum getMatch(){
		return this.Match;
	}
	/**
	 * Returns the database version of the role
	 */
	public int getVersion(){
		return version;
	}
	/**
	 * Sets the database version of the role
	 */
	public void setVersion(int version){
		this.version = version;
	}
	/**
	 * Returns the affilation of the Role
	 */
	public String getAffiliation(){
		return this.affiliation;
	}
	/**
	 * Returns the categories of the Role
	 */
	public String[] getCategory(){
		return this.category;
	}
	/**
	 * Returns the Action Category, aka the order of operations
	 */
	public String getActionCat(){
		return actionCat;
	}
	/**
	 * Sets the action category, aka the order of operations
	 */
	public void setActionCat(String cat){
		this.actionCat = cat;
	}
 	/**
 	 * Sets the inplay player number
 	 */
 	public void setPlayerNum(int num){
		this.playernum = num;
	}
	/**
	 * Returns the inplay player number
	 */
	public int getPlayerNum(){
		return this.playernum;
	}
	/**
	 * Returns the inplay player health
	 */
	public int getHp(){
		return this.hp;
	}
	/**
	 * Sets the inplay player health, 0 will kill the player/role
	 */
	public void setHp(int value){
		this.hp = value;
		if(this.hp <= 0){setIsAlive(false);}
	}
	/**
	 * Returns a Map of all Flags attached to this Role
	 */
	public Map<String, Flag> getFlags(){
		return flags;
	}
 	/**
 	 * Attaches a Flag to this Role
 	 */
 	public void addFlag(Flag flag){
		flags.put(flag.getName(), flag);
	}
	/**
	 * Creates and attaches a Flag to this Role with the name
	 * @param name name of Flag to create
	 */
	public void addFlag(String name){
		Flag flag = new Flag(name);
		flags.put(flag.getName(), flag);
	}
	/**
	 * Removes the attached attached to this Role by the name of
	 */
	public void removeFlag(String name){
		if(flags.containsKey(name)){
			flags.remove(name);
		}
	}
	/**
	 * Returns a Flag attached to this Role by name of
	 * @param name name of Flag
	 * @return null if nonexistant
	 */
	public Flag getFlag(String name){
		Flag theReturn = null;
		if(flags.containsKey(name)){
			theReturn = flags.get(name);
		}
		return theReturn;
	}
	/**
	 * Checks whether Role has specified Flag or not
	 * @param name name of Flag
	 */
	public boolean hasFlag(String name){
		boolean theReturn = false;
		if(flags.containsKey(name)){
			theReturn = true;
		}
		return theReturn;
	}
	/**
	 * Returns a Map of all normal(non Flag) scripts of the Role
	 */
	public Map<String, String> getScriptMap(){
		return ersScript;
	}
	/**Returns the requested script for the specified event. Returns null if no existing event or script.
	 * @param eventCall name of script
	 * @return script to process
	 */
	public String getScript(String eventCall){
		String theReturn = null;
		if(StringUtils.isNotEmpty(eventCall)){
			if(ersScript.containsKey(eventCall)){
				theReturn = ersScript.get(eventCall);
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
			ersScript.put(eventCall, script);
		}
	}
	/**Is role suppose to be on a team?
	 * @return
	 */
	public boolean getOnTeam(){
		return onTeam;
	}
	/**Sets whether role is suppose to be on a team
	 * @param bool
	 */
	public void setOnTeam(boolean bool){
		onTeam = bool;
	}
	public TeamForum getTeam(){
		TeamForum team = null;
		if(getOnTeam()){
			team = Match.getTeam(getTeamName());
		}
		return team;
	}
	/**Get the team name of role(If has one), The name is
	 * set by role setup options or if empty, auto set to Affiliation
	 * @return
	 */
	public String getTeamName(){
		return teamName;
	}
	/**Sets the team name for the role
	 * @param name
	 */
	public void setTeamName(String name){
		teamName = name;
	}
	/**Is win conditions shared by team?*/
	public boolean getTeamWin(){
		return teamWin;
	}
	/**Set if win conditions are shared by team*/
	public void setTeamWin(boolean share){
		teamWin = share;
	}
	/**Can see who teammates are?*/
	public boolean getTeamVisible(){
		return teamVisible;
	}
	/**Set if can see who teammates are*/
	public void setTeamVisible(boolean visible){
		teamVisible = visible;
	}
	/**Can chat with Team at Night?*/
	public boolean getChatAtNight(){
		return chatAtNight;
	}
	/**Set if can chat with Team at Night*/
	public void setChatAtNight(boolean chat){
		chatAtNight = chat;
	}
	/**Is game allowed to end?*/
	public boolean getMayGameEnd(){
		return mayGameEnd;
	}
	/**Set if game is allowed to end using these conditions*/
	public void setMayGameEnd(boolean end){
		mayGameEnd = end;
	}
	/**Has the role won?*/
	public boolean getVictory(){
		return victoryMet;
	}
	/**Set if role won or not*/
	public void setVictory(boolean win){
		victoryMet = win;
	}
	/**Return if role is alive or not*/
	public boolean isAlive(){
		return isAlive;
	}
	/**Set if role is alive or not*/
	public void setIsAlive(boolean bool){
		this.isAlive = bool;
	}
//////////////////////////
/////////Targeting////////
//////////////////////////
	/**Add a playerNum to have being visited this role/player*/
 	public void addVisitedBy(int playerNum){
		if(!visitedBy.contains(playerNum)){ visitedBy.add(playerNum);}
	}
	/**Returns the visitedBy list*/
	public List<Integer> getVisitedBy(int playerNum){
		return visitedBy;
	}
	/**Sets to have being visited by no one. */
	public void clearVisitedBy(){
		visitedBy.clear();
	}
	/**Processes this Role as having been visited by the provided
	 * @param visitor the Role visitor this role
	 */
	public void visited(RoleForum visitor){
		addVisitedBy(visitor.getPlayerNum());
		for(Flag flag : getFlags().values()){
			if(flag.isScriptedPre()){
				new scriptProcess("onVisit", flag.getScriptPre("onVisit"), this, visitor);
			}
		}
		if(StringUtils.isNotEmpty(getScript("onVisit"))){
			new scriptProcess("onVisit", getScript("onVisit"), this, visitor);
		}
		for(Flag flag : getFlags().values()){
			if(flag.isScriptedPost()){
				new scriptProcess("onVisit", flag.getScriptPost("onVisit"), this, visitor);
			}
		}
	}
	/**
	 * Sets the target1 for this Role to visit
	 */
	public void setTarget1(int target){
		this.target1 = target;
	}
	/**
	 * Returns the target1 that this Role is goin got visit<br>
	 * 0 generally means that target is none
	 */
	public int getTarget1(){
		return target1;
	}
	/**
	 * Sets the target2 for this Role to visit
	 */
	public void setTarget2(int target){
		this.target2 = target;
	}
	/**
	 * Returns the target2 that this Role is goin got visit<br>
	 * 0 generally means that target is none
	 */
	public int getTarget2(){
		return target2;
	}
	/**Sets target 1 and 2 to 0 (none)*/
	public void clearTargets(){
		setTarget1(0);setTarget2(0);
	}
	/**
	 * Saves a Boolean in this Role for later retrieval
	 * @param var name of boolean to save
	 */
	public void setCustomVarBoolean(String var,boolean value){
		customVarBoolean.put(var, value);
	}
	/**
	 * Saves an Int in this Role for later retrieval
	 * @param var name of Tnt to save
	 */
	public void setCustomVarInt(String var,int value){
		customVarInt.put(var, value);
	}
	/**
	 * Saves a String in this Role for later retrieval
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
	 * Sends a chat screen message to the client of this Role's player/character
	 */
	public void send(String message){
		getMatch().sendToPlayerNum(getPlayerNum(), "blah", message);//TODO fix the title...
	}
	/**
	 * Returns the ERSClass for scripting purposes
	 */
	public RoleForumERS getERSClass(){
		if(this.roleERS == null){
			this.roleERS = new RoleForumERS(this);
		}
		return this.roleERS;
	}
}