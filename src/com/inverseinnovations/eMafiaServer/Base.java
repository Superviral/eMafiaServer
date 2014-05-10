/* eMafiaServer - Base.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer;

import com.inverseinnovations.eMafiaServer.includes.*;
import com.inverseinnovations.eMafiaServer.includes.classes.*;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.*;
import com.inverseinnovations.eMafiaServer.includes.classes.Server.*;
import com.inverseinnovations.VBulletinAPI.*;
//import com.inverseinnovations.VBulletinAPI.VBulletinAPI;

public final class Base {
	private boolean GAME_IS_RUNNING = true;
	public int program_faults = 0;
	public final Console Console = new Console(this);
	public final Settings Settings = new Settings(this);
	public final SocketServer Server = new SocketServer(this, Settings.SERVER_HOST, Settings.SERVER_PORT, Settings.SERVER_MAX_CLIENTS);
	public final MySqlDatabaseHandler MySql = new MySqlDatabaseHandler(this);
	public final VBulletinAPI ForumAPI = new VBulletinAPI(Settings.GAMEMASTERNAME, Settings.GAMEMASTERPASS, Settings.APIURL, Settings.APIKEY, "eMafiaServer Debugging atm - Hi Nick", Constants.VERSION);
	public final Game Game = new Game(this);

	public Base(){
		synchronized(this){
			while (GAME_IS_RUNNING){
				try {
					this.wait();
				}
				catch (InterruptedException e) {
					Console.printStackTrace(e);
				}
			}
		}
		Console.warning("The eMafia Server is no longer running.");
		Console.saveLog();
		System.exit(0);
	}
	/**
	 * Called after MySQL is initilized to continue with startup.
	 */
	public void mysqlReady(){
		if(program_faults == 0){
			Server.create();
			//Load usergroups from the database
			MySql.loadUsergroups(Game);
			// Setup a lobby for players to join //
			new Lobby(Game, 1, "The Game Lobby");//This shall remain
			//put 2 matchs up for debugging..remove later(will remain until release to public)
			new Match(Game, "A First Match");
			new Match(Game, "Another Match");
			Server.listen();//should only be called once

			//Game.scheduleTicker();
			Console.config("eMafia Server "+Constants.VERSION+" is now up and running!");
		}
		else{
			Console.severe("eMafia Server is experiencing errors and cannot continue.");
		}
	}
	/**
	 * Immediatly closes the program
	 */
	public synchronized void shutdown(){
		GAME_IS_RUNNING = false;
		this.notify();
	}
	public static void main(String[] args) {
		new Base();
	}
}
