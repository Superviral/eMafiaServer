/* eMafiaServer - Game.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;


import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.ContextFactory;

import com.inverseinnovations.eMafiaServer.*;
import com.inverseinnovations.eMafiaServer.includes.SandboxContextFactory;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.*;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character;
import com.inverseinnovations.eMafiaServer.includes.classes.Server.*;
/**Manages characters,matches,and the lobby*/
public class Game {
	public Base Base;//back reference to parent

	public boolean GAME_IS_RUNNING = true;
	public boolean GAME_PAUSED = true;
	private long start_time; // Game instance start time //
	/** Connects (Game)Character EID to Character class*/
	private Map<Integer, Character> characters = new HashMap<Integer, Character>();
	public int char_counter = 1;//stores last character eid
	private Map<Integer, Lobby> lobbys = new HashMap<Integer, Lobby>();
	private Map<Integer, Match> matches = new HashMap<Integer, Match>();
	private HashMap<Integer, Usergroup> usergroups = new HashMap<Integer, Usergroup>();

	/**
	 * Prepares main Game handler
	 */
	public Game(Base base){
		this.Base = base;
		this.start_time = System.nanoTime();
		ContextFactory.initGlobal(new SandboxContextFactory());
	}
	/**
	 * Assigns a Usergroup to Game(),
	 */
	public void addUsergroup(Usergroup userg){
		this.usergroups.put(userg.getEID(), userg);
	}
	/**
	 * Returns a Usergroup based on id
	 * @return null if usergroup id is nonexistant
	 */
	public Usergroup getUsergroup(int id){
		if (this.usergroups.containsKey(id)){return this.usergroups.get(id);}
		return null;
	}
	/**
	 * Assigns a new Lobby to Game()
	 * @param l Lobby
	 */
	public void addLobby(Lobby l){
		this.lobbys.put(l.getEID(), l);
	}
	/**
	 * Removes the Lobby from Game()
	 */
	public void removeLobby(Lobby l){
		this.lobbys.remove(l.getEID());
	}
	/**
	 * Returns a Lobby from the Game()
	 * @return null if Lobby is nonexistant
	 */
	public Lobby getLobby(int id){
		if (this.lobbys.containsKey(id)){return this.lobbys.get(id);}
		return null;
	}
	/**
	 * Returns a Map of Lobbys in Game()
	 */
	public Map<Integer, Lobby> getLobbys(){
		return this.lobbys;
	}
	/**
	 * Assigns a new Match to Game()
	 */
	public void addMatch(Match m){
		this.matches.put(m.getEID(), m);
		Base.Console.fine("\""+m.getName()+"\" match created");
		//TODO Client: add match to client's Match_List
	}
	/**
	 * Removes a Match from Game()
	 */
	public void removeMatch(Match m){
		this.matches.remove(m.getEID());
		//TODO Client: remove match from client's Match_List
	}
	/**
	 * Returns a Match based on id
	 * @return null if nonexistant
	 */
	public Match getMatch(int id){
		if (this.matches.containsKey(id)){return this.matches.get(id);}
		return null;
	}
	/**
	 * Returns a Map of all Matchs
	 */
	public Map<Integer, Match> getMatchs(){
		return this.matches;
	}
	/**
	 * Assigns a Character to the Game()
	 */
	public void addCharacter(Character c){
		Base.Console.debug(c.getName()+" added to game");
		this.characters.put(c.getEID(), c);
	}
	/**
	 * Removes a Character from Game()
	 */
	public void removeCharacter(Character c){
		this.characters.remove(c.getEID());
	}
	/**
	 * Returns a Character from Game()
	 * @return null if nonexistant
	 */
	public Character getCharacter(int id){
		if (this.characters.containsKey(id)){return this.characters.get(id);}
		return null;
	}
	/**
	 * Returns a Map of all Characters
	 */
	public Map<Integer, Character> getCharacters(){
		return this.characters;
	}
	/**
	 * Returns SocketClient bound to the given client id
	 * @return null if nonexistant
	 */
	public SocketClient getConnection(int id){
		return Base.Server.getClient(id);
	}
}
