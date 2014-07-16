/* eMafiaServer - LobbyCmd.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;

import org.apache.commons.lang3.StringUtils;

import com.inverseinnovations.VBulletinAPI.Exception.*;
import com.inverseinnovations.eMafiaServer.includes.Constants;
import com.inverseinnovations.eMafiaServer.includes.StringFunctions;
import com.inverseinnovations.eMafiaServer.includes.scriptProcess;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.MatchForum;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.MatchForum.Players;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.RoleForum;

/**
 * Provides list of all commands a Character may call when inside a Lobby<br>
 * All method names must be appended to CMDLIST[] to be callable
 */
public class ForumCmd {
	public static String[] CMDLIST = {
		"info","lastwill","sign","target","target1","target2","reserve","withdraw","unknown_command"
	};
	public static void info(final Game Game, int forumId, String username, String phrase){
		System.out.println(username+" doing info");
		//String theReturn = Game.Base.ForumAPI.pm_SendNew(username, "Info Request", "You asked for it, you got.<br>Well...not much to say, I'm a bot after all.");
	}
	public static void lastwill(final Game Game, int forumId, String username, String phrase){
		if(Game.getMatchOngoing() != null){
			MatchForum m = Game.getMatchOngoing();
			if(m.getPhaseMain()==Constants.PHASEMAIN_INPLAY){
				Players player;
				if((player = m.getPlayerByForumId(forumId)) != null){
					RoleForum role = m.getPlayerRole(player);
					if(StringUtils.isEmpty(phrase)){phrase = null;}
					role.setLastwill(phrase);
					if(phrase != null){
						m.sendToPlayerNum(player.getPlayerNumber(), m.getName(), "Your Last Will has been saved as:\n\n"+phrase);
					}
					else{
						m.sendToPlayerNum(player.getPlayerNumber(), m.getName(), "Your Last Will has been removed.");
					}
				}
			}
		}
	}
	public static void sign(final Game Game, int forumId, String username, String phrase){
		Game.Base.Console.debug(username+" signing");
		if(Game.getMatchSignup() != null){
			if(Game.getMatchSignup().getPhaseMain() <= Constants.PHASEMAIN_STARTING){//only if in starting/signup phase
				Game.getMatchSignup().addUserSignup(forumId, username);
				try {
					Game.Base.ForumAPI.pm_SendNew(username, Game.getMatchSignup().getName()+" Signups", "You have signed up for "+Game.getMatchSignup().getName()+"!\n" +
								"If you feel you cannot participate in the future, please -withdraw.\n" +
								"Thanks for helping out.");
				}catch (VBulletinAPIException e) {}
			}
		}
	}
	public static void target(final Game Game, int forumId, String username, String phrase){//TODO Targets need to be targetable
		target1(Game, forumId, username, phrase);
	}
	public static void target1(final Game Game, int forumId, String username, String phrase){//TODO Targets need to be targetable
		Game.Base.Console.debug(username+" entered a target1");
		if(Game.getMatchOngoing() != null){
			MatchForum m = Game.getMatchOngoing();
			if(m.getPhaseMain()==Constants.PHASEMAIN_INPLAY){
				Players player;
				if((player = m.getPlayerByForumId(forumId)) != null){
					RoleForum role = m.getPlayerRole(player);
					if(phrase.equals("self")){role.setTarget1(player.getPlayerNumber());}
					else if(phrase.equals("noone")){role.setTarget1(0);}
					//if(role.getTarget1() == Integer.parseInt(phrase)){role.setTarget1(0);}//if selecting the same target same...cancel it
					else if(StringFunctions.isInteger(phrase)){role.setTarget1(Integer.parseInt(phrase));}
					try{
						String dayNight = null;
						if(m.getPhaseDayType()==Constants.PHASEDAYTYPE_NIGHT){dayNight = "onNightTargetChoice";}
						else if(m.getPhaseDayType()==Constants.PHASEDAYTYPE_NORMAL){dayNight = "onDayTargetChoice";}
						if(dayNight != null){
							Game.Base.Console.debug("doing script for target1");
							new scriptProcess(dayNight, role.getScript(dayNight), role);
						}
					}catch(Exception e){m.Game.Base.Console.printStackTrace(e);}
				}
			}
		}
	}
	public static void target2(final Game Game, int forumId, String username, String phrase){//TODO Targets need to be targetable
		Game.Base.Console.debug(username+" entered a target2");
		if(Game.getMatchOngoing() != null){
			MatchForum m = Game.getMatchOngoing();
			if(m.getPhaseMain()==Constants.PHASEMAIN_INPLAY){
				Players player;
				if((player = m.getPlayerByForumId(forumId)) != null){
					RoleForum role = m.getPlayerRole(player);
					if(phrase.equals("self")){role.setTarget2(player.getPlayerNumber());}
					else if(phrase.equals("noone")){role.setTarget2(0);}
					//if(role.getTarget1() == Integer.parseInt(phrase)){role.setTarget1(0);}//if selecting the same target same...cancel it
					else if(StringFunctions.isInteger(phrase)){role.setTarget2(Integer.parseInt(phrase));}
					try{
						String dayNight = null;
						if(m.getPhaseDayType()==Constants.PHASEDAYTYPE_NIGHT){dayNight = "onNightTargetChoice";}
						else if(m.getPhaseDayType()==Constants.PHASEDAYTYPE_NORMAL){dayNight = "onDayTargetChoice";}
						if(dayNight != null){
							Game.Base.Console.debug("doing script for target2");
							new scriptProcess(dayNight, role.getScript(dayNight), role);
						}
					}catch(Exception e){m.Game.Base.Console.printStackTrace(e);}
				}
			}
		}
	}
	public static void reserve(final Game Game, int forumId, String username, String phrase){
		Game.Base.Console.debug(username+" reserving");
		if(Game.getMatchSignup() != null){
			if(Game.getMatchSignup().getPhaseMain() <= Constants.PHASEMAIN_STARTING){//only if in starting/signup phase
				Game.getMatchSignup().addUserReserve(forumId, username);
				try{
					Game.Base.ForumAPI.pm_SendNew(username, Game.getMatchSignup().getName()+" Signups", "You have reserved a spot for "+Game.getMatchSignup().getName()+"!\n" +
								"If a player is removed from the game, or not enough players sign up, you will be notified as being a replacement.\n" +
								"If you feel you cannot participate in the future, please -withdraw.\n" +
								"Thanks for helping out.");
				}catch (VBulletinAPIException e) {}
			}
		}
	}
	public static void withdraw(final Game Game, int forumId, String username, String phrase){
		Game.Base.Console.debug(username+" withdrawing");
		//TODO detect if withdrawing from signup or ongoing
		if(Game.getMatchSignup() != null){
				Game.getMatchSignup().removeUserSignup(forumId);
				Game.getMatchSignup().removeUserReserve(forumId);
				try{
					Game.Base.ForumAPI.pm_SendNew(username, Game.getMatchSignup().getName()+" Signups", "You have withdrawn from "+Game.getMatchSignup().getName()+"!\n" +
								"Thanks for helping out.");
				}catch (VBulletinAPIException e) {}
		}
	}

	public static void unknown_command(final Game Game, int forumId, String username, String phrase){
		Game.Base.Console.debug(username+" unknown command: "+phrase);
		try{
			Game.Base.ForumAPI.pm_SendNew(username, "Unknown Command", "[CENTER]The command you sent is unknown and cannot be processed. command in question:[/CENTER]\n" +
						"\n" +
						phrase+"\n" +
						"\n" +
						"[CENTER]Please be sure not to use text formatting at anytime.[/CENTER]");
		}catch (VBulletinAPIException e) {}
	}
}
