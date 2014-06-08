/* eMafiaServer - Match.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.GameObjects;
//FIXME refresh needs to refresh the votes/targeting to reappear client-side as well(was this done already?)

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import com.inverseinnovations.eMafiaServer.includes.CmdCompile;
import com.inverseinnovations.eMafiaServer.includes.Constants;
import com.inverseinnovations.eMafiaServer.includes.StringFunctions;
import com.inverseinnovations.eMafiaServer.includes.scriptProcess;
import com.inverseinnovations.eMafiaServer.includes.classes.Game;
import com.inverseinnovations.eMafiaServer.includes.classes.ERS.MatchERS;
/** Match object is holder of most on-going interactions for gameplay.
 *  Holds each character in lieu of Lobby*/
public class Match extends GameObject{
	public Game Game;
	private MatchERS matchERS = null;
	private Map<Integer, Character> characters = Collections.synchronizedMap(new ConcurrentHashMap <Integer, Character>());
	private Map<String, Integer> settings = new LinkedHashMap<String, Integer>();
	private Timer timer;
	private advancePhaseTimer timerTask;
	private int timerremain;
	/**0 = setup/1 = naming/2 = inplay/3 = endgame*/
	private int phaseMain = Constants.PHASEMAIN_SETUP;
	/**Which day/night it is(only should apply is PhaseSetup is "inplay")*/
	private int phaseDay = 1;
	/**1=discuss/2=normal/3=trialplead/4=trialvote/6=lynch/8=night*/
	private int phaseDayType;
	private Players[] players;//players[playernum]=[eID,inGameName,roleNumber]
	private Players[] graveyard;
	private Role[] roles;
	private ChatGroup chatGroup = new ChatGroup();
	private Map<Integer, Integer> rolesActionOrder = new LinkedHashMap<Integer, Integer>();
	private ArrayList<Role> rolesPossible = new ArrayList<Role>();
	private ArrayList<RolesOrig> roleSetup = new ArrayList<RolesOrig>();
	private ArrayList<String> actionCat = new ArrayList<String>();
	private Map<String, Team> teams = new LinkedHashMap<String, Team>();
	/** Target tracker - alive[playerNum] = targetNum. affects vote target */
	private int[] votes;// alive[playernum] = target / tracks which players the person is voting/and inno/guilty
	/** Vote tracker - ballot[playerNum] = numVoteAgainst. How many votes are on player *OR* ballot[1] = numInnocent/ballot[2] = numGuilty */
	private int[] ballot;//ballot[playernum] = numVotesAgainst / tracks the current number of votes against the target
	/** The playerNum of player on trial/lynch*/
	private int trialplayer;//trialplayer = playernum / the current person on trial
	private Map<Integer, Integer> playerNumSwitch = new LinkedHashMap<Integer, Integer>();
///////////////////////
///////CONSTANTS///////
///////////////////////
	/** Creates a new Match with default settings*/
	public Match(Game game, String name){
		super(0, name, Constants.TYPE_GAMEOB_MATCH);
		this.Game = game;
		Map<Integer, Match> gameMatches = Game.getMatchs();
		for (int i = 1; ; i++){
			if (!gameMatches.containsKey(i)){this.setEID(i);break;}
		}
		Game.addMatch(this);

		this.settings.put("host_id", 0);
		this.settings.put("max_chars", 4);//the max num of chars for a game, 15 defualt
		this.settings.put("start_game_at", 1);//0=day/1=day no lynch/2=night
		this.settings.put("discussion", 1);//0=no discuss mode/1=discuss
		this.settings.put("day_length", 30);//# in secs. 60-600 default 60
		this.settings.put("night_length", 6);//30-120 default 30
		this.settings.put("discuss_length", 6);//30-180 default 30
		this.settings.put("trial_length", 30);//30-120 default 30
		this.settings.put("trial_pause_day", 0);//0=no/1=yes
		this.settings.put("choose_names", 1);//0=no/1=yes
		this.settings.put("trial_defense", 1);//0=no defense/1=plead

		//misc
		this.settings.put("naming_length", 10);//# in secs to choose a name

		//unimplemented settings
		this.settings.put("day_type", 1);//0=majority/1=trial/2=ballot/3=ballot+trial
		this.settings.put("pm_allowed", 0);//0=false/1=true
		this.settings.put("last_will", 0);//0=no/1=show last will
		this.settings.put("description", 1);//0=night seq/1=death desc/2=classic night

		//add action categorys for default
		String[] actionsToAdd = new String[]{"Jail","Vest","Witch","Busdrive","Roleblock","Frame","Douse","Heal","Kill","Clean","Invest","Disguise","Recruit"};
		for(String action: actionsToAdd){
			addActionCategory(action);
		}

		//Default Roles Possible
		int[] rolesToAdd = new int[]{1,2,3,4,5,7};//Cit,Sheriff,Doc,Mafiso,Escort,GF
		for(int role: rolesToAdd){
			addRolesPossible(role);
		}
	}
	/**
	 * Changes a choosen setting variable to one inputted: $settings[$setting] = $value
	 * @param setting Which_Setting
	 * @param value New_value
	 * @return TRUE if setting exist|FALSE
	 */
	public boolean setSetting(String setting,int value){
		if(this.settings.containsKey(setting)){
			this.settings.put(setting, value);
			return true;
		}
		return false;
	}
	/**
	 * Returns the value of inputted setting
	 * @param setting Which_Setting
	 * @return value|FALSE if $setting nonexistant
	 */
	public Integer getSetting(String setting){
		if(this.settings.containsKey(setting)){
			return this.settings.get(setting);
		}
		return null;
	}
	/**Returns the match chatController*/
	public ChatGroup chatController(){
		return chatGroup;
	}
//////////////////////////
////////Characters////////
//////////////////////////
	/**
	 * Character is added to the room
	 * @param chara Character
	 */
 	public void addCharacter(Character chara){
		this.characters.put(chara.getEID(), chara);
		this.send(CmdCompile.playerEnter(chara),chara);
		if(getHostId() == 0){//if there is no host
			if(chara.getType() != Constants.TYPE_GAMEOB_NPC){//NPC couldn't be host
				setHost(chara);
			}
		}
	}
	/**
	 * Character is removed from the room
	 * @param chara Character
	 */
	public void removeCharacter(Character chara){
		this.characters.remove(chara.getEID());
		this.send(CmdCompile.playerLeave(chara),chara);
	}
	/**
	 * Returns character in match based on EID
	 */
	public Character getCharacter(int eid){
		return this.characters.get(eid);
	}
	/**
	 * Returns Map of all Characters in the match
	 */
	public Map<Integer, Character> getCharacters(){
		return this.characters;
	}
	/** Returns number of characters in room */
	public int getNumChars(){
		return this.characters.size();
	}
	/**Returns List of Characters currently in the match */
	public List<Character> getCharacterList(){
		List<Character> list = new ArrayList<Character>();
		for(Character chara : this.characters.values()){
			list.add(chara);
		}
		return list;
	}
	/**Gets the host id of the match, 0 if there is none*/
	public int getHostId(){
		return getSetting("host_id");
	}
	/**Sets a host*/
	public void setHost(Character chara){
		if(characters.containsKey(chara.getEID())){
			if(chara.getType() != Constants.TYPE_GAMEOB_NPC){//NPC can't be host
				setSetting("host_id",chara.getEID());
				this.send(CmdCompile.chatScreen(chara.getName()+" is the host."));
			}
		}
	}
	/**Sets the next host avaiable, sets 0 if none found*/
	public boolean findNewHost(){
		boolean success = false;
		for(Character chara : characters.values()){
			if(getHostId() != chara.getEID()){
				if(chara.getType() != Constants.TYPE_GAMEOB_NPC){//NPC can't be host
					setHost(chara);
					success = true;
					break;
				}
			}
		}
		if(!success){setSetting("host_id",0);}
		return success;
	}
	/**Removes all characters and kills the match*/
	public void endMatch(){
		for(Character charas : getCharacters().values()){
			charas.leaveMatch();
		}
		Game.Base.Console.debug("Match should have ended");
		Game.removeMatch(this);//remove match
	}
//////////////////////////
/////////Players//////////
//////////////////////////
	/**Returns List of alive Players*/
	public List<Players> getAliveList(){
		List<Players> list = new ArrayList<Players>();
		for(Players player : this.players){
			if(player != null){
				list.add(player);
			}
		}
		return list;
	}
	/**Returns List of dead Players*/
	public List<Players> getDeadList(){
		List<Players> list = new ArrayList<Players>();
		for(Players player : this.graveyard){
			if(player != null){
				list.add(player);
			}
		}
		return list;
	}
	/**Returns List of All Players*/
	public List<Players> getPlayerList(){
		List<Players> list = new ArrayList<Players>();
		for(Players player : this.players){
			if(player != null){
				list.add(player);
			}
		}
		for(Players player : this.graveyard){
			if(player != null){
				list.add(player);
			}
		}
		return list;
	}
	/**Returns Player whether alive or not
	 * @param playerNum
	 */
	public Players getPlayer(int playerNum){//may need to check if non-existant depending how its used
		if(getAlivePlayer(playerNum) != null)return getAlivePlayer(playerNum);
		return getDeadPlayer(playerNum);
	}
	/**Returns Player and takes into account of playerNum switches
	 * @param playerNum
	 * @return
	 */
	public Players getPlayerWithSwitch(int playerNum){
		if(playerNumSwitch.containsKey(playerNum)){
			playerNum = playerNumSwitch.get(playerNum);
		}
		return getPlayer(playerNum);
	}
	/**Returns Player only if alive
	 * @param playerNum
	 */
	public Players getAlivePlayer(int playerNum){//may need to check if non-existant depending how its used
		return players[playerNum];
	}
	/**Returns Player only if dead
	 * @param playerNum
	 */
	public Players getDeadPlayer(int playerNum){//may need to check if non-existant depending how its used
		Players player = null;
		for(int i=0;i < graveyard.length;i++){
			if(graveyard[i].getPlayerNumber() == playerNum){
				player = graveyard[i];
				break;
			}
		}
		return player;
	}
	/**Returns number of total Players */
	public int getNumPlayers(){
		return players.length - 1;
	}
	/** Returns number of players listed as 'living' */
	public int getNumPlayersAlive(){
		int numAlive = 0;
		for(Players player : this.players){
			if(player != null){
				numAlive++;
			}
		}
		return numAlive;
	}
	/**
	 * Changes which player the orginiator voting aginst OR inno/guilty
	 * @param voter person targeting someone
	 * @param vote the person being voted against
	 */
	public void setPlayerVote(int voter,int vote){
		this.votes[voter]=vote;
	}
	/** Returns which player the voter is vote against OR inno/guilty */
	public int getPlayerVote(int voter){
		return this.votes[voter];
	}
	/** Removes the votes of all players */
	public void clearVotes(){
		for(int i = 0; i < this.votes.length;i++){
			this.votes[i] = 0;
			this.ballot[i] = 0;
		}
		this.send(CmdCompile.voteCountClear());
	}
	/**
	 * Gives each player a random color
	 */
	private void assignRandomColors(){
		ArrayList<String> primarys = new ArrayList<String>();
			primarys.add("FF0000");//bright red
			primarys.add("00FF00");//bright green
			primarys.add("0000FF");//bright blue
			primarys.add("FFFF00");//bright yellow
			primarys.add("00FFFF");//bright cyan
			primarys.add("FF00FF");//bright magenta
			primarys.add("FFFFFF");//white
			primarys.add("181818");//'black' XXX should make pure black to be visible?
		ArrayList<String> secondarys = new ArrayList<String>();
			secondarys.add("800000");//dark red
			secondarys.add("003300");//dark green
			secondarys.add("000006");//dark blue
			secondarys.add("336600");//brown
			secondarys.add("006666");//dark cyan
			secondarys.add("660066");//dark magenta
			secondarys.add("a0a0a0");//gray
		ArrayList<String> tetras = new ArrayList<String>();
			tetras.add("ff6699");//pink
			tetras.add("ff6600");//orange
			tetras.add("6600cc");//purple
			tetras.add("666633");//warped green
		ArrayList<String> colorsToAssign = new ArrayList<String>();
		//add number of colors as same as number of players
		int numColors = 0;
		while(getNumPlayers() != colorsToAssign.size()){
			numColors = colorsToAssign.size();
			if((getNumPlayers()-numColors) >= primarys.size()){
				colorsToAssign.addAll(primarys);
			}
			else if(getNumPlayers() > numColors){
				Collections.shuffle(primarys);
				for(int i = 0;i < (getNumPlayers()-numColors);i++){
					if(primarys.size() > i){
						colorsToAssign.add(primarys.get(i));
					}
				}
			}
			numColors = colorsToAssign.size();
			if((getNumPlayers()-numColors) >= secondarys.size()){
				colorsToAssign.addAll(secondarys);
			}
			else if(getNumPlayers() > numColors){
				Collections.shuffle(secondarys);
				for(int i = 0;i < (getNumPlayers()-numColors);i++){
					if(secondarys.size() > i){
						colorsToAssign.add(secondarys.get(i));
					}
				}
			}
			numColors = colorsToAssign.size();
			if((getNumPlayers()-numColors) >= tetras.size()){
				colorsToAssign.addAll(tetras);
			}
			else if(getNumPlayers() > numColors){
				Collections.shuffle(tetras);
				for(int i = 0;i < (getNumPlayers()-numColors);i++){
					if(tetras.size() > i){
						colorsToAssign.add(tetras.get(i));
					}
				}
			}
		}
		Collections.shuffle(colorsToAssign);
		for(Players player:getPlayerList()){
			if(colorsToAssign.size() > 0){
				player.hexcolor = colorsToAssign.get(0);
				colorsToAssign.remove(0);
			}
		}
	}
//////////////////////////
///////////Roles//////////
//////////////////////////
	/**Allows a Role to be possible*/
	public boolean addRolesPossible(int roleId){
		boolean success = false;
		Role role = Game.Base.MySql.grabRole(roleId);
		if(role==null){Game.Base.Console.warning("Could not retrieve a role the list based on manuel id, role not added");}
		else{
			if(!rolesPossible.contains(role.getClass())){
				rolesPossible.add(role);
				addActionCategory(role.getActionCat());
				success = true;
			}
		}
		return success;
	}
	/**Removes a possible Role*/
	public boolean removeRolesPossible(int roleId){
		boolean success = false;
		Role theRole = null;
		for(Role role : rolesPossible){
			if(role.getEID() == roleId){
				theRole = role;
				break;
			}
		}
		if(theRole != null){
			rolesPossible.remove(theRole);
			removeImpossibleFromSetup();
			success = true;
			if(!isRolePossibleWithActionCat(theRole.getActionCat())){
				removeActionCategory(theRole.getActionCat());
			}
		}
		return success;
	}
	/**Returns all roles possible
	 * @return ArrayList [RolesOrig]
	 */
	public ArrayList<Role> getRolesPossible(){
		return rolesPossible;
	}
	/**Removes all Roles from being possible*/
	public void clearRolesPossible(){
		rolesPossible.clear();
	}
	/**Returns is RoleID is in the Roles Possible list*/
	public boolean isRolePossible(int roleId){
		boolean success = false;
		for(Role checkedRole:rolesPossible){
			if(checkedRole.getEID() == roleId){
				success = true;
				break;
			}
		}
		return success;
	}
	/**Returns is Role is in the Roles Possible list
	 * @param aff Affliation
	 * @param cat Category
	 */
	public boolean isRolePossible(String aff, String cat){
		boolean success = false;
		if(aff.equals("RANDOM") && cat.equals("RANDOM")){
			if(!rolesPossible.isEmpty()){success = true;}//As long as there is a single Role...RANDOM RANDOM is possible
		}
		else if(aff.equals("RANDOM")){
			for(Role checkedRole:rolesPossible){
				if(checkedRole.getCategory()[0].equals(cat)){
					success = true;
					break;
				}
				else if(checkedRole.getCategory()[1] != null){
					if(checkedRole.getCategory()[1].equals(cat)){
						success = true;
						break;
					}
				}
			}
		}
		else if(cat.equals("RANDOM")){
			for(Role checkedRole:rolesPossible){
				if(checkedRole.getAffiliation().equals(aff)){
					success = true;
					break;
				}
			}
		}
		else{
			for(Role checkedRole:rolesPossible){
				if(checkedRole.getAffiliation().equals(aff)){
					if(checkedRole.getCategory()[0].equals(cat)){
						success = true;
						break;
					}
					else if(checkedRole.getCategory()[1] != null){
						if(checkedRole.getCategory()[1].equals(cat)){
							success = true;
							break;
						}
					}
				}
			}
		}
		return success;
	}
	/**Return if any of the roles possible have the inputted action category*/
	public boolean isRolePossibleWithActionCat(String cat){
		boolean theReturn = false;
		for(Role role:getRolesPossible()){
			if(role.getActionCat().equals(cat)){
				theReturn = true;
				break;
			}
		}
		return theReturn;
	}
	/**Removals all unobtainable roles from the Role Setup*/
	public void removeImpossibleFromSetup(){
		ArrayList<RolesOrig> roles = roleSetup;
		for(RolesOrig role:roles){
			if(role != null){
				if(role.eID > 0){
					if(!isRolePossible(role.eID)){
						removeFromRoleSetup(role.eID);
					}
				}
				else{
					if(!isRolePossible(role.affiliation,role.category[0])){
						removeFromRoleSetup(role.affiliation,role.category[0]);
					}
				}
			}
		}
	}
	/** Adds a role to setup by database ID*/
	public boolean addToRoleSetup(int id){
		boolean success = false;
		if(isRolePossible(id)){
			Role role = Game.Base.MySql.grabRole(id);
			if(role==null){Game.Base.Console.warning("Could not retrieve a role the list based on manuel id, role not added");}
			else{
				RolesOrig roleAdded = new RolesOrig();
				roleAdded.eID = role.getEID();
				roleAdded.roleName = role.getName();
				roleAdded.affiliation = role.getAffiliation();
				roleAdded.category = role.getCategory();
				this.roleSetup.add(roleAdded);
				success = true;
			}
		}
		return success;
	}
	/** Adds a role to setup by  affliation and category*/
	public boolean addToRoleSetup(String aff, String cat){
		boolean success = false;
		if(isRolePossible(aff,cat)){
			RolesOrig roleAdded = new RolesOrig();
			roleAdded.eID = 0;
			roleAdded.affiliation = aff;
			roleAdded.category[0] = cat;
			this.roleSetup.add(roleAdded);
			success = true;
		}
		return success;
	}
	/** Removes a choosen role from the setup list*/
	public boolean removeFromRoleSetup(int id){
		boolean success = false;
		ArrayList<RolesOrig> temp = roleSetup;
		for(RolesOrig role : temp){
			if(role.eID == id){
				roleSetup.remove(role);
				success = true;
				break;
			}
		}
		return success;
	}
	/** Removes a choosen role from the setup list*/
	public boolean removeFromRoleSetup(String aff, String cat){
		boolean success = false;
		ArrayList<RolesOrig> temp = roleSetup;
		for(RolesOrig role : temp){
			if(role.affiliation == aff && role.category[0] == cat){
				roleSetup.remove(role);
				success = true;
				break;
			}
		}
		return success;
	}
	/** Returns all roles in the setup */
	public ArrayList<RolesOrig> getRoleSetup(){
		return this.roleSetup;
	}
	/**Removes all Roles from setup*/
	public void clearRoleSetup(){
		roleSetup.clear();
	}
	/**Returns list of action categories*/
	public ArrayList<String> getActionCategories(){
		return actionCat;
	}
	/**Removes all action categories*/
	public void clearActionCategories(){
		actionCat.clear();
	}
	/**Adds an action category*/
	public void addActionCategory(String cat){
		if(!actionCat.contains(cat)){
			actionCat.add(cat);
		}
	}
	/**Removes an action category*/
	public void removeActionCategory(String cat){
		actionCat.remove(cat);
	}
	/**Moves an action category up*/
	public void moveActionCategoryUp(String cat){
		int catIndex = actionCat.indexOf(cat);
		if(catIndex > 0){
			Collections.swap(actionCat,catIndex,catIndex-1);
		}
	}
	/**Moves an action category down*/
	public void moveActionCategoryDown(String cat){
		int catIndex = actionCat.indexOf(cat);
		if(catIndex != -1){
			if(catIndex < actionCat.size()-1){
				Collections.swap(actionCat,catIndex,catIndex+1);
			}
		}
	}
	/**Returns Role based on role number inputted
	 * @param roleNum
	 */
	public Role getRole(int roleNum){
		return roles[roleNum];
	}
	/**Returns Role based on playerNum inputted
	 * @param playerNum
	 */
	public Role getPlayerRole(int playerNum){
		Role theReturn = null;
		Players player = getPlayer(playerNum);
		if(player != null){theReturn = getRole(player.getRoleNumber());}
		return theReturn;
		//return getPlayer(playerNum).getRole();
	}
	/**Returns Role based on Player inputted
	 * @param Player
	 */
	public Role getPlayerRole(Players player){
		return getRole(player.getRoleNumber());
		//return getPlayer(playerNum).getRole();
	}
	/**Returns Role based on playerNum inputted with possible playerNum switches
	 * @param playerNum
	 */
	public Role getPlayerRoleWithSwitch(int playerNum){
		return getRole(getPlayerWithSwitch(playerNum).getRoleNumber());
	}
	/**Return Roles that have the inputted action Category*/
	public ArrayList<Role> getRolesWithActionCat(String cat){
		ArrayList<Role> roleList = new ArrayList<Role>();
		for(Role role:roles){
			if(role != null){
				if(role.getActionCat().equals(cat)){
					roleList.add(role);
				}
			}
		}
		return roleList;
	}
	/** Removes the votes and targets of all players for the next phase and well as night action targets*/
	public void clearRoleTargets(){
		//alive[playernum] = target
		//ballot[playernum] = numVotes
		for(int i = 0; i < this.roles.length;i++){
			if(roles[i]!=null)roles[i].clearTargets();
		}
	}
	/**Changes the role of the player
	 * @param playerNum
	 * @param newRoleId the Database id of new Role
	 */
	public void changePlayerRole(int playerNum, int newRoleId){
		Players player = getPlayer(playerNum);
		if(player != null){
			Role oldRole = getPlayerRole(playerNum);
			Role newRole;
			newRole = Game.Base.MySql.grabRole(newRoleId);
			if(newRole != null){
				newRole.setMatch(this);
				newRole.setPlayerNum(playerNum);
				newRole.setHp(oldRole.getHp());
				newRole.deathDesc = oldRole.deathDesc;
				newRole.deathTypes = oldRole.deathTypes;
				roles[player.getRoleNumber()] = newRole;
			}
		}
	}
	/**Returns whether the role exists in play or not
	 * @param roleId database id
	 * @return
	 */
	public boolean isRoleExist(int roleId){
		boolean theReturn = false;
		List<Players> players = getPlayerList();
		for(Players player : players){
			if(player != null){
				if(getPlayerRole(player).getEID() == roleId){
					theReturn = true;
					break;
				}
			}
		}
		return theReturn;
	}
	/**Returns whether role exists in play and is alive or not
	 * @param roleId database id
	 * @return
	 */
	public boolean isRoleAlive(int roleId){
		boolean theReturn = false;
		List<Players> players = getPlayerList();
		for(Players player : players){
			if(player != null){
				if(getPlayerRole(player).isAlive()){
					if(getPlayerRole(player).getEID() == roleId){
						theReturn = true;
						break;
					}
				}
			}
		}
		return theReturn;
	}
	/**Returns the number of this role that are still alive
	 * @param roleId database id
	 * @return
	 */
	public int numRoleAlive(int roleId){
		int theReturn = 0;
		List<Players> players = getPlayerList();
		for(Players player : players){
			if(player != null){
				if(getPlayerRole(player).isAlive()){
					if(getPlayerRole(player).getEID() == roleId){
						theReturn++;
						break;
					}
				}
			}
		}
		return theReturn;
	}

//////////////////////////
//////////Messages////////
//////////////////////////
	/** Send to all in Match */
	public void send(byte[] message){
		this.send(message,(String)null);
	}
	/** Send to all in Match except this chara */
	public void send(byte[] message, Character except){
		int exception = except.getEID();
		for(Character chara : this.characters.values()){
			try{
				if(chara.getEID()!=exception){chara.send(message);}//, mType);
			}
			catch (Exception e){
				Game.Base.Console.warning("Failed to send to char "+chara.getEID()+": "+e.getMessage());
			}

		}
	}
	/** Sends to all in Match formatted my mType
	 * @param message
	 * @param mType
	 */
	public void send(byte[] message, String mType){
		for(Character chara : this.characters.values()){
			try{
				chara.send(message);//, mType);
			}
			catch (Exception e){
				Game.Base.Console.warning("Failed to send to char "+chara.getEID()+": "+e.getMessage());
			}

		}
	}
	/**Sends to all in Match formatted my mType<br>
	 * Currently used to send Character talking messages..needs to expand
	 * @param message
	 * @param mType "roomSay"
	 * @param from Character speaking
	 */
	public void send(String message, String mType, Character from){//assuming is mType is roomsay for now
		for(Character chara : this.characters.values()){
			try{
				chara.send(CmdCompile.chatScreen2(from, message));//, mType);
			}
			catch (Exception e){
				Game.Base.Console.warning("Failed to send to char "+chara.getEID()+": "+e.getMessage());
			}

		}
	}
	/** Send to this player */
	public void sendToPlayerNum(int playerNum, String message){
		if(getPhaseMain() != Constants.PHASEMAIN_SETUP && playerNum != 0){//if not in setup
			Character chara = getCharacter(getPlayer(playerNum).getEID());
			try{
				chara.send(CmdCompile.chatScreen(message));
			}
			catch (Exception e){
				Game.Base.Console.warning("Failed to send to char "+chara.getEID()+" playerNum "+playerNum+" : "+e.getMessage());
			}
		}
	}
	/** Send to this player */
	public void sendToPlayerNum(int playerNum, String message, Players from){
		if(getPhaseMain() != Constants.PHASEMAIN_SETUP && playerNum != 0){//if not in setup
			Character chara = getCharacter(getPlayer(playerNum).getEID());
			try{
				chara.send(CmdCompile.chatScreen(from, message));
			}
			catch (Exception e){
				Game.Base.Console.warning("Failed to send to char "+chara.getEID()+" playerNum "+playerNum+" : "+e.getMessage());
			}
		}
	}
	/**
	 * Passes 'chat' through the chatController for inplay speaking
	 * @param fromPlayerNum the speaking players number
	 * @param message the player is saying
	 */
	public void chatter(int fromPlayerNum, String message){//TODO ChatChannels: expand greatly
		if(getPhaseMain() == Constants.PHASEMAIN_INPLAY && fromPlayerNum != 0){//as long as in play
			//cycle through each of his channels
			for(int chanId : chatGroup.getPlayer(fromPlayerNum).channels.values()){
				com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Match.ChatGroup.ChatChannel channel = chatGroup.getChannel(chanId);
				//if this channel can speak at this time....yes i know it looks complex...maybe need to find better way
				//Game.Base.Console.debug("channel is "+channel.dayOrNight+" and is is now "+getPhaseDayType());
				if((channel.dayOrNight == 0 && (getPhaseDayType() == Constants.PHASEDAYTYPE_DISCUSSION || getPhaseDayType() == Constants.PHASEDAYTYPE_NORMAL || getPhaseDayType() == Constants.PHASEDAYTYPE_TRIALVOTE || getPhaseDayType() == Constants.PHASEDAYTYPE_LYNCH || (getPhaseDayType() == Constants.PHASEDAYTYPE_TRIALPLEAD && fromPlayerNum == trialplayer))) ||
					(channel.dayOrNight == 1 && getPhaseDayType() == Constants.PHASEDAYTYPE_NIGHT) ||
					channel.dayOrNight == 2){
					if(channel.players.containsKey(fromPlayerNum)){
						if(channel.getPlayer(fromPlayerNum).talkRights == 1){//if this player is allowed to talk in this channel
							//cycle through all players in this channel
							for(com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Match.ChatGroup.ChatChannel.PlayerRights player : channel.players.values()){
								if(player.listenRights == 1){//if this player is allowed in listen in this channel
									sendToPlayerNum(player.id, message, players[fromPlayerNum]);//send message to this player
								}
							}
						}
					}
				}
			}
		}
	}
//////////////////////////
///////////Other//////////
//////////////////////////
	/**Returns the specified Team by name
	 * @param name
	 * @return null if non existant
	 */
	public Team getTeam(String name){
		Team team = null;
		if(teams.containsKey(name)){
			team = teams.get(name);
		}
		return team;
	}
	/**Returns all Teams in play*/
	public ArrayList<Team> getTeams(){
		ArrayList<Team> theReturn = new ArrayList<Team>();
		for(Team team: teams.values()){
			theReturn.add(team);
		}
		return theReturn;
	}
	/** Returns number of votes required for democracy to kick in */
 	public int requiredVotes(){
		int required = (int) Math.ceil(getNumPlayersAlive() / 2)+1;
		/*if(required == (getNumPlayersAlive() / 2)){
			required++;
		}*/
		return required;
	}
	/**Performs standard scriptProcess for the specified onEvent for all players by order of rolesActionOrder (Script, Role)
	 * @param event onEventScript
	 */
	public void doScriptProcess(String event){
		for(Team team:getTeams()){
			if(StringUtils.isNotEmpty(team.getScript(event))){
				new scriptProcess(event, team.getScript(event), team);
			}
		}
		//RoleBlock makes user have no target
		for(Role role : roles){
			if(role != null){
				if(role.hasFlag("ROLEBLOCKED")){
					role.clearTargets();
				}
			}
		}
		//end RoleBlock
		for(String action:actionCat){
			ArrayList<Role> roleList = getRolesWithActionCat(action);
			for(Role role:roleList){
				for(Flag flag : role.getFlags().values()){//TODO Flags: test if work
					if(flag.isScriptedPre()){
						new scriptProcess(event, flag.getScriptPre(event), role);
					}
				}
				if(StringUtils.isNotEmpty(role.getScript(event))){
					try{
						new scriptProcess(event, role.getScript(event), role);
					}
					catch(Exception e){Game.Base.Console.printStackTrace(e);}
				}
				for(Flag flag : role.getFlags().values()){//TODO Flags: test if work
					if(flag.isScriptedPost()){
						new scriptProcess(event, flag.getScriptPost(event), role);
					}
				}
			}
		}
	}
	/**Performs standard scriptProcess for the specified onEvent for a single player (Script, Role)
	 * @param playerNum
	 * @param event
	 */
	public void doScriptProcess(int playerNum, String event){
		Role role = getPlayerRole(playerNum);
		for(Flag flag : role.getFlags().values()){//TODO Flags" test if work
			if(flag.isScriptedPre()){
				new scriptProcess(event, flag.getScriptPre(event), role);
			}
		}
		if(StringUtils.isNotEmpty(role.getScript(event))){//if there is a event script
				new scriptProcess(event, role.getScript(event), role);//does event script
		}
		for(Flag flag : role.getFlags().values()){//TODO Flags" test if work
			if(flag.isScriptedPost()){
				new scriptProcess(event, flag.getScriptPost(event), role);
			}
		}
	}
	/** Player votes for another if in day, or player votes inno/guilty if on trial. */
	public void votePlayer(int voter, int vote){
		if(getPhaseMain()==Constants.PHASEMAIN_INPLAY){//if inplay
			if(getPhaseDayType()==Constants.PHASEDAYTYPE_NORMAL){//in normal type
				//if(array_key_exists($target, $this->alive)){//check if $target(playernum) is alive/exists
				if(this.players[vote]!=null){//check if $target(playernum) is alive/exists
					//echo "player $voter's target is currently ".$this->alive[$voter]."\n";
					if(vote==voter){
					//do nothing(can't vote against yourself..duhh)
					}
					else if(getPlayerVote(voter)==0){//if voter wasnt orginally targeting anyone yet
						this.ballot[vote]++;
						setPlayerVote(voter, vote);//set the voter to target the ..target
						this.send(CmdCompile.chatScreen(getPlayer(voter).getName()+" voted to lynch "+getPlayer(vote).getName()+"."));
						if(this.ballot[vote] >= requiredVotes()){//if half/over votes
							beginTrialDefense(vote);//send target to trial
						}
						else{
							this.send(CmdCompile.voteCount(vote, this.ballot[vote]));
						}
					}
					else if(getPlayerVote(voter)==vote){//if voter targets the target again
						this.ballot[vote]--;
						setPlayerVote(voter, 0);
						this.send(CmdCompile.chatScreen(getPlayer(voter).getName()+" cancelled their vote."));
						this.send(CmdCompile.voteCount(vote, this.ballot[vote]));
					}
					else{//if voter is targeting someone else if
						this.ballot[getPlayerVote(voter)]--;//remove the old vote
						this.send(CmdCompile.voteCount(getPlayerVote(voter), this.ballot[getPlayerVote(voter)]));
						this.ballot[vote]++;
						setPlayerVote(voter, vote);//target them
						this.send(CmdCompile.chatScreen(getPlayer(voter).getName()+" voted to lynch "+getPlayer(vote).getName()+" instead."));
						if(this.ballot[vote] >= requiredVotes()){//if half/over votes
							beginTrialDefense(vote);//send target to trial
						}
						else{
							this.send(CmdCompile.voteCount(vote, this.ballot[vote]));
						}
					}
				}
				else{//if player doesnt exist..do this(later remove all code from here)
					getCharacter(getPlayer(voter).getEID()).send(CmdCompile.chatScreen("There is no such player!"));
				}
			}
			else if(getPhaseDayType() == Constants.PHASEDAYTYPE_TRIALVOTE){//if in trial vote phase
				//alive[1] = num inno votes;alive[2] = num guilty votes
				if(voter != this.trialplayer){//make sure person on trial isnt voting...
					if(vote == 1 || vote == 2){//accept $target as only 1 or 2(inno/guilty)
						if(getPlayerVote(voter)==0){//if voter isnt voting yet
							this.ballot[vote]++;
							this.setPlayerVote(voter, vote);//set the voter to target the ..target
							this.send(CmdCompile.chatScreen(getPlayer(voter).getName()+" placed a vote."));
						}
						else if(getPlayerVote(voter)==vote){//if voter targets the target again
							this.ballot[vote]--;
							this.setPlayerVote(voter, 0);//cancel the vote
							this.send(CmdCompile.chatScreen(getPlayer(voter).getName()+" cancelled their vote."));
						}
						else{//if voter is voting something else..change vote
							this.ballot[getPlayerVote(voter)]--;//remove the old vote
							this.ballot[vote]++;
							this.setPlayerVote(voter, vote);//target them
							this.send(CmdCompile.chatScreen(getPlayer(voter).getName()+" changed their vote."));
						}
					}
				}
			}
		}
	}
	/**Adds a reason for player death to be displayed
	 * @param playerNum
	 * @param deathType
	 * @param deathDesc
	 */
	public void playerDeathReasons(int playerNum,String deathType,String deathDesc){
		Role playerRole = getRole(getAlivePlayer(playerNum).getRoleNumber());
		playerRole.deathTypes.add(deathType);
		if(deathDesc != null){playerRole.deathDesc.add(deathDesc);}
	}
	/**Removes all reasons for death
	 * @param playerNum
	 */
	public void playerDeathReasonsClear(int playerNum){
		Role playerRole = getRole(getAlivePlayer(playerNum).getRoleNumber());
		playerRole.deathTypes.clear();
		playerRole.deathDesc.clear();
	}
	/** Applys damages to player, such as being attacked, doesn't mean death until end of phase
	 * includes reason for death if it DOES occur
	 * @param playerNum
	 */
	public void damagePlayer(int playerNum,int attacker, String deathType,String deathDesc){
		Role role = getRole(getAlivePlayer(playerNum).getRoleNumber());
		Role attackerRole = getRole(getAlivePlayer(attacker).getRoleNumber());
		if(getPhaseDayType() == Constants.PHASEDAYTYPE_NIGHT){
			new scriptProcess("onAttacked", role.getScript("onAttacked"), role, attackerRole);
			if(!role.hasFlag("NIGHTIMMUNE")){
				playerDeathReasons(playerNum,deathType,deathDesc);
				role.setHp(role.getHp() - 1);
			}
		}
	}
	/** Sends player to graveyard */
	public void killPlayer(int playerNum){
		String name = getPlayer(playerNum).getName();
		String deathDesc = "";
		if(!getPlayerRole(playerNum).deathDesc.isEmpty()){
			for(String death : getPlayerRole(playerNum).deathDesc){
				if(!deathDesc.equals("")){deathDesc += ", ";}
				deathDesc += death;
			}
		}
		else{
			deathDesc = "randomly and unknowingly slaughtered";
		}
		this.send(CmdCompile.chatScreen(name+" was "+deathDesc));
		this.send(CmdCompile.chatScreen(name+"'s role was "+getPlayerRole(playerNum).getName()));
		getPlayerRole(playerNum).setIsAlive(false);
		doScriptProcess(playerNum,"onDeath");
		int nextSlot = 0;
		for(int i = 0; i <= this.graveyard.length; i++){
			if(this.graveyard[i] == null){nextSlot = i;break;}
		}
		this.graveyard[nextSlot] = getPlayer(playerNum);
		//getRole(this.graveyard[nextSlot].getRoleNumber()).deathTypes.add(deathType);
		this.players[playerNum] = null;
	}
	/** Sends player to graveyard */
	public void killPlayer(Role role){
		if(role != null){
			killPlayer(role.getPlayerNum());
		}
	}
	/**Returns TRUE if a inputted Affiliation member is still alive*/
	public boolean isAffliationAlive(String aff){
		for(Players living:getAliveList()){
			if(getRole(living.getRoleNumber()).getAffiliation().equals(aff)){
				return true;
			}
		}
		return false;
	}
	/**Returns TRUE if a inputted Category member is still alive*/
	public boolean isCategoryAlive(String cat){
		String[] temp;
		boolean theReturn = false;
		for(Players player:getAliveList()){
			temp = getPlayerRole(player).getCategory();
			if(temp != null){
				if(temp[0] != null){
					if(temp[0].equals(cat)){
						theReturn = true;
						break;
					}
				}
				if(temp[1] != null){
					if(temp[1].equals(cat)){
						theReturn = true;
						break;
					}
				}
			}
		}
		return theReturn;
	}
	/**
	 * Temporarly diverts all visits/script actions against playerNum to another instead
	 * @param playerFrom Player diverting From
	 * @param playerTo Player diverting To
	 */
	public void setSwitchedPlayerNum(int playerFrom, int playerTo){
		playerNumSwitch.put(playerFrom, playerTo);
	}
	/** Function will run Victory checks for all living characters then
	 *  do MayGameEnd checks on them. If all living players state game is allowed to end, will return TRUE.
	 * @return True for the game to end; False for the game to continue
	 */
	private boolean checkVictoryAndGameEnd(){
		int numberChecked,numberMet = 0;
		for(Team team : teams.values()){
			new scriptProcess("victoryCon", team.getScript("victoryCon"), team);
			Game.Base.Console.debug(team.getName()+"'s victory: "+team.getVictory());
		}
		for(Team team : teams.values()){
			if(StringUtils.isNotEmpty(team.getScript("mayGameEndCon"))){
				new scriptProcess("mayGameEndCon", team.getScript("mayGameEndCon"), team);
			}
			else{team.setMayGameEnd(true);Game.Base.Console.debug(team.getName()+" has no mayGameEndCon");}
			Game.Base.Console.debug(team.getName()+"'s mayGameEnd: "+team.getMayGameEnd());
		}/*
		for(Team team : getTeams()){
			//new scriptProcess(team.getEndGameCon());
		}*/
		//Victory condition of all players
		for(Players player : getPlayerList()){
			if(getPlayerRole(player).getTeamWin()){
				getPlayerRole(player).setVictory(getTeam(getPlayerRole(player).getTeamName()).getVictory());//sets victory for player
			}
			else{
				new scriptProcess("victoryCon", getPlayerRole(player).getScript("victoryCon"), getPlayerRole(player));//does victory checks
			}
		}
		//Check if game may end for all living players
		for(Players player : getAliveList()){
			if(getPlayerRole(player).getTeamWin()){
				getPlayerRole(player).setMayGameEnd(getTeam(getPlayerRole(player).getTeamName()).getMayGameEnd());//sets gameMayEnd for player
			}
			else{
				if(StringUtils.isNotEmpty(getPlayerRole(player).getScript("mayGameEndCon"))){
					new scriptProcess("mayGameEndCon", getPlayerRole(player).getScript("mayGameEndCon"), getPlayerRole(player));//does game may end con
				}
				else{getPlayerRole(player).setMayGameEnd(true);}
			}
		}
		//Last check if game may now end
		numberChecked = 0;numberMet = 0;
		for(Players player : getAliveList()){
			numberChecked++;
			if(getPlayerRole(player).getMayGameEnd()){numberMet++;}
		}
		if(numberMet >= numberChecked){
			//end the game
			return true;
		}
		return false;
	}
	/** Starts timer to start game with current settings, only in setup mode */
	public void gameStart(){
		if(getPhaseMain() == Constants.PHASEMAIN_SETUP && !isAdvancePhaseTimer()){
			if(this.roleSetup.size() == getNumChars()){
				this.addAdvancePhaseTimer(10);
				this.send(CmdCompile.chatScreen("Game starting in 10 seconds."));
				this.send(CmdCompile.timerStart(10));
			}
			else{
				this.send(CmdCompile.chatScreen("<b><font color=\"FF0000\">Game has "+getNumChars()+" players and "+this.roleSetup.size()+" roles. Unable to start.</font></b>"));
			}
		}
	}
	/** Deletes the gameStart() timer when in setup mode */
	public void gameCancel(){
		if(this.getPhaseMain() == Constants.PHASEMAIN_SETUP && isAdvancePhaseTimer()){
			//$game->removeTimer($this->timer);
			removeAdvancePhaseTimer();
			this.send(CmdCompile.chatScreen("Game start cancelled."));
			this.send(CmdCompile.timerStop());
		}
	}
//////////////////////////
//////////Phases//////////
//////////////////////////
	/** Changes the Match object's Main phase<br>
	 * @param phase 0 = setup/1 = naming/2 = inplay/3 = endgame
	 */
	public void setPhaseMain(int phase){
		this.phaseMain = phase;
	}
	/**
	 * Returns which Main phase the Match object is in<br>
	 * @return 0 = setup/1 = naming/2 = inplay/3 = endgame
	 */
	public int getPhaseMain(){
		return this.phaseMain;
	}
	/**
	 * Changes the Match object's day<br>
	 * @param phase day
	 */
	public void setPhaseDay(int phase){
		this.phaseDay = phase;
	}
	/**
	 * Returns which day the Match object is in<br>
	 * @return Day
	 */
	public int getPhaseDay(){
		return this.phaseDay;
	}
	/**
	 * Changes the Match object's day's type<br>
	 * @param phase 1=discuss/2=normal/3=trialplead/4=trialvote/6=lynch/8=night
	 */
	public void setPhaseDayType(int phase){
		this.phaseDayType = phase;
		send(CmdCompile.setTimeOfDay(phase));
	}
	/**
	 * Returns which type the day of the Match object is in<br>
	 * @return int 1=discuss/2=normal/3=trialplead/4=trialvote/6=lynch/8=night
	 */
	public int getPhaseDayType(){
		return this.phaseDayType;
	}
	/**
	 * Changes the Match object's phase based on current settings
	 * and the current phase. Basically goes to next day/night<br>
	 * *Primarily ran through use of a timer.<br>
	 * *Function loops itself through use of a timer.
	 */
	public void advancePhase(){
		switch(getPhaseMain()){
		case Constants.PHASEMAIN_SETUP://setup ending
			if(assignRoles()){//give random roles
				beginNaming();//change to naming phase
			}
			else{
				setPhaseMain(Constants.PHASEMAIN_SETUP);//retturns to setup
			}
			break;
		case Constants.PHASEMAIN_NAMING://naming ending
			this.send(CmdCompile.closeLayer("nameSelection"));//close name selection window(if open)
			for(int i = 1; i < this.players.length; i++){//Give names to the nameless
				if(this.players[i].getName() == null){
					chooseName(i, StringFunctions.make_rand_name());
				}
			}
			doScriptProcess("onStartup");
			for(int i = 1; i < this.players.length; i++){//tell each of their roles
				getCharacter((getPlayer(i).getEID())).send(CmdCompile.chatScreen(StringFunctions.bbColor("FFFF00", getPlayer(i).inGameName+", your role is ")+StringFunctions.bbColor("00FF00", getPlayerRole(i).getName())));
				getCharacter((getPlayer(i).getEID())).send(CmdCompile.setPlayerNum(i));
				getCharacter((getPlayer(i).getEID())).send(CmdCompile.setTargetables(getPlayerRole(i)));
				getCharacter((getPlayer(i).getEID())).send(CmdCompile.matchStart());
			}

			//TODO Match Delay needed here to allow players to view role
			if(getSetting("start_game_at")==0) beginDay();//Start day sequence
			else if(getSetting("start_game_at")==1) beginDiscuss();//Start discuss sequence
			else if(getSetting("start_game_at")==2) beginNight();//Start night sequence
			break;
		case Constants.PHASEMAIN_INPLAY:
			switch(getPhaseDayType()){
			case Constants.PHASEDAYTYPE_DISCUSSION://from discuss
				if(getPhaseDay()==1 && getSetting("start_game_at")==1){
					beginNight();//Start night sequence
				}
				else{
					beginDay();//Start day sequence
				}
				break;
			case Constants.PHASEDAYTYPE_NORMAL://coming from day
				if(checkVictoryAndGameEnd()){beginGameEnd();}
				else{
					doScriptProcess("onDayEnd");
					beginNight();//Start night sequence
				}
				break;
			case Constants.PHASEDAYTYPE_TRIALPLEAD://from trialplead
				beginTrialVote();//go trialvote
				break;
			case Constants.PHASEDAYTYPE_TRIALVOTE://from trialvote inno/guilty
				this.send(CmdCompile.chatScreen("The trial is over and the votes have been counted."));
				if(this.ballot[1] < this.ballot[2]){//if there are more guilty than inno votes
					beginLynch();
				}
				else{//if inno
					this.send(CmdCompile.chatScreen("The town has decided to pardon "+getPlayer(trialplayer).getName()+"."));
					this.trialplayer = 0;//no one on trial anymore
					if(getSetting("trial_pause_day")==1){
						beginDay(this.timerremain);//if inno and paused time, go to 2(normal)
					}
					else if((this.timerremain - System.currentTimeMillis()) > 0){
						beginDay((this.timerremain - Math.round((System.currentTimeMillis()/1000))) + 2);
					}
					else{
						beginNight();//if inno no time pause, go to 8(night)
					}
				}
				break;
			case Constants.PHASEDAYTYPE_LYNCH://from Lynch
				if(checkVictoryAndGameEnd()){beginGameEnd();}
				else beginNight();//Start night sequence
				break;
			case Constants.PHASEDAYTYPE_NIGHT://night ending
				//Night actions of all players here
				Game.Base.Console.debug("Night is about to end...");
				/*for(int i : rolesActionOrder.values()){
					if((getPlayerRole(i).getTarget1() > 0 || getPlayerRole(i).getTarget2() > 0) && getPlayerRole(i).hp > 0){//if any target is set and role is alive
						try{
							Game.Base.Console.debug("Player "+getPlayer(i).getPlayerNumber()+" "+getPlayer(i).getName()+" is targeting Player "+getPlayerRole(i).getTarget1());
							new scriptProcess(getPlayerRole(i).getScript("onNightEnd"), getPlayerRole(i));//does their night action
						}
						catch(Exception e){Game.Base.Console.printStackTrace(e);}
					}
				}*/
				doScriptProcess("onNightEnd");
				Game.Base.Console.debug("Checked night actions");
				clearRoleTargets();
				for(Players player:getAliveList()){//process all players if to see if alive or dead
					if(!getPlayerRole(player).isAlive()){
						killPlayer(player.getPlayerNumber());
					}
					else{//if still alive
						playerDeathReasonsClear(player.getPlayerNumber());
						getPlayerRole(player).clearVisitedBy();
					}
				}
				Game.Base.Console.debug("Checking wins ect");
				if(checkVictoryAndGameEnd()){Game.Base.Console.debug("Night going gameEnd"); beginGameEnd();}
				else {Game.Base.Console.debug("Night going discuss"); beginDiscuss();}//Start discuss sequence
				break;
			}
			break;
		case Constants.PHASEMAIN_ENDGAME://at end game
			Game.Base.Console.debug("EndGame was just completed, booting all");
			for(Character chara : characters.values()){
				chara.leaveMatch();
			}
			Game.removeMatch(this);
			break;
		}
	}
	/** Assigns roles to each player, then randomizes their order. Returns true on success*/
	public boolean assignRoles(){
		boolean noError = true;
		roles = new Role[getNumChars()+1];
		Random rand = new Random();//makes new random number
		if(noError){
			for (int i = 1; i <= getNumChars(); i++){
				RolesOrig origRole = roleSetup.get(i-1);
				if(origRole.eID > 0){//if a role id...grab the single id instead of category
					roles[i] = Game.Base.MySql.grabRole(origRole.eID);
					roles[i].setMatch(this);
					if(roles[i]==null){Game.Base.Console.warning("Could not retrieve a role the list based on manuel id, Start Cancelled");noError = false;break;}
					origRole.roleName = roles[i].getName();
				}
				else{//grab category
					Map<Integer, Integer> list = Game.Base.MySql.grabRoleCatList("DEFAULT",origRole.affiliation,origRole.category[0]);
					if(list.size() == 0){send(CmdCompile.chatScreen("Could not retrieve a role from selected role category list("+origRole.affiliation+" "+origRole.category[0]+"), Start Cancelled"));noError=false;break;}
					int randNum = rand.nextInt(list.size());
					Game.Base.Console.debug("getting random number: "+randNum);
					Role role;
					if((role = Game.Base.MySql.grabRole(list.get(randNum))) != null){
						Game.Base.Console.debug("Grab random role "+role.getName());

						roles[i] = role;
						roles[i].setMatch(this);
					}
					else{//if error grabbing role
						Game.Base.Console.warning("Error grabbing random role "+randNum);
						noError = false;
						break;
					}
				}
			}
		}
		if(noError){
			this.players = null;//emptys the list
			//this.players = new Players[getNumChars()];//set as 0-chars for now
			Players[] playersTemp = new Players[getNumChars()];//set as 0-chars for now
			players = new Players[getNumChars()+1];//set the real players var
			graveyard = new Players[getNumChars()+1];
			int loop = 0;
			for(Character chara : characters.values()){
				playersTemp[loop] = new Players();
				playersTemp[loop].eID = chara.getEID();
				loop++;
			}
			Collections.shuffle(Arrays.asList(playersTemp));
				//$this->players = array_offset($this->players);//increases all keys by +1
				//ksort($this->players);//puts the keys back in order numically
			for (int i = 0; i < playersTemp.length; i++){//attaches role number to each player
				playersTemp[i].roleNumber = i+1;
				Game.Base.Console.debug("player EID="+playersTemp[i].eID+", roleNumber="+(i+1)+", roleEID="+this.roles[i+1].getEID()+", roleName="+this.roles[i+1].getName());

			}
			Collections.shuffle(Arrays.asList(playersTemp));
				//$this->players = array_offset($this->players);
			//loop through and assign player numbers 1+ through max(offseting array by +1)
			for(int i = 0; i < playersTemp.length; i++){
				this.players[i+1] = playersTemp[i];
			}
			votes = new int[players.length+1];
			ballot = new int[players.length+1];
			clearVotes();
			chatGroup.addChannel("daychat",0);
			//chatGroup.addChannel("mafiachat",1);
			chatGroup.addChannel("deadchat",2);
			for (int i = 1; i < players.length; i++) {//sets everyone as alive
				//getCharMem($this->players[$i][0])->setPlayerNum($i);//then set playernum to characters
				getCharacter(getPlayer(i).getEID()).setPlayerNum(i);
				getPlayer(i).playerNumber = i;
				Role tempRole = getPlayerRole(i);//getPlayerRole(i);//getRole(getPlayer(i).roleNumber);
				tempRole.setPlayerNum(i);
				chatGroup.addPlayer(i);
				chatGroup.addPlayerToChannel(i, "daychat", 1, 1);
				if(tempRole.getOnTeam()){//should be on Team(change to getOnTeam())
					if(StringUtils.isEmpty(tempRole.getTeamName())){tempRole.setTeamName(tempRole.getAffiliation());}
					if(!teams.containsKey(tempRole.getTeamName())){teams.put(tempRole.getTeamName(), new Team(this,tempRole.getTeamName()));}//if non existant, make the team
					teams.get(tempRole.getTeamName()).addTeammate(i);
					if(StringUtils.isEmpty(teams.get(tempRole.getTeamName()).getScript("victoryCon"))){teams.get(tempRole.getTeamName()).setScript("victoryCon", tempRole.getScript("victoryCon"));}//
					if(StringUtils.isEmpty(teams.get(tempRole.getTeamName()).getScript("mayGameEndCon"))){teams.get(tempRole.getTeamName()).setScript("mayGameEndCon", tempRole.getScript("mayGameEndCon"));}
				}
				tempRole = null;
			}
			assignRandomColors();
		}
		return noError;
	}
	/** Applys a name to the given player number and announce it.
	 * Only useable in naming phase
	 *
	 * @param playernum
	 * @param name
	 */
	public void chooseName(int playerNum,String name){//TODO chooseName(): expand function more(add duplicate name detect, ect)
		if(this.getPhaseMain() == Constants.PHASEMAIN_NAMING){//if in naming phase
			if(getPlayer(playerNum).getName() == null){
				this.send(CmdCompile.chatScreen(name+" moved into town."));
				getPlayer(playerNum).inGameName = name;
			}
			else{
				this.send(CmdCompile.chatScreen(getPlayer(playerNum).getName()+" changed their name to "+name));
				getPlayer(playerNum).inGameName = name;
			}
		}
	}
	/**
	 * Entering this phase constitutes the 'Start' of the game.<br>
	 * Upon starting, players are choosen roles, then asked to make a name(If allowed in options)
	 */
	private void beginNaming(){
		//Start naming mode
		//	Notify of time til day
		//	Set timer to advancePhase
		this.setPhaseMain(Constants.PHASEMAIN_NAMING);
		if(getSetting("choose_names")>0){//if names are allowed to be choosen
			this.send(CmdCompile.nameSelectPrompt());
			this.addAdvancePhaseTimer(getSetting("naming_length"));
			this.send(CmdCompile.timerStart(getSetting("naming_length")));
		}
		else{//skip if not
			this.advancePhase();
		}
	}
	/**
	* Start the beginning of a Day's Discussion mode.<br>
	* <br>Allows players to talk, but not vote.<br>
	* This mode will be skipped automatically if the options tell it to.
	*/
	private void beginDiscuss(){
		//Start day discussion sequence
		//playe
		playerNumSwitch.clear();//reset all the player switches(bus drives)
		//Do daily BeforeDay scripts
		doScriptProcess("onDayStart");

		if(getPhaseMain() == Constants.PHASEMAIN_NAMING){//if coming from naming phase(game is just starting)
			setPhaseMain(Constants.PHASEMAIN_INPLAY);//then goto main game
			setPhaseDay(1);//then goto day 1
			setPhaseDayType(Constants.PHASEDAYTYPE_DISCUSSION);//then goto discussion time
		}
		else{//coming from night
			setPhaseDay(getPhaseDay()+1);//change day
			setPhaseDayType(Constants.PHASEDAYTYPE_DISCUSSION);
		}

		if(getPhaseMain() != Constants.PHASEMAIN_NAMING && getSetting("discussion")==0){//if is not coming from naming phase and there is no discuss in settigns
			//if there is no discussion mode
			beginDay();//skip to day
		}
		else{
			//	Notify of time til normal mode
			//	Set timer to advancePhase
			this.addAdvancePhaseTimer(getSetting("discuss_length"));
			this.send(CmdCompile.timerStart(getSetting("discuss_length")));
			this.send(CmdCompile.chatScreen("(Day "+getPhaseDay()+" Discussion)You have "+getSetting("discuss_length")+" secs til discussions end"));
		}
	}
	/**
	 * Starts the Normal Day phase of Match
	 * <p>*If $timer provided, the phase will end according to provided timer
	 * instead of checking option.<br> Used to 'unpause' after leaving a voting session.</p>
	 * @param number $timer (optional)
	 */
	private void beginDay(){
		beginDay(null);
	}
	/**
	 * Starts the Normal Day phase of Match
	 * <p>*If $timer provided, the phase will end according to provided timer
	 * instead of checking option.<br> Used to 'unpause' after leaving a voting session.</p>
	 * @param number $timer (optional)
	 */
	private void beginDay(Integer timeR){
		//Start day sequence
		//	Notify of time til normal mode
		//	Set timer to advancePhase
		clearVotes();
		if(getPhaseMain()==Constants.PHASEMAIN_NAMING){//if coming from naming(game just starting)
			setPhaseMain(Constants.PHASEMAIN_INPLAY);
			setPhaseDay(1);
			setPhaseDayType(Constants.PHASEDAYTYPE_NORMAL);
		}
		else{//if discuss/night ending
			setPhaseDayType(Constants.PHASEDAYTYPE_NORMAL);
		}
		int daytimer;
		if(timeR != null){daytimer=timeR;}else{daytimer=getSetting("day_length");}
			//echo "in day ".floor($this->phase)." normal mode going to phase".$this->phase."\n";
		//$this->timer = $game->addTimer($daytimer,"match",$matchid,"advancePhase");
		this.addAdvancePhaseTimer(daytimer);
		this.send(CmdCompile.timerStart(daytimer));
		this.send(CmdCompile.chatScreen("(Day "+getPhaseDay()+")You have "+daytimer+" secs til night"));
	}
	/**
	 * Starts the Night sequence for all players.<br>
	 * Depending on the player's role, may be able to perform actions or talk to their team
	 */
	private void beginNight(){
		//Start night sequence
		//Do daily BeforNight scripts
		doScriptProcess("onNightStart");
		if(getPhaseMain()==Constants.PHASEMAIN_NAMING){//if coming from naming(game just started)
			setPhaseMain(Constants.PHASEMAIN_INPLAY);
			setPhaseDay(1);
			setPhaseDayType(Constants.PHASEDAYTYPE_NIGHT);
		}
		else{
			setPhaseDayType(Constants.PHASEDAYTYPE_NIGHT);
		}
		//		Notify of time til day
		//	Set timer to advancePhase
		this.addAdvancePhaseTimer(getSetting("night_length"));
		this.send(CmdCompile.timerStart(getSetting("night_length")));
		this.send(CmdCompile.chatScreen("(Night "+getPhaseDay()+")You have "+getSetting("night_length")+" secs til day"));
	}
	/**
	 * Entered upon the majority of players voting a single player during day phase.<br>
	 * The voted player is given a moment to defend themselves before moving to TrialVote.<br><br>
	 * *Only defending player may speak.<br>
	 * *Mode will be skipped to TrialVote if options allow.
	 */
	private void beginTrialDefense(int player){//TODO TrialDefense: Only trialplayer may speak
		//Start defense mode
		//	Notify of time trial vote
		//	Set timer to advancePhase
		this.trialplayer = player;
		if(getSetting("trial_defense")>0){//if there can be a trial defense
			setPhaseDayType(Constants.PHASEDAYTYPE_TRIALPLEAD);
			this.addAdvancePhaseTimer(getSetting("trial_length"));
			this.send(CmdCompile.timerStart(getSetting("trial_length")));
			this.send(CmdCompile.chatScreen(getPlayer(player).getName()+", you are on trial for conspiracy against the town. What is your defense?"));
			this.send(CmdCompile.chatScreen("(Trial Defense)You have "+getSetting("trial_length")+" seconds to plead your defense."));
		}
		else{//skipping defense
			this.beginLynch();
		}
	}
	/**
	 * Mode prompts every player execpt the defended to vote inno or guilty, Majority wins<br><br>
	 * *Tie = Inno
	 */
	private void beginTrialVote(){
		//Start TrialVote mode
		//	Notify of time til votes counted
		//	Set timer to advancePhase
		clearVotes();
		setPhaseDayType(Constants.PHASEDAYTYPE_TRIALVOTE);

			//echo "in trial vote phase ".$this->phase."\n";

		//$this->timer2 = $game->addTimer($this->settings["trial_length"],"match",$matchid,"advancePhase");
		this.addAdvancePhaseTimer(getSetting("trial_length"));
		this.send(CmdCompile.timerStart(getSetting("trial_length")));
		this.send(CmdCompile.chatScreen("(Trial Vote)You have "+getSetting("trial_length")+" secs to vote Guity/Innocent."));
	}
	/** Mode will announce players role and send them to graveyard */
	private void beginLynch(){
		clearVotes();
		setPhaseDayType(Constants.PHASEDAYTYPE_LYNCH);
		playerDeathReasonsClear(trialplayer);
		playerDeathReasons(trialplayer,"Lynched","lynched by an angry mob");
		killPlayer(trialplayer);
		chatGroup.addPlayerToChannel(trialplayer, "daychat", 0, 1);//take away day talking rights, but still let listen
		chatGroup.addPlayerToChannel(trialplayer, "deadchat", 1, 1);//allowing talking in deadchat
		this.addAdvancePhaseTimer(5);
		this.send(CmdCompile.timerStart(5));
		this.send(CmdCompile.chatScreen("(Lynch)"+getPlayer(trialplayer).getName()+" has been lynched by the Town!"));
		this.send(CmdCompile.chatScreen("(Lynch)"+getPlayer(trialplayer).getName()+"'s role was "+getPlayerRole(trialplayer).getName()));
	}
	/** Mode displays the winning teams and players as well as their roles. Adds timer to kill match after certain time*/
	private void beginGameEnd(){
		//TODO Client: remove vote/target buttons
		//Check victoryConditctions for all players
		Game.Base.Console.debug("The Game has completed...");
		setPhaseMain(Constants.PHASEMAIN_ENDGAME);
		//check for winning teams and display
		for(Team team : teams.values()){
			new scriptProcess("victoryCon", team.getScript("victoryCon"), team);
			if(team.getVictory()){
				send(CmdCompile.chatScreen(team.getName()+" won!"));
			}
		}
		//check for player wins
		List<Players> winners = new ArrayList<Players>();
		for(Players player : getPlayerList()){
			if(getPlayerRole(player).getTeamWin()){
				getPlayerRole(player).setVictory(getTeam(getPlayerRole(player).getTeamName()).getVictory());//sets victory for player
			}
			else{
				new scriptProcess("victoryCon", getPlayerRole(player).getScript("victoryCon"), getPlayerRole(player));//does victory checks
			}

			if(getPlayerRole(player).getVictory()){
				winners.add(player);
			}
		}
		//display winning players
		int numWinners;
		String message = "";
		if((numWinners = winners.size()) > 1){
			int loop = numWinners;
			for(Players player:winners){
				loop--;
				message += StringFunctions.bbColor(player.getHexcolor(), player.getName());
				if(loop != 0){if(numWinners > 2){message += ",";}}
				if(loop == 1){message += " and";}
				message += " ";
			}
			message += "have ";
		}
		else if(numWinners == 1){
			for(Players player:winners){
				message += StringFunctions.bbColor(player.getHexcolor(), player.getName())+" ";
			}
			message += "has ";
		}
		else{
			message += "NO ONE ";
		}
		message += "won the game!";
		send(CmdCompile.chatScreen(message));
		//TODO GameEnd: lists off everyone's roles
		this.addAdvancePhaseTimer(300);
		this.send(CmdCompile.timerStart(300));
	}
//////////////////////////
///////Dataholders////////
//////////////////////////
	/** Dataholder for Characters playing match */
	public class Players{
		//public Match match;
		public int eID;
		public String inGameName;
		public int playerNumber;
		public int roleNumber;
		public String hexcolor = "FFFFFF";

		public int getEID(){
			return eID;
		}
		public String getName(){
			return inGameName;
		}
		public int getPlayerNumber(){
			return playerNumber;
		}
		//public Role getRole(){
		//	return match.getRole(this.getRoleNumber());
		//}
		public int getRoleNumber(){
			return roleNumber;
		}
		public String getHexcolor(){
			return hexcolor;
		}
	}
	/** Dataholder for Roles inputted at start of match
	 *  Allows for end game to display how started as what..(cults/disguiser)
	 */
	public class RolesOrig{
		public int eID;
		public String affiliation;
		public String[] category = new String[2];
		public String roleName;
		//public Map options;
	}
	/** Dataholder containing chat channels in game<br>
	 * Controls permission of each channel and who is speaking to who. */
	public class ChatGroup{
		private Map<Integer, ChatChannel> channels = new LinkedHashMap<Integer, ChatChannel>();//<channelNum, ChatChannel settings>
		private Map<Integer, PlayerChannels> players = new LinkedHashMap<Integer, PlayerChannels>();//<playerNum, PlayerChannel list>

		/**Creates a new Channel
		 * @param name
		 * @param dayOrNight 0-day 1-night 2-anytime
		 */
		public void addChannel(String name, int dayOrNight){
			//find next channel number
			int chanNum = 0;
			for(int i = 0; ; i++){
				if(!channels.containsKey(i)){
					chanNum = i;
					break;
				}
			}
			ChatChannel channel = new ChatChannel(chanNum, name, dayOrNight);
			channels.put(chanNum, channel);
		}
		/**Deletes a channel*/
		public void removeChannel(String name){
			for(ChatChannel channel : channels.values()){
				if(channel.channelName.equals(name)){
					channels.remove(channel.id);
					break;
				}
			}
		}
		/**Returns ChatChannel based on id*/
		public ChatChannel getChannel(int id){
			if(channels.containsKey(id)){
				return channels.get(id);
			}
			return null;
		}
		/**Returns ChatChannel based on name*/
		public ChatChannel getChannel(String name){
			ChatChannel theReturn = null;
			for(ChatChannel channel : channels.values()){
				if(channel.channelName.equals(name)){
					theReturn = channel;
					break;
				}
			}
			return theReturn;
		}
		/**Creates this player in the list of ChatGroups*/
		public void addPlayer(int playerNum){
			if(!players.containsKey(playerNum)){
				players.put(playerNum, new PlayerChannels(playerNum));
			}
		}
		/**Deletes this player from the list of ChatGroups including all ChatChannels they are members of*/
		public void removePlayer(int playerNum){
			if(players.containsKey(playerNum)){
				for(int chanId : players.get(playerNum).channels.values()){
					removePlayerFromChannel(playerNum,chanId);
				}
				players.remove(playerNum);
			}
		}
		/**Returns PlayerChannels based on playerNumber*/
		public PlayerChannels getPlayer(int playerNum){
			if(players.containsKey(playerNum)){
				return players.get(playerNum);
			}
			return null;
		}
		/**Adds/edits player to/from the Chat Channel
		 * @param playerNum
		 * @param chanName
		 * @param talkRights 0-none 1-normal 2-anonymous(listeners can't see your name)(unimplemented)
		 * @param listenRights 0-none 1-normal 2-anonymous(can't see anyone's name)(unimplemented)
		 */
		public void addPlayerToChannel(int playerNum, String chanName, int talkRights, int listenRights){
			//TODO ChatChannel: need function to allow player to speak anonymously
			//TODO ChatChannel: need function for player not to see other speakers names
			if(players.containsKey(playerNum)){
				for(ChatChannel channel : channels.values()){
					if(channel.channelName.equals(chanName)){
						channel.addPlayer(playerNum, talkRights, listenRights);//add player to channel
						players.get(playerNum).addChannel(channel.id);//place channel with player
						break;
					}
				}
			}
		}
		/**Removes player from the Chat Channel
		 * @param playerNum
		 * @param chanName
		 */
		public void removePlayerFromChannel(int playerNum, String chanName){
			if(players.containsKey(playerNum)){
				for(ChatChannel channel : channels.values()){
					if(channel.channelName.equals(chanName)){
						channel.removePlayer(playerNum);
						players.get(playerNum).removeChannel(channel.id);
						break;
					}
				}
			}
		}
		/**Removes player from the Chat Channel
		 * @param playerNum
		 * @param chanId
		 */
		public void removePlayerFromChannel(int playerNum, int chanId){
			if(players.containsKey(playerNum)){
				for(ChatChannel channel : channels.values()){
					if(channel.id == chanId){
						channel.removePlayer(playerNum);
						players.get(playerNum).removeChannel(channel.id);
						break;
					}
				}
			}
		}

		/**Holds all channels that this player is related to*/
		private class PlayerChannels{
			private int playerNum;
			private Map<Integer, Integer> channels = new LinkedHashMap<Integer, Integer>();//<ChannelNum, ChannelNum>
			/**Holds all channels that this player is related to*/
			public PlayerChannels(int playerNum){
				this.playerNum = playerNum;
			}
			/**Adds ChatChannel to player's list*/
			public void addChannel(int channel){
				channels.put(channel, channel);
			}
			/**Removes ChatChannel from player's list*/
			public void removeChannel(int channel){
				if(channels.containsKey(channel)){
					channels.remove(channel);
				}
			}
		}

		/**Holds all data within a certain ChatChannel*/
		private class ChatChannel{
			public int id;
			public String channelName;
			public int dayOrNight = 1;//0 = day 1 = night 2 = anytime;
			public Map<Integer, PlayerRights> players = new LinkedHashMap<Integer, PlayerRights>();//<playerNum, PlayerRights>

			/**Holds all data within a certain ChatChannel*/
			public ChatChannel(int id,String name,int dayOrNight){
				this.id = id;
				this.channelName = name;
				this.dayOrNight = dayOrNight;
			}

			/**Adds/edits player to/from the Chat Channel
			 * @param playerNum
			 * @param talkRights
			 * @param listenRights
			 */
			public void addPlayer(int playerNum,int talkRights, int listenRights){
				if(!players.containsKey(playerNum)){
					players.put(playerNum, new PlayerRights(playerNum,talkRights,listenRights));
				}
				else{//just change the settings
					players.get(playerNum).talkRights=talkRights;
					players.get(playerNum).listenRights=listenRights;
				}
			}
			/**Removes player from the Chat Channel*/
			public void removePlayer(int playerNum){
				if(players.containsKey(playerNum)){
					players.remove(playerNum);
				}
			}
			/**Returns the PlayerRights of the playerNumber*/
			public PlayerRights getPlayer(int playerNum){
				if(players.containsKey(playerNum)){
					return players.get(playerNum);
				}
				return null;
			}
			/**Holds player Rights(priviledges) with the chat channel*/
			private class PlayerRights{
				public int id;//playerNum
				//public String playerName;
				/**0-none 1-normal 2-anonymous(listeners can't see your name)(unimplemented)*/
				public int talkRights = 0;//defualt none
				/**0-none 1-normal 2-anonymous(can't see anyone's name)(unimplemented)*/
				public int listenRights = 0;//defualt none
				/**Holds player Rights(priviledges) with the chat channel*/
				public PlayerRights(int id, int talkRights, int listenRights){
					this.id = id;
					this.talkRights = talkRights;
					this.listenRights = listenRights;
				}
			}
		}
	}

	/** Returns ERS Class for scripting support */
	public MatchERS getERSClass(){
		if(this.matchERS == null){
			this.matchERS = new MatchERS(this);
		}
		return this.matchERS;
	}

//////////////////////////
//////////Timer///////////
//////////////////////////
	//XXX Should this be one whole class?
	/** Does a Timer currently exist?
	 * @return True/False
	 */
	public boolean isAdvancePhaseTimer(){
		if(this.timer == null)return false;
		return true;
	}
	/** Outputs remaining time of current timer
	 * @return
	 */
	public long getAdvancePhaseTimerLeft(){
		if(this.isAdvancePhaseTimer()){
			return this.timerTask.getRemaining();
		}
		return 0;
	}
	/** Creates and starts a timer...countdown until performing advancePhase()
	 * @param sec seconds til advancePhase()
	 */
	private void addAdvancePhaseTimer(long sec){
		this.removeAdvancePhaseTimer();
		this.timerTask = new advancePhaseTimer(this, sec*1000);
		this.timer = new Timer();this.timer.schedule(this.timerTask, sec*1000);
	}
	/** Ends and deletes the current Timer */
	private void removeAdvancePhaseTimer(){
		if(this.isAdvancePhaseTimer()){
			this.timer.cancel();
			this.timer.purge();
			this.timer = null;
		}
	}
	/** Must only be used by AdvancePhaseTimer functions */
	private class advancePhaseTimer extends TimerTask  {
		 public Match match;
		 public long start;
		 public long delay;


		 public advancePhaseTimer(Match match, long delay) {
			 this.match = match;
			 this.start = System.currentTimeMillis();
			 this.delay = delay;
		 }
		 /** Returns remianing time left in millisecs **/
		 public long getRemaining(){
			 return delay-(this.start-System.currentTimeMillis());
		 }
		 @Override
		 public void run() {
			 match.advancePhase();
		 }
	}
}
