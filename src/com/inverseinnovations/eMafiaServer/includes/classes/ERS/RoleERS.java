/* eMafiaServer - RoleERS.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2013  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.ERS;

import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Flag;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Role;

public class RoleERS{
	private Role role;
	public RoleERS(final Role role){
		if(this.role == null){this.role = role;}
	}
	public void addFlag(Flag flag){
		role.addFlag(flag);
	}
	public void addFlag(String flag){
		role.addFlag(flag);
	}
	public void attack(int playerNumToAttack,String graveyardDesc,String DescOfDeath){//[ATTACK](playerNum),(graveyardDesc),(DescOfDeath)[/ATTACK] potnentaily kill playerNum
		role.Match.damagePlayer(playerNumToAttack,role.getPlayerNum(),graveyardDesc,DescOfDeath);
	}
	public void attack(RoleERS playerToAttack,String graveyardDesc,String DescOfDeath){//[ATTACK](playerNum),(graveyardDesc),(DescOfDeath)[/ATTACK] potnentaily kill playerNum
		attack(playerToAttack.getPlayerNum(),graveyardDesc,DescOfDeath);
	}
	public void clearTargets(){//sets targets 1 and 2 to 0(none)
		role.clearTargets();
	}
	/*public int getActionOrder(){
		return role.getActionOrder();
	}*/
	public String getAffiliation(){
		return role.getAffiliation();
	}
	public String[] getCategory(){
		return role.getCategory();
	}
	public FlagERS getFlag(String name){
		return role.getFlag(name).getERSClass();
	}
	public int getHp(){
		return role.getHp();
	}
	public boolean getMayGameEnd(){
		return role.getMayGameEnd();
	}
	public String getName(){
		return role.Match.getPlayer(getPlayerNum()).inGameName;
	}
	public int getPlayerNum(){
		return role.getPlayerNum();
	}
	public String getRoleName(){
		return role.getName();
	}
	public int getRoleId(){
		return role.getEID();
	}
	public String getScript(String eventCall){
		return role.getScript(eventCall);
	}
	public RoleERS getTarget1(){
		RoleERS theReturn = null;
		if(role.getTarget1() > 0){
			Role theRole = role.Match.getPlayerRole(role.getTarget1());
			if(theRole != null){theReturn = theRole.getERSClass();}
		}
		return theReturn;
	}
	public RoleERS getTarget2(){
		RoleERS theReturn = null;
		if(role.getTarget2() > 0){
			Role theRole = role.Match.getPlayerRole(role.getTarget2());
			if(theRole != null){theReturn = theRole.getERSClass();}
		}
		return theReturn;
	}
	public TeamERS getTeam(){
		return role.getTeam().getERSClass();
	}
	public boolean getVarBoolean(String varName){
		boolean theReturn = false;
		theReturn = role.getCustomVarBoolean(varName);
		return  theReturn;
	}
	public int getVarInt(String varName){
		int theReturn = 0;
		theReturn = role.getCustomVarInt(varName);
		return  theReturn;
	}
	public String getVarString(String varName){
		String theReturn = "";
		theReturn = role.getCustomVarString(varName);
		return  theReturn;
	}
	public boolean getVictory(){
		return role.getVictory();
	}
	public boolean hasFlag(String name){
		return role.hasFlag(name);
	}
	public boolean isAlive(){
		return role.isAlive();
	}
	public void removeFlag(String name){
		role.removeFlag(name);
	}
	/*public void setActionOrder(int order){
		role.setActionOrder(order);
	}*/
	public void setMayGameEnd(boolean end){
		role.setMayGameEnd(end);
	}
	public void setHp(int hpValue){
		role.setHp(hpValue);
	}
	public void setScript(String eventCall, String script){
		role.setScript(eventCall, script);
	}
	public void setTarget1(RoleERS target){
		if(target != null){role.setTarget1(target.getPlayerNum());}
		else{role.setTarget1(0);}
	}
	public void setTarget2(RoleERS target){
		if(target != null){role.setTarget2(target.getPlayerNum());}
		else{role.setTarget2(0);}
	}
	public void setVarBoolean(String varName,boolean value){
		role.setCustomVarBoolean(varName, value);
	}
	public void setVarInt(String varName,int value){
		role.setCustomVarInt(varName, value);
	}
	public void setVarString(String varName,String value){
		role.setCustomVarString(varName, value);
	}
	public void setVictory(boolean win){
		role.setVictory(win);
	}
	public void text(String msg){
		role.Match.sendToPlayerNum(getPlayerNum(), msg);
	}
	public void visit(RoleERS player){
		Role theRole = role.Match.getPlayerRoleWithSwitch(player.getPlayerNum());
		if(role != null){role.visited(theRole);}
	}
}
