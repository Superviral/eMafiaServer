/* eMafiaServer - SocketClient.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.inverseinnovations.eMafiaServer.includes.CmdCompile;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Character;

public class SocketClient extends Thread{
	//Beginning of reserved variables//
	public SocketServer Server;//reference
	private String ipAddress;
	private int clientEID = 0;
	private Socket socket;
	private boolean RUNNING = true;
	private boolean ONLINE = false;
	//private BufferedReader in;
	private DataInputStream in;
	private DataOutputStream out;
	//Login scripts
	public int loginState = 2;//1 = new. 2 = user/password
	public int loginSubState = 1;//used for Loginstate(username,password,new) / holds the write buffer
	public String usernameBit;//used for $temp_name & $new_name in login / defines what type of item writing buffer is saving to
	public String pass1bit;//used for pass1 in login
	public String pass2bit;//used for pass2 in login
	public int Ebit;//new account state(in login/new account)
	/////Game Data below//////
	private int characterEID;
	private long lastInput = 0;//detrimine idleness

	/**
	 * Creates Client to allow sending and retrieving infomation
	 * @param server
	 * @param id
	 * @param socket
	 */
	public SocketClient(SocketServer server, int id, Socket socket){
		this.clientEID = id;
		this.socket = socket;
		this.Server = server;
		this.setName("Client "+id+" Socket "+socket);
		this.setDaemon(true);
	}
	/**
	 * Sets the Client to continue reading
	 */
	public void run(){
		try{
			ipAddress = socket.getInetAddress().getHostAddress();//get ip
			//get socket writing and reading streams
			//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			//out = new PrintStream(socket.getOutputStream());

			//Send welcome message to client
			Server.Base.Console.info("Client "+this.clientEID+" connected from "+ipAddress);
			Server.onConnect(this);

			//Now start reading input from client
			while(RUNNING){
				//if((line = in.readLine()) != null){
				while(in.readUnsignedByte() == 0) {// gets 1 byte starter
					try{
						//Server.onRead(this,line);
						int cmdSize;
						byte[] commandByte = null;
						String command;
						int dataSize;
						byte[] data = null;
						cmdSize = in.readUnsignedByte();// gets 1 byte size of command
						if (cmdSize > 0) {
							commandByte = new byte[cmdSize];
						}
						//Server.Base.Console.debug("cmdSize = "+cmdSize);
						in.readFully(commandByte, 0, cmdSize);// gets command
						if(commandByte != null){
							command = new String(commandByte,"ISO-8859-1");//convert byte to String
						}
						else{
							command = "";
						}

						dataSize = in.readUnsignedShort();	// gets 2 byte size of data(may
														// increase size later..only
														// supports 65.5 kbs of data
														// transfer per command)
						if (dataSize > 0) {
							data = new byte[dataSize];
						}
						in.readFully(data, 0, dataSize);// gets data

						if (in.readUnsignedByte() == 255) {// gets 1 byte ender, if data
							// too big, this wont be
							// reached then theres error
							Server.onRead(this,command,data);
						}

					}
					catch(Exception e){
						Server.Base.Console.warning("Exception on SocketClient during onRead: client "+clientEID);
						Server.Base.Console.printStackTrace(e);
					}
				}
				/*else{
					break;
				}*/
			}
			this.offline();
			this.close();
		}
		catch (IOException e){
			//Server.Base.Console.warning("IOException on SocketClient(run) "+clientEID+"(Client-side closing bad?)");
			this.offline();
			this.close();
			//Server.Base.Console.printStackTrace(e);
		}
	}
	/**
	 * Returns the Client ID
	 */
	public int getClientEID(){
		return this.clientEID;
	}
	/**
	 * Set the Character ID in which the client is attached to
	 */
	public void setCharEID(int eid){
		this.characterEID = eid;
		this.ONLINE = true;
	}
	/**
	 * Returns the Character ID in which the client is attached to
	 */
	public int getCharEID(){
		return this.characterEID;
	}
	/**
	 * Returns the IP address of the Client
	 */
	public String getIPAddress(){
		return this.ipAddress;
	}
	/** Gets last time command submitted(Used to detrimine idlers) */
	public long getLastTime(){
		return this.lastInput;
	}
	/** Sets current time as last time command submitted(Used to detrimine idlers) */
	public void setLastTime(){
		this.lastInput = System.currentTimeMillis();
	}
	public void input(String input){//test
		//Server.testOut(id, input);
	}
	/**
	 * Send DataPacket to client
	 */
	public void send(byte[] output){
		try {
			if(socket.isConnected()){
				out.write(output);
			}
		}
		catch (IOException e) {
			//e.printStackTrace();
			Server.Base.Console.warning(e.getMessage()+" on SocketClient(send) "+clientEID);
		}
	}
	/**
	 * Sets Client as offline and removes Character from Game
	 */
	public void offline(){
		if(ONLINE){
			if(this.getCharEID()>0){//FIXME Character leave properly: more work on the closing process
				Character me = Server.Base.Game.getCharacter(getCharEID());
				if(me!=null){
					if(me.getInGame()){
						if(me.getMatch().getPhaseMain() == 0 || me.getMatch().getPhaseMain() == 3){//if in setup or endgame
							me.leave();
						}
						else{
							//XXX Character leave properly: if match is actually playing...

						}
					}
					else{
						me.getLobby().removeCharacter(me);
					}
					Server.Base.Console.fine(me.getName()+"...quit!");
					Server.Base.Game.removeCharacter(me);//XXX should not remove if character is left in the playing match
				}
			}
		}
		ONLINE = false;
		RUNNING = false;
		//this.close();
	}
	/**
	 * Closes the Client connection
	 */
	public void close(){
		try {
			send(CmdCompile.disconnect());
		}catch(Exception e){}
		try {
			Server.Base.Console.fine("Client "+clientEID+" disconnected.");
			socket.close();
			Server.removeClient(this.clientEID);

		}
		catch (IOException e) {
			Server.Base.Console.warning("IOException on SocketClient attempting to close socket");
			Server.Base.Console.printStackTrace(e);
		}
	}
}
