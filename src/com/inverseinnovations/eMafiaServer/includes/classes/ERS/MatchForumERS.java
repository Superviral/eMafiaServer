/* eMafiaServer - MatchForumERS.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2014  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.ERS;

import java.util.Random;

import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.MatchForum;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.MatchForum.Players;

public class MatchForumERS{
	private MatchForum match;
	public MatchForumERS(final MatchForum match){
		if(this.match == null){this.match = match;}
	}
	public void changePlayerRole(int playerNum, int newRoleId){
		match.changePlayerRole(playerNum, newRoleId);
	}
	public void chatAddPlayer(String channelname,int playerNum,int talkPrive,int listenPriv){//[CHAT_ADDPLAYER](channelname),(playerNum),(talkPrive),(ListenPriv)[/CHAT_ADDPLAYER] edits the player in this channel is allow/disallow tlk/listen
		//[CHAT_ADDPLAYER]mafiachat,<PLAYER_NUMBER>,0,1[/CHAT_ADDPLAYER]
		//[CHAT_ADDPLAYER]channelname,playerNum,talkPrive,ListenPriv[/CHAT_ADDPLAYER] will overwrite previous istances, so is considered and edit too
		//Privs : 0 = none, 1 = normal, 2 = anony
		//match.chatController().addPlayerToChannel(playerNum, channelname, talkPrive, listenPriv);
	}
	public void chatCreate(String channelName, int timePhase){//[CHAT_CREATE](channelName),(timePhase)[/CHAT_CREATE]
		//[CHAT_CREATE]mafiachat,1[/CHAT_CREATE] creates a channel if one doesnt exist
		//[CHAT_CREATE]channelname,0-2[/CHAT_CREATE] 0-2 is the phase they may talk during. 0 = day, 1= night 2 = anytime
		//match.chatController().addChannel(channelName, timePhase);
	}
	public void chatRemovePlayer(String channelname,int playerNum){//[CHAT_REMOVEPLAYER]mafiachat,<PLAYER_NUMBER>[/CHAT_REMOVEPLAYER] removes the playerNum from the Channel if either exist
		//[CHAT_REMOVEPLAYER]channelname,playerNum[/CHAT_REMOVEPLAYER]
		//match.chatController().addChannel(channelname, playerNum);
	}
	public RoleForumERS[] getAlivePlayers(){//Returns int[] array of all alive player numbers
		Players[] players = (Players[])match.getAliveList().toArray();
		RoleForumERS[] theReturn = new RoleForumERS[players.length];
		for(int i = 0; i < players.length; i++){
			if(match.getPlayerRole(players[i].getPlayerNumber()) != null){
			theReturn[i] = match.getPlayerRole(players[i].getPlayerNumber()).getERSClass();
			}
		}
		return theReturn;
	}
	public int getDay(){//Returns which Day(or Night) it is
		return match.getPhaseDay();
	}
	public RoleForumERS[] getDeadPlayers(){//Returns int[] array of all alive player numbers
		Players[] players = (Players[])match.getDeadList().toArray();
		RoleForumERS[] theReturn = new RoleForumERS[players.length];
		for(int i = 0; i < players.length; i++){
			if(match.getPlayerRole(players[i].getPlayerNumber()) != null){
			theReturn[i] = match.getPlayerRole(players[i].getPlayerNumber()).getERSClass();
			}
		}
		return theReturn;
	}
	public int getNumPlayersAlive(){//Returns number of players still alive
		return match.getNumPlayersAlive();
	}
	public int getNumPlayers(){//returns total number of players
		return match.getNumPlayers();
	}
	public RoleForumERS getPlayer(int playerNum){
		RoleForumERS theReturn = null;
		if(match.getPlayerRoleWithSwitch(playerNum) != null){
			theReturn = match.getPlayerRoleWithSwitch(playerNum).getERSClass();
		}
		return theReturn;
	}
	public RoleForumERS[] getPlayers(){//Returns int[] array of all alive player numbers
		Players[] players = (Players[])match.getPlayerList().toArray();
		RoleForumERS[] theReturn = new RoleForumERS[players.length];
		for(int i = 0; i < players.length; i++){
			if(match.getPlayerRole(players[i].getPlayerNumber()) != null){
			theReturn[i] = match.getPlayerRole(players[i].getPlayerNumber()).getERSClass();
			}
		}
		return theReturn;
	}
	public TeamForumERS getTeam(String teamName){
		return match.getTeam(teamName).getERSClass();
	}
	public boolean isAffAlive(String affName){//[IS_AFF_ALIVE](name of affliation)[/IS_AFF_ALIVE] Returns true if any members of entered Affliliation are still alive
		return match.isAffliationAlive(affName);
	}
	public boolean isCatAlive(String catName){//[IS_CAT_ALIVE](name of category)[/IS_CAT_ALIVE] Returns true if any members of entered category are still alive
		return match.isCategoryAlive(catName);
	}
	public boolean isRoleAlive(int roleId){
		return match.isRoleAlive(roleId);
	}
	public boolean isRoleExist(int roleId){
		return match.isRoleExist(roleId);
	}
	public int numRoleAlive(int roleId){
		return match.numRoleAlive(roleId);
	}
	public int randomNumber(int[] numbers){
		int theReturn = 0;
		Random rand = new Random();
		int max = rand.nextInt(numbers.length);
		theReturn = numbers[max];
		return theReturn;
	}
	public int randomNumber(int min, int max){
		int theReturn = 0;
		if(min < max){
			int randMax = max - min;
			Random rand = new Random();
			theReturn = rand.nextInt(randMax) - min;
		}
		return theReturn;
	}
	public RoleERS randomPlayer(RoleERS[] players){
		RoleERS theReturn = null;
		Random rand = new Random();
		int max = rand.nextInt(players.length);
		theReturn = players[max];
		return theReturn;
	}
 	public void switchPlayerTemp(int playerNum, int playerNum2){//[SWITCH_PLAYER_TEMP](playerNum),(playerNum2)[/SWITCH_PLAYER_TEMP] everyone targeting the first player will instead target the second player(this is one way and only lasts until day)
		//[SWITCH_PLAYER_TEMP]<TARGET1>,<TARGET2>[/SWITCH_PLAYER_TEMP]
		match.setSwitchedPlayerNum(playerNum,playerNum2);
	}
	public void switchPlayerTemp(RoleERS player, RoleERS player2){//[SWITCH_PLAYER_TEMP](playerNum),(playerNum2)[/SWITCH_PLAYER_TEMP] everyone targeting the first player will instead target the second player(this is one way and only lasts until day)
		//[SWITCH_PLAYER_TEMP]<TARGET1>,<TARGET2>[/SWITCH_PLAYER_TEMP]
		match.setSwitchedPlayerNum(player.getPlayerNum(),player2.getPlayerNum());
	}
	public void text(String msg){
		//match.send(CmdCompile.chatScreen(msg)); //TODO fix me...
	}

}