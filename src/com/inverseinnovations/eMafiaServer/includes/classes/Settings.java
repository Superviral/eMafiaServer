/* eMafiaServer - Settings.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Properties;

import com.inverseinnovations.eMafiaServer.*;
import com.inverseinnovations.eMafiaServer.includes.StringFunctions;
/**Server user-definable settings to be loaded from file*/
public class Settings {
	public Base Base;
	Properties p = new Properties();
	boolean loadError = false;

	// Server config variables
	//Keep in mind that on a live host, you'll want to bind to the outbound IP or host name, not localhost or 127.0.0.1, etc. 0 should work just fine
	public String SERVER_HOST;
	public int SERVER_PORT;
	public String SERVER_ADDRESS;//default is "localhost"
	public int SERVER_MAX_CLIENTS;//default is "256" is is how many players can connect at once
	public int CLIENT_BUILD;

	// Connection timeout for activity-less connections (in seconds)
	public int CONN_TIMEOUT;

	//MySql Config
	public String MYSQL_URL;
	public String MYSQL_USER;
	public String MYSQL_PASS;

	public String APIKEY;
	public String APIURL;
	public String GAMEMASTERNAME;
	public String GAMEMASTERPASS;

	public int LOGGING;

	/** Initialize and attempting to load server settings from settings.ini.
	 * If setting or file doesn't exist, will make file and choose default settings during this session.
	 */
	public Settings(Base base){
		this.Base = base;
		try {p.load(new FileInputStream("settings.ini"));}
		catch (Exception e) {
			loadError = true;
			Base.Console.config("Unable to read from settings.ini, attempting to make a new one.");
			PrintWriter writer;
			try {
				writer = new PrintWriter("settings.ini", "UTF-8");
				writer.println("# Server config variables");
				writer.println("#SERVER_HOST is the ip address other users connect to the server with.");
				writer.println("#Keep in mind that on a live host, you'll want to bind to the outbound IP or host name, not localhost or 127.0.0.1, etc.");
				writer.println("SERVER_HOST=		0");
				writer.println("SERVER_PORT=		1234");//1234 is dev server - 3689 is public server(Apo normally keeps public server up and running)
				writer.println("SERVER_ADDRESS=	localhost");
				writer.println("SERVER_MAX_CLIENTS=	256");
				writer.println("CLIENT_BUILD=		1");//Used for Client update control
				writer.println();
				writer.println("# Connection timeout for activity-less connections (in seconds)");
				writer.println("CONN_TIMEOUT=		1200");
				writer.println();
				writer.println("MYSQL_ADDR=		127.0.0.1");
				writer.println("MYSQL_PORT=		3306");
				writer.println("MYSQL_DBNAME=		mafiamud");
				writer.println("MYSQL_USER=		*****");
				writer.println("MYSQL_PASS=		*****");
				writer.println();
				writer.println("# SC2Mafia Forum Settings");
				writer.println("APIKEY=			*****");//Getting permission for Oops to release the key publicly
				writer.println("APIURL=			http://sc2mafia.com/forum/api.php");
				writer.println("GAMEMASTERNAME=		eMafia Game Master");//Any account really works..this is just a dedicated bot
				writer.println("GAMEMASTERPASS=		*****");
				writer.println();
				writer.println("# Logging Level:");
				writer.println("#OMIT = Only log Severe errors, Warnings, and Config settings");
				writer.println("#NORMAL = Log everything but the small details");
				writer.println("#EXPAND = Log everything including the small details");
				writer.println("#DEBUG= Every tid bit is displayed, may include sensitive data such as encrypted passwords and cause exessive garbage.");
				writer.println("LOGGING=		DEBUG");
				writer.close();
			}
			catch (Exception e2) {
				Base.Console.warning("Error: Unable to create a settings file, will continue to run with default");
			}
		}

		SERVER_HOST = loadVariableFromSettings("SERVER_HOST","0");
		SERVER_PORT = loadVariableFromSettings("SERVER_PORT",1234);
		SERVER_ADDRESS = loadVariableFromSettings("SERVER_ADDRESS","localhost");
		SERVER_MAX_CLIENTS = loadVariableFromSettings("SERVER_MAX_CLIENTS",256);
		CLIENT_BUILD = loadVariableFromSettings("CLIENT_BUILD",5);

		CONN_TIMEOUT = loadVariableFromSettings("CONN_TIMEOUT",1200);

		//"jdbc:mysql://192.168.1.80:3306/mafiamud";
		MYSQL_URL = "jdbc:mysql://"+loadVariableFromSettings("MYSQL_ADDR","127.0.0.1")+":"+loadVariableFromSettings("MYSQL_PORT",3306)+"/"+loadVariableFromSettings("MYSQL_DBNAME","mafiamud");
		MYSQL_USER = loadVariableFromSettings("MYSQL_USER","*****");
		MYSQL_PASS = loadVariableFromSettings("MYSQL_PASS","*****");

		APIKEY = loadVariableFromSettings("APIKEY","g9nZkHeE");
		APIURL = loadVariableFromSettings("APIURL","http://sc2mafia.com/forum/api.php");
		GAMEMASTERNAME = loadVariableFromSettings("GAMEMASTERNAME","eMafia Game Master");
		GAMEMASTERPASS = loadVariableFromSettings("GAMEMASTERPASS","*****");

		switch(loadVariableFromSettings("LOGGING","NORMAL").toUpperCase()){
			case "OMIT":LOGGING = 0;break;
			case "NORMAL":LOGGING = 1;break;
			case "EXPAND":LOGGING = 2;break;
			case "DEBUG":LOGGING = 3;break;
			default:LOGGING = 1;break;
		}

    }
    /** Attempt to retrieve the set String from settings.ini
     * @param varName
     * @param defaultValue
     * @return settings or defaultValue if non-exisitant
     */
    private String loadVariableFromSettings(String varName, String defaultValue){
    	if(!loadError){
		    String var = p.getProperty(varName);
			if(var != null && !var.equals("")){
				return var;
			}
    	}
		return defaultValue;
    }
    /** Attempt to retrieve the set int from settings.ini
     * @param varName
     * @param defaultValue
     * @return settings or defaultValue if non-exisitant
     */
    private int loadVariableFromSettings(String varName, int defaultValue){
	    if(!loadError){
    		String var = p.getProperty(varName);
			if(var != null && !var.equals("")){
				if(StringFunctions.isInteger(var)){
					return Integer.parseInt(var);
				}
			}
	    }
		return defaultValue;
    }
}
