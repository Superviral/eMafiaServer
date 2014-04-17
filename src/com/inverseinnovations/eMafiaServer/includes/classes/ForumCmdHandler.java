/* eMafiaServer - CmdHandler.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;

import java.lang.reflect.InvocationTargetException;

public class ForumCmdHandler {

	/**
	 * Performs commands based on location the character is at(match or lobby),
	 * and detrimines whether the command even exists or not.
	 * @param c Character doing the command
	 * @param command The snytax
	 * @param phrase parameters for snytax
	 */
	public static void processCmd(Game Game,String username,String command,String phrase){
		System.out.println("processing forum cmd: "+command+" for "+username);
		if(command.startsWith("-")){
			command = command.toLowerCase().substring(1);
			for(String fullword : ForumCmd.CMDLIST){
				if(command.equals(fullword)){
					System.out.println("invoking "+command);
					doInvoke(grabMethod(ForumCmd.class,fullword),ForumCmd.class,Game, username,phrase);
				}
			}
		}
		return;
	}

	private static java.lang.reflect.Method grabMethod(Object clas, String name){
		java.lang.reflect.Method method = null;
		try {
			@SuppressWarnings("rawtypes")
			Class[] par=new Class[3];
			par[0]=Game.class;
			par[1]=String.class;
			par[2]=String.class;
			method = ((Class<?>) clas).getMethod(name,par);
		}
		catch (SecurityException e) {}
		catch (NoSuchMethodException e) {}
		return method;
	}
	private static void doInvoke(java.lang.reflect.Method method, Object clas, Game Game, String username, String phrase){
		if(method != null){
			try {
					Object[] arg=new Object[3];
					arg[0]=Game;
					arg[1]=username;
					arg[2]=phrase;
					method.invoke(clas,arg);
			}
			catch (IllegalArgumentException e) {}
			catch (IllegalAccessException e) {}
			catch (InvocationTargetException e) {}
		}
	}
}
