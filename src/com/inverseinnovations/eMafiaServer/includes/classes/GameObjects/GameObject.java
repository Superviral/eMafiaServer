/* eMafiaServer - GameObject.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.GameObjects;

public class GameObject {
	/**
	 * Object Types: examples are DEFINED
	 * @example
	 * GO_TYPE_ROOM	=> Room
	 * <br>GO_TYPE_CHAR 	=> Player
	 * <br>GO_TYPE_NPC		=> Mobile NPC
	 * <br>TYPE_USERGROUP	=> Usergroup
	 */
	protected int type;
	protected int id; // Entity ID //
	protected String name;

	public GameObject(){
	}
	public GameObject(int id, String name, int type){
		this.id = id;
		this.name = name;
		this.type = type;
	}
	/** Returns EID */
	public int getEID(){
		return this.id;
	}
	/** Change/Set the EID */
	public void setEID(int id){
		this.id = id;
	}
	/** Returns type */
	public int getType(){
		return this.type;
	}
	/** Sets type */
	public void setType(int type){
		this.type = type;
	}
	/** Returns name of object */
	public String getName(){
		return this.name;
	}
	/** Sets name of object
	 * @param String new_name*/
	public void setName(String newn){
		this.name = newn;
	}

}
