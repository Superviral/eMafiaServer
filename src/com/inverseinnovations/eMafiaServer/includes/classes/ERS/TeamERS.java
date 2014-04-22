/* eMafiaServer - TeamERS.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2013  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.ERS;

import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Team;

public class TeamERS{
	private Team team;
	public TeamERS(final Team team){
		if(this.team == null){this.team = team;}
	}
	public void addTeammate(int playerNum){
		team.addTeammate(playerNum);
	}
	public void addTeammate(RoleERS player){
		addTeammate(player.getPlayerNum());
	}
	public RoleERS[] getAliveTeammates(){
		RoleERS[] theReturn = new RoleERS[team.getAliveTeammates().size()];
		int loop = 0;
		for(Integer i:team.getAliveTeammates()){
			if(team.getMatch().getPlayerRole(i) != null){
				theReturn[loop] = team.getMatch().getPlayerRole(i).getERSClass();
				loop++;
			}
		}
		return theReturn;
	}
	public boolean getMayGameEnd(){
		return team.getMayGameEnd();
	}
	public String getName(){
		return team.getName();
	}
	public String getScript(String eventCall){
		return team.getScript(eventCall);
	}
	public RoleERS[] getTeammates(){
		RoleERS[] theReturn = new RoleERS[team.getTeammates().size()];
		int loop = 0;
		for(Integer i:team.getTeammates()){
			if(team.getMatch().getPlayerRole(i) != null){
				theReturn[loop] = team.getMatch().getPlayerRole(i).getERSClass();
				loop++;
			}
		}
		return theReturn;
	}
	public boolean getVarBoolean(String varName){
		boolean theReturn = false;
		theReturn = team.getCustomVarBoolean(varName);
		return  theReturn;
	}
	public int getVarInt(String varName){
		int theReturn = 0;
		theReturn = team.getCustomVarInt(varName);
		return  theReturn;
	}
	public String getVarString(String varName){
		String theReturn = "";
		theReturn = team.getCustomVarString(varName);
		return  theReturn;
	}
	public boolean getVictory(){
		return team.getVictory();
	}
	public boolean isRoleExist(int roleId){
		return team.isRoleExist(roleId);
	}
	public boolean isRoleAlive(int roleId){
		return team.isRoleAlive(roleId);
	}
	public int numRoleAlive(int roleId){
		return team.numRoleAlive(roleId);
	}
	public void pollClear(String pollName){
		team.pollClear(pollName);
	}
	public int pollHighestVote(String pollName){
		return team.pollHighestVote(pollName);
	}
	public void pollVoteAdd(String pollName, int option){
		team.pollVoteAdd(pollName, option);
	}
	public void pollVoteRemove(String pollName, int option){
		team.pollVoteAdd(pollName, option);
	}
	public void removeTeammate(int playerNum){
		team.removeTeammate(playerNum);
	}
	public void removeTeammate(RoleERS player){
		removeTeammate(player.getPlayerNum());
	}
	public void setMayGameEnd(boolean end){
		team.setMayGameEnd(end);
	}
	public void setScript(String eventCall, String script){
		team.setScript(eventCall, script);
	}
	public void setVarBoolean(String varName,boolean value){
		team.setCustomVarBoolean(varName, value);
	}
	public void setVarInt(String varName,int value){
		team.setCustomVarInt(varName, value);
	}
	public void setVarString(String varName,String value){
		team.setCustomVarString(varName, value);
	}
	public void setVictory(boolean vict){
		team.setVictory(vict);
	}
	public void text(String msg){
		for(int playerNum: team.getTeammates()){
			team.getMatch().sendToPlayerNum(playerNum, msg);//XXX need to send to chatChannel instead
		}
	}
}
