/* eMafiaServer - SC2MafiaAPI.java
GNU GENERAL PUBLIC LICENSE V3*/
package com.inverseinnovations.eMafiaServer.includes.classes.Server;
//Created by Nick(Oops_ur_dead)
//Modified by Apocist
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;				//Copyright 2008-2011 Google Inc. http://www.apache.org/licenses/LICENSE-2.0
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;	//Copyright 2008-2011 Google Inc. http://www.apache.org/licenses/LICENSE-2.0
import com.google.gson.stream.JsonReader;	//Copyright 2008-2011 Google Inc. http://www.apache.org/licenses/LICENSE-2.0
import com.inverseinnovations.eMafiaServer.includes.StringFunctions;


/** A class to provide an easy to use wrapper around the vBulletin REST API.*/
public class SC2MafiaAPI extends Thread{
	public com.inverseinnovations.eMafiaServer.Base Base;
	public boolean CONNECTED = false;
	private String clientname;
	private String clientversion;
	private String apikey;
	private String apiURL;
	private String apiAccessToken;
	private String apiClientID;
	private String secret;

	/**
	 * Instantiates a new vBulletin API wrapper. This will initialise the API
	 * connection as well, with OS name and version pulled from property files
	 * and unique ID generated from the hashcode of the system properties
	 *
	 * @param apiURL
	 *            the URL of api.php on the given vBulletin site
	 * @param apikey
	 *            the API key for the site
	 * @param clientname
	 *            the name of the client
	 * @param clientversion
	 *            the version of the client
	 * @throws IOException
	 *             If the URL is wrong, or a connection is unable to be made for
	 *             whatever reason.
	 */
	public SC2MafiaAPI(com.inverseinnovations.eMafiaServer.Base base, String apiURL, String apikey, String clientname,String clientversion){ //throws IOException {
		this.Base = base;
		this.apiURL = apiURL;
		this.apikey = apikey;
		this.clientname = clientname;
		this.clientversion = clientversion;
		this.setName("SC2MafiaAPI");
		this.setDaemon(true);
		this.start();
	}
	public void run(){
		Properties props = System.getProperties();
		String errorMsg;
		//handshake with the forum
		if((errorMsg = responseError(init(clientname, clientversion, props.getProperty("os.name"),props.getProperty("os.version"),Integer.toString(props.hashCode()),false))) == null){
			Base.Console.config("SC2Mafia Forum API connected.");
			//attempt to login
			loginAttempt();
		}
		else{
			Base.Console.warning("SC2Mafia Forum API unable to connect! Registration is disabled. Reason: '"+errorMsg+"'");
			setConnected(false);
		}
	}
	/**
	 * Gets the API key.
	 *
	 * @return the API key
	 */
	public String getAPIkey() {
		return apikey;
	}
	/**
	 * Sets the API key.
	 *
	 * @param apikey
	 *            the new API key
	 */
	public void setAPIkey(String apikey) {
		this.apikey = apikey;
	}
	/**
	 * Gets the URL of api.php
	 *
	 * @return the URL
	 */
	public String getAPIURL() {
		return apiURL;
	}
	/**
	 * Sets the URL of api.php
	 *
	 * @param apiURL
	 *            the new URL
	 */
	public void setAPIURL(String apiURL) {
		this.apiURL = apiURL;
	}
	/**
	 * Gets the API access token.
	 *
	 * @return the API access token
	 */
	private String getAPIAccessToken() {
		return apiAccessToken;
	}
	/**
	 * Sets the API access token. You shouldn't need to use this if you use the
	 * init function.
	 *
	 * @param apiAccessToken
	 *            the new API access token
	 */
	private void setAPIAccessToken(String apiAccessToken) {
		this.apiAccessToken = apiAccessToken;
	}
	/**
	 * Gets the API client ID.
	 *
	 * @return the API client ID
	 */
	public String getAPIClientID() {
		return apiClientID;
	}
	/**
	 * Sets the API client ID. You shouldn't need to use this if you use the
	 * init function.
	 *
	 * @param apiClientID
	 *            the new API client ID
	 */
	public void setAPIClientID(String apiClientID) {
		this.apiClientID = apiClientID;
	}
	/**
	 * Gets the secret value.
	 *
	 * @return the secret value
	 */
	private String getSecret() {
		return secret;
	}
	/**
	 * Sets the secret value. You shouldn't need to use this if you use the init
	 * function.
	 *
	 * @param secret
	 *            the new secret value
	 */
	private void setSecret(String secret) {
		this.secret = secret;
	}
	/**
	 * Sets if the API successfully connected to the Forum
	 */
	public void setConnected(boolean arg){
		this.CONNECTED = arg;
	}
	/**
	 * Returns if connected AND logged into sc2maf forum
	 */
	public boolean getConnected(){
		return CONNECTED;
	}
	/**Grabs the 'errormessage' from within the json pulled form callMethod()
	 * Known errors:
	 * 		pm_messagesent = message successfully sent
	 * 		pmrecipientsnotfound = Forum user doesn't exist
	 * 		invalid_accesstoken
	 * @param response data from callMethod()
	 * @return the 'errormessage' inside, if none: null
	 */
	@SuppressWarnings("rawtypes")
	public String responseError(LinkedTreeMap<String, Object> response){
		//LinkedTreeMap response = (LinkedTreeMap) response2;
		String theReturn = null;
		String className = null;
		if(response != null){
			if(response.containsKey("response")){
				//response -> errormessage
				if(((LinkedTreeMap)response.get("response")).containsKey("errormessage")){
					className = ((LinkedTreeMap)response.get("response")).get("errormessage").getClass().getName();
					if(className.equals("java.lang.String")){
						theReturn = ((String) ((LinkedTreeMap)response.get("response")).get("errormessage"));
					}
					else if(className.equals("java.util.ArrayList")){
						Object[] errors = ((ArrayList) ((LinkedTreeMap)response.get("response")).get("errormessage")).toArray();
						if(errors.length > 0){
							theReturn = errors[0].toString();
						}
					}
					else{
						Base.Console.warning("responseError  response -> errormessage type unknown: "+className);
					}
				}
				else if(((LinkedTreeMap)response.get("response")).containsKey("HTML")){
					LinkedTreeMap HTML = (LinkedTreeMap) ((LinkedTreeMap)response.get("response")).get("HTML");
					if(HTML.containsKey("postpreview")){
						className = HTML.get("postpreview").getClass().getName();
						if(className.equals("com.google.gson.internal.LinkedTreeMap")){
							LinkedTreeMap postpreview = (LinkedTreeMap) HTML.get("postpreview");
							if(postpreview.containsKey("errorlist")){
								className = postpreview.get("errorlist").getClass().getName();
								if(className.equals("com.google.gson.internal.LinkedTreeMap")){
									LinkedTreeMap errorlist = (LinkedTreeMap) postpreview.get("errorlist");
									if(errorlist.containsKey("errors")){
										className = errorlist.get("errors").getClass().getName();
										if(className.equals("java.util.ArrayList")){
											ArrayList errors = (ArrayList) errorlist.get("errors");
											className = errors.get(0).getClass().getName();
											if(className.equals("java.util.ArrayList")){
												//response -> postpreview -> errorlist -> errors[0]
												ArrayList errorSub = (ArrayList) errors.get(0);
												theReturn = errorSub.get(0).toString();
											}
										}
									}

								}
							}
						}
					}
				}
			}
			else if(response.containsKey("custom")){
				theReturn = (String) response.get("custom");
			}
		}
		//Base.Console.debug("SC2Mafia API return error: "+theReturn);
		return theReturn;
	}
	/**
	 * Attempts to login no more than 3 times
	 */
	public void loginAttempt(){
		String errorMsg = "";
		for(int i = 0;i < 3;i++){
			errorMsg = responseError(login());if(errorMsg == null){errorMsg = "";}
			if(errorMsg.equals("redirect_login")){//if login is succesful
				Base.Console.config("SC2Mafia Forum API logged in.");
				setConnected(true);
				break;
			}
		}
		if(!errorMsg.equals("redirect_login")){//login failed
			Base.Console.warning("SC2Mafia Forum API unable to login! Registration is disabled. Reason: '"+errorMsg+"'");
			setConnected(false);
		}


	}
	/**Login using the Game Master credientals*/
	private LinkedTreeMap<String, Object> login(){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("vb_login_username", Base.Settings.GAMEMASTERNAME);
		params.put("vb_login_password", Base.Settings.GAMEMASTERPASS);
		return callMethod("login_login", params, true);
	}
	/**Sends a message to the 'user' using the saved Forum User Proxy(should be eMafia Game Master)
	 * @param user
	 * @param title subject
	 * @param message
	 * @return
	 */
	public String sendMsg(String user,String title,String message){
		String errorMsg;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("title", title);
		params.put("message", message);
		params.put("recipients", user);
		params.put("signature", "1");
		errorMsg = responseError(callMethod("private_insertpm", params, true));
		if(errorMsg != null){
			if(errorMsg.equals("pm_messagesent")){
				return errorMsg;
			}
			else if(errorMsg.equals("nopermission_loggedout")){
				loginAttempt();
				if(getConnected()){
					return sendMsg(user, title, message);
				}
				return errorMsg;
			}
			else{
				Base.Console.warning("SC2Mafia Forum API unable send message! Reason: '"+errorMsg+"'");
				return errorMsg;
			}
		}
		Base.Console.warning("SC2Mafia Forum API unable send message! Reason: '"+errorMsg+"'");
		return errorMsg;
	}
	/**Grabs all data with this username - must be parsed with parseViewMember*/
	public LinkedTreeMap<String, Object> viewMember(String user){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("username", user);
		return callMethod("member", params, true);
	}
	/** Parses json from viewMember into
	 * username
	 * forumid
	 * forumjoindate
	 * avatarurl
	 * @param response from viewMember (callMethod)
	 * @return HashMap<String, String>
	 */
	@SuppressWarnings("rawtypes")
	public HashMap<String, String> parseViewMember(LinkedTreeMap<String, Object> response){
		HashMap<String, String> theReturn = new HashMap<String, String>();
		theReturn.put("forumid", null);
		theReturn.put("forumjoindate", null);
		theReturn.put("avatarurl", null);
		String className = null;
		if(response.containsKey("response")){
			//response -> prepared
			if(((LinkedTreeMap)response.get("response")).containsKey("prepared")){
				className = ((LinkedTreeMap)response.get("response")).get("prepared").getClass().getName();
				if(className.equals("com.google.gson.internal.LinkedTreeMap")){
					LinkedTreeMap prepared = (LinkedTreeMap) ((LinkedTreeMap)response.get("response")).get("prepared");
					if(prepared.containsKey("username")){
						className = prepared.get("username").getClass().getName();
						if(className.equals("java.lang.String")){
							theReturn.put("username", (String) prepared.get("username"));
						}
					}
					if(prepared.containsKey("userid")){
						className = prepared.get("userid").getClass().getName();
						if(className.equals("java.lang.String")){
							theReturn.put("forumid", (String) prepared.get("userid"));
						}
					}
					if(prepared.containsKey("joindate")){
						className = prepared.get("joindate").getClass().getName();
						if(className.equals("java.lang.String")){
							theReturn.put("forumjoindate", (String) prepared.get("joindate"));
						}
					}
					if(prepared.containsKey("avatarurl")){
						className = prepared.get("avatarurl").getClass().getName();
						if(className.equals("java.lang.String")){
							theReturn.put("avatarurl", (String) prepared.get("avatarurl"));
						}
					}
				}
			}
		}
		return theReturn;
	}
	/**
	 * Inits the connection to SC2MafiaForum and retrieves the secret
	 *
	 * @param clientname
	 *            the name of the client
	 * @param clientversion
	 *            the version of the client
	 * @param platformname
	 *            the name of the platform this application is running on
	 * @param platformversion
	 *            the version of the platform this application is running on
	 * @param uniqueid
	 *            the unique ID of the client. This should be different for each
	 *            user, and remain the same across sessions
	 * @return the array returned by the server
	 * @throws IOException
	 *             If the URL is wrong, or a connection is unable to be made for
	 *             whatever reason.
	 */
	private LinkedTreeMap<String, Object> init(String clientname, String clientversion,String platformname, String platformversion, String uniqueid, boolean loggedIn){// throws IOException{
		try{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("clientname", clientname);
			params.put("clientversion", clientversion);
			params.put("platformname", platformname);
			params.put("platformversion", platformversion);
			params.put("uniqueid", uniqueid);
			LinkedTreeMap<String, Object> initvalues = callMethod("api_init", params, loggedIn);
			apiAccessToken = (String) initvalues.get("apiaccesstoken");
			apiClientID = String.valueOf(initvalues.get("apiclientid"));
			if((String) initvalues.get("secret") != null){secret = (String) initvalues.get("secret");}
			//Base.Console.debug("apiAccessToken = "+apiAccessToken);
			//Base.Console.debug("apiClientID = "+apiClientID);
			//Base.Console.debug("secret = "+secret);
			return initvalues;
		}
		catch(Exception e){
			return null;
		}
	}
	/**
	 * Calls a method through the API.
	 *
	 * @param methodname
	 *            the name of the method to call
	 * @param params
	 *            the parameters as a map
	 * @param sign
	 *            if the request should be signed or not. Generally, you want this to be true
	 * @return the array returned by the server
	 * @throws IOException
	 *             If the URL is wrong, or a connection is unable to be made for
	 *             whatever reason.
	 */
	private LinkedTreeMap<String, Object> callMethod(String methodname,Map<String, String> params, boolean sign){// throws IOException{
		LinkedTreeMap<String, Object> map = null;

		try{

			StringBuffer queryStringBuffer = new StringBuffer("api_m=" + methodname);
			SortedSet<String> keys = new TreeSet<String>(params.keySet());
			for (String key : keys) {
				queryStringBuffer.append("&" + key + "=" + URLEncoder.encode(params.get(key), "UTF-8"));
			}
			if (sign) {
				//queryStringBuffer.append("&api_sig="+ generateHash( (queryStringBuffer.toString() + apiAccessToken+ apiClientID + secret + apikey)).toLowerCase());
				queryStringBuffer.append("&api_sig="+ StringFunctions.MD5( (queryStringBuffer.toString() + apiAccessToken+ apiClientID + secret + apikey)).toLowerCase());
			}

			queryStringBuffer.append("&api_c=" + apiClientID);
			queryStringBuffer.append("&api_s=" + apiAccessToken);
			String queryString = queryStringBuffer.toString();
			queryString = queryString.replace(" ", "%20");
			URL apiUrl = new URL(apiURL + "?" + queryString);
			HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
			conn.setRequestMethod("POST");

			conn.setConnectTimeout(10000); //set timeout to 10 seconds
			conn.setReadTimeout(10000);//set timeout to 15 seconds
			conn.setDoOutput(true);
			conn.setDoInput(true);
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.writeBytes(queryString);
			//StringBuffer returnBuffer = new StringBuffer();
			InputStream is = null;
			try{
				is = conn.getInputStream();
			}
			finally{
				if(is != null){
					String json = IOUtils.toString( is );

					//System.out.print(json);

					Gson gson = new Gson();
					JsonReader reader = new JsonReader(new StringReader(json));
					reader.setLenient(true);
					map = gson.fromJson(reader,new TypeToken<Map<String, Object>>() {}.getType());
				}

			}
			conn.disconnect();
		}
		catch (java.net.SocketTimeoutException e) {
			Base.Console.warning("SocketTimeoutException in Forum API");
			map = new LinkedTreeMap<String, Object>();
			map.put("custom", new String("SocketTimeoutException"));
			//Base.Console.printStackTrace(e);
		}
		catch(IOException e){
			Base.Console.warning("IOException in Forum API");
			map = new LinkedTreeMap<String, Object>();
			map.put("custom", new String("IOException"));
			Base.Console.printStackTrace(e);
		}
		return map;
	}

}
