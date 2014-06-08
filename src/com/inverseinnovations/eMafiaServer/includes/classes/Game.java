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
import com.inverseinnovations.eMafiaServer.includes.Constants;
import com.inverseinnovations.eMafiaServer.includes.SandboxContextFactory;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.*;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character;
import com.inverseinnovations.eMafiaServer.includes.classes.Server.*;
import com.inverseinnovations.VBulletinAPI.VBulletinAPI.Message;
import com.inverseinnovations.VBulletinAPI.Exception.VBulletinAPIException;
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
	public void setMatchSignup(final MatchForum match){
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
	 * Returns a Date of the very next hour on the hour(0 min/0 sec)
	 * @return
	 */
	public Date nextHour(){
		return nextXHour(1);
    }
	/**
	 * Returns a Date of X hours from now on the hour(0 min/0 sec)
	 * @return
	 */
	public Date nextXHour(int hours){
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.add(Calendar.HOUR, hours);
		return cal.getTime();
    }
	/**
	 * Performs each hourly check within the Server. Includes Parsing PMs, checking matches for advancePhases, Starting Matches, ect
	 */
	public void hourlyChecks(){
		//Do everything here!
		Base.Console.debug("Performing hourly checks");
		PMcheck();
		//Controls the Ongoing
		if(getMatchOngoing() != null){
			getMatchOngoing().checkLynch();
			if(getMatchOngoing().isTimerUp()){
				//the timer came up and phase should have advanced
			}
		}


		//Controls the Signups
		if(getMatchSignup() != null){
			getMatchSignup().postSignup(true);
			if(getMatchSignup().getPhaseMain() <= Constants.PHASEMAIN_STARTING){//if in starting mode or below(signup too)
				if(getMatchOngoing() == null){//only if there isnt a match ongoing
					if(getMatchSignup().gameStart()){//perform possible game start(two phases) and if started..do this
						setMatchOngoing(getMatchSignup());
						setMatchSignup(null);//switch slots
						Base.Console.debug("Signup is now Ongoing");
					}
				}
			}
		}
	}
	/**
	 * Checks the PMs, performs the commands issued via PM, and empties the inbox
	 * @return false on error
	 */
	public boolean PMcheck(){
		boolean theReturn = true;
		Base.Console.debug("Performing hourly PM checks");
		ArrayList<Message> PMlist = new ArrayList<Message>();
		try {
			PMlist = Base.ForumAPI.pm_ListPMs();
		}catch (VBulletinAPIException e1) {theReturn = false;}
		if(theReturn){
			if(PMlist != null){
				if(!PMlist.isEmpty()){
					boolean empty = false;
					try {
						empty = Base.ForumAPI.pm_EmptyInbox();
					}catch (VBulletinAPIException e1) {}
					if(empty){
						Base.Console.debug("Emptied PM box");
					}
					else{
						Base.Console.debug("Emptied PM box failed... ");
					}
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
							TimeUnit.SECONDS.sleep(Constants.DELAY_BETWEEN_PMS);
						}catch (InterruptedException e) {e.printStackTrace();}
						System.out.println("PMList processed PM "+msg.title+" from "+msg.username);
					}
				}
			}
			else{
				Base.Console.debug("PM checks failed... ");
			}
		}
		else{
			Base.Console.debug("PM checks error... ");
		}
		return theReturn;
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
