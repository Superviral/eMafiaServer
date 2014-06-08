/* eMafiaServer - Constants.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes;

public class Constants {
	public static final String VERSION = "0.1.8";

	public static final String CMDVARDIVIDER = "";
	public static final String CMDVARSUBDIVIDER = "";

	// GameObject Types //
	public static final int TYPE_GAMEOB_CHAR = 1;
	public static final int TYPE_GAMEOB_NPC = 2;
	public static final int TYPE_GAMEOB_LOBBY = 3;
	public static final int TYPE_GAMEOB_MATCH = 4;
	public static final int TYPE_GAMEOB_MATCHFORUM = 5;
	public static final int TYPE_GAMEOB_USERGROUP = 6;
	public static final int TYPE_GAMEOB_ROLE_DEFAULT = 7;
	public static final int TYPE_GAMEOB_ROLE_CUSTOM = 8;
	public static final int TYPE_GAMEOB_ROLE_FORUM = 9;
	public static final int TYPE_GAMEOB_TEAM = 10;
	public static final int TYPE_GAMEOB_FLAG = 11;

	//Match
	public static final int PHASEMAIN_SETUP = 0;
	public static final int PHASEMAIN_SIGNUP = 1;//forum mafia only
	public static final int PHASEMAIN_NAMING = 2;//realtime mafia only
	public static final int PHASEMAIN_STARTING = 3;//forum mafia only
	public static final int PHASEMAIN_INPLAY = 4;
	public static final int PHASEMAIN_ENDGAME = 5;

	public static final int PHASEDAYTYPE_DISCUSSION = 1;
	public static final int PHASEDAYTYPE_NORMAL = 2;
	public static final int PHASEDAYTYPE_TRIALPLEAD = 3;
	public static final int PHASEDAYTYPE_TRIALVOTE = 4;
	public static final int PHASEDAYTYPE_LYNCH = 6;
	public static final int PHASEDAYTYPE_NIGHT = 8;

	//Forum
	public static final int FORUM_SIMPLE_SIGNUPS = 104;
	public static final int FORUM_SIMPLE_ONGOING = 292;
	public static final int GODFATHER_ID = 8070;

	//misc
	public static final int DELAY_BETWEEN_PMS = 5;//secs
}
