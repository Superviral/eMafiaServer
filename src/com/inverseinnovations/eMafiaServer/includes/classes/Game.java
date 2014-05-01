/* eMafiaServer - Game.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.mozilla.javascript.ContextFactory;

import com.inverseinnovations.eMafiaServer.*;
import com.inverseinnovations.eMafiaServer.includes.SandboxContextFactory;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.*;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character;
import com.inverseinnovations.eMafiaServer.includes.classes.Server.*;
import com.inverseinnovations.eMafiaServer.includes.classes.Server.SC2MafiaAPI.Message;
/**Manages characters,matches,and the lobby*/
public final class Game {
	public final Base Base;//back reference to parent

	public boolean GAME_IS_RUNNING = true;
	public boolean GAME_PAUSED = true;
	private long start_time; // Game instance start time //
	/** Connects (Game)Character EID to Character class*/
	private Map<Integer, Character> characters = new HashMap<Integer, Character>();
	public int char_counter = 1;//stores last character eid
	private Map<Integer, Lobby> lobbys = new HashMap<Integer, Lobby>();
	private Map<Integer, Match> matches = new HashMap<Integer, Match>();
	private HashMap<Integer, Usergroup> usergroups = new HashMap<Integer, Usergroup>();
	private Timer ticker;
	private TickTask tickTask;
	private MatchForum matchOngoing = null;
	private MatchForum matchSignup = null;

	/**
	 * Prepares main Game handler
	 */
	public Game(final Base base){
		this.Base = base;
		this.start_time = System.nanoTime();
		ContextFactory.initGlobal(new SandboxContextFactory());

		tickerSchedule();
		//tickTask.doTask();//do without schedule
	}
	/**
	 * Sets the Timer to the next hour
	 */
	public void tickerSchedule(){
		/*if(ticker != null){
			try {
				ticker.cancel();
				ticker.purge();
			} catch (java.lang.IllegalStateException e) {}//ignore it
		}*/
		if(this.tickTask == null){
			this.tickTask = new TickTask();
		}
		if(this.ticker == null){
			this.ticker = new Timer();
		}
		try{
			ticker.schedule(tickTask,nextHour(),1000*60*60);//and repaet every hour
		} catch (java.lang.IllegalStateException e) {
			tickerCancel();
			ticker.schedule(tickTask,nextHour(),1000*60*60);//and repaet every hour
		}
	}
	/**
	 * Sets the Timer to the next hour
	 */
	public void tickerCancel(){
		if(ticker != null){
			try {
				ticker.cancel();
				ticker.purge();
			} catch (java.lang.IllegalStateException e) {}//ignore it
			ticker = null;
		}
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
	/**Returns the current Forum Match, if there is one
	 * @return null is none
	 */
	public MatchForum getMatchOngoing(){
		return matchOngoing;
	}
	/**Assigns a game as the current Forum Match
	 * @param match
	 */
	public void setMatchOngoing(MatchForum match){
		this.matchOngoing = match;
	}
	/**Returns the current Forum Signups, if there is one
	 * @return null is none
	 */
	public MatchForum getMatchSignup(){
		return matchSignup;
	}
	/**Assigns a game as the current Forum Signups
	 * @param match
	 */
	public void setMatchSignup(MatchForum match){
		this.matchSignup = match;
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
	/**
	 * Returns a Date of the very next hour
	 * @return
	 */
	private Date nextHour(){
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.add(Calendar.HOUR, 1);
		return cal.getTime();
    }
	public void hourlyChecks(){
		//Do everything here!
		Base.Console.debug("Attempting to view pms");
		ArrayList<Message> PMlist = Base.ForumAPI.pm_ListPMs();
		String empty = Base.ForumAPI.pm_EmptyInbox();
		if(empty.equals("pm_messagesdeleted")){
			Base.Console.debug("Emptied PM box");
		}
		else{
			Base.Console.debug("Emptied PM box failed... : "+empty);
		}
		if(PMlist != null){
			for(Message msg:PMlist){
				if(msg.message.contains(" ")){
					String[] cmdPhrase = msg.message.split(" ", 2);
					String cmd = cmdPhrase[0];
					String para = cmdPhrase[1];

					System.out.println("PMList split the msg...about to process "+cmd+" from "+msg.username);
					ForumCmdHandler.processCmd(this, msg.userid, msg.username, cmd, para);
				}
				else{
					System.out.println("...about to process "+msg.message+" from "+msg.username);
					ForumCmdHandler.processCmd(this, msg.userid, msg.username, msg.message, null);
					System.out.println("PMList sent PM ");
				}
				try {
					TimeUnit.SECONDS.sleep(7);
				}catch (InterruptedException e) {e.printStackTrace();}
				System.out.println("PMList processed PM "+msg.title+" from "+msg.username);
			}
			getMatchSignup().postSignup(true);
		}
		else{
			Base.Console.debug("view failed... : "+PMlist);
		}
	}
	/**
	 * Timer Ticker for executing hourly tasks
	 */
	private class TickTask extends TimerTask {

		public TickTask(){

		}
		@Override
		public void run() {
			hourlyChecks();
			//Game.scheduleTicker();
		}
	}
}
