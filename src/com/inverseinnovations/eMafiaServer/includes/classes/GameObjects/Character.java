/* eMafiaServer - Character.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.GameObjects;

import java.util.Map;
import com.inverseinnovations.eMafiaServer.includes.*;
import com.inverseinnovations.eMafiaServer.includes.classes.Game;
import com.inverseinnovations.eMafiaServer.includes.classes.Server.SocketClient;


public class Character extends GameObject{
	public Game Game;
	private Integer connection;
	private Integer accountId;
	private int usergroup;
	private String avatar;
	private int location;// Lobby/Match EID
	private boolean ingame;//if in match
	private int playernum; //playernum ingame

	/**
	 * Creates Character for representation of a client within Game, Lobby, and Match<br>
	 * Automatically assigns character within Game() but not inside a Lobby/Match
	 * @param game Game reference
	 * @param name name of Character
	 * @param usergroup Usergroup character is assigned to
	 */
	public Character(Game game, String name, int usergroup) {
		super(0, name, Constants.TYPE_GAMEOB_CHAR);
		this.Game = game;
		Map<Integer, Character> gameChars = Game.getCharacters();
		for (int i = Game.char_counter; ; i++){
			if (!gameChars.containsKey(i)){
				this.setEID(i);
				Game.char_counter++;
				break;
			}
		}
		this.location = 1;//start off in lobby 1
		this.ingame = false;//not in a game by default...false = lobby
		this.usergroup = usergroup;
		Game.addCharacter(this);

	}
	/**
	 * Assigns the database Id which the Character is attached to
	 */
	public void setAccountID(Integer id){
		this.accountId = id;
	}
	/**
	 * Returns the database Id which the Character is assigned to
	 */
	public Integer getAccountId(){
		return this.accountId;
	}
	/**
	 * Sets the Usergroup which the Character is assigned to
	 */
	public void setUsergroup(int usergroup){
		this.usergroup = usergroup;
	}
	/**
	 * Returns the Usergroup the Character is assigned to
	 */
	public Usergroup getUsergroup(){
		return Game.getUsergroup(usergroup);
	}
	/**
	 * Sets the forum Url path of the forum Avatar of the Character
	 */
	public void setAvatar(String path){
		this.avatar = path;
	}
	/**
	 * Returns the forum Url path of the forum Avatar of the Character
	 */
	public String getAvatar(){
		return avatar;
	}
	/**
	 * Sets if the Character is in a Match or Lobby
	 */
	private void setInGame(boolean ingame){//should never be called manuelly
		this.ingame = ingame;
	}
	/**
	 * Checks whether Character is in a Match or Lobby
	 * @return true = Match, false = Lobby
	 */
	public boolean getInGame(){
		return this.ingame;
	}
	/**
	 * Sets the player num of the Character if in a Match
	 */
	public void setPlayerNum(int playernum){
		this.playernum = playernum;
	}
	/**
	 * Returns the player num of the Character if in a Match
	 */
	public int getPlayerNum(){
		return this.playernum;
	}
	/**
	 * Sets the Lobby of which the Character is aprat of
	 */
	public void setLocation(Lobby lobby){
		this.location = lobby.getEID();
		this.setInGame(false);
	}
	/**
	 * Sets the Match of which the Character is apart of
	 */
	public void setLocation(Match match){
		this.location = match.getEID();
		this.setInGame(true);
	}
	/**
	 * Returns the id of either the Lobby or Match the Character is apart of
	 */
	public int getLocation(){
		return this.location;
	}
	/**
	 * Returns the Lobby the Character is apart of
	 * @return null if in a Match
	 */
	public Lobby getLobby(){
		if(getInGame()){return null;}
		return Game.getLobby(getLocation());
	}
	/**
	 * Returns the Match the Character is apart of
	 * @return null if in a Lobby
	 */
	public Match getMatch(){
		if(getInGame()){return Game.getMatch(getLocation());}
		return null;
	}
	/**
	 * If possible to join the Macth, removes Character from current Lobby and joins the Match<br>
	 * Auto sets location and adds to Match
	 * @param match Match to join
	 * @return true on successs, false on failure
	 */
	public boolean joinMatch(Match match){//Join the match (the object) not eid
		if(match.getNumChars() < match.getSetting("max_chars")){//if theres room in the match
			this.getLobby().removeCharacter(this);
			this.setLocation(match);//set to new room
			match.addCharacter(this);
			this.send(CmdCompile.chatScreen("<hr>"));
			return true;
		}
		return false;
	}
	/**
	 * Removes the Character from the current Match if apart of one<br>
	 * leaveMatch() is preferred to rejoin the Lobby
	 */
	public void leave(){
		if(getInGame()){
			this.getMatch().removeCharacter(this);
			this.getMatch().gameCancel();
			if(this.getType() != Constants.TYPE_GAMEOB_NPC){
				if(getEID() == getMatch().getHostId()){
					if(!getMatch().findNewHost()){//end match if no new host found
						getMatch().endMatch();
					}
				}
				send(CmdCompile.closeLayer("matchSetup"));//close the match window
				send(CmdCompile.enterLobby());//open Lobby,client will call look
			}
		}
	}
	/**
	 * Removes the Character from the current Match if apart of one and rejoins the main Lobby
	 */
	public void leaveMatch(){//leave the match, join lobby
		if(getInGame()){
			this.leave();
			if(this.getType() != Constants.TYPE_GAMEOB_NPC){
				this.setLocation(Game.getLobby(1));//join lobby 1
				this.getLobby().addCharacter(this);
				this.send(CmdCompile.chatScreen("<hr>"));
			}
		}
	}
	/**
	 * Sets the Client id Character is attached to
	 */
	public void setConnection(Integer id){
		this.connection = id;
	}
	/**
	 * Returns the Client the Character is attached to
	 * @return null is not attached
	 */
	public SocketClient getConnection(){
		if(connection != null) return Game.getConnection(connection);
		return null;
	}
	/**
	 * Sets the Character and Client as offline, and disconnects Client
	 */
	public void setOffline(){
		SocketClient cConn = this.getConnection();
		if(cConn != null) cConn.offline();
	}
	/**
	 * Sends DataPacket to attached Client
	 */
	public void send(byte[] message){//this is basiclly sendDirect atm
		SocketClient cConn = this.getConnection();
		if(cConn != null){
			cConn.send(message);
		}
	}
}
