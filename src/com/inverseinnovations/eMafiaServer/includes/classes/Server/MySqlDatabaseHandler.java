/* eMafiaServer - MySqlDataBaseHandler.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes.Server;


import java.sql.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import com.inverseinnovations.eMafiaServer.*;
import com.inverseinnovations.eMafiaServer.includes.*;
import com.inverseinnovations.eMafiaServer.includes.classes.*;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.*;


/**Manages database interaction*/
public class MySqlDatabaseHandler {//FIXME Need a 'secure' method to not allow mysql injections
	public Base Base;//back reference to parent

	private Connection con = null;
	//private Statement st = null;
	private PreparedStatement st = null;
	private ResultSet rs = null;

	/**
	 * Creates instance and connects to defined MySQL Database
	 * @param url e.g. jdbc:mysql://localhost:3306/testdb
	 * @param user DB username
	 * @param password DB password
	 */
	public MySqlDatabaseHandler(Base base, String url, String user, String password){
		this.Base = base;
		try {
			Class.forName("com.mysql.jdbc.Driver");//.newInstance();
			con = DriverManager.getConnection(url, user, password);
			//st = con.createStatement();

			if(con != null){Base.Console.config("MySQL DB connected to "+url+"");}
			else{Base.program_faults++;Base.Console.severe("MySQL DB failed to connect, server will not run correctly!");}

			//Base.Console.debug(BCrypt.gensalt());
		}
		catch (Exception e) {
			Base.program_faults++;
			Base.Console.severe("MySQL DB failed to connect, server will not run correctly!");
			Base.Console.printStackTrace(e);

		}

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

	public Map<Integer, Integer> grabRoleCatList(String setup,String aff,String cat){//TODO error testing needs to be done
		Map<Integer, Integer> list = new LinkedHashMap<Integer, Integer>();
		//setup = dbHandler::secure(setup);aff = dbHandler::secure(aff); cat = dbHandler::secure(cat);
		//if(!setup.equals("DEFAULT") && !setup.equals("CUSTOM")){Base.Console.warning("BAD SETUP VALUE: "+setup);return null;}
		//if(!aff.equals("TOWN") && !aff.equals("MAFIA") && !aff.equals("NUTERAL") && !aff.equals("ANY")){Base.Console.warning("BAD AFF VALUE: "+aff);return null;}
		//if(!cat.equals("CORE") && !cat.equals("INVESTIGATIVE")){Base.Console.warning("BAD CAT VALUE: "+cat);return null;}
			//$result=mysql_query("SELECT id, name FROM roles WHERE setup = '$setup' AND affiliation = '$aff' AND (cat1 = '$cat' OR cat2 = '$cat')");
		try {
			//rs = st.executeQuery( "SELECT id, name FROM roles WHERE setup = '"+setup+"' AND affiliation = '"+aff+"' AND (cat1 = '"+cat+"' OR cat2 = '"+cat+"')");
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
		catch (SQLException e){Base.Console.severe("Error retrieving Role Catergory List");Base.Console.printStackTrace(e);}
		return list;
		//}
		//else{echo "NO RESULTS\n";return FALSE;}
	}

	public Role grabRole(int id){
		Role role = null;
		int setup;
		String[] category = new String[2];
		try {
			st = con.prepareStatement("SELECT * FROM roles WHERE id = ?");
			st.setInt(1, id);
			rs = st.executeQuery();
			//rs = st.executeQuery( "SELECT * FROM roles WHERE id = '"+id+"'");
			if(rs.next()){ // If match.
					if(rs.getString("setup").equals("DEFAULT")){
						setup = Constants.TYPE_GAMEOB_ROLE_DEFAULT;
					}
					else{setup = Constants.TYPE_GAMEOB_ROLE_CUSTOM;
					}
					category[0] = rs.getString("cat1");
					category[1] = rs.getString("cat2");

					/*$role = new Role($row['id'],$row['name'],$setup,$row['affiliation'],$category);
					if(!empty($row['nightaction'])){
						$role->createRoleFunction('nightAction',$row['nightaction']);
					}*/
					role = new Role(null, rs.getInt("id"),rs.getString("name"),setup,rs.getString("affiliation"), category);//,category);
					role.setVersion(rs.getInt("version"));
					if(rs.getInt("teamWin") == 1){
						role.setOnTeam(true);
						role.setTeamName(rs.getString("teamName"));
						if(rs.getInt("teamWin") == 1){role.setTeamWin(true);}
						if(rs.getInt("visibleTeam") == 1){}//TODO
						if(rs.getInt("chatAtNight") == 1){}//TODO
					}

					//role.setActionOrder(rs.getInt("actionorder"));
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

					role.targetablesNight1=rs.getInt("targetsN1");
					role.targetablesNight2=rs.getInt("targetsN2");
					role.targetablesDay1=rs.getInt("targetsD1");
					role.targetablesDay2=rs.getInt("targetsD2");
			}
			else{Base.Console.warning("ERROR NO RESULTS for id "+id+"!\n");}
		}
		catch (SQLException e){Base.Console.severe("GrabRole error");Base.Console.printStackTrace(e);}
		return role;
	}
	/**Does NOT grab entirity of a Role...used for only quick searches **/
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
					//role.setActionOrder(rs.getInt("actionorder"));
					//role.setMayGameEndCon(rs.getString("maygameend"));
					//role.setVictoryCon(rs.getString("victorycon"));
					//role.setActionStartup(rs.getString("scriptstartup"));
					//role.setActionNight(rs.getString("scriptnight"));
					//role.setActionNightClicked(rs.getString("scriptnightclick"));
					//role.setActionDay(rs.getString("scriptday"));
					//role.setActionBeforeDay(rs.getString("scriptbeforeday"));
					//role.setActionBeforeNight(rs.getString("scriptbeforenight"));
					//role.targetablesNight1=rs.getInt("targetsN1");
					//role.targetablesNight2=rs.getInt("targetsN2");
					//role.targetablesDay1=rs.getInt("targetsD1");
					//role.targetablesDay2=rs.getInt("targetsD2");
			}
			//else{Base.Console.warning("ERROR NO RESULTS for id "+id+"!\n");}
		}
		catch (SQLException e){Base.Console.severe("searchRole error");Base.Console.printStackTrace(e);}
		return list;
	}
	/**
	 * Checks if user is able to connect with provided password
	 * @param username inputted username
	 * @param pass inputted password(should already be in MD5 format)
	 * @return ArrayList<Object>[boolean success, String username, int usergroup]
	 */
	public HashMap<String,Object> connectUserPass(String username, String pass) {//$pass must already be passed in MD5 format
		username = username.toLowerCase();
		//Base.Console.debug("password crypted is: "+crypt_password);
		//Base.Console.debug(crypt_password);
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
		catch(Exception e){
			Base.Console.severe("MySqlDatabaseHanlder.ConnectUserPass error");
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
	public int checkUsername(String username) {
		try{
			//rs=st.executeQuery("SELECT * FROM user_account WHERE username='"+username+"'");
			st = con.prepareStatement("SELECT * FROM user_account WHERE username=?");
			st.setString(1,username);
			rs=st.executeQuery();
			if(rs.next()){ // If match.
				return 2;
			}
			else{
				//rs=st.executeQuery("SELECT * FROM user_verify WHERE username='"+username+"'");
				st = con.prepareStatement("SELECT * FROM user_verify WHERE username=?");
				st.setString(1,username);
				rs=st.executeQuery();
				if(rs.next()){ // If match.
					return 1;
				}
				else{
					//rs=st.executeQuery("SELECT * FROM user_spam WHERE username='"+username+"'");
					st = con.prepareStatement("SELECT * FROM user_spam WHERE username=?");
					st.setString(1,username);
					rs=st.executeQuery();
					if(rs.next()){ // If match.
						return 3;
					}
					else{
						return 0;
					}
				}
			}
		}
		catch(Exception e){
			Base.Console.severe("MySqlDatabaseHanlder.CheckUsername error");
			Base.Console.printStackTrace(e);
			return 2;
		}
	}

	/**
	 * Creates entry in database for new account to be verified
	 * @param username
	 * @param pass must be in MD5 format
	 * @return boolean if create was made
	 */
	public String createAccount(String username, String pass) {
		//$username = dbHandler::secure($username);
		String temp = "";
		String salt = generateSalt();
		String crypt_password = crypt(pass,salt);
		Random rand;rand = new Random();
		String token = StringFunctions.Base64encode(StringFunctions.substr(username, 1, 1)+rand.nextInt(9999999)+StringFunctions.substr(username, 0, 1));
		token = token.replace("==", "s8").replace("=", "").replace("+/", "");
		Long reg_time = System.currentTimeMillis()/1000;
		if((temp = Base.ForumAPI.sendMsg(username,"eMafia Account Verification","[table][tr][td]Welcome to [B][COLOR=#DAA520]e[/COLOR]Mafia[/B], "+username+"![/td][/tr][tr][td][/td][/tr][tr][td] Your account has been created, but still needs verification within 24 hours. Below is your verification code:[/td][/tr][tr][td][CENTER][COLOR=WHITE][SIZE=6][B]"+token+"[/B][/SIZE][/COLOR][/CENTER][/td][/tr][tr][td][/td][/tr][tr][td] If you are not the one that started the creation process, please ignore this email or click the link below to never receive any messages about [B][COLOR=#DAA520]e[/COLOR]Mafia[/B] again(link not available at this moment, contact Apocist):[/td][/tr][tr][td][URL=http://eMafia.hikaritemple.com/spam?v="+token+"]http://eMafia.hikaritemple.com/spam?v="+token+"[/URL][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][tr][td][/td][/tr][/table]")).equals("pm_messagesent")){
		//if(Base.ForumAPI.sendMsg(username,"eMafia Account Verification","a test messgae with token: "+token)){
			try {
				//st.executeUpdate("INSERT INTO user_verify (username, pass, token, reg_time) VALUES ('"+username+"','"+pass+"','"+token+"','"+reg_time+"')");
				st = con.prepareStatement("INSERT INTO user_verify (username, pass, pass2, token, reg_time) VALUES (?,?,?,?,?)");
				st.setString(1, username);st.setString(2, crypt_password);st.setString(3, salt);st.setString(4, token);st.setLong(5, reg_time);
				st.executeUpdate();
				return temp;
			} catch (Exception e) {
				Base.Console.severe("MySqlDatabaseHanlder.CreateAccount error");
				Base.Console.printStackTrace(e);
				return "MySqlDatabaseHanlder.CreateAccount error";
			}
		}
		return temp;
	}

	/**Checks if username/password/token combo is correct
	 * then creates the user accounts and removes from awaiting verification.
	 * @param username
	 * @param pass
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
					//st.executeUpdate("DELETE FROM user_verify WHERE id='"+id+"'");
					st = con.prepareStatement("DELETE FROM user_verify WHERE id=?");
					st.setInt(1, id);
					st.executeUpdate();
					//check if they were preregistered
					//rs=st.executeQuery("SELECT * FROM user_preregister WHERE username='"+user+"'");
					usergroup = 5;//defualt members to normal member
					st = con.prepareStatement("SELECT * FROM user_preregister WHERE username=?");
					st.setString(1, user);
					rs = st.executeQuery();
					if(rs.next()){ // If match.
						usergroup = rs.getInt("user_group");
						//st.executeUpdate("DELETE FROM user_preregister WHERE username='"+user+"'");
						st = con.prepareStatement("DELETE FROM user_preregister WHERE username=?");
						st.setString(1, user);
						st.executeUpdate();
					}
					HashMap<String,String> forumData = Base.ForumAPI.parseViewMember(Base.ForumAPI.viewMember(user));
					st = con.prepareStatement("INSERT INTO user_account (username, pass, pass2, reg_time, usergroup) VALUES (?,?,?,?,?)");
					st.setString(1, user);st.setString(2, crypt_password);st.setString(3, salt);st.setLong(4, reg_time);st.setInt(5, usergroup);
					st.executeUpdate();
					updateForumData(user);
					return true;
				}
			}
		}
		catch(Exception e){
			Base.Console.severe("Verify Account error");
			Base.Console.printStackTrace(e);
			return false;
		}
		return false;
	}

	/**Grabs forum data from Forum based on username and uploads to eMafia database
	 * @param username
	 * @return
	 */
	public boolean updateForumData(String username){
		try{
			st = con.prepareStatement("SELECT * FROM user_account WHERE username=?");
			st.setString(1, username);
			rs = st.executeQuery();
			if(rs.next()){ // If match.
				HashMap<String,String> forumData = Base.ForumAPI.parseViewMember(Base.ForumAPI.viewMember(username));
				if(forumData.get("username").length() < 4){forumData.put("username", username);}
				st = con.prepareStatement("UPDATE user_account SET forumid=?, forumjoindate=?, avatarurl=?, username=? WHERE username=?");
				st.setInt(1, Integer.parseInt(forumData.get("forumid")));st.setLong(2, Long.parseLong(forumData.get("forumjoindate")));st.setString(3, forumData.get("avatarurl"));st.setString(4, forumData.get("username"));st.setString(5, username);
				st.executeUpdate();
				return true;
			}
			else{
				return false;
			}
		}
		catch(Exception e){
			Base.Console.severe("updateForumData error");
			Base.Console.printStackTrace(e);
			return false;
		}
	}

	/**Returns a crypted pass for database
	 * encSalt shall received from generateSalt() or from database directly
	 */
	private String crypt(String password, String encSalt){//,String salt){
		//return StringFunctions.MD5(StringFunctions.substr(username, 0, 1)+password+StringFunctions.substrLastChar(username));
		//encSalt has fake in back
		// MD5( BCrypt( MD5(password[-1] password + password[3] + password[4]) ), savedSalt) )
		String salt = encSalt;
		if(salt.length() > 3)salt = encSalt.substring(3);//removes front 3
		if(salt.length() > 3)salt = StringFunctions.substr(salt, 0, salt.length()-3);//removes back 3
		String crypted = BCrypt.hashpw(StringFunctions.MD5(StringFunctions.substrLastChar(password)+password+StringFunctions.substr(password, 2, 2)),salt);
		return StringFunctions.MD5("!4f/"+crypted+"rJ");

	}

	/**
	 * Generates salt for use with crypt() or save to database
	 */
	private String generateSalt(){
		Random rand = new Random();
		String front = "$"+rand.nextInt(9)+StringFunctions.rndChar();
		String back = java.lang.Character.toString(StringFunctions.rndChar())+rand.nextInt(9)+java.lang.Character.toString(StringFunctions.rndChar());char whee;
		return front+BCrypt.gensalt(12)+back;
	}

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