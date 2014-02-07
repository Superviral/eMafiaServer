/* eMafiaServer - MatchCmd.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;

import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import com.inverseinnovations.eMafiaServer.includes.CmdCompile;
import com.inverseinnovations.eMafiaServer.includes.Constants;
import com.inverseinnovations.eMafiaServer.includes.StringFunctions;
import com.inverseinnovations.eMafiaServer.includes.scriptProcess;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Lobby;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Match;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Role;
import com.inverseinnovations.eMafiaServer.includes.classes.Server.SocketClient;
import com.inverseinnovations.sharedObjects.RoleData;

/**
 * Provides list of all commands a Character may call when inside a Match<br>
 * All method names must be appended to CMDLIST[] to be callable
 */
public class MatchCmd {
	public static String[] CMDLIST = {
		//basic commands
		//"help","look","match",
		"charaupdate","endmatch","gamestart","gamecancel","getsetting","leave","name","orderofop","refresh","refreshplist","rolecreate","roleedit","rolespossible","rolesetup", "rolesearch","roleview",
		"say","setting","target1","target2","quit","vote",
		//admin commands
		//"_show_commands","_shutdown","timer_add","_setupbots","_makenpc","_force"
		//experimental commands
		"var_dump","_makenpc","_force","_setupdebug"
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
	public static void endmatch(Character c, String phrase, byte[] data){
		Match m = c.getMatch();
		if(c.getEID() == m.getHostId()){
			if(m.getPhaseMain()==Constants.PHASEMAIN_SETUP){//check if in setup mode
				m.send(CmdCompile.chatScreen("Host ended the match."));
				m.endMatch();
			}
		}
		return;
	}
	public static void gamestart(Character c, String phrase, byte[] data){
		Match m = c.getMatch();
		if(c.getEID() == m.getHostId()){
			m.gameStart();
		}
	}
	public static void gamecancel(Character c, String phrase, byte[] data){
		Match m = c.getMatch();
		if(c.getEID() == m.getHostId()){
			c.getMatch().gameCancel();
		}
	}
	public static void getsetting(Character c, String phrase, byte[] data){//TODO need to set cmdCompile
		Match m = c.getMatch();
		c.send(CmdCompile.matchSettings(m));
	}
	public static void leave(Character c, String phrase, byte[] data){
		if(c.getMatch().getPhaseMain()==Constants.PHASEMAIN_SETUP || c.getMatch().getPhaseMain()==Constants.PHASEMAIN_ENDGAME){//if in setup or endgame
			c.leaveMatch();
			//c.send(CmdCompile.closeLayer("matchSetup"));//remove lobby chat and list
			//c.send(CmdCompile.enterLobby());//open Lobby,client will call look
		}
		else{
			c.send(CmdCompile.chatScreen("You can't just leave in the middle of a game!"));
		}
	}
	public static void name(Character c, String phrase, byte[] data){
		Match m = c.getMatch();
		//$matchid = $match->getMatchID();
		if(m.getPhaseMain()==Constants.PHASEMAIN_NAMING){//checking naming
			m.chooseName(c.getPlayerNum(),phrase);
		}
	}
	public static void orderofop(Character c, String phrase, byte[] data){
		//-orderofop (parameters)
		//(parameters):
		//	list	- lists current roles in the possibily array
		//	up		- moves the action up on the list
		//	down	- moves the action down on the list
		System.out.println("orderOfOps cmd recieved");
		Match m = c.getMatch();
		if(phrase.equals("list")){
			c.send(CmdCompile.orderOfOps(m.getActionCategories()));
		}
		else if(c.getEID() == m.getHostId()){
			if(m.getPhaseMain()==Constants.PHASEMAIN_SETUP){//checking if in setup
				if(phrase.contains(" ")){
					String[] ephrase = phrase.split(" ");
					if(ephrase.length >= 1){
						if(ephrase[1].equals("up")){
							if(ephrase.length >= 2){//need atleast 2 parameters
								m.moveActionCategoryUp(ephrase[0]);
							}
						}
						if(ephrase[1].equals("down")){
							if(ephrase.length >= 2){//need atleast 2 parameters
								m.moveActionCategoryDown(ephrase[0]);
							}
						}
					}
				}
			}
		}
	}
	public static void refresh(Character c, String phrase, byte[] data) {//shall depend on game phase as well..
		refreshplist(c, phrase, null);
		if(c.getMatch().getPhaseMain() == Constants.PHASEMAIN_SETUP){getsetting(c, phrase, null);}//if in setup, show settings
		return;
	}
	public static void refreshplist(Character c, String phrase, byte[] data) {//get player list
		if(c.getMatch().getPhaseMain() != Constants.PHASEMAIN_INPLAY){//as long as not playing
			List<Character> charas = c.getMatch().getCharacterList();//c.getLobby().getPlayerList();
			for(Character chara:charas){
				c.send(CmdCompile.charaUpdate(chara));
			}
			c.send(CmdCompile.refreshPList(charas));
		}
		else{
			c.send(CmdCompile.refreshAliveList(c.getMatch().getAliveList()));//refresh living players
			c.send(CmdCompile.refreshDeadList(c.getMatch(),c.getMatch().getDeadList()));//refresh graveyard
		}
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
	public static void rolespossible(Character c, String phrase, byte[] data){
		//-rolepossible (parameters)
		//(parameters):
		//	list - lists current roles in the possibily array
		//	clear - clears teh list
		//	add (id) - adds the role to the possibilities
		//	remove (id) - removes role from possibilities
		Match m = c.getMatch();
		if(phrase.equals("list")){
			c.send(CmdCompile.rolesPossible(m.getRolesPossible()));
		}
		else if(c.getEID() == m.getHostId()){
			if(m.getPhaseMain()==Constants.PHASEMAIN_SETUP){//checking if in setup
				if(phrase.equals("clear")){
					m.clearRolesPossible();
					c.send(CmdCompile.rolesPossible(m.getRolesPossible()));
				}
				else if(phrase.contains(" ")){
					String[] ephrase = phrase.split(" ");
					if(ephrase.length >= 1){
						if(ephrase[0].equals("add")){
							if(ephrase.length >= 2){//need atleast 2 parameters
								if(StringFunctions.isInteger(ephrase[1])){
									if(m.addRolesPossible(Integer.parseInt(ephrase[1]))){
										//message = "Added "+ephrase[1];
										c.send(CmdCompile.rolesPossible(m.getRolesPossible()));
									}
								}
							}
						}
						else if(ephrase[0].equals("remove")){
							if(ephrase.length >= 2){//need atleast 2 parameters
								if(StringFunctions.isInteger(ephrase[1])){
									if(m.removeRolesPossible(Integer.parseInt(ephrase[1]))){
										//message = "Removed role "+ephrase[1];
										c.send(CmdCompile.rolesPossible(m.getRolesPossible()));
									}
								}
							}
						}
					}
				}
			}
		}
	}
	public static void rolesetup(Character c, String phrase, byte[] data){
		//-rolesetup (parameters)
		//(parameters):
		//	list - lists current roles in the setup
		//	clear - clears list
		//	add (id OR aff + cat) - adds the role to the setup...(checks if it possible to add it first by checking the roles possbile)
		//	remove (id) - removes role from setup
		Match m = c.getMatch();
		if(phrase.equals("list")){
			c.send(CmdCompile.roleSetup(m.getRoleSetup()));
		}
		else if(c.getEID() == m.getHostId()){
			if(m.getPhaseMain()==Constants.PHASEMAIN_SETUP){//checking if in setup
				if(phrase.equals("clear")){
					m.clearRoleSetup();
					c.send(CmdCompile.roleSetup(m.getRoleSetup()));
				}
				else if(phrase.contains(" ")){
					String[] ephrase = phrase.split(" ");
					if(ephrase.length >= 1){
						if(ephrase[0].equals("add")){
							if(ephrase.length == 2){//need 2 parameters
								if(StringFunctions.isInteger(ephrase[1])){
									if(m.addToRoleSetup(Integer.parseInt(ephrase[1]))){
										c.send(CmdCompile.roleSetup(m.getRoleSetup()));
									}
								}
							}
							else if(ephrase.length >= 3){//need atleast 3 parameters
								if(ephrase[1] != null && ephrase[2] != null){
									if(m.addToRoleSetup(ephrase[1],ephrase[2])){
										c.send(CmdCompile.roleSetup(m.getRoleSetup()));
									}
								}
							}
						}
						else if(ephrase[0].equals("remove")){
							if(ephrase.length == 2){//need 2 parameters
								if(StringFunctions.isInteger(ephrase[1])){
									if(m.removeFromRoleSetup(Integer.parseInt(ephrase[1]))){
										//message = "Removed role "+ephrase[1];
										c.send(CmdCompile.roleSetup(m.getRoleSetup()));
									}
								}
							}
							else if(ephrase.length >= 3){//need atleast 3 parameters
								if(ephrase[1] != null && ephrase[2] != null){
									if(m.removeFromRoleSetup(ephrase[1],ephrase[2])){
										c.send(CmdCompile.roleSetup(m.getRoleSetup()));
									}
								}
							}
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

	public static void say(Character c, String phrase, byte[] data){
		//later will add a wholoe chat channel function that this
		if(c.getMatch().getPhaseMain() != Constants.PHASEMAIN_INPLAY){//normal talk while no in play
			c.getMatch().send(phrase,"roomSay",c);
		}
		else{
			//now talk in assigned channels XXX check for non players
			c.getMatch().chatter(c.getPlayerNum(), phrase);
		}
	}
	public static void setting(Character c, String phrase, byte[] data){//sets all the options for a match..before starting
		Match m = c.getMatch();
		//need to make host checks
		String[] ephrase = phrase.split(" ");
		if(c.getEID() == m.getHostId()){
			if(m.getPhaseMain()==Constants.PHASEMAIN_SETUP){//check if in setup mode
				if(m.setSetting(ephrase[0],Integer.parseInt(ephrase[1]))){
					c.send(CmdCompile.chatScreen(ephrase[0]+" set to "+ephrase[1]));
				}
				else{
					c.send(CmdCompile.chatScreen(ephrase[0]+" setting does not exist."));
				}
			}
		}
	}
	public static void target1(Character c, String phrase, byte[] data){//TODO Targets need to be targetable
		Match m = c.getMatch();
		//m.Game.Base.Console.debug("entered a target1");
		if(m.getPhaseMain()==Constants.PHASEMAIN_INPLAY && StringFunctions.isInteger(phrase)){
			//m.Game.Base.Console.debug("target1 succesuful");
			if(m.getPlayerRole(c.getPlayerNum()).getTarget1() == Integer.parseInt(phrase)){m.getPlayerRole(c.getPlayerNum()).setTarget1(0);}//if selecting the same target same...cancel it
			else{m.getPlayerRole(c.getPlayerNum()).setTarget1(Integer.parseInt(phrase));}
			try{
				String dayNight = null;
				if(m.getPhaseDayType()==Constants.PHASEDAYTYPE_NIGHT){dayNight = "onNightTargetChoice";}
				else if(m.getPhaseDayType()==Constants.PHASEDAYTYPE_NORMAL){dayNight = "onDayTargetChoice";}
				if(dayNight != null){
					new scriptProcess(dayNight, m.getPlayerRole(c.getPlayerNum()).getScript(dayNight), m.getPlayerRole(c.getPlayerNum()));
				}
			}catch(Exception e){m.Game.Base.Console.printStackTrace(e);}
		}
	}
	public static void target2(Character c, String phrase, byte[] data){//TODO Targets need to be targetable
		Match m = c.getMatch();
		//m.Game.Base.Console.debug("entered a target2");
		if(m.getPhaseMain()==Constants.PHASEMAIN_INPLAY && StringFunctions.isInteger(phrase)){
			//m.Game.Base.Console.debug("target2 succesuful");
			if(m.getPlayerRole(c.getPlayerNum()).getTarget2() == Integer.parseInt(phrase)){m.getPlayerRole(c.getPlayerNum()).setTarget2(0);}//if selecting the same target same...cancel it
			else{m.getPlayerRole(c.getPlayerNum()).setTarget2(Integer.parseInt(phrase));}
			try{
				String dayNight = null;
				if(m.getPhaseDayType()==Constants.PHASEDAYTYPE_NIGHT){dayNight = "onNightTargetChoice";}
				else if(m.getPhaseDayType()==Constants.PHASEDAYTYPE_NORMAL){dayNight = "onDayTargetChoice";}
				if(dayNight != null){
					new scriptProcess(dayNight, m.getPlayerRole(c.getPlayerNum()).getScript(dayNight), m.getPlayerRole(c.getPlayerNum()));
				}
			}catch(Exception e){m.Game.Base.Console.printStackTrace(e);}
		}
	}

	public static void quit(Character c, String phrase, byte[] data) {//dissconnecting command
		c.setOffline();
	}
	public static void vote(Character c, String phrase, byte[] data){
		c.getMatch().votePlayer(c.getPlayerNum(), Integer.parseInt(phrase));
	}
	//DEBUGGING //

	public static void var_dump(Character c, String phrase, byte[] data){
		if(c.getUsergroup().getName().equals("Administrator")){
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
	}
	public static void _makenpc(Character c, String phrase, byte[] data){//testing making a npc
		if(c.getUsergroup().getName().equals("Administrator")){
			Match match = c.getMatch();
			if(match.getNumChars() < match.getSetting("max_chars")){//if theres room in the match
				Random rand;rand = new Random();
				String name = "NPC "+rand.nextInt(5001);
				Character newc = new Character(c.Game, name, 5);
				if (newc != null){
					newc.setType(Constants.TYPE_GAMEOB_NPC);
					newc.joinMatch(match);
					c.send(CmdCompile.chatScreen(name+" created"));
					c.Game.Base.Console.fine("Logging "+name+" in!");
				}
			}
		}
	}
	public static void _force(Character c, String phrase, byte[] data){//testing if har targetable
		if(c.getUsergroup().getName().equals("Administrator")){
			String message = "";
			String defaultNONE = "Force error";
			if(phrase != null){//make sure there is a target
				String[] ephrase = phrase.split(" ",2);
				if(ephrase[0] != null && ephrase.length > 1){
					if(c.getMatch().getPhaseMain() == 0 || c.getMatch().getPhaseMain() == 3){//if in setup/endgame
						//the order of targets can matter
						//Character target = c.Game.getCharacter(Integer.parseInt(ephrase[0]));
						Character target = c.getMatch().getCharacter(Integer.parseInt(ephrase[0]));
						if(target != null){
							//$target = $target->getEID();
							//$targetName = getCharMem($target)->getName();
							//$splitone = splitOnce($phrase, " ");//target-+
							//$new_phrase = splitOnce($splitone[1], " ");//command-everything else
							String[] new_phrase = {null,null};
							if(ephrase[1].contains(" ")){
								new_phrase = ephrase[1].split(" ",2);//command-everything else
							}
							else{new_phrase[0] = ephrase[1];}
							CmdHandler.processCmd(target, new_phrase[0], new_phrase[1], null);
							message = "Forcing "+target.getName()+" to do a '"+new_phrase[0]+"' command...";

						}
						else{message = defaultNONE;}
					}
					else{//if in play
						//the order of targets can matter
						//Character target = c.Game.getCharacter(Integer.parseInt(ephrase[0]));
						Character target = c.getMatch().getCharacter(c.getMatch().getPlayer(Integer.parseInt(ephrase[0])).getEID());
						if(target != null){
							//$target = $target->getEID();
							//$targetName = getCharMem($target)->getName();
							//$splitone = splitOnce($phrase, " ");//target-+
							//$new_phrase = splitOnce($splitone[1], " ");//command-everything else
							String[] new_phrase = {null,null};
							if(ephrase[1].contains(" ")){
								new_phrase = ephrase[1].split(" ",2);//command-everything else
							}
							else{new_phrase[0] = ephrase[1];}
							CmdHandler.processCmd(target, new_phrase[0], new_phrase[1], null);
							message = "Forcing "+target.getName()+"("+target.getMatch().getPlayer(target.getPlayerNum()).getName()+") to do a '"+new_phrase[0]+"' command...";
						}
						else{message = defaultNONE;}
					}
				}
				else{message = defaultNONE;}
			}
			else{message = defaultNONE;}
			c.send(CmdCompile.chatScreen(message));
			if(message.equals(defaultNONE)){
				if(c.getMatch().getPhaseMain() == 0 || c.getMatch().getPhaseMain() == 3){//if in setup/endgame
					c.send(CmdCompile.chatScreen("List of in game characters:"));
					for(Character chara : c.getMatch().getCharacterList()){
						c.send(CmdCompile.chatScreen(chara.getEID()+" "+chara.getName()));
					}
				}
				else{
					c.send(CmdCompile.chatScreen("List of players shown on right:"));
				}
			}
		}
	}
	public static void _setupdebug(Character c, String phrase, byte[] data){//testing making a npc
		if(c.getUsergroup().getName().equals("Administrator")){
			Match m =c.getMatch();
			m.send(CmdCompile.chatScreen("Setting up room for a debug run!"));
			for(int i = (m.getSetting("max_chars")-m.getNumChars()); i > 0; i--){
				CmdHandler.processCmd(c, "-_makenpc", null, null);
			}
			for(int i = (m.getSetting("max_chars")-m.getRoleSetup().size()); i > 0; i--){
				CmdHandler.processCmd(c, "-rolesetup", "add TOWN CORE", null);
			}
		}
	}
}
