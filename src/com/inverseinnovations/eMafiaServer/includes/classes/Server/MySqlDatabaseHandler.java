/* eMafiaServer - MySqlDataBaseHandler.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.Server;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.*;
import java.util.*;

import libraries.BCrypt;

import org.apache.commons.lang3.StringUtils;
import com.inverseinnovations.eMafiaServer.*;
import com.inverseinnovations.eMafiaServer.includes.*;
import com.inverseinnovations.eMafiaServer.includes.classes.*;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.*;

import com.inverseinnovations.VBulletinAPI.VBulletinAPI.Member;
import com.inverseinnovations.VBulletinAPI.Exception.*;
import com.inverseinnovations.sharedObjects.RoleData;

/**Manages database interaction*/
public class MySqlDatabaseHandler extends Thread{
	public Base Base;

	private Connection con = null;
	private PreparedStatement st = null;
	private ResultSet rs = null;
	private boolean connected = false;

	/**
	 * Creates instance for MySQL database references
	 */
	public MySqlDatabaseHandler(Base base){
		this.Base = base;
		this.setName("MySQL");
		this.setDaemon(true);
		this.start();
	}
	public void run(){
		if(this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS)){
			Base.mysqlReady();
		}
	}
	public boolean getConnected(){
		return connected;
	}
	public void setConnected(boolean connect){
		this.connected = connect;
	}
	/**
	 * Initilizes and connects to defined MySQL Database
	 * @param url e.g. jdbc:mysql://localhost:3306/testdb
	 * @param user DB username
	 * @param password DB password
	 */
	private boolean init(String url, String user, String password){
		try {
			Class.forName("com.mysql.jdbc.Driver");//.newInstance();
			con = DriverManager.getConnection(url, user, password);

			if(con != null){
				setConnected(true);
				Base.Console.config("MySQL DB connected to "+url+"");
			}
			else{Base.program_faults++;Base.Console.severe("MySQL DB failed to connect, server will not run correctly!");setConnected(false);}
		}
		catch(SQLException e){
			Base.program_faults++;
			if(e.getSQLState().equals("28000")){
				Base.Console.severe("MySQL DB username/password incorrect, cannot connect. Server will not run correctly!");
				setConnected(false);
			}
			else{
				Base.Console.severe("MySQL DB failed to connect, server will not run correctly!");
				setConnected(false);
				Base.Console.printStackTrace(e);
			}
		}
		catch (Exception e) {
			Base.program_faults++;
			Base.Console.severe("MySQL DB failed to connect, server will not run correctly!");
			setConnected(false);
			Base.Console.printStackTrace(e);

		}
		//finally{
			return this.getConnected();
		//}
	}
	/**
	 * Loads all Usergroups into the game(If usergroups change and are reloaded, there might be some issues)
	 * @param Game Game Class Reference
	 */
	public void loadUsergroups(Game Game){
		try{
			st = con.prepareStatement("SELECT * FROM user_groups");
			rs = st.executeQuery();
			while(rs.next()){
				//Game.addUsergroup(new Usergroup(rs.getInt("id"),rs.getString("name"),rs.getString("hexcolor")));
				new Usergroup(Game, rs.getInt("id"),rs.getString("name"),rs.getString("hexcolor"));
			}
		}
		catch(Exception e){
			Base.program_faults++;
			Base.Console.severe("MySqlDatabaseHanlder.loadUsergroups error");
			Base.Console.printStackTrace(e);
		}
	}
	/**
	 * Returns list of all possible roles(by database id) based on parameters
	 * @param setup
	 * @param aff affilation
	 * @param cat category
	 */
	public Map<Integer, Integer> grabRoleCatList(String setup,String aff,String cat){//TODO error testing needs to be done
		Map<Integer, Integer> list = new LinkedHashMap<Integer, Integer>();
		try {
			st = con.prepareStatement("SELECT id, name FROM roles WHERE setup = ? AND affiliation = ? AND (cat1 = ? OR cat2 = ?)");
			st.setString(1, setup);st.setString(2, aff);st.setString(3, cat);st.setString(4, cat);
			rs = st.executeQuery();
		//if(mysql_num_rows($result)!='0'){ // If match.
				//while($row = mysql_fetch_array($result)){
			int i = 0;
			while(rs.next()){
				list.put(i, rs.getInt("id"));
				i++;
			}
		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase disconnected..attempting connection");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return grabRoleCatList(setup,aff, cat);
			}
			Base.Console.severe("Error retrieving Role Catergory List");Base.Console.printStackTrace(e);
		}
		catch (SQLException e){Base.Console.severe("Error retrieving Role Catergory List");Base.Console.printStackTrace(e);}
		return list;
		//}
		//else{echo "NO RESULTS\n";return FALSE;}
	}
	/**
	 * Fetch Role from Database
	 * @param id database id
	 * @return null upon error or role not found
	 */
	public Role grabRole(int id){
		Role role = null;
		int setup;
		String[] category = new String[2];
		try {
			st = con.prepareStatement("SELECT * FROM roles WHERE id = ?");
			st.setInt(1, id);
			rs = st.executeQuery();
			if(rs.next()){ // If match.
					if(rs.getString("setup").equals("DEFAULT")){
						setup = Constants.TYPE_GAMEOB_ROLE_DEFAULT;
					}
					else{setup = Constants.TYPE_GAMEOB_ROLE_CUSTOM;
					}
					category[0] = rs.getString("cat1");
					category[1] = rs.getString("cat2");

					role = new Role(null, rs.getInt("id"),rs.getString("name"),setup,rs.getString("affiliation"), category);//,category);
					role.setVersion(rs.getInt("version"));
					if(rs.getInt("teamWin") == 1){
						role.setOnTeam(true);
						role.setTeamName(rs.getString("teamName"));
						if(rs.getInt("teamWin") == 1){role.setTeamWin(true);}
						if(rs.getInt("visibleTeam") == 1){role.setTeamVisible(true);}
						if(rs.getInt("chatAtNight") == 1){role.setChatAtNight(true);}
					}

					role.setActionCat(rs.getString("actionCat"));
					if(StringUtils.isNotEmpty(rs.getString("victoryCon"))){role.setScript("victoryCon", rs.getString("victoryCon"));}
					if(StringUtils.isNotEmpty(rs.getString("mayGameEndCon"))){role.setScript("mayGameEndCon", rs.getString("mayGameEndCon"));}
					if(StringUtils.isNotEmpty(rs.getString("onStartup"))){role.setScript("onStartup", rs.getString("onStartup"));}
					if(StringUtils.isNotEmpty(rs.getString("onDayStart"))){role.setScript("onDayStart", rs.getString("onDayStart"));}
					if(StringUtils.isNotEmpty(rs.getString("onDayTargetChoice"))){role.setScript("onDayTargetChoice", rs.getString("onDayTargetChoice"));}
					if(StringUtils.isNotEmpty(rs.getString("onDayEnd"))){role.setScript("onDayEnd", rs.getString("onDayEnd"));}
					if(StringUtils.isNotEmpty(rs.getString("onNightStart"))){role.setScript("onNightStart", rs.getString("onNightStart"));}
					if(StringUtils.isNotEmpty(rs.getString("onNightTargetChoice"))){role.setScript("onNightTargetChoice", rs.getString("onNightTargetChoice"));}
					if(StringUtils.isNotEmpty(rs.getString("onNightEnd"))){role.setScript("onNightEnd", rs.getString("onNightEnd"));}
					if(StringUtils.isNotEmpty(rs.getString("onAttacked"))){role.setScript("onAttacked", rs.getString("onAttacked"));}
					if(StringUtils.isNotEmpty(rs.getString("onVisit"))){role.setScript("onVisit", rs.getString("onVisit"));}
					if(StringUtils.isNotEmpty(rs.getString("onLynch"))){role.setScript("onLynch", rs.getString("onLynch"));}
					if(StringUtils.isNotEmpty(rs.getString("onDeath"))){role.setScript("onDeath", rs.getString("onDeath"));}
					//role.setScript("onRoleBlock?", rs.getString("onRoleBlock?"));
					if(StringUtils.isNotEmpty(rs.getString("customScript"))){
						String[] scripts;
						if(rs.getString("customScript").contains(Constants.CMDVARDIVIDER)){
							scripts = rs.getString("customScript").split(Constants.CMDVARDIVIDER);
						}
						else{
							scripts = new String[]{rs.getString("customScript")};
						}
						String[] eventPlusScript;
						for(String script : scripts){
							if(script.contains(Constants.CMDVARSUBDIVIDER)){
								eventPlusScript = script.split(Constants.CMDVARSUBDIVIDER);
								role.setScript(eventPlusScript[0], eventPlusScript[1]);
							}
						}

					}

					role.targetablesNight1=rs.getInt("targetsN1");
					role.targetablesNight2=rs.getInt("targetsN2");
					role.targetablesDay1=rs.getInt("targetsD1");
					role.targetablesDay2=rs.getInt("targetsD2");
			}
			else{Base.Console.warning("ERROR NO RESULTS for id "+id+"!\n");}
		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase disconnected..attempting connection");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return grabRole(id);
			}
			Base.Console.severe("GrabRole error");Base.Console.printStackTrace(e);
		}
		catch (SQLException e){Base.Console.severe("GrabRole error");Base.Console.printStackTrace(e);}
		return role;
	}
	/**
	 * Fetch RoleForum from Database(Forum Variant)
	 * @param id database id
	 * @return null upon error or role not found
	 */
	public RoleForum grabRoleForum(int id){
		RoleForum role = null;
		int setup;
		String[] category = new String[2];
		try {
			st = con.prepareStatement("SELECT * FROM roles WHERE id = ?");
			st.setInt(1, id);
			rs = st.executeQuery();
			if(rs.next()){ // If match.
					if(rs.getString("setup").equals("DEFAULT")){
						setup = Constants.TYPE_GAMEOB_ROLE_DEFAULT;
					}
					else{setup = Constants.TYPE_GAMEOB_ROLE_CUSTOM;
					}
					category[0] = rs.getString("cat1");
					category[1] = rs.getString("cat2");

					role = new RoleForum(null, rs.getInt("id"),rs.getString("name"),setup,rs.getString("affiliation"), category);//,category);
					role.setVersion(rs.getInt("version"));
					if(rs.getInt("teamWin") == 1){
						role.setOnTeam(true);
						role.setTeamName(rs.getString("teamName"));
						if(rs.getInt("teamWin") == 1){role.setTeamWin(true);}
						if(rs.getInt("visibleTeam") == 1){role.setTeamVisible(true);}
						if(rs.getInt("chatAtNight") == 1){role.setChatAtNight(true);}
					}

					role.setActionCat(rs.getString("actionCat"));
					role.desc = rs.getString("desc");
					role.winCondDesc = rs.getString("winCondDesc");
					if(StringUtils.isNotEmpty(rs.getString("alias"))){
						Map<String, String> aliasMap = new LinkedHashMap<String, String>();
						String alias = rs.getString("alias");
						if(alias.contains("\n")){//if mulitple alias
							String aliass[] = alias.split("\n");
							for(String single:aliass){
								String[] split = single.split("|");
								aliasMap.put(split[0], split[1]);
							}
						}
						else{
							String[] split = alias.split("|");
							aliasMap.put(split[0], split[1]);
						}
						role.setAlias(aliasMap);
					}
					if(StringUtils.isNotEmpty(rs.getString("victoryCon"))){role.setScript("victoryCon", rs.getString("victoryCon"));}
					if(StringUtils.isNotEmpty(rs.getString("mayGameEndCon"))){role.setScript("mayGameEndCon", rs.getString("mayGameEndCon"));}
					if(StringUtils.isNotEmpty(rs.getString("onStartup"))){role.setScript("onStartup", rs.getString("onStartup"));}
					if(StringUtils.isNotEmpty(rs.getString("onDayStart"))){role.setScript("onDayStart", rs.getString("onDayStart"));}
					if(StringUtils.isNotEmpty(rs.getString("onDayTargetChoice"))){role.setScript("onDayTargetChoice", rs.getString("onDayTargetChoice"));}
					if(StringUtils.isNotEmpty(rs.getString("onDayEnd"))){role.setScript("onDayEnd", rs.getString("onDayEnd"));}
					if(StringUtils.isNotEmpty(rs.getString("onNightStart"))){role.setScript("onNightStart", rs.getString("onNightStart"));}
					if(StringUtils.isNotEmpty(rs.getString("onNightTargetChoice"))){role.setScript("onNightTargetChoice", rs.getString("onNightTargetChoice"));}
					if(StringUtils.isNotEmpty(rs.getString("onNightEnd"))){role.setScript("onNightEnd", rs.getString("onNightEnd"));}
					if(StringUtils.isNotEmpty(rs.getString("onAttacked"))){role.setScript("onAttacked", rs.getString("onAttacked"));}
					if(StringUtils.isNotEmpty(rs.getString("onVisit"))){role.setScript("onVisit", rs.getString("onVisit"));}
					if(StringUtils.isNotEmpty(rs.getString("onLynch"))){role.setScript("onLynch", rs.getString("onLynch"));}
					if(StringUtils.isNotEmpty(rs.getString("onDeath"))){role.setScript("onDeath", rs.getString("onDeath"));}
					//role.setScript("onRoleBlock?", rs.getString("onRoleBlock?"));
					if(StringUtils.isNotEmpty(rs.getString("customScript"))){
						String[] scripts;
						if(rs.getString("customScript").contains(Constants.CMDVARDIVIDER)){
							scripts = rs.getString("customScript").split(Constants.CMDVARDIVIDER);
						}
						else{
							scripts = new String[]{rs.getString("customScript")};
						}
						String[] eventPlusScript;
						for(String script : scripts){
							if(script.contains(Constants.CMDVARSUBDIVIDER)){
								eventPlusScript = script.split(Constants.CMDVARSUBDIVIDER);
								role.setScript(eventPlusScript[0], eventPlusScript[1]);
							}
						}

					}

					role.targetablesNight1=rs.getInt("targetsN1");
					role.targetablesNight2=rs.getInt("targetsN2");
					role.targetablesDay1=rs.getInt("targetsD1");
					role.targetablesDay2=rs.getInt("targetsD2");
			}
			else{Base.Console.warning("ERROR NO RESULTS for id "+id+"!\n");}
		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase disconnected..attempting connection");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return grabRoleForum(id);
			}
			Base.Console.severe("GrabRole error");Base.Console.printStackTrace(e);
		}
		catch (SQLException e){Base.Console.severe("GrabRole error");Base.Console.printStackTrace(e);}
		return role;
	}
	/**
	 * Creates new role in database
	 * @param role
	 * @param setup an aproval based system for later
	 * @return success
	 */
	public boolean insertRole(RoleData role, String setup){
		return addEditRole(role,true,1,setup);
	}
	/**
	 * Updates an existing role in database
	 * @param role
	 * @param currentVersion the roles version before updating
	 * @param setup an aproval based system for later
	 * @return success
	 */
	public boolean updateRole(RoleData role, int currentVersion, String setup){
		return addEditRole(role,false,currentVersion+1,setup);
	}
	/**
	 * Either creates a new or updates an existing role
	 * @param role
	 * @param newRole if creating or updating
	 * @param version version number that is going to be saved
	 * @param setup an aproval based system for later
	 * @return success
	 */
	private boolean addEditRole(RoleData role, boolean newRole, int version, String setup){
		boolean theReturn = false;
		if(role != null){
			try {
				int id = 0;
				if(newRole){//create new
					st = con.prepareStatement("INSERT INTO roles (name, version) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
				}
				else{//update existing
					st = con.prepareStatement("UPDATE roles SET name=?, version=? WHERE id= ?", Statement.RETURN_GENERATED_KEYS);
					st.setInt(3, role.id);
					id = role.id;
				}
				st.setString(1, role.name);
				st.setInt(2, version);
				st.executeUpdate();

				//get the last auto_inc id
				ResultSet rs = st.getGeneratedKeys();
				rs.next();
				if(newRole){id = rs.getInt(1);}
				st = con.prepareStatement("UPDATE roles SET setup=?, affiliation=?, cat1=?, cat2=?, onTeam=?, teamName=?, teamWin=?, visibleTeam=?, chatAtNight=?, actionCat=?, targetsN1=?, targetsN2=?, targetsD1=?, targetsD2=?, customScript=NULL WHERE id = ?");
				st.setString(1, "CUSTOM");
				st.setString(2, role.affiliation);
				if(role.category != null){
					if(role.category.length >= 1){
						st.setString(3, role.category[0]);
						if(role.category.length >= 2){
							st.setString(4, role.category[1]);
						}
						else{
							st.setString(4, null);
						}
					}
					else{
						st.setString(3, null);
						st.setString(4, null);
					}
				}
				else{
					st.setString(3, null);
					st.setString(4, null);
				}
				int onTeam = 0;
				if(role.onTeam){onTeam = 1;}
				st.setInt(5, onTeam);
				st.setString(6, role.teamName);
				int teamWin = 0;
				if(role.teamWin){teamWin = 1;}
				st.setInt(7, teamWin);
				int visibleTeam = 0;
				if(role.visibleTeam){visibleTeam = 1;}
				st.setInt(8, visibleTeam);
				int chatAtNight = 0;
				if(role.chatAtNight){chatAtNight = 1;}
				st.setInt(9, chatAtNight);
				st.setString(10, role.actionCat);
				st.setInt(11, role.targetablesNight1);
				st.setInt(12, role.targetablesNight2);
				st.setInt(13, role.targetablesDay1);
				st.setInt(14, role.targetablesDay2);
				st.setInt(15, id);
				st.executeUpdate();

				List<String> events = Arrays.asList("onStartup", "onDayStart", "onDayTargetChoice", "onDayEnd", "onNightStart", "onNightTargetChoice", "onNightEnd", "onVisit", "onAttacked", "onLynch", "onDeath", "victoryCon", "mayGameEndCon");
				LinkedHashMap<String, String> customEvents = new LinkedHashMap<String, String>();
				for(String event : role.ersScript.keySet()){
					if(events.contains(event)){
						if(!role.ersScript.get(event).isEmpty()){
							//st = con.prepareStatement("UPDATE roles SET ?=? WHERE id = ?");
							st = con.prepareStatement("UPDATE roles SET "+event+"=? WHERE id = ?");
							//st.setString(1, event);
							st.setString(1, role.ersScript.get(event));
							st.setInt(2, id);
							st.executeUpdate();
						}
					}
					else{
						if(!role.ersScript.get(event).isEmpty() & !role.ersScript.get(event).equals(" ")){
							customEvents.put(event,role.ersScript.get(event));
						}
					}
				}
				String customScriptsString = "";
				if(!customEvents.isEmpty()){
					for(String event : customEvents.keySet()){
						if (customScriptsString != ""){customScriptsString += Constants.CMDVARDIVIDER;}
						customScriptsString += event+Constants.CMDVARSUBDIVIDER+customEvents.get(event);
					}
					if(customScriptsString != ""){
						st = con.prepareStatement("UPDATE roles SET customScript=? WHERE id = ?");
						//st.setString(1, event);
						st.setString(1, customScriptsString);
						st.setInt(2, id);
						st.executeUpdate();
					}
				}
				theReturn = true;
			}
			catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
				Base.Console.severe("MySqlDatabase disconnected..attempting connection");
				if(getConnected()){
					this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
					return addEditRole(role, newRole, version, setup);
				}
				Base.Console.severe("Error in insertRole");
				Base.Console.printStackTrace(e);
			}
			catch (SQLException e) {
				Base.Console.severe("Error in insertRole");
				Base.Console.printStackTrace(e);
			}
		}
		return theReturn;
	}
	/**
	 * Returns list of possible roles fitting the parameters provided.<br>
	 * Does NOT grab entirity of a Role...used for only quick searches
	 * @param aff affilation
	 * @param cat category
	 * @param page page row of 10
	 */
	public ArrayList<Role> searchRoles(String aff, String cat, int page){
		ArrayList<Role> list = new ArrayList<Role>();
		//Role role = null;
		page--;page = page*10;
		int setup;
		String[] category = new String[2];
		try {
			String sql = "SELECT * FROM roles";
			if(!aff.equals("ANY")){sql += " WHERE affiliation = ?";
				if(!cat.equals("ANY")){sql += " AND (cat1 = ? OR cat2 = ?)";}
			}
			else if(!cat.equals("ANY")){
				sql += " WHERE (cat1 = ? OR cat2 = ?)";
			}
			sql += " LIMIT "+page+", 10";
			st = con.prepareStatement(sql);
			if(!aff.equals("ANY")){st.setString(1, aff);if(!cat.equals("ANY")){st.setString(2, cat);st.setString(3, cat);}}
			else if(!cat.equals("ANY")){st.setString(1, cat);st.setString(2, cat);}
			rs = st.executeQuery();
			//rs = st.executeQuery( "SELECT * FROM roles WHERE id = '"+id+"'");
			while(rs.next()){ // If match.
					if(rs.getString("setup").equals("DEFAULT")){
						setup = Constants.TYPE_GAMEOB_ROLE_DEFAULT;
					}
					else{setup = Constants.TYPE_GAMEOB_ROLE_CUSTOM;
					}
					category[0] = rs.getString("cat1");
					category[1] = rs.getString("cat2");

					list.add(new Role(null, rs.getInt("id"),rs.getString("name"),setup,rs.getString("affiliation"), category));//,category);
			}
		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase disconnected..attempting connection");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return searchRoles(aff, cat, page);
			}
			Base.Console.severe("searchRole error");Base.Console.printStackTrace(e);
		}
		catch (SQLException e){Base.Console.severe("searchRole error");Base.Console.printStackTrace(e);}
		return list;
	}
	/**
	 * Checks if user is able to connect with provided password.<br>
	 * Correct creditails will return boolean variable 'success' as TRUE
	 * @param username inputted username
	 * @param pass inputted password(should already be in MD5 format)
	 * @return ArrayList<Object>[boolean success, String username, int usergroup]
	 */
	public HashMap<String,Object> connectUserPass(String username, String pass){//$pass must already be passed in MD5 format
		username = username.toLowerCase();
		HashMap<String,Object> data = new HashMap<String,Object>();
		try{
			st = con.prepareStatement("SELECT * FROM user_account WHERE username=?");
			st.setString(1,username);
			rs=st.executeQuery();
			if(rs.next()){ // If user exists
				String salt = rs.getString("pass2");
				String crypt_password = crypt(pass, salt);

				st = con.prepareStatement("SELECT * FROM user_account WHERE username=? and pass=?");
				st.setString(1,username);st.setString(2,crypt_password);
				rs=st.executeQuery();
				if(rs.next()){ // If match.
					data.put("success",new Boolean(true));
					data.put("accountid",rs.getInt("id"));
					data.put("username",new String(rs.getString("username")));
					data.put("usergroup",rs.getInt("usergroup"));
					data.put("avatar",new String(rs.getString("avatarurl")));
				}
				else{
					data.put("success",new Boolean(false));
				}
			}
			else{
				data.put("success",new Boolean(false));
			}
		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase disconnected..attempting connection");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return connectUserPass(username, pass);
			}
			Base.Console.severe("MySqlDatabaseHandler.ConnectUserPass error");
			Base.Console.printStackTrace(e);
			data.put("success",new Boolean(false));
		}
		catch(Exception e){//com.mysql.jdbc.exceptions.jdbc4.CommunicationsException
			Base.Console.severe("MySqlDatabaseHandler.ConnectUserPass error");
			Base.Console.printStackTrace(e);
			data.put("success",new Boolean(false));
		}
		return data;
	}
	/**
	 * Returns whether user exists in database or not
	 * @param username
	 * @return int 0(no user exists),1(awaiting validation),2(has account),3(opt-out)
	 */
	public int checkUsername(String username){
		try{
			//rs=st.executeQuery("SELECT * FROM user_account WHERE username='"+username+"'");
			st = con.prepareStatement("SELECT * FROM user_account WHERE username=?");
			st.setString(1,username);
			rs=st.executeQuery();
			if(rs.next()){ // If match.
				return 2;
			}
			//rs=st.executeQuery("SELECT * FROM user_verify WHERE username='"+username+"'");
			st = con.prepareStatement("SELECT * FROM user_verify WHERE username=?");
			st.setString(1,username);
			rs=st.executeQuery();
			if(rs.next()){ // If match.
				return 1;
			}
			//rs=st.executeQuery("SELECT * FROM user_spam WHERE username='"+username+"'");
			st = con.prepareStatement("SELECT * FROM user_spam WHERE username=?");
			st.setString(1,username);
			rs=st.executeQuery();
			if(rs.next()){ // If match.
				return 3;
			}
			return 0;
		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase disconnected..attempting connection");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return checkUsername(username);
			}
			Base.Console.severe("MySqlDatabaseHanlder.CheckUsername error");
			Base.Console.printStackTrace(e);
			return 2;
		}
		catch(Exception e){
			Base.Console.severe("MySqlDatabaseHanlder.CheckUsername error");
			Base.Console.printStackTrace(e);
			return 2;
		}
	}
	/**
	 * Creates entry in database for new account to be verified
	 * and PMs a token through the SC@ forum to the user, needed for verification<br>
	 * Only Forum account names are possible
	 * @param username
	 * @param pass must be in MD5 format
	 * @return "pm_messagesent" if creation was succesful
	 */
	public String createAccount(String username, String pass){
		Random rand;rand = new Random();
		String token = StringFunctions.Base64encode(StringFunctions.substr(username, 1, 1)+rand.nextInt(9999999)+StringFunctions.substr(username, 0, 1));
		token = token.replace("==", "s8").replace("=", "").replace("+/", "");
		return createAccount(username, pass, token, false);
	}
	/**
	* Creates entry in database for new account to be verified
	* and PMs a token through the SC@ forum to the user, needed for verification<br>
	* Only Forum account names are possible
	* @param username
	* @param pass must be in MD5 format
	* @param token token to be used in verification
	* @param retry true is token already sent to forum
	* @return "pm_messagesent" if creation was succesful
	*/
	private String createAccount(String username, String pass, String token, boolean retry){
		String salt = generateSalt();
		String crypt_password = crypt(pass,salt);
		Long reg_time = System.currentTimeMillis()/1000;
		boolean success = false;
		//TODO SPAM: need to setup the said web service
		if(retry == false){
			try {
				success = Base.ForumAPI.pm_SendNew(username,"eMafia Account Verification",
					"[table][tr][td]Welcome to [B][COLOR=#DAA520]e[/COLOR]Mafia[/B], "+username+"![/td][/tr]" +
					"[tr][td][/td][/tr][tr][td] Your account has been created, but still needs verification within 24 hours. Below is your verification code:[/td][/tr]" +
					"[tr][td][CENTER][COLOR=WHITE][SIZE=6][B]"+token+"[/B][/SIZE][/COLOR][/CENTER][/td][/tr]" +
					"[tr][td][/td][/tr][tr][td] If you are not the one that started the creation process, please ignore this email or click the link below to never receive any messages about [B][COLOR=#DAA520]e[/COLOR]Mafia[/B] again(link not available at this moment, contact Apocist):[/td][/tr]" +
					"[tr][td][URL=http://eMafia.hikaritemple.com/spam?v="+token+"]http://eMafia.hikaritemple.com/spam?v="+token+"[/URL][/td][/tr]" +
					"[tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][/table]");
			} catch (PMRecipTurnedOff e) {
				return "pmrecipturnedoff";
			} catch (PMRecipientsNotFound e) {
				return "pmrecipientsnotfound";
			} catch (VBulletinAPIException e) {
				return "unknownerror";
			}
		}
		if(success || retry){
			try {
				st = con.prepareStatement("INSERT INTO user_verify (username, pass, pass2, token, reg_time) VALUES (?,?,?,?,?)");
				st.setString(1, username);st.setString(2, crypt_password);st.setString(3, salt);st.setString(4, token);st.setLong(5, reg_time);
				st.executeUpdate();
				return "pm_messagesent";
			}
			catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
				Base.Console.severe("MySqlDatabase disconnected..attempting connection");
				if(getConnected()){
					this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
					return createAccount(username, pass, token, true);
				}
				Base.Console.severe("Verify Account error");
				Base.Console.printStackTrace(e);
				return "MySqlDatabaseHanlder.CreateAccount error";
			}
			catch (Exception e) {
				Base.Console.severe("MySqlDatabaseHanlder.CreateAccount error");
				Base.Console.printStackTrace(e);
				return "MySqlDatabaseHanlder.CreateAccount error";
			}
		}
		return "unknownerror";
	}
	/**
	 * Checks if username/password/token combo is correct
	 * then creates the user accounts and removes from awaiting verification.
	 * @param username
	 * @param pass in MD5 format
	 * @param verify token
	 * @return true for successful verification
	 */
	public boolean verifyAccount(String username, String pass, String verify){
		try{
			st = con.prepareStatement("SELECT * FROM user_verify WHERE username=?");
			st.setString(1,username);
			rs=st.executeQuery();
			if(rs.next()){ // If user exists
				String salt = rs.getString("pass2");
				String crypt_password = crypt(pass, salt);
				st = con.prepareStatement("SELECT * FROM user_verify WHERE username=? and pass=? and token=?");
				st.setString(1, username);st.setString(2, crypt_password);st.setString(3, verify);
				rs = st.executeQuery();
				if(rs.next()){ // If match.
					int id = rs.getInt("id");
					String user = rs.getString("username");
					long reg_time = rs.getLong("reg_time");
					int usergroup = 5;
					st = con.prepareStatement("DELETE FROM user_verify WHERE id=?");
					st.setInt(1, id);
					st.executeUpdate();
					//check if they were preregistered
					usergroup = 5;//defualt members to normal member
					st = con.prepareStatement("SELECT * FROM user_preregister WHERE username=?");
					st.setString(1, user);
					rs = st.executeQuery();
					if(rs.next()){ // If match.
						usergroup = rs.getInt("user_group");
						st = con.prepareStatement("DELETE FROM user_preregister WHERE username=?");
						st.setString(1, user);
						st.executeUpdate();
					}
					//HashMap<String,String> forumData = Base.ForumAPI.parseViewMember(Base.ForumAPI.viewMember(user));
					st = con.prepareStatement("INSERT INTO user_account (username, pass, pass2, reg_time, usergroup) VALUES (?,?,?,?,?)");
					st.setString(1, user);st.setString(2, crypt_password);st.setString(3, salt);st.setLong(4, reg_time);st.setInt(5, usergroup);
					st.executeUpdate();
					updateForumData(user);
					return true;
				}
			}
		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase disconnected..attempting connection");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return verifyAccount(username, pass, verify);
			}
			Base.Console.severe("Verify Account error");
			Base.Console.printStackTrace(e);
			return false;
		}
		catch(Exception e){
			Base.Console.severe("Verify Account error");
			Base.Console.printStackTrace(e);
			return false;
		}
		return false;
	}
	/**
	 * Grabs user data from Forum based on username and uploads to eMafia database
	 * @param username
	 * @return boolean base on success
	 */
	public boolean updateForumData(String username){
		try{
			st = con.prepareStatement("SELECT * FROM user_account WHERE username=?");
			st.setString(1, username);
			rs = st.executeQuery();
			if(rs.next()){ // If match.
				try{
					Member forumData =Base.ForumAPI.forum_ViewMember(username);
					if(forumData.username != null){
						if(forumData.username.length() < 2){forumData.username = username;}
						st = con.prepareStatement("UPDATE user_account SET forumid=?, forumjoindate=?, avatarurl=?, username=? WHERE username=?");
						st.setInt(1, Integer.parseInt(forumData.userid));st.setLong(2, Long.parseLong(forumData.joindate));st.setString(3, forumData.avatarurl);st.setString(4, forumData.username);st.setString(5, username);
						st.executeUpdate();
						return true;
					}
				}
				catch(VBulletinAPIException e){}//will return in a moment
			}
			return false;
		}
		catch(Exception e){
			Base.Console.severe("updateForumData error");
			Base.Console.printStackTrace(e);
			return false;
		}
	}
	//XXX Password Crypt: is this secure enough?
	/**
	 * Returns a crypted pass for database<br>
	 * encSalt shall received from generateSalt() or from database directly
	 */
	private String crypt(String password, String encSalt){
		String salt = encSalt;
		if(salt.length() > 3)salt = encSalt.substring(3);
		if(salt.length() > 3)salt = StringFunctions.substr(salt, 0, salt.length()-3);
		String crypted = BCrypt.hashpw(StringFunctions.MD5(StringFunctions.substrLastChar(password)+password+StringFunctions.substr(password, 2, 2)),salt);
		return StringFunctions.MD5("!4f/"+crypted+"rJ");

	}
	/**
	 * Generates salt for use with crypt() or save to database
	 */
	private String generateSalt(){
		Random rand = new Random();
		String front = "$"+rand.nextInt(9)+StringFunctions.rndChar();
		String back = java.lang.Character.toString(StringFunctions.rndChar())+rand.nextInt(9)+java.lang.Character.toString(StringFunctions.rndChar());
		return front+BCrypt.gensalt(12)+back;
	}
	/**
	 * Saves or deletes a persistent match to the database. Use a Null object to remove
	 * @param ongoing
	 * @param signup
	 * @return
	 */
	public boolean saveMatchs(MatchForum ongoing, MatchForum signup){
		try {
			st = con.prepareStatement("UPDATE persistence SET ongoing=?, signup=? WHERE id=1");
			if(ongoing != null){st.setObject(1, ongoing);}else{st.setNull(1, java.sql.Types.BLOB);}
			if(signup != null){st.setObject(2, signup);}else{st.setNull(2, java.sql.Types.BLOB);}
			st.executeUpdate();
			return true;
		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase connection error saving matches");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return saveMatchs(ongoing, signup);
			}
			Base.Console.severe("MySqlDatabase saveMatchs connection error saving matches ERROR!");
			Base.Console.printStackTrace(e);
		}
		catch(Exception e){
			Base.Console.severe("MySqlDatabase saveMatchs other error");
			Base.Console.printStackTrace(e);
		}
		return false;
	}
	public MatchForum loadOngoingMatch(){
		Object match = null;
		try {
			st = con.prepareStatement("SELECT ongoing FROM persistence WHERE id = 1");
			rs = st.executeQuery();
			if(rs.next()){ // If match.

				byte[] buf = rs.getBytes("ongoing");
				ObjectInputStream objectIn = null;
				if (buf != null)
				objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
				if(objectIn != null){
					match = objectIn.readObject();
				}

				if(match != null){
					if(match.getClass().getSimpleName().equals("MatchForum")){
						return (MatchForum) match;
					}
					System.out.println("mysql ongoing match is a "+match.getClass().getSimpleName());
				}
			}

		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase connection error saving matches");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return null;
			}
			Base.Console.severe("MySqlDatabase saveMatchs connection error saving matches ERROR!");
			Base.Console.printStackTrace(e);
		}
		catch(Exception e){
			Base.Console.severe("MySqlDatabase saveMatchs other error");
			Base.Console.printStackTrace(e);
		}
		return null;
	}
	public MatchForum loadSignupMatch(){
		Object match = null;
		try {
			st = con.prepareStatement("SELECT signup FROM persistence WHERE id = 1");
			rs = st.executeQuery();
			if(rs.next()){ // If match.


				byte[] buf = rs.getBytes("signup");
				ObjectInputStream objectIn = null;
				if (buf != null)
				objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
				if(objectIn != null){
					match = objectIn.readObject();
				}

				if(match != null){
					if(match.getClass().getSimpleName().equals("MatchForum")){
						return (MatchForum) match;
					}
					System.out.println("mysql signup match is a "+match.getClass().getSimpleName());
				}
			}

		}
		catch(com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e){
			Base.Console.severe("MySqlDatabase connection error saving matches");
			if(getConnected()){
				this.init(Base.Settings.MYSQL_URL, Base.Settings.MYSQL_USER, Base.Settings.MYSQL_PASS);
				return null;
			}
			Base.Console.severe("MySqlDatabase saveMatchs connection error saving matches ERROR!");
			Base.Console.printStackTrace(e);
		}
		catch(Exception e){
			Base.Console.severe("MySqlDatabase saveMatchs other error");
			Base.Console.printStackTrace(e);
		}
		return null;
	}
	/**Prints MySql version to the Console*/
	public void getVersion(){//a test
		try {
			st = con.prepareStatement("SELECT VERSION()");
			rs = st.executeQuery();
			if (rs.next()) {
				Base.Console.debug("MySql Version: "+rs.getString(1));
			}
		}
		catch (Exception e) {Base.Console.printStackTrace(e);}
	}
}
