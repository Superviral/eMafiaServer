/* eMafiaServer - LoginHandler.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;

import java.util.HashMap;

import com.inverseinnovations.eMafiaServer.*;
import com.inverseinnovations.eMafiaServer.includes.CmdCompile;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character;
import com.inverseinnovations.eMafiaServer.includes.classes.Server.*;

/**
 * Class provides a means of moving a recently connected client between 'states'<br>
 * This class should lead to atleast 4 results:<br>
 * -Creditails verified and logged in<br>
 * -Creditails rejected and asked to verify again<br>
 * -Creation of new account<br>
 * -Verifying a new account
 */
public class LoginHandler {
	public static void parse(Base Base, SocketClient cObj, String command){
		switch(cObj.loginState){
			case 1:
				newAccount(Base, cObj, command);
				break;
			case 2:
				login(Base, cObj, command);
				break;
			case 3:
				verify(Base, cObj, command);
				break;
		}
	}
	private static void newAccount(Base Base, SocketClient cObj, String command){
		String temp = "";
		switch(cObj.loginSubState){
		case 1://either clicked back or entered name
			if(command.equals("<-")){//if they hit back button
				cObj.loginState = 2;//return to login
				cObj.loginSubState = 1;//return to login
				cObj.send(CmdCompile.closeLayer("register"));
				cObj.send(CmdCompile.loginPrompt());
			}
			else if(command.equals("ver")){//if they hit verify  button
				cObj.loginState = 3;//goto verification
				cObj.loginSubState = 1;//goto verification
				cObj.send(CmdCompile.closeLayer("register"));
				cObj.send(CmdCompile.verifyPrompt());
			}
			else if(command.length() <= 24 && command.length() >= 2){//if they enter a name between 4-18 chars
				int doExist = Base.MySql.checkUsername(command);
				if(doExist==0){//if name dont exist
					cObj.loginSubState = 2;//await password
					cObj.usernameBit = command;//save the username
					cObj.send(CmdCompile.request_password());//request password
				}
				else if(doExist == 2){//if name is registered
					//closes register window, opens login, then tells error
					cObj.send(CmdCompile.closeLayer("register"));
					cObj.send(CmdCompile.loginPrompt());
					cObj.send(CmdCompile.genericPopup("Unfortunately, the forum account you entered, already has an account."));
					cObj.loginState = 2;//return to login
					cObj.loginSubState = 1;//return to login
				}
				else if(doExist == 1){//if name is awaiting verification
					//closes register window, opens login, then tells error
					cObj.send(CmdCompile.closeLayer("register"));
					cObj.send(CmdCompile.verifyPrompt());
					cObj.send(CmdCompile.genericPopup("The forum account you entered is still awaiting validation. Please check your <a href=\"http://www.sc2mafia.com/forum/private.php\">SC2Mafia Inbox</a>."));
					cObj.loginState = 3;//return to verify
					cObj.loginSubState = 1;//return to verify
				}
				else{//or 3?
					//closes register window, opens login, then tells error
					cObj.send(CmdCompile.closeLayer("register"));
					cObj.send(CmdCompile.loginPrompt());
					cObj.send(CmdCompile.genericPopup("<html><center>The forum account you entered has opted-out of partaking in eMafia.<br>Please contact <a href=\"http://www.sc2mafia.com/forum/member.php/3359-Apocist\">Apocist</a> or any other eMafia admin if you wish to change your status.</center></html>"));
					cObj.loginState = 2;//return to login
					cObj.loginSubState = 1;//return to login
				}
			}
			else{//is less or more than 2-24 chars
				cObj.send(CmdCompile.closeLayer("register"));
				cObj.send(CmdCompile.loginPrompt());
				cObj.send(CmdCompile.genericPopup("Forum accounts only have between 2-24 characters.."));
				cObj.loginState = 2;//return to login
				cObj.loginSubState = 1;//return to login
			}
			break;
		case 2://received password //should be md5
			if(command.length() <= 35 && command.length() >= 5){//if they enter a password between 5 chars and 35 chars
				cObj.send(CmdCompile.closeLayer("register"));
				if((temp = Base.MySql.createAccount(cObj.usernameBit, command)).equals("pm_messagesent")){//Create account...if msg sent to forum
					cObj.send(CmdCompile.verifyPrompt());
					cObj.loginState = 3;//return to verify
					cObj.loginSubState = 1;//return to verify
					cObj.send(CmdCompile.genericPopup("<html><center>A verification Private Message was sent to your SC2Mafia Forum Account.<br>Follow the instructions within 24 hours to activate your account and play!<br><a href=\"http://www.sc2mafia.com/forum/private.php\">SC2Mafia Inbox</a></center></html>"));
				}
				else{
					cObj.send(CmdCompile.registerPrompt());
					cObj.loginSubState = 1;//return to register
					cObj.loginState = 1;//going to register
					if(temp.equals("pmrecipturnedoff")){
						cObj.send(CmdCompile.genericPopup("<html><center>The forum account you entered is not accepting PMs!<br>Be sure to first enable PM capabilities at <a href=\"http://www.SC2Mafia.com\">www.SC2Mafia.com</a> first, then check back in here.</center></html>"));
					}
					else if(temp.equals("pmrecipientsnotfound")){//SC2Mafia account doesnt exist
						cObj.send(CmdCompile.genericPopup("<html><center>The forum account you entered does not exist!<br>Be sure to first register at <a href=\"http://www.SC2Mafia.com\">www.SC2Mafia.com</a> first, then check back in here.</center></html>"));
					}
					else{
						cObj.send(CmdCompile.genericPopup("<html><center>Unable to register your account for reason: \""+temp+"\"<br>Please report this occurence to Apocist so he may correctly this issue.</center></html>"));
					}

				}
				temp = "";
			}
			else{
				cObj.send(CmdCompile.closeLayer("register"));
				cObj.send(CmdCompile.registerPrompt());
				cObj.send(CmdCompile.genericPopup("You need a password between 5-35 chars.(I honestly don't even know how you got here you hacker...)"));
				cObj.loginState = 1;//return to new account
				cObj.loginSubState = 1;//return to new account
			}
			break;
		}
	}
	private static void login(Base Base, SocketClient cObj, String command){
		switch(cObj.loginSubState){
		case 1://player inputed username or new
			if(command.equalsIgnoreCase("new")){
				cObj.loginSubState = 1;//login state waiting for username for register
				cObj.loginState = 1;//going to register
				cObj.send(CmdCompile.closeLayer("login"));
				cObj.send(CmdCompile.registerPrompt());
			}
			else{
				cObj.usernameBit = command;//username saved
				cObj.send(CmdCompile.request_password());//ask for password
				cObj.loginSubState = 2;//login state waiting for password
			}
			break;
		case 2://player inputted password
			cObj.pass1bit = command;//password saved(should already be in MD5 format)

			HashMap<String,Object> data = Base.MySql.connectUserPass(cObj.usernameBit, cObj.pass1bit);
			if((boolean)data.get("success") == true){
				Base.MySql.updateForumData((String) data.get("username"));//update database with any possible new info
				boolean charaFound = false;
				for(Character charList : Base.Game.getCharacters().values()){
					if(charList.getAccountId() == (Integer)data.get("accountid")){
						//character exists
						charaFound = true;
						Character chara = Base.Game.getCharacter(charList.getEID());

						//close the old client if exist
						Integer oldClient;
						if(chara.getConnection() != null){
							oldClient = chara.getConnection().getClientEID();
							chara.getConnection().close();
						}

						//setup chara for game
						chara.setConnection(cObj.getClientEID());
						cObj.setCharEID(chara.getEID());
						cObj.setLastTime();

						//initize the client
						cObj.send(CmdCompile.closeLayer("login"));//close login
						//TODO TEST THIS: tell client exactly where we left off at
						cObj.send(CmdCompile.setCharacterId(chara.getEID()));
						if(chara.getInGame()){//if in a match
							if(chara.getMatch().getPhaseMain() == 0){//if in setup
								cObj.send(CmdCompile.enterMatch());//enter the match setup screen
								cObj.send(CmdCompile.timerStart(Math.round(chara.getMatch().getAdvancePhaseTimerLeft() / 1000)));//get the timer
							}
							else{//game is in session still
								cObj.send(CmdCompile.matchStart());//enter the game screen
								cObj.send(CmdCompile.timerStart(Math.round(chara.getMatch().getAdvancePhaseTimerLeft() / 1000)));//get the timer
							}
						}
						else{//in lobby..just refresh the lobby screen
							cObj.send(CmdCompile.enterLobby()); //enter lobby
						}
						Base.Console.info("Relogging "+chara.getName()+" (eID "+chara.getEID()+") in from Client "+cObj.getClientEID()+" kicking Client ");
						break;
					}
				}
				if(!charaFound){
					Character chara = new Character(Base.Game, (String)data.get("username"), (int)data.get("usergroup"));

					//setup chara for game
					chara.setAccountID((Integer)data.get("accountid"));
					chara.setAvatar((String)data.get("avatar"));
					chara.setConnection(cObj.getClientEID());
					cObj.setCharEID(chara.getEID());
					cObj.setLastTime();
					chara.getLobby().addCharacter(chara);//add to lobby(the defualt lobby should already be applied, just need to give to lobby)

					//initize the client
					cObj.send(CmdCompile.closeLayer("login"));//close login
					cObj.send(CmdCompile.setCharacterId(chara.getEID()));
					cObj.send(CmdCompile.enterLobby()); //enter lobby
					Base.Console.info("Logging "+chara.getName()+" (eID "+chara.getEID()+") in from Client "+cObj.getClientEID());
				}
			}
			else{
				//TODO need to limit number of fail login attempts
				Base.Console.fine("Client "+cObj.getClientEID()+" entered incorrect password for user '"+cObj.usernameBit+"'");
				cObj.send(CmdCompile.closeLayer("login"));//close login
				cObj.send(CmdCompile.loginPrompt());//Login Prompt(asking for username)
				cObj.send(CmdCompile.genericPopup("Incorrect username and/or password"));//error popup
				cObj.loginSubState = 1;//login state waiting for username
			}
			break;
		}
	}
	private static void verify(Base Base, SocketClient cObj, String command){
		switch(cObj.loginSubState){
		case 1://either clicked back or entered name
			if(command.equals("<-")){//if they hit back button
				cObj.loginState = 1;//return to new account
				cObj.loginSubState = 1;//return to new account start
				cObj.send(CmdCompile.closeLayer("verify"));
				cObj.send(CmdCompile.registerPrompt());
			}
			else if(command.length() <= 24 && command.length() >= 2){//if they enter a name between 2-24 chars
				int doExist = Base.MySql.checkUsername(command);
				if(doExist==0){//if name dont exist
					cObj.send(CmdCompile.closeLayer("verify"));
					cObj.send(CmdCompile.registerPrompt());
					cObj.send(CmdCompile.genericPopup("Unfortunately, the username you entered has not registered yet or the registration has expired."));
					cObj.loginState = 1;//return to register
					cObj.loginSubState = 1;//return to register
				}
				else if(doExist == 2){//if name is registered
					cObj.send(CmdCompile.closeLayer("verify"));
					cObj.send(CmdCompile.loginPrompt());
					cObj.send(CmdCompile.genericPopup("The username you entered already has an account."));
					cObj.loginState = 2;//return to login
					cObj.loginSubState = 1;//return to login
				}
				else if(doExist == 3){//or 3?
					cObj.send(CmdCompile.closeLayer("verify"));
					cObj.send(CmdCompile.loginPrompt());
					cObj.send(CmdCompile.genericPopup("<html><center>The forum account you entered has opted-out of partaking in eMafia.<br>Please contact <a href=\"http://www.sc2mafia.com/forum/member.php/3359-Apocist\">Apocist</a> or any other eMafia admin if you wish to change your status.</center></html>"));
					cObj.loginState = 2;//return to login
					cObj.loginSubState = 1;//return to login
				}
				else if(doExist == 1){//if name is awaiting verification
					cObj.loginSubState = 2;//await password
					cObj.usernameBit = command;//save the username
					cObj.send(CmdCompile.request_password());//request password
				}
			}
			else{//is less or more than 2-24 chars
				cObj.send(CmdCompile.closeLayer("verify"));
				cObj.send(CmdCompile.loginPrompt());
				cObj.send(CmdCompile.genericPopup("Forum accounts only have between 2-24 characters.."));
				cObj.loginState = 2;//return to login
				cObj.loginSubState = 1;//return to login
			}
			break;
		case 2://recieved password, will check later
			cObj.loginSubState = 3;//await verify code
			cObj.pass1bit = command;//password saved(should already be in MD5 format)
			cObj.send(CmdCompile.request_verify());//request verification code
			break;
		case 3://recieved verification code
			if(Base.MySql.verifyAccount(cObj.usernameBit, cObj.pass1bit, command)){
				cObj.send(CmdCompile.closeLayer("verify"));
				cObj.send(CmdCompile.loginPrompt());
				cObj.send(CmdCompile.genericPopup("Welcome "+cObj.usernameBit+", you are now registered. Feel free to login."));
				cObj.loginState = 2;//return to login
				cObj.loginSubState = 1;//return to login
			}
			else{
				cObj.send(CmdCompile.closeLayer("verify"));
				cObj.send(CmdCompile.verifyPrompt());
				cObj.send(CmdCompile.genericPopup("The token you entered is invalid, please recheck your PM."));
				cObj.loginState = 3;//return to verify
				cObj.loginSubState = 1;//return to verify
			}
			break;
		}
	}
}
