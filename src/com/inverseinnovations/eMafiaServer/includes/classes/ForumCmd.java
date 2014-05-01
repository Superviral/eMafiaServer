/* eMafiaServer - LobbyCmd.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;

import com.inverseinnovations.eMafiaServer.includes.StringFunctions;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.MatchForum.Players;

/**
 * Provides list of all commands a Character may call when inside a Lobby<br>
 * All method names must be appended to CMDLIST[] to be callable
 */
public class ForumCmd {
	public static String[] CMDLIST = {
		"info","sign","reserve","withdraw","unknown_command"
	};
	public static void info(final Game Game, int forumId, String username, String phrase) {
		System.out.println(username+" doing info");
		//String theReturn = Game.Base.ForumAPI.pm_SendNew(username, "Info Request", "You asked for it, you got.<br>Well...not much to say, I'm a bot after all.");
	}
	public static void sign(final Game Game, int forumId, String username, String phrase) {
		Game.Base.Console.debug(username+" signing");
		if(Game.getMatchSignup() != null){
				String string = "";
				for(Players player:Game.getMatchSignup().getSignupList()){
					string += player.getName()+" ";
				}
				Game.Base.Console.debug("Before adding signup had: "+string);
			Game.getMatchSignup().addUserSignup(forumId, username);
				string = "";
				for(Players player:Game.getMatchSignup().getSignupList()){
					string += player.getName()+" ";
				}
				Game.getMatchSignup().testNum = StringFunctions.randInt(1, 200);
				Game.Base.Console.debug(Game.getMatchSignup().testNum+" After adding signup had: "+string);
			Game.Base.ForumAPI.pm_SendNew(username, Game.getMatchSignup().getName()+" Signups", "You've signed up for "+Game.getMatchSignup().getName()+"!<BR>If you feel you can't participate in the future, please -withdraw.<BR>Thanks for helping out.");
		}
	}
	public static void reserve(final Game Game, int forumId, String username, String phrase) {
		Game.Base.Console.debug(username+" reserving");
		if(Game.getMatchSignup() != null){
			Game.getMatchSignup().addUserReserve(forumId, username);
			Game.Base.ForumAPI.pm_SendNew(username, Game.getMatchSignup().getName()+" Signups", "You've reserved a spot for "+Game.getMatchSignup().getName()+"!<BR>If a player is removed from the game, or not enough players sign up, you'll be notified as being a replacement.<BR>If you feel you can't participate in the future, please -withdraw.<BR>Thanks for helping out.");
		}
	}
	public static void withdraw(final Game Game, int forumId, String username, String phrase) {
		Game.Base.Console.debug(username+" withdrawing");
		if(Game.getMatchSignup() != null){
			Game.getMatchSignup().removeUserSignup(forumId);
			Game.getMatchSignup().removeUserReserve(forumId);
			Game.Base.ForumAPI.pm_SendNew(username, Game.getMatchSignup().getName()+" Signups", "You've withdrawn from "+Game.getMatchSignup().getName()+"!<BR>Thanks for helping out.");
		}
	}

	public static void unknown_command(final Game Game, int forumId, String username, String phrase) {
		Game.Base.Console.debug(username+" unknown command: "+phrase);
		Game.Base.ForumAPI.pm_SendNew(username, "Unknown Command", "<CENTER>The command you sent is unknown and cannot be processed. command in question:<CENTER><BR><BR>"+phrase+"<BR><BR><CENTER>Please be sure not to use text formatting at anytime.<CENTER>");
	}
}
