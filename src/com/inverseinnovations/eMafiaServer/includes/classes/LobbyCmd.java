/* eMafiaServer - LobbyCmd.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;

import java.util.List;
import java.util.Map.Entry;
import com.inverseinnovations.eMafiaServer.includes.CmdCompile;
import com.inverseinnovations.eMafiaServer.includes.StringFunctions;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Lobby;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Match;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.MatchForum;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Role;
import com.inverseinnovations.eMafiaServer.includes.classes.Server.SocketClient;
import com.inverseinnovations.sharedObjects.RoleData;

/**
 * Provides list of all commands a Character may call when inside a Lobby<br>
 * All method names must be appended to CMDLIST[] to be callable
 */
public class LobbyCmd {
	public static String[] CMDLIST = {
		//basic commands
		//"help","look","match",
		"charaupdate","match","refresh","refreshplist","rolecreate","roleedit","rolesearch","roleview","say","quit",
		//admin commands
		//"_show_commands","_shutdown","timer_add","_setupbots","_makenpc","_force"
		//experimental commands
		"test","var_dump","_editpost","_newthread","_newpost","_parsepms","_emptyinbox","_forumsignup"
	};
	public static void charaupdate(Character c, String phrase, byte[] data) {
		String[] ephrase = phrase.split(" ");
		//int[] intPhrase = new int[ephrase.length];
		//int loop = 0;
		for(String charaString:ephrase){
			if(StringFunctions.isInteger(charaString)){
				Character chara = c.Game.getCharacter(Integer.parseInt(charaString));
				if(chara != null){
					c.send(CmdCompile.charaUpdate(chara));
				}
			}
		}
	}
	public static void _forumsignup(Character c, String phrase, byte[] data) {
		c.Game.Base.Console.debug("_forumsignup hit..");
		String[] ephrase = phrase.split(" ");
		Match match;
		if(ephrase.length > 0){
			if(ephrase[0].equals("list")){
				c.send(CmdCompile.refreshMList(c.Game.getMatchs()));
			}
			else if(ephrase[0].equals("join") && ephrase.length > 1){
				match = c.Game.getMatch(Integer.parseInt(ephrase[1]));
				if(match != null){
					if(c.joinMatch(match)){//attempt to join + check character joined
						c.send(CmdCompile.closeLayer("lobby"));//remove lobby layer
						c.send(CmdCompile.enterMatch());//open matchSetup,client will call look
					}
					else{
						c.send(CmdCompile.chatScreen("Match "+ephrase[1]+" is full."));
					}
				}
				else{
					c.send(CmdCompile.chatScreen("That match doesn't even exist!"));
				}
			}
			else if(ephrase[0].equals("create")){
				c.Game.Base.Console.debug("_forumsignup create hit..");
				if(c.Game.getMatchSignup() == null){
					MatchForum matchF = new MatchForum(c.Game, "A Test Match 1");
					c.Game.setMatchSignup(matchF);
					c.Game.getMatchSignup().setHost(3359);//apo
						c.Game.getMatchSignup().addToRoleSetup(7);//gf
						c.Game.getMatchSignup().addToRoleSetup(4);//maf
						c.Game.getMatchSignup().addToRoleSetup(4);//maf

						/*c.Game.getMatchSignup().addToRoleSetup(7);//GF
						c.Game.getMatchSignup().addToRoleSetup(4);//mafiaso*/
						c.Game.getMatchSignup().addToRoleSetup(2);//sheriff
						c.Game.getMatchSignup().addToRoleSetup("TOWN", "PROTECTIVE");
						c.Game.getMatchSignup().addToRoleSetup(1);//cit
						c.Game.getMatchSignup().addToRoleSetup(1);//cit
						c.Game.getMatchSignup().addToRoleSetup(1);//cit*/
						c.Game.getMatchSignup().addToRoleSetup(5);//electro
					c.Game.getMatchSignup().postMatch();
				}
			}
		}
	}
	public static void match(Character c, String phrase, byte[] data) {
		String[] ephrase = phrase.split(" ");
		Match match;
		if(ephrase.length > 0){
			if(ephrase[0].equals("list")){
				c.send(CmdCompile.refreshMList(c.Game.getMatchs()));
			}
			else if(ephrase[0].equals("join") && ephrase.length > 1){
				match = c.Game.getMatch(Integer.parseInt(ephrase[1]));
				if(match != null){
					if(c.joinMatch(match)){//attempt to join + check character joined
						c.send(CmdCompile.closeLayer("lobby"));//remove lobby layer
						c.send(CmdCompile.enterMatch());//open matchSetup,client will call look
					}
					else{
						c.send(CmdCompile.chatScreen("Match "+ephrase[1]+" is full."));
					}
				}
				else{
					c.send(CmdCompile.chatScreen("That match doesn't even exist!"));
				}
			}
			else if(ephrase[0].equals("create")){
				match = new Match(c.Game, "New Match");
				LobbyCmd.match(c, "join "+match.getEID(), null);
			}
		}
	}
	public static void refresh(Character c, String phrase, byte[] data) {
		refreshplist(c, phrase, null);
		match(c, "list", null);
		return;
	}
	public static void refreshplist(Character c, String phrase, byte[] data) {//get player list
		List<Character> charas = c.getLobby().getPlayerList();
		for(Character chara:charas){
			c.send(CmdCompile.charaUpdate(chara));
		}
		c.send(CmdCompile.refreshPList(charas));
		return;
	}
	public static void rolecreate(Character c, String phrase, byte[] data){
		//-rolecreate) - attempt to create a new role
		if(data != null){
			Object objData = StringFunctions.byteToObject(data);
			if(objData instanceof RoleData){
				if(c.Game.Base.MySql.insertRole(((RoleData)objData), "CUSTOM")){
					c.Game.Base.Console.info("Role Created: "+((RoleData)objData).name);
				}
				else{
					c.Game.Base.Console.warning("Role Creation of "+((RoleData)objData).name+" failed!");
				}
			}
		}
	}
	public static void roleedit(Character c, String phrase, byte[] data){
		//-roleedit - attempt to edit a role
		if(data != null){
			Object objData = StringFunctions.byteToObject(data);
			if(objData instanceof RoleData){
				if(((RoleData)objData).id > 0){
					Role role = c.Game.Base.MySql.grabRole(((RoleData)objData).id);
					if(role != null){
						if(c.Game.Base.MySql.updateRole(((RoleData)objData), role.getVersion(), "CUSTOM")){
							c.Game.Base.Console.info("Role Edited: "+role.getName());
						}
						else{
							c.Game.Base.Console.warning("Role Edit of "+role.getName()+" failed!");
						}
					}
				}
			}
		}
	}
	public static void rolesearch(Character c, String phrase, byte[] data){
		//lets user search database for roles...
		//-rolesearch (aff) (cat) (page)
		//(parameters):
		//	aff (text)- display list of roles in the inputted affiation
		//	cat (text)- display list of roles in the inputted category
		//	page- page shows next batch of '10' roles
		if(phrase.contains(" ")){
			String[] ephrase = phrase.split(" ");
			if(ephrase.length >= 3){//need atleast than 3 parameters
				if(StringFunctions.isInteger(ephrase[2])){
					c.send(CmdCompile.roleSearchResults(c.Game.Base.MySql.searchRoles(ephrase[0], ephrase[1], Integer.parseInt(ephrase[2]))));
				}
			}

		}
	}
	public static void roleview(Character c, String phrase, byte[] data){
		//-roleview (id) - attempt to view the role by id number
		String[] ephrase = phrase.split(" ");
		if(StringFunctions.isInteger(ephrase[0])){
			Role role = c.Game.Base.MySql.grabRole(Integer.parseInt(ephrase[0]));
			if(role != null){
				if(ephrase.length > 1){if(StringFunctions.isInteger(ephrase[1])){
					if(role.getVersion() > Integer.parseInt(ephrase[1])){
						c.send(CmdCompile.roleView(role));
					}
				}}
				else{
					c.send(CmdCompile.roleView(role));
				}
			}
		}
	}
	public static void say(Character c, String phrase, byte[] data){//need to remove..copyrighted
		//later will add a wholoe chat channel function that this
		c.getLobby().send(phrase,"roomSay",c);
		return;
	}
	public static void quit(Character c, String phrase, byte[] data) {//dissconnecting command
		c.setOffline();
		return;
	}
	public static void var_dump(Character c, String phrase, byte[] data){
		c.Game.Base.Console.warning("");
		c.Game.Base.Console.warning("    ==== Variable Dump ====");
		c.Game.Base.Console.warning("  --- Client Connections ---");
		for (Entry<Integer, SocketClient> entry : c.Game.Base.Server.getClients().entrySet()){ //c.Game.Base.Server.getClients().entrySet()){
			c.Game.Base.Console.warning("Client: "+entry.getValue().getClientEID()+" | Char: "+entry.getValue().getCharEID()+" | IP: "+entry.getValue().getIPAddress());
		}
		c.Game.Base.Console.warning("  --- Characters ---");
		String location = "";
		for (Entry<Integer, Character> entry : c.Game.getCharacters().entrySet()){ //c.Game.Base.Server.getClients().entrySet()){
			if(entry.getValue().getInGame()){location = "Match ";}else{location = "Lobby ";}
			c.Game.Base.Console.warning("EID: "+entry.getValue().getEID()+" | Name: "+entry.getValue().getName()+" | Location: "+location+entry.getValue().getLocation());// + "/" + entry.getValue();
		}
		c.Game.Base.Console.warning("  --- Lobbys ---");
		for (Entry<Integer, Lobby> entry : c.Game.getLobbys().entrySet()){ //c.Game.Base.Server.getClients().entrySet()){
			c.Game.Base.Console.warning("EID: "+entry.getValue().getEID()+" | Name: "+entry.getValue().getName()+" | Players: "+entry.getValue().getNumChars());// + "/" + entry.getValue();
		}
		c.Game.Base.Console.warning("  --- Matchs ---");
		for (Entry<Integer, Match> entry : c.Game.getMatchs().entrySet()){ //c.Game.Base.Server.getClients().entrySet()){
			c.Game.Base.Console.warning("EID: "+entry.getValue().getEID()+" | Name: "+entry.getValue().getName()+" | Players: "+entry.getValue().getNumChars());// + "/" + entry.getValue();
		}
	}
	public static void _editpost(Character c, String phrase, byte[] data) {
		// works -> c.Game.Base.ForumAPI.pm_SendNew("Apocist", "Test Request", "this is just a test..."); can't use a '
		c.Game.Base.ForumAPI.pm_SendNew("Apocist", "Test Request", "You asked for it, you got. <br> Well...not much to say, Im a after all.");
		return;
	}
	public static void _newthread(Character c, String phrase, byte[] data) {
		//This is just a test of the Emergency Broadcast System. There is no danger, do not be alarmed. Momentarily agents with break through the windows adjacent to you It is advised that you heed their instructions to the best of your abilities to avoid being shot in the face.<br><br> That is all.
		c.Game.Base.Console.debug("Attempting new thread");
		String threadMsg = c.Game.Base.ForumAPI.thread_New("292", "This is just a test, do not panic", phrase);
		if(StringFunctions.isInteger(threadMsg.substring(0, 1))){
			if(threadMsg.contains(" ")){
				String[] ids = threadMsg.split(" ");
				c.Game.Base.Console.debug("New Thread successful... thread ID is "+ids[0]+" post id is "+ids[1]);
			}
			else{
				c.Game.Base.Console.debug("New Thread response size error..: "+threadMsg);
			}
		}
		else{
			c.Game.Base.Console.debug("New Thread failed... : "+threadMsg);
		}
		//2 in general
		//292 is Simple OnGoing
		return;
	}
	public static void _newpost(Character c, String phrase, byte[] data) {
		//This is just a test of the Emergency Broadcast System. There is no danger, do not be alarmed. Momentarily agents with break through the windows adjacent to you It is advised that you heed their instructions to the best of your abilities to avoid being shot in the face.<br><br> That is all.
		c.Game.Base.Console.debug("Attempting new post");
		String postMsg = c.Game.Base.ForumAPI.post_New("26877", phrase);
		if(StringFunctions.isInteger(postMsg)){
			c.Game.Base.Console.debug("New Reply successful...Post id is "+postMsg);
		}
		else{
			c.Game.Base.Console.debug("New Reply failed... : "+postMsg);
		}
		return;
	}
	public static void _parsepms(Character c, String phrase, byte[] data) {
		c.Game.hourlyChecks();
		return;
	}
	public static void _emptyinbox(Character c, String phrase, byte[] data) {
		c.Game.Base.Console.debug("Attempting empty the inbox");
		//HashMap<String,String> PmMsg = c.Game.Base.ForumAPI.forum_ViewMember(3359);
		String PmMsg = c.Game.Base.ForumAPI.pm_EmptyInbox();
		if(PmMsg != null){
			c.Game.Base.Console.debug("send successful...:"+PmMsg.toString());
		}
		else{
			c.Game.Base.Console.debug("send failed... : "+PmMsg);
		}
		return;
	}
}
