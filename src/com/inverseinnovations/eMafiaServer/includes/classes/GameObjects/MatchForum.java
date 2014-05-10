/* eMafiaServer - MatchForum.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2014  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.GameObjects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import com.inverseinnovations.eMafiaServer.includes.Constants;
import com.inverseinnovations.eMafiaServer.includes.StringFunctions;
import com.inverseinnovations.eMafiaServer.includes.scriptProcess;
import com.inverseinnovations.eMafiaServer.includes.classes.Game;
import com.inverseinnovations.eMafiaServer.includes.classes.ERS.MatchForumERS;
import com.inverseinnovations.VBulletinAPI.Exception.*;

public class MatchForum extends GameObject{
	public Game Game;
	private int signupThreadId;
	private int signupPostId;
	private int signupSignId;
	private int matchThreadId;
	private int matchPostId;
	private boolean signupChanges = false;//if the post needs to be editted on the hour
	private MatchForumERS matchERS = null;
	private Map<Integer, Players> characters = Collections.synchronizedMap(new ConcurrentHashMap <Integer, Players>());//will make the signup
	private Map<Integer, Players> signups = new LinkedHashMap<Integer, Players>();
	private Map<Integer, Players> reserves = new LinkedHashMap<Integer, Players>();
	private Map<String, Integer> settings = new LinkedHashMap<String, Integer>();
	/**0 = setup/1 = naming/2 = inplay/3 = endgame*/
	private int phaseMain = 0;
	/**Which day/night it is(only should apply is PhaseSetup is "inplay")*/
	private int phaseDay = 1;
	/**1=discuss/2=normal/3=trialplead/4=trialvote/6=lynch/8=night*/
	private int phaseDayType;
	private Players[] players;//players[playernum]=[eID,inGameName,roleNumber]
	private Players[] graveyard;
	private RoleForum[] roles;
	private ChatGroup chatGroup = new ChatGroup();
	private Map<Integer, Integer> rolesActionOrder = new LinkedHashMap<Integer, Integer>();
	private ArrayList<RoleForum> rolesPossible = new ArrayList<RoleForum>();
	private ArrayList<RolesOrig> roleSetup = new ArrayList<RolesOrig>();
	private ArrayList<String> actionCat = new ArrayList<String>();
	private Map<String, TeamForum> teams = new LinkedHashMap<String, TeamForum>();
	private Map<Integer, Integer> playerNumSwitch = new LinkedHashMap<Integer, Integer>();

	/** Creates a new Match with default settings for Forum games*/
	public MatchForum(final Game game, String name) {
		super(0, "E-FM "+name, Constants.TYPE_GAMEOB_MATCHFORUM);
		this.Game = game;
		//Game.setMatchForum(this);

		this.settings.put("host_id", 0);
		this.settings.put("max_chars", 4);//the max num of chars for a game, 15 defualt
		this.settings.put("start_game_at", 1);//0=day/1=day no lynch/2=night
		this.settings.put("day_length", 30);//# in secs. 60-600 default 60
		this.settings.put("night_length", 6);//30-120 default 30
		this.settings.put("discuss_length", 6);//30-180 default 30

		//misc
		this.settings.put("naming_length", 10);//# in secs to choose a name

		//unimplemented settings
		this.settings.put("last_will", 0);//0=no/1=show last will

		//add action categorys for default
		String[] actionsToAdd = new String[]{"Jail","Vest","Witch","Busdrive","Roleblock","Frame","Douse","Heal","Kill","Clean","Invest","Disguise","Recruit"};
		for(String action: actionsToAdd){
			addActionCategory(action);
		}

		//Default Roles Possible
		int[] rolesToAdd = new int[]{1,2,3,4,5,6,7};//Cit,Sheriff,Doc,Mafiso,Escort,GF
		for(int role: rolesToAdd){
			addRolesPossible(role);
		}
	}
	/**
	 * Releases the Match to public signups
	 */
	public void postMatch(){
		//role setup
		String setup = "";
		for(RolesOrig role : this.getRoleSetup()){
			if (role != null){
				if(role.eID > 0){//is a pure role
					RoleForum pureRole = Game.Base.MySql.grabRoleForum(role.eID);
					if(pureRole !=null){//[COLOR=#FF0000]
						String color = "A9A9A9";
						if(pureRole.getAffiliation().equals("TOWN")){color = "00FF00";}else if(pureRole.getAffiliation().equals("MAFIA")){color = "FF0000";}
						//setup += "[URL=http://emafia.inverseinnovations.com/role?id="+pureRole.getEID()+"]"+pureRole.getName()+"[/URL]<BR>";
						//setup += "[URL=http://emafia.inverseinnovations.com/role?id="+pureRole.getEID()+"][COLOR="+color+"]"+pureRole.getName()+"[/COLOR][/URL]<BR>";
						setup += "[COLOR="+color+"]"+pureRole.getName()+"[/COLOR]<BR>";
						//setup += pureRole.getName()+"<BR>";
					}
				}
				else{//is Category
					String color = "A9A9A9";
					if(role.affiliation.equals("TOWN")){color = "00FF00";}else if(role.affiliation.equals("MAFIA")){color = "FF0000";}
					//setup += role.affiliation+" "+role.category[0]+"<BR>";
					setup += "[COLOR="+color+"]"+role.affiliation+" "+role.category[0]+"[/COLOR]<BR>";
				}
			}
		}
		//roles possible
		String possible = "";
		for(RoleForum role : getRolesPossible()){
			if (role != null){
				String color = "A9A9A9";
				if(role.getAffiliation().equals("TOWN")){color = "00FF00";}else if(role.getAffiliation().equals("MAFIA")){color = "FF0000";}
				//possible += "[URL=http://emafia.inverseinnovations.com/role?id="+role.getEID()+"]"+role.getName()+"[/URL]<BR>";
				possible += "[URL=http://emafia.inverseinnovations.com/role?id="+role.getEID()+"][COLOR="+color+"]"+role.getName()+"[/COLOR][/URL]<BR>";
				//possible += role.getName()+"<BR>";
			}
		}
		//order of op
		String order = "";
		for(String action : getActionCategories()){
			order += action+"<BR>";
		}
		String message =
				"[COLOR=#AFEEEE][B][CENTER][COLOR=#DDA0DD][SIZE=6]"+getName()+"[/SIZE][/COLOR]<BR>" +
				"<BR>" +
				"[COLOR=#DDA0DD][SIZE=5]Setup :[/SIZE][/COLOR]<BR>" +
				"<BR>" +
				setup + //SETUP HERE
				"<BR>" +
				"[COLOR=#DDA0DD][SIZE=5]Order of Operations :[/SIZE][/COLOR][SPOILER=]" +
				order + //ORDER OF OP HERE
				"[/SPOILER]<BR>" +
				"[/CENTER][/B][/COLOR]";

		boolean nextPost = false;
		int[] threadMsg = new int[2];
		int[] postMsg = new int[2];
		boolean success = false;
		try {
			threadMsg = Game.Base.ForumAPI.thread_New(Constants.FORUM_SIGNUPS, getName()+" Signups", message);
			success = true;
		}
		catch (VBulletinAPIException e) {}
		if(success){
			setSignupThreadId(threadMsg[0]);
			setSignupPostId(threadMsg[1]);
			Game.Base.Console.debug("New Setup successful... thread ID is "+threadMsg[0]+" post id is "+threadMsg[1]);
			nextPost = true;
		}
		else{
			Game.Base.Console.debug("New Setup failed... : "+threadMsg);
		}
		if(nextPost){
			nextPost = false;
			message =
					"[COLOR=#AFEEEE][B][CENTER]" +
					"[COLOR=#DDA0DD][SIZE=5]Win Conditions :[/SIZE][/COLOR][SPOILER=]" +
					"Win Conditions ToDo" + //WIN CONS HERE
					"[/SPOILER]<BR>" +
					"[SIZE=5][COLOR=#DDA0DD]Possible Roles :[/COLOR][/SIZE][SPOILER=]" +
					possible + //POSSIBLE ROLES HERE
					"[/SPOILER]<BR>" +
					"[COLOR=#DDA0DD][SIZE=5]Rules :[/SIZE][/COLOR]<BR>" +
					"<BR>" +
					"Vote using [Vote] tags.<BR>" +
					"<BR>" +
					"You can post pictures, though follow the forum picture rule.<BR>" +
					"Videos are only allowed when not in autoplay.<BR>" +
					"<BR>" +
					"Show activity with a minimum of 3 constructive or non-forced posts.<BR>" +
					"Lurking is discouraged. If you are forced inactive, please -withdraw from the game so another may take your place.<BR>" +
					"<BR>" +
					"No outside of game communication, other than provided chat channels.(Exception for night chats at this time.)<BR>" +
					"No editing or deleting of posts.<BR>" +
					"No sharing of night chats.<BR>" +
					"[/CENTER][/B][/COLOR]";

			success = false;
			try {
				postMsg = Game.Base.ForumAPI.post_New(getSignupThreadId(), message);
				success = true;
			}
			catch (VBulletinAPIException e) {}
			if(success){
				Game.Base.Console.debug("Second Setup successful... thread ID is "+postMsg[0]+" post id is "+postMsg[1]);
				nextPost = true;
			}
			else{
				Game.Base.Console.debug("Second Setup failed... : ");
			}
		}
		if(nextPost){
			postSignup(false);
		}
	}
	/**
	 * Posts/edits the sign up post within the Signup/setup thread
	 * @param edit true is editting the already made post, false is making new
	 */
	public void postSignup(boolean edit){
		String signs = "";
		int loop = 1;
		for(Players player : getSignupList()){
			if (player != null){
				signs += loop+".) [URL=http://www.sc2mafia.com/forum/member.php?u="+player.getFID()+"]"+player.getName()+"[/URL]<BR>";
				loop++;
			}
		}
		if(loop == 1){signs = "No one has signed<BR>";}
		String reserves = "";
		loop = 1;
		for(Players player : getReserveList()){
			if (player != null){
				reserves += loop+".) [URL=http://www.sc2mafia.com/forum/member.php?u="+player.getFID()+"]"+player.getName()+"[/URL]<BR>";
				loop++;
			}
		}
		if(loop == 1){reserves = "No one has reserved<BR>";}

		String message =
				"[COLOR=#AFEEEE][B][CENTER][COLOR=#DDA0DD][SIZE=5]Sign Up List :[/SIZE][/COLOR]<BR>" +
				signs + //SETUP HERE
				"<BR>" +
				"[COLOR=#DDA0DD][SIZE=5]Reserve List :[/SIZE][/COLOR]<BR>" +
				reserves + //ORDER OF OP HERE
				"<BR>" +
				"To sign up for this match PM the message [COLOR=#FF0000]-sign[/COLOR] or [COLOR=#FF0000]-reserve[/COLOR] to me!<BR>" +
				"This list will update every hour(may not during testing if offline)" +
				"[/CENTER][/B][/COLOR]";
		if(edit){
			if(signupChanges){
				boolean threadMsg = false;
				try {
					threadMsg = Game.Base.ForumAPI.post_Edit(getSignupSignId(), message);
				}catch (VBulletinAPIException e) {}
				if(threadMsg){
					Game.Base.Console.debug("Edit Signup successful...");
					signupChanges = false;
				}
				else{
					Game.Base.Console.debug("Edit Signup failed... id: "+getSignupSignId());
				}
			}
		}
		else{
			int[] threadMsg = new int[2];
			boolean success = false;
			try{
				threadMsg = Game.Base.ForumAPI.post_New(getSignupThreadId(), message);
				success = true;
			}
			catch (VBulletinAPIException e) {e.printStackTrace();}
			if(success){
				Game.Base.Console.debug("Post Signup successful... thread ID is "+threadMsg[0]+" post id is "+threadMsg[1]);
				setSignupSignId(threadMsg[1]);
			}
			else{
				Game.Base.Console.debug("Post Signup failed... : ");
			}
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
	/**
	 * Returns the forum thread id of the generated Signup thread
	 * @return
	 */
	public int getSignupThreadId(){
		return signupThreadId;
	}
	/**
	 * Returns the forum thread id of the generated Match thread
	 * @return
	 */
	public int getMatchThreadId(){
		return matchThreadId;
	}
	/**
	 * Sets the forum thread id of the generated Signup thread
	 * @param id
	 */
	public void setSignupThreadId(int id){
		this.signupThreadId = id;
	}
	/**
	 * Sets the forum thread id of the generated Match thread
	 * @param id
	 */
	public void setMatchThreadId(int id){
		this.matchThreadId = id;
	}
	/**
	 * Returns the forum post id of the first post of the Signup thread
	 * @return
	 */
	public int getSignupPostId(){
		return signupPostId;
	}
	/**
	 * Sets the forum post id of the first post of the Signup thread
	 * @param id
	 */
	public void setSignupPostId(int id){
		this.signupPostId = id;
	}
	/**
	 * Returns the forum post id of the Signing post of the Signup thread
	 * @return
	 */
	public int getSignupSignId(){
		return signupSignId;
	}
	/**
	 * Sets the forum post id of the Signing post of the Signup thread
	 * @param id
	 */
	public void setSignupSignId(int id){
		this.signupSignId = id;
	}
	/**
	 * Returns the forum post id of the first post of the Match thread
	 * @return
	 */
	public int getMatchPostId(){
		return matchPostId;
	}
	/**
	 * Sets the forum post id of the first post of the Match thread
	 * @param id
	 */
	public void setMatchPostId(int id){
		this.matchPostId = id;
	}

//////////////////////////
///////////Users//////////
//////////////////////////
	/**
	 * User is added to the signup list
	 * @param chara Character
	 */
 	public void addUserSignup(int forumId, String username){
 		Players chara = new Players();
 		chara.fID = forumId;
 		chara.inGameName = username;

 		addUserSignup(chara);
	}
 	/**
 	 * User is added to the signup list
 	 * @param chara
 	 */
 	public void addUserSignup(Players chara){
 		signupChanges = true;
 		removeUserReserve(chara.getFID());//remove from reserves first
 		signups.put(chara.getFID(), chara);
 	}
	/**
	 * User is removed from the signup list
	 * @param chara Character
	 */
 	public void removeUserSignup(int forumId){
 		signupChanges = true;
		signups.remove(forumId);
	}
 	/**
 	 * User is removed from the signup list
 	 * @param chara
 	 */
 	public void removeUserSignup(Players chara){
 		removeUserSignup(chara.getFID());
	}
 	/**
 	 * User is added to the reserves list
 	 * @param forumId
 	 * @param username
 	 */
 	public void addUserReserve(int forumId, String username){
 		Players chara = new Players();
 		chara.fID = forumId;
 		chara.inGameName = username;
 		addUserReserve(chara);
	}
 	/**
 	 * User is added to the reserves list
 	 * @param chara
 	 */
 	public void addUserReserve(Players chara){
 		signupChanges = true;
 		removeUserSignup(chara.getFID());//remove from signup first
 		reserves.put(chara.getFID(), chara);
 	}
 	/**
 	 * User is removed from the reserves list
 	 * @param forumId
 	 */
 	public void removeUserReserve(int forumId){
		reserves.remove(forumId);
	}
 	/**
 	 * User is removed from the reserves list
 	 * @param chara
 	 */
 	public void removeUserReserve(Players chara){
 		removeUserReserve(chara.getFID());
	}
	/**
	 * Returns character in match based on EID
	 */
	public Players getCharacter(int eid){
		return this.characters.get(eid);
	}
	/**
	 * Returns Map of all Characters in the match
	 */
	public Map<Integer, Players> getCharacters(){
		return this.characters;
	}
	/**
	 * Returns Map of all Players signed up for the match
	 */
	public Map<Integer, Players> getSignups(){
		return this.signups;
	}
	/**
	 * Returns Map of all Players reserved for the match
	 */
	public Map<Integer, Players> getReserves(){
		return this.reserves;
	}
	/**
	 * Returns List of all Players signed up for the match
	 */
	public List<Players> getSignupList(){
		List<Players> list = new ArrayList<Players>();
		Iterator<Entry<Integer, Players>> it = getSignups().entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Integer, Players> pairs = it.next();
	        if(pairs.getValue() != null){
				list.add(pairs.getValue());
			}
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
	    return list;
	}
	/**
	 * Returns List of all Players reserved for the match
	 */
	public List<Players> getReserveList(){
		List<Players> list = new ArrayList<Players>();
		Iterator<Entry<Integer, Players>> it = getReserves().entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Integer, Players> pairs = it.next();
	        if(pairs.getValue() != null){
				list.add(pairs.getValue());
			}
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
	    return list;
	}
	/** Returns number of characters in room */
	public int getNumChars(){
		return this.characters.size();
	}
	/**Returns List of Characters currently in the match */
	public List<Players> getCharacterList(){//bad TODO
		List<Players> list = new ArrayList<Players>();
		for(Players chara : this.characters.values()){
			list.add(chara);
		}
		return list;
	}
	/**Gets the host id of the match, 0 if there is none*/
	public int getHostId(){
		return getSetting("host_id");
	}
	/**Sets a host*/
	public void setHost(int forumid){
		setSetting("host_id",forumid);
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
//////////////////////////
///////////Roles//////////
//////////////////////////
	/**Allows a Role to be possible*/
	public boolean addRolesPossible(int roleId){
		boolean success = false;
		RoleForum role = Game.Base.MySql.grabRoleForum(roleId);
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
		RoleForum theRole = null;
		for(RoleForum role : rolesPossible){
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
	public ArrayList<RoleForum> getRolesPossible(){
		return rolesPossible;
	}
	/**Removes all Roles from being possible*/
	public void clearRolesPossible(){
		rolesPossible.clear();
	}
	/**Returns is RoleID is in the Roles Possible list*/
	public boolean isRolePossible(int roleId){
		boolean success = false;
		for(RoleForum checkedRole:rolesPossible){
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
			for(RoleForum checkedRole:rolesPossible){
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
			for(RoleForum checkedRole:rolesPossible){
				if(checkedRole.getAffiliation().equals(aff)){
					success = true;
					break;
				}
			}
		}
		else{
			for(RoleForum checkedRole:rolesPossible){
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
		for(RoleForum role:getRolesPossible()){
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
			RoleForum role = Game.Base.MySql.grabRoleForum(id);
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
	public RoleForum getRole(int roleNum){
		return roles[roleNum];
	}
	/**Returns Role based on playerNum inputted
	 * @param playerNum
	 */
	public RoleForum getPlayerRole(int playerNum){
		RoleForum theReturn = null;
		Players player = getPlayer(playerNum);
		if(player != null){theReturn = getRole(player.getRoleNumber());}
		return theReturn;
		//return getPlayer(playerNum).getRole();
	}
	/**Returns Role based on Player inputted
	 * @param Player
	 */
	public RoleForum getPlayerRole(Players player){
		return getRole(player.getRoleNumber());
		//return getPlayer(playerNum).getRole();
	}
	/**Returns Role based on playerNum inputted with possible playerNum switches
	 * @param playerNum
	 */
	public RoleForum getPlayerRoleWithSwitch(int playerNum){
		return getRole(getPlayerWithSwitch(playerNum).getRoleNumber());
	}
	/**Return Roles that have the inputted action Category*/
	public ArrayList<RoleForum> getRolesWithActionCat(String cat){
		ArrayList<RoleForum> roleList = new ArrayList<RoleForum>();
		for(RoleForum role:roles){
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
			RoleForum oldRole = getPlayerRole(playerNum);
			RoleForum newRole;
			newRole = Game.Base.MySql.grabRoleForum(newRoleId);
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
	/** Send message to Signup thread
	 * @return true on successful post
	 */
	public boolean sendSignup(String message){
		boolean success = false;
		try {
			Game.Base.ForumAPI.post_New(getSignupThreadId(), message);
			success = true;
		}
		catch (VBulletinAPIException e) {}
		return success;
	}
	/** Edits the orginal Signup post */
	public void editSignup(String message){
		try {
			Game.Base.ForumAPI.post_Edit(getSignupPostId(), message);
		}catch (VBulletinAPIException e) {}
	}
	/** Send message to Match thread
	 * @return true on successful post
	 */
	public boolean sendMatch(String message){
		boolean success = false;
		try {
			Game.Base.ForumAPI.post_New(getMatchThreadId(), message);
			success = true;
		}
		catch (VBulletinAPIException e) {}
		return success;
	}
	/** Edits the orginal Match post */
	public void editMatch(String message){
		try {
			Game.Base.ForumAPI.post_Edit(getMatchPostId(), message);
		}catch (VBulletinAPIException e) {}
	}
	/** Send PM to this player */
	public void sendToPlayerNum(int playerNum, String title, String message){
		if(getPhaseMain() != Constants.PHASEMAIN_SETUP && playerNum != 0){//if not in setup
			Players player = getPlayer(playerNum);
			try{
				Game.Base.ForumAPI.pm_SendNew(player.getName(), title, message);
			}
			catch (Exception e){
				Game.Base.Console.warning("Failed to send to char "+player.getFID()+" : "+player.getName()+" playerNum "+playerNum+" : "+e.getMessage());
			}
		}
	}
	/**
	 * Passes 'chat' through the chatController for inplay speaking
	 * @param fromPlayerNum the speaking players number
	 * @param message the player is saying
	 */
	/*public void chatter(int fromPlayerNum, String message){//TODO ChatChannels: expand greatly
		if(getPhaseMain() == Constants.PHASEMAIN_INPLAY && fromPlayerNum != 0){//as long as in play
			//cycle through each of his channels
			for(int chanId : chatGroup.getPlayer(fromPlayerNum).channels.values()){
				com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.MatchForum.ChatGroup.ChatChannel channel = chatGroup.getChannel(chanId);
				//if this channel can speak at this time....yes i know it looks complex...maybe need to find better way
				//Game.Base.Console.debug("channel is "+channel.dayOrNight+" and is is now "+getPhaseDayType());
				if((channel.dayOrNight == 0 && (getPhaseDayType() == Constants.PHASEDAYTYPE_DISCUSSION || getPhaseDayType() == Constants.PHASEDAYTYPE_NORMAL || getPhaseDayType() == Constants.PHASEDAYTYPE_TRIALVOTE || getPhaseDayType() == Constants.PHASEDAYTYPE_LYNCH || (getPhaseDayType() == Constants.PHASEDAYTYPE_TRIALPLEAD && fromPlayerNum == trialplayer))) ||
					(channel.dayOrNight == 1 && getPhaseDayType() == Constants.PHASEDAYTYPE_NIGHT) ||
					channel.dayOrNight == 2){
					if(channel.players.containsKey(fromPlayerNum)){
						if(channel.getPlayer(fromPlayerNum).talkRights == 1){//if this player is allowed to talk in this channel
							//cycle through all players in this channel
							for(com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.MatchForum.ChatGroup.ChatChannel.PlayerRights player : channel.players.values()){
								if(player.listenRights == 1){//if this player is allowed in listen in this channel
									sendToPlayerNum(player.id, message, players[fromPlayerNum]);//send message to this player
								}
							}
						}
					}
				}
			}
		}
	}*/

//////////////////////////
///////////Other//////////
//////////////////////////
	/**Returns the specified Team by name
	 * @param name
	 * @return null if non existant
	 */
	public TeamForum getTeam(String name){
		TeamForum team = null;
		if(teams.containsKey(name)){
			team = teams.get(name);
		}
		return team;
	}
	/**Returns all Teams in play*/
	public ArrayList<TeamForum> getTeams(){
		ArrayList<TeamForum> theReturn = new ArrayList<TeamForum>();
		for(TeamForum team: teams.values()){
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
		for(TeamForum team:getTeams()){
			if(StringUtils.isNotEmpty(team.getScript(event))){
				new scriptProcess(event, team.getScript(event), team);
			}
		}
		//RoleBlock makes user have no target
		for(RoleForum role : roles){
			if(role != null){
				if(role.hasFlag("ROLEBLOCKED")){
					role.clearTargets();
				}
			}
		}
		//end RoleBlock
		for(String action:actionCat){
			ArrayList<RoleForum> roleList = getRolesWithActionCat(action);
			for(RoleForum role:roleList){
				for(Flag flag : role.getFlags().values()){
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
				for(Flag flag : role.getFlags().values()){
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
		RoleForum role = getPlayerRole(playerNum);
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
	/**Adds a reason for player death to be displayed
	 * @param playerNum
	 * @param deathType
	 * @param deathDesc
	 */
	public void playerDeathReasons(int playerNum,String deathType,String deathDesc){
		RoleForum playerRole = getRole(getAlivePlayer(playerNum).getRoleNumber());
		playerRole.deathTypes.add(deathType);
		if(deathDesc != null){playerRole.deathDesc.add(deathDesc);}
	}
	/**Removes all reasons for death
	 * @param playerNum
	 */
	public void playerDeathReasonsClear(int playerNum){
		RoleForum playerRole = getRole(getAlivePlayer(playerNum).getRoleNumber());
		playerRole.deathTypes.clear();
		playerRole.deathDesc.clear();
	}
	/** Applys damages to player, such as being attacked, doesn't mean death until end of phase
	 * includes reason for death if it DOES occur
	 * @param playerNum
	 */
	public void damagePlayer(int playerNum,int attacker, String deathType,String deathDesc){
		RoleForum role = getRole(getAlivePlayer(playerNum).getRoleNumber());
		RoleForum attackerRole = getRole(getAlivePlayer(attacker).getRoleNumber());
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
		this.sendMatch(
				name+" was "+deathDesc+"<BR>"+
				name+"'s role was "+getPlayerRole(playerNum).getName());
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
	public void killPlayer(RoleForum role){
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
		for(TeamForum team : teams.values()){
			new scriptProcess("victoryCon", team.getScript("victoryCon"), team);
			Game.Base.Console.debug(team.getName()+"'s victory: "+team.getVictory());
		}
		for(TeamForum team : teams.values()){
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
		//if(getPhaseMain() == Constants.PHASEMAIN_SETUP && !isAdvancePhaseTimer()){
			if(this.roleSetup.size() == getNumChars()){
				//this.addAdvancePhaseTimer(10);
				//this.send(CmdCompile.chatScreen("Game starting in 10 seconds."));
				//this.send(CmdCompile.timerStart(10));
			}
			else{
				//this.send(CmdCompile.chatScreen("<b><font color=\"FF0000\">Game has "+getNumChars()+" players and "+this.roleSetup.size()+" roles. Unable to start.</font></b>"));
			}
		//}
	}
	/** Deletes the gameStart() timer when in setup mode */
	public void gameCancel(){
		//if(this.getPhaseMain() == Constants.PHASEMAIN_SETUP && isAdvancePhaseTimer()){
			//$game->removeTimer($this->timer);
			//removeAdvancePhaseTimer();
			//this.send(CmdCompile.chatScreen("Game start cancelled."));
			//this.send(CmdCompile.timerStop());
		//}
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
		//send(CmdCompile.setTimeOfDay(phase));
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
			//this.send(CmdCompile.closeLayer("nameSelection"));//close name selection window(if open)
			for(int i = 1; i < this.players.length; i++){//Give names to the nameless
				if(this.players[i].getName() == null){
					//chooseName(i, StringFunctions.make_rand_name());
				}
			}
			doScriptProcess("onStartup");
			for(int i = 1; i < this.players.length; i++){//tell each of their roles
				this.sendToPlayerNum(i, getName(),
						StringFunctions.HTMLColor("FFFF00", getPlayer(i).inGameName+", your role is ")+StringFunctions.HTMLColor("00FF00", getPlayerRole(i).getName())
					);
				//getCharacter((getPlayer(i).getEID())).send(CmdCompile.setPlayerNum(i));
				//getCharacter((getPlayer(i).getEID())).send(CmdCompile.setTargetables(getPlayerRole(i)));
				//getCharacter((getPlayer(i).getEID())).send(CmdCompile.matchStart());
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
			/*case Constants.PHASEDAYTYPE_TRIALVOTE://from trialvote inno/guilty
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
				break;*/
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
			/*for(Character chara : characters.values()){
				chara.leaveMatch();
			}
			Game.removeMatch(this);*/
			break;
		}
	}
	/** Assigns roles to each player, then randomizes their order. Returns true on success*/
	public boolean assignRoles(){
		boolean noError = true;
		roles = new RoleForum[getNumChars()+1];
		Random rand = new Random();//makes new random number
		if(noError){
			for (int i = 1; i <= getNumChars(); i++){
				RolesOrig origRole = roleSetup.get(i-1);
				if(origRole.eID > 0){//if a role id...grab the single id instead of category
					roles[i] = Game.Base.MySql.grabRoleForum(origRole.eID);
					roles[i].setMatch(this);
					if(roles[i]==null){Game.Base.Console.warning("Could not retrieve a role the list based on manuel id, Start Cancelled");noError = false;break;}
					origRole.roleName = roles[i].getName();
				}
				else{//grab category
					Map<Integer, Integer> list = Game.Base.MySql.grabRoleCatList("DEFAULT",origRole.affiliation,origRole.category[0]);
					if(list.size() == 0){sendSignup("Could not retrieve a role from selected role category list("+origRole.affiliation+" "+origRole.category[0]+"), Start Cancelled");noError=false;break;}
					int randNum = rand.nextInt(list.size());
					Game.Base.Console.debug("getting random number: "+randNum);
					RoleForum role;
					if((role = Game.Base.MySql.grabRoleForum(list.get(randNum))) != null){
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
			for(Players play : signups.values()){
				playersTemp[loop] = play;
				//playersTemp[loop].eID = chara.getEID();
				loop++;
			}
			Collections.shuffle(Arrays.asList(playersTemp));
				//$this->players = array_offset($this->players);//increases all keys by +1
				//ksort($this->players);//puts the keys back in order numically
			for (int i = 0; i < playersTemp.length; i++){//attaches role number to each player
				playersTemp[i].roleNumber = i+1;
				Game.Base.Console.debug("player EID="+playersTemp[i].fID+", roleNumber="+(i+1)+", roleEID="+this.roles[i+1].getEID()+", roleName="+this.roles[i+1].getName());

			}
			Collections.shuffle(Arrays.asList(playersTemp));
				//$this->players = array_offset($this->players);
			//loop through and assign player numbers 1+ through max(offseting array by +1)
			for(int i = 0; i < playersTemp.length; i++){
				this.players[i+1] = playersTemp[i];
			}
			chatGroup.addChannel("daychat",0);
			//chatGroup.addChannel("mafiachat",1);
			chatGroup.addChannel("deadchat",2);
			for (int i = 1; i < players.length; i++) {//sets everyone as alive
				//getCharMem($this->players[$i][0])->setPlayerNum($i);//then set playernum to characters
				//getCharacter(getPlayer(i).getEID()).setPlayerNum(i);
				getPlayer(i).playerNumber = i;
				RoleForum tempRole = getPlayerRole(i);//getPlayerRole(i);//getRole(getPlayer(i).roleNumber);
				tempRole.setPlayerNum(i);
				chatGroup.addPlayer(i);
				chatGroup.addPlayerToChannel(i, "daychat", 1, 1);
				if(tempRole.getOnTeam()){//should be on Team(change to getOnTeam())
					if(StringUtils.isEmpty(tempRole.getTeamName())){tempRole.setTeamName(tempRole.getAffiliation());}
					if(!teams.containsKey(tempRole.getTeamName())){teams.put(tempRole.getTeamName(), new TeamForum(this,tempRole.getTeamName()));}//if non existant, make the team
					teams.get(tempRole.getTeamName()).addTeammate(i);
					if(StringUtils.isEmpty(teams.get(tempRole.getTeamName()).getScript("victoryCon"))){teams.get(tempRole.getTeamName()).setScript("victoryCon", tempRole.getScript("victoryCon"));}//
					if(StringUtils.isEmpty(teams.get(tempRole.getTeamName()).getScript("mayGameEndCon"))){teams.get(tempRole.getTeamName()).setScript("mayGameEndCon", tempRole.getScript("mayGameEndCon"));}
				}
				tempRole = null;
			}
		}
		return noError;
	}
	/**
	 * Entering this phase constitutes the 'Start' of the game.<br>
	 * Upon starting, players are choosen roles, then asked to make a name(If allowed in options)
	 */
	private void beginNaming(){
		/*//Start naming mode
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
		}*/
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
			//this.addAdvancePhaseTimer(getSetting("discuss_length"));
			//this.send(CmdCompile.timerStart(getSetting("discuss_length")));
			this.sendMatch("(Day "+getPhaseDay()+" Discussion)You have "+getSetting("discuss_length")+" hours until discussions end");
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
		//clearVotes();
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
		//this.addAdvancePhaseTimer(daytimer);
		//this.send(CmdCompile.timerStart(daytimer));
		this.sendMatch("(Day "+getPhaseDay()+")You have "+daytimer+" hours until the day end");
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
		//this.addAdvancePhaseTimer(getSetting("night_length"));
		//this.send(CmdCompile.timerStart(getSetting("night_length")));
		//this.send(CmdCompile.chatScreen("(Night "+getPhaseDay()+")You have "+getSetting("night_length")+" secs til day"));
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
		/*this.trialplayer = player;
		if(getSetting("trial_defense")>0){//if there can be a trial defense
			setPhaseDayType(Constants.PHASEDAYTYPE_TRIALPLEAD);
			this.addAdvancePhaseTimer(getSetting("trial_length"));
			this.send(CmdCompile.timerStart(getSetting("trial_length")));
			this.send(CmdCompile.chatScreen(getPlayer(player).getName()+", you are on trial for conspiracy against the town. What is your defense?"));
			this.send(CmdCompile.chatScreen("(Trial Defense)You have "+getSetting("trial_length")+" seconds to plead your defense."));
		}
		else{//skipping defense
			this.beginLynch();
		}*/
	}
	/**
	 * Mode prompts every player execpt the defended to vote inno or guilty, Majority wins<br><br>
	 * *Tie = Inno
	 */
	private void beginTrialVote(){
		//Start TrialVote mode
		//	Notify of time til votes counted
		//	Set timer to advancePhase
		/*clearVotes();
		setPhaseDayType(Constants.PHASEDAYTYPE_TRIALVOTE);

			//echo "in trial vote phase ".$this->phase."\n";

		//$this->timer2 = $game->addTimer($this->settings["trial_length"],"match",$matchid,"advancePhase");
		this.addAdvancePhaseTimer(getSetting("trial_length"));
		this.send(CmdCompile.timerStart(getSetting("trial_length")));
		this.send(CmdCompile.chatScreen("(Trial Vote)You have "+getSetting("trial_length")+" secs to vote Guity/Innocent."));*/
	}
	/** Mode will announce players role and send them to graveyard */
	private void beginLynch(){
		//clearVotes();
		setPhaseDayType(Constants.PHASEDAYTYPE_LYNCH);
		/*playerDeathReasonsClear(trialplayer);
		playerDeathReasons(trialplayer,"Lynched","lynched by an angry mob");
		killPlayer(trialplayer);
		chatGroup.addPlayerToChannel(trialplayer, "daychat", 0, 1);//take away day talking rights, but still let listen
		chatGroup.addPlayerToChannel(trialplayer, "deadchat", 1, 1);//allowing talking in deadchat
		this.addAdvancePhaseTimer(5);
		this.send(CmdCompile.timerStart(5));
		this.send(CmdCompile.chatScreen("(Lynch)"+getPlayer(trialplayer).getName()+" has been lynched by the Town!"));
		this.send(CmdCompile.chatScreen("(Lynch)"+getPlayer(trialplayer).getName()+"'s role was "+getPlayerRole(trialplayer).getName()));*/
	}
	/** Mode displays the winning teams and players as well as their roles. Adds timer to kill match after certain time*/
	private void beginGameEnd(){
		//TODO Client: remove vote/target buttons
		//Check victoryConditctions for all players
		Game.Base.Console.debug("The Game has completed...");
		setPhaseMain(Constants.PHASEMAIN_ENDGAME);
		//check for winning teams and display
		for(TeamForum team : teams.values()){
			new scriptProcess("victoryCon", team.getScript("victoryCon"), team);
			if(team.getVictory()){
				sendMatch(team.getName()+" won!");
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
				message += StringFunctions.HTMLColor(player.getHexcolor(), player.getName());
				if(loop != 0){if(numWinners > 2){message += ",";}}
				if(loop == 1){message += " and";}
				message += " ";
			}
			message += "have ";
		}
		else if(numWinners == 1){
			for(Players player:winners){
				message += StringFunctions.HTMLColor(player.getHexcolor(), player.getName())+" ";
			}
			message += "has ";
		}
		else{
			message += "NO ONE ";
		}
		message += "won the game!";
		sendMatch(message);
		//TODO GameEnd: lists off everyone's roles
		//this.addAdvancePhaseTimer(300);
		//this.send(CmdCompile.timerStart(300));
	}

//////////////////////////
///////Dataholders////////
//////////////////////////
	/** Dataholder for Characters playing match */
	public class Players{
	//public Match match;
		public int fID;
		public String inGameName;
		public int playerNumber;
		public int roleNumber;
		public String hexcolor = "FFFFFF";

		public int getFID(){
			return fID;
		}
		public String getName(){
			return inGameName;
		}
		public int getPlayerNumber(){
			return playerNumber;
		}
		//public RoleForum getRole(){
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
	public MatchForumERS getERSClass(){
		if(this.matchERS == null){
			this.matchERS = new MatchForumERS(this);
		}
		return this.matchERS;
	}

}
