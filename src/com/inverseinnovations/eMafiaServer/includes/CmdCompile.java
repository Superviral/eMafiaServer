/* eMafiaServer - CmdCompile.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.inverseinnovations.sharedObjects.*;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Match;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Role;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Match.Players;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Match.RolesOrig;


/**
 * Class handler for returning bytes read as DataPackets in the eMafiaClient
 */
public class CmdCompile {
	/*List of layers currently in client:
	 * lobby
	 * matchSetup
	 * matchInplay
	 * login
	 * register
	 * nameSelection
	 * popup
	 */

	//-Connection-// 0 - 9
	public static byte[] connected(int version){//0
		return cmdCompiler(0,version);
	}
	public static byte[] request_password(){//1
		return cmdCompiler(1,"FINAL");
	}
	public static byte[] request_verify(){//2
		return cmdCompiler(2,"");
	}
	public static byte[] loginPrompt(){//3 //pops up login screnn(also asks for username)
		return cmdCompiler(3,"");
	}
	public static byte[] registerPrompt(){//4 //pops up register screen
		return cmdCompiler(4,"");
	}
	public static byte[] verifyPrompt(){//5 //pops up verify screen
		return cmdCompiler(5,"");
	}
	public static byte[] disconnect(){//6
		return cmdCompiler(6,"");
	}

	//-Entering/Exiting screens-// 50 - 69
	public static byte[] closeLayer(String layer){//50 closes a layer(match/lobby/ect)
		return cmdCompiler(50,layer);
	}
	public static byte[] enterLobby(){//51 open Lobby
		return cmdCompiler(51,"");
	}
	public static byte[] enterMatch(){//52 open matchSetup,client will call look
		return cmdCompiler(52,"");
	}
	public static byte[] matchStart(){//53 open matchInplay
		return cmdCompiler(53,"");
	}

	//-Chat functions-// 70 - 99
	public static byte[] chatScreen(String message){//70
		return cmdCompiler(70,message);
	}
	/*public static byte[] chatScreen(Character chara, String message){//71
		//hexcolor | name | msg
		//return Constants.CMDSTARTER+"[202]<b><font color=\""+chara.Game.getUsergroup(chara.getUsergroup()).getHexcolor()+"\">"+chara.getName()+"</font></b>: "+message+Constants.CMDENDER;
		return cmdCompiler(71,chara.getUsergroup().getHexcolor()+Constants.CMDVARDIVIDER+chara.getName()+Constants.CMDVARDIVIDER+message);

	}*/
	public static byte[] chatScreen(Players player, String message) {//71
		return cmdCompiler(71,player.getHexcolor()+Constants.CMDVARDIVIDER+player.getName()+Constants.CMDVARDIVIDER+message);
	}
	public static byte[] chatScreen2(Character chara, String message){//72
		//chara id | msg
		return cmdCompiler(72,chara.getEID()+Constants.CMDVARDIVIDER+message);

	}
	public static byte[] refreshPList(List<Character> list){//75
		//Array of:
		//EID | Name | HexColor | AvatarUrl
		String string = "";
		/*for(com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character chara : list){
			if (string != ""){string += Constants.CMDVARDIVIDER;}
			//string += chara.getEID()+Constants.CMDVARSUBDIVIDER+chara.getName()+Constants.CMDVARSUBDIVIDER+chara.Game.getUsergroup(chara.getUsergroup()).getHexcolor();
			string += chara.getEID()+Constants.CMDVARSUBDIVIDER+chara.getName()+Constants.CMDVARSUBDIVIDER+chara.getUsergroup().getHexcolor()+Constants.CMDVARSUBDIVIDER+chara.getAvatar();
		}*/
		for(com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character chara : list){
			if (string != ""){string += Constants.CMDVARDIVIDER;}
			string += chara.getEID();
		}
		return cmdCompiler(75,string);
	}
	public static byte[] playerEnter(Character chara){//76 player enter chat
		//EID | Name | Hexcolor | AvatarUrl
		//return cmdCompiler(76,chara.getEID()+Constants.CMDVARDIVIDER+chara.getName()+Constants.CMDVARDIVIDER+chara.getUsergroup().getHexcolor()+Constants.CMDVARDIVIDER+chara.getAvatar());
		return cmdCompiler(76,chara.getEID());
	}
	public static byte[] playerLeave(Character chara){//77 player leave chat
		//EID | Name
		//return Constants.CMDSTARTER+"[205]"+pos+Constants.CMDVARDIVIDER+chara.getEID()+Constants.CMDVARDIVIDER+chara.getName()+Constants.CMDENDER;
		return cmdCompiler(77,chara.getEID());
	}

	//-Lobby functions-// 100 - 109
	public static byte[] refreshMList(Map<Integer, Match> matchs){//100
		//Array of:
		//EID | Name | Current Players | Max Players
		String string = "";
		for(Match match : matchs.values()){
			if (string != ""){string += Constants.CMDVARDIVIDER;}
			string += match.getEID()+Constants.CMDVARSUBDIVIDER+match.getName()+Constants.CMDVARSUBDIVIDER+match.getNumChars()+Constants.CMDVARSUBDIVIDER+match.getSetting("max_chars");
		}
		return cmdCompiler(100,string);
	}

	//-Match functions-// 200+
	public static byte[] setTargetables(Role role){//200 sets the targetable players
		//Returns array of 4:
		//0 = none
		//-1 = everyone
		//-2 = everyone except self
		//-3 = only self
		//later array of numbers above 0< will be certain players..suc as teammates
		int tN1 = role.targetablesNight1*-1;
		int tN2 = role.targetablesNight2*-1;
		int tD1 = role.targetablesDay1*-1;
		int tD2 = role.targetablesDay2*-1;
		return cmdCompiler(200,tN1+Constants.CMDVARDIVIDER+tN2+Constants.CMDVARDIVIDER+tD1+Constants.CMDVARDIVIDER+tD2);
	}
	public static byte[] setTimeOfDay(int phase){//201 set which phase it is
		return cmdCompiler(201,phase);
	}
	public static byte[] setPlayerNum(int playerNum){//202 sets the playerNum
		return cmdCompiler(202,playerNum);
	}
	public static byte[] refreshAliveList(List<Players> list){//203 updates living players
		//Array of:
		//Num | Name | HexColor
		String string = "";
		for(Players play : list){
			//System.out.println("got in the players iterator");
			if(play != null){
				if (string != ""){string += Constants.CMDVARDIVIDER;}
				string += play.getPlayerNumber()+Constants.CMDVARSUBDIVIDER+play.getName()+Constants.CMDVARSUBDIVIDER+play.getHexcolor();
			}
		}
		return cmdCompiler(203,string);
	}
	public static byte[] refreshDeadList(Match match, List<Players> list){//204 updates dead players
		//Array of:
		//Num | Name | HexColor | Death
		String string = "";
		for(Players play : list){
			if(play != null){
				match.Game.Base.Console.debug("found "+play.getName());
				if (string != ""){string += Constants.CMDVARDIVIDER;}
				string += play.getPlayerNumber()+Constants.CMDVARSUBDIVIDER+play.getName()+Constants.CMDVARSUBDIVIDER+play.getHexcolor()+Constants.CMDVARSUBDIVIDER+match.getRole(play.getRoleNumber()).deathTypes.get(0);
			}
		}
		return cmdCompiler(204,string);
	}
	public static byte[] timerStart(int secs){//205
		return cmdCompiler(205,secs);
	}
	public static byte[] timerStop(){//205
		return cmdCompiler(205,0);
	}
	public static byte[] nameSelectPrompt(){//206 ask for inGameName
		return cmdCompiler(206,"");
	}
	public static byte[] matchSettings(Match m){//207 updates match settings
		String name = null;
		try{
			name = m.getCharacter(m.getSetting("host_id")).getName();
		}
		catch(NullPointerException e){}
		String string =
				m.getEID()+""+
				m.getName()+""+
				m.getSetting("host_id")+""+
				name+""+
				m.getNumChars()+""+
				m.getSetting("max_chars")+""+
				m.getSetting("start_game_at")+""+
				m.getSetting("discussion")+""+
				m.getSetting("day_length")+""+
				m.getSetting("night_length")+""+
				m.getSetting("discuss_length")+""+
				m.getSetting("trial_length")+""+
				m.getSetting("day_type")+""+
				m.getSetting("pm_allowed")+""+
				m.getSetting("trial_pause_day")+""+
				m.getSetting("trial_defense")+""+
				m.getSetting("choose_names")+""+
				m.getSetting("last_will")+""+
				m.getSetting("trial_pause_day")+""+
				m.getSetting("description");
		return cmdCompiler(207,string);
	}
	public static byte[] voteCount(int playerNum, int votes){//208 updates the votes counter of player
		String string = "";
		if(votes > 0){
			string += votes;
		}
		return cmdCompiler(208,playerNum+Constants.CMDVARDIVIDER+string);
	}
	public static byte[] voteCountClear(){//209 removes all votes from players
		return cmdCompiler(209,"");
	}


	//-Other-// 800+
	public static byte[] roleSearchResults(ArrayList<Role> roles){//800
		//Array of:
		//EID | Name | Aff | HexColor(faked atm)
		String string = "";
		for(Role role : roles){
			if (string != ""){string += Constants.CMDVARDIVIDER;}
			string += role.getEID()+Constants.CMDVARSUBDIVIDER+role.getName()+Constants.CMDVARSUBDIVIDER+role.getAffiliation()+Constants.CMDVARSUBDIVIDER+"B5B2B6";
		}
		return cmdCompiler(800,string);
	}
	public static byte[] rolesPossible(ArrayList<Role> roles){//801
		//Array of:
		//EID | Name | Aff | HexColor(faked atm)
		String string = "";
		for(Role role : roles){
			if (string != ""){string += Constants.CMDVARDIVIDER;}
			string += role.getEID()+Constants.CMDVARSUBDIVIDER+role.getName()+Constants.CMDVARSUBDIVIDER+role.getAffiliation()+Constants.CMDVARSUBDIVIDER+"B5B2B6";
		}
		return cmdCompiler(801,string);
	}
	public static byte[] roleSetup(ArrayList<RolesOrig> roles){//802
		//Array of:
		//EID | Name | Aff | HexColor(faked atm)
		String string = "";
		String name = "";
		for(RolesOrig role : roles){
			if (string != ""){string += Constants.CMDVARDIVIDER;}
			if(role.eID == 0){name = role.affiliation+" "+role.category[0];}else{name = role.roleName;}
			string += role.eID+Constants.CMDVARSUBDIVIDER+name+Constants.CMDVARSUBDIVIDER+role.affiliation+Constants.CMDVARSUBDIVIDER+"B5B2B6";
		}
		return cmdCompiler(802,string);
	}
	public static byte[] setCharacterId(int id){//803 sets the playerNum
		return cmdCompiler(803,id);
	}

	public static byte[] orderOfOps(ArrayList<String> actions){//805 Order of Operations
		//Array of:
		//EID | Name | Aff | HexColor(faked atm)
		String string = "";
		for(String action : actions){
			if (string != ""){string += Constants.CMDVARDIVIDER;}
			string += action;
		}
		return cmdCompiler(805,string);
	}

	//-Popups/prompts-//

	public static byte[] genericPlainPopup(String message){//998
		return cmdCompiler(998,message);
	}
	public static byte[] genericPopup(String message){//999
		return cmdCompiler(999,message);
	}
	public static byte[] roleView(Role role){//1001
		RoleData data = new RoleData();
		data.id = role.getEID();
		data.version = role.getVersion();
		data.name = role.getName();
		data.affiliation = role.getAffiliation();
		data.category = role.getCategory();
		data.actionCat = role.getActionCat();
		data.targetablesNight1 = role.targetablesNight1;
		data.targetablesNight2 = role.targetablesNight2;
		data.targetablesDay1 = role.targetablesDay1;
		data.targetablesDay2 = role.targetablesDay2;
		data.onTeam = role.getOnTeam();
		data.teamName = role.getTeamName();
		data.teamWin = role.getTeamWin();
		data.visibleTeam = role.getTeamVisible();
		data.chatAtNight = role.getChatAtNight();
		data.ersScript = role.getScriptMap();
		return cmdCompiler(1001,data);
	}
	public static byte[] charaUpdate(Character chara){//1002
		//EID | Name | Hexcolor | AvatarUrl
		CharacterData c = new CharacterData();
		c.eid = chara.getEID();
		c.name = chara.getName();
		c.hexcolor = chara.getUsergroup().getHexcolor();
		c.avatarUrl = chara.getAvatar();
		return cmdCompiler(1002,c);
	}

	//-Other-//

	private static byte[] cmdCompiler(int control,int num){
		return cmdCompiler(control,""+num);
	}
	private static byte[] cmdCompiler(int control,String string){
		//control
		byte[] controlBytes = new byte[2];
		int i;
		for (i=1; i>=0; i--) {
			controlBytes[i] = (byte) (control & 0xff);
			control >>= 8;
		}
		//data + size
		byte[] dataBytes = null;
		int dataSize = 0;
		if(string != null){if(string != ""){
			dataBytes = string.getBytes(Charset.forName("ISO-8859-1"));
			dataSize = dataBytes.length;
		}}
		byte[] sizeBytes = new byte[2];
		for (i=1; i>=0; i--) {
			sizeBytes[i] = (byte) (dataSize & 0xff);
			dataSize >>= 8;
		}
		//compile
		ByteArrayList array = new ByteArrayList(new byte[]{0x00});
		array.append(controlBytes);
		array.append(sizeBytes);
		if(string != null){if(string != ""){array.append(dataBytes);}}
		array.append(new byte[]{(byte) 0xff});

		return array.toByteArray();
	}
	private static byte[] cmdCompiler(int control,Object object){
		//control
		byte[] controlBytes = new byte[2];
		int i;
		for (i=1; i>=0; i--) {
			controlBytes[i] = (byte) (control & 0xff);
			control >>= 8;
		}
		//data + size
		byte[] dataBytes = null;
		int dataSize = 0;
		if(object != null){
			dataBytes = objectToByte(object);
			dataSize = dataBytes.length;
		}
		byte[] sizeBytes = new byte[2];
		for (i=1; i>=0; i--) {
			sizeBytes[i] = (byte) (dataSize & 0xff);
			dataSize >>= 8;
		}
		//compile
		ByteArrayList array = new ByteArrayList(new byte[]{0x00});
		array.append(controlBytes);
		array.append(sizeBytes);
		if(object != null){array.append(dataBytes);}
		array.append(new byte[]{(byte) 0xff});

		return array.toByteArray();
	}

	/**
	 * Wrapper class to provide ease of converting Strings, ints, and bytes
	 * into a series of bytes.
	 */
	public static class ByteArrayList extends ArrayList<Byte>{
		private static final long serialVersionUID = 1L;
		/**
		 * Wrapper class to provide ease of converting Strings, ints, and bytes
		 * into a series of bytes.
		 */
		public ByteArrayList(){
			super();
		}
		/**
		 * Wrapper class to provide ease of converting Strings, ints, and bytes
		 * into a series of bytes.
		 */
		public ByteArrayList(byte[] byteArray){
			super();
			for(byte b : byteArray){
				this.add(b);
			}
		}
		/**Appends a series of bytes*/
		public void append(byte[] byteArray){
			for(byte b : byteArray){
				this.add(b);
			}
		}
		/**Appends a ByteArrayList*/
		public void append(ByteArrayList arrayList){
			for(byte b : arrayList){
				this.add(b);
			}
		}
		/**Appends an int*/
		public void append(int i){
			append(BigInteger.valueOf(i).toByteArray());
		}
		/**Appends a String*/
		public void append(String i){
			append(String.valueOf(i).getBytes());
		}
		/**Converts the ByteArrayList into byte[]*/
		public byte[] toByteArray(){
			byte[] byteArray = new byte[this.size()];
			for(int i = 0; i<this.size(); i++){
				byteArray[i] = this.get(i);
			}
			return byteArray;
		}
	}
	/**
	 * Converts an Object to byte[]
	 * @param object Object to Convert to byte[]
	 * @return null on IOException
	 */
	public static byte[] objectToByte(Object object){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] yourBytes = null;
		try {
			try {
				out = new ObjectOutputStream(bos);
				out.writeObject(object);
			}
			catch (IOException e) {
			}
			yourBytes = bos.toByteArray();
		}
		finally{
			try{
				if (out != null){
				out.close();
				}
			}
			catch (IOException ex){
				// ignore close exception
			}
			try{
				bos.close();
			}
			catch(IOException ex){
				// ignore close exception
			}
		}
		return yourBytes;
	}
}
