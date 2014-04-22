/* eMafiaServer - LobbyCmd.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;

/**
 * Provides list of all commands a Character may call when inside a Lobby<br>
 * All method names must be appended to CMDLIST[] to be callable
 */
public class ForumCmd {
	public static String[] CMDLIST = {
		"info"
	};
	public static void info(Game Game, String username, String phrase) {
		System.out.println("doing info");
		//String theReturn = Game.Base.ForumAPI.pm_SendNew(username, "Info Request", "You asked for it, you got.<br>Well...not much to say, I'm a bot after all.");
	}
}
