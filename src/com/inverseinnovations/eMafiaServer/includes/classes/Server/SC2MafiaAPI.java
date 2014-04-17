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
	public class Message{
		public String pmid;
		public String sendtime;
		public String statusicon;
		public String title;
		public int userid;
		public String username;
		public String message;

		public void setUserid(String id){
			if(StringFunctions.isInteger(id)){
				userid = Integer.parseInt(id);
			}
		}
	}
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
	/**
	 * Attempts to login no more than 3 times
	 */
	public void forum_Login(){
		String errorMsg = "";
		for(int i = 0;i < 3;i++){
			errorMsg = parseResponse(forum_LoginDirect());if(errorMsg == null){errorMsg = "";}
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
	private LinkedTreeMap<String, Object> forum_LoginDirect(){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("vb_login_username", Base.Settings.GAMEMASTERNAME);
		params.put("vb_login_password", Base.Settings.GAMEMASTERPASS);
		return callMethod("login_login", params, true);
	}
	/**Grabs all data with this username
	 * Returning:
	 * username
	 * forumid
	 * forumjoindate
	 * avatarurl
	 * */
	public HashMap<String, String> forum_ViewMember(String user){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("username", user);
		return parseViewMember(callMethod("member", params, true));
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
	 * Gets the API client ID.
	 *
	 * @return the API client ID
	 */
	public String getAPIClientID() {
		return apiClientID;
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
	 * Gets the URL of api.php
	 *
	 * @return the URL
	 */
	public String getAPIURL() {
		return apiURL;
	}
	/**
	 * Returns if connected AND logged into sc2maf forum
	 */
	public boolean getConnected(){
		return CONNECTED;
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
	/**Parses response, designed specifically for gathering the list of all messages. Messages only have the header at this point, the actual message is not included
	 * @param response
	 * @return ArrayList<Message>
	 */
	@SuppressWarnings("rawtypes")
	private ArrayList<Message> parseMessages(LinkedTreeMap<String, Object> response){
		ArrayList<Message> messages = new ArrayList<Message>();
		if(response != null){
			if(response.containsKey("response")){
				if(((LinkedTreeMap)response.get("response")).containsKey("HTML")){
					LinkedTreeMap HTML = (LinkedTreeMap) ((LinkedTreeMap)response.get("response")).get("HTML");
					if(HTML.containsKey("messagelist_periodgroups")){
						if(HTML.get("messagelist_periodgroups") instanceof LinkedTreeMap){
							LinkedTreeMap messageGroup = (LinkedTreeMap) HTML.get("messagelist_periodgroups");
							if(messageGroup.containsKey("messagesingroup")){
								if((double)(messageGroup.get("messagesingroup"))>0){//if there are messages
									if(messageGroup.containsKey("messagelistbits")){
										if(messageGroup.get("messagelistbits") instanceof LinkedTreeMap){//single message
											Message parsedMessage = new Message();
											LinkedTreeMap message = (LinkedTreeMap) messageGroup.get("messagelistbits");
											parsedMessage.pmid = (String) ((LinkedTreeMap)message.get("pm")).get("pmid");
											parsedMessage.sendtime = (String) ((LinkedTreeMap)message.get("pm")).get("sendtime");
											parsedMessage.statusicon = (String) ((LinkedTreeMap)message.get("pm")).get("statusicon");
											parsedMessage.title = (String) ((LinkedTreeMap)message.get("pm")).get("title");

											parsedMessage.setUserid((String) ((LinkedTreeMap)((LinkedTreeMap) message.get("userbit")).get("userinfo")).get("userid"));
											parsedMessage.username = (String) ((LinkedTreeMap)((LinkedTreeMap) message.get("userbit")).get("userinfo")).get("username");
											messages.add(parsedMessage);
										}
										else if(messageGroup.get("messagelistbits") instanceof ArrayList){//multiple messages
											for(Object objInner : (ArrayList) messageGroup.get("messagelistbits")){
												Message parsedMessage = new Message();
												LinkedTreeMap message = (LinkedTreeMap) objInner;
												parsedMessage.pmid = (String) ((LinkedTreeMap)message.get("pm")).get("pmid");
												parsedMessage.sendtime = (String) ((LinkedTreeMap)message.get("pm")).get("sendtime");
												parsedMessage.statusicon = (String) ((LinkedTreeMap)message.get("pm")).get("statusicon");
												parsedMessage.title = (String) ((LinkedTreeMap)message.get("pm")).get("title");

												parsedMessage.setUserid((String) ((LinkedTreeMap)((LinkedTreeMap) message.get("userbit")).get("userinfo")).get("userid"));
												parsedMessage.username = (String) ((LinkedTreeMap)((LinkedTreeMap) message.get("userbit")).get("userinfo")).get("username");
												messages.add(parsedMessage);
											}
										}
									}
								}
							}
						}
						else if(HTML.get("messagelist_periodgroups") instanceof ArrayList){
							ArrayList messageGroups = (ArrayList) HTML.get("messagelist_periodgroups");
							for(Object obj : messageGroups){
								LinkedTreeMap messageGroup = (LinkedTreeMap)obj;
								if(messageGroup.containsKey("messagesingroup")){
									if((double)(messageGroup.get("messagesingroup"))>0){//if there are messages
										if(messageGroup.containsKey("messagelistbits")){
											if(messageGroup.get("messagelistbits") instanceof LinkedTreeMap){//single message
												Message parsedMessage = new Message();
												LinkedTreeMap message = (LinkedTreeMap) messageGroup.get("messagelistbits");
												parsedMessage.pmid = (String) ((LinkedTreeMap)message.get("pm")).get("pmid");
												parsedMessage.sendtime = (String) ((LinkedTreeMap)message.get("pm")).get("sendtime");
												parsedMessage.statusicon = (String) ((LinkedTreeMap)message.get("pm")).get("statusicon");
												parsedMessage.title = (String) ((LinkedTreeMap)message.get("pm")).get("title");

												parsedMessage.setUserid((String) ((LinkedTreeMap)((LinkedTreeMap) message.get("userbit")).get("userinfo")).get("userid"));
												parsedMessage.username = (String) ((LinkedTreeMap)((LinkedTreeMap) message.get("userbit")).get("userinfo")).get("username");
												messages.add(parsedMessage);
											}
											else if(messageGroup.get("messagelistbits") instanceof ArrayList){//multiple messages
												for(Object objInner : (ArrayList) messageGroup.get("messagelistbits")){
													Message parsedMessage = new Message();
													LinkedTreeMap message = (LinkedTreeMap) objInner;
													parsedMessage.pmid = (String) ((LinkedTreeMap)message.get("pm")).get("pmid");
													parsedMessage.sendtime = (String) ((LinkedTreeMap)message.get("pm")).get("sendtime");
													parsedMessage.statusicon = (String) ((LinkedTreeMap)message.get("pm")).get("statusicon");
													parsedMessage.title = (String) ((LinkedTreeMap)message.get("pm")).get("title");

													parsedMessage.setUserid((String) ((LinkedTreeMap)((LinkedTreeMap) message.get("userbit")).get("userinfo")).get("userid"));
													parsedMessage.username = (String) ((LinkedTreeMap)((LinkedTreeMap) message.get("userbit")).get("userinfo")).get("username");
													messages.add(parsedMessage);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return messages;
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
	private String parseResponse(LinkedTreeMap<String, Object> response){
		//LinkedTreeMap response = (LinkedTreeMap) response2;
		String theReturn = null;
		String className = null;
		if(response != null){
			if(response.containsKey("response")){
				//errormessage
				if(((LinkedTreeMap)response.get("response")).containsKey("errormessage")){
					className = ((LinkedTreeMap)response.get("response")).get("errormessage").getClass().getName();
					if(className.equals("java.lang.String")){
						System.out.println("response->errormessage->java.lang.String");
						theReturn = ((String) ((LinkedTreeMap)response.get("response")).get("errormessage"));
						if(theReturn.equals("redirect_postthanks")){//this is for newthread and newpost
							if(response.containsKey("show")){
								if(((LinkedTreeMap)response.get("show")).containsKey("threadid")){
									theReturn = (String) ((LinkedTreeMap)response.get("show")).get("threadid");
									theReturn += " "+(double) ((LinkedTreeMap)response.get("show")).get("postid");
								}
							}
						}
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
				//HTML
				else if(((LinkedTreeMap)response.get("response")).containsKey("HTML")){
					LinkedTreeMap HTML = (LinkedTreeMap) ((LinkedTreeMap)response.get("response")).get("HTML");
					if(HTML.containsKey("totalmessages")){
						theReturn = "totalmessages";
					}
					else if(HTML.containsKey("postbit")){
						if(HTML.get("postbit") instanceof LinkedTreeMap){
							LinkedTreeMap postbit = (LinkedTreeMap) HTML.get("postbit");
							if(postbit.containsKey("post")){
								if(postbit.get("post") instanceof LinkedTreeMap){
									LinkedTreeMap post = (LinkedTreeMap) postbit.get("post");
									if(post.containsKey("message")){
										theReturn = (String) post.get("message");
									}
								}
							}
						}
					}
					else if(HTML.containsKey("postpreview")){
						if(HTML.get("postpreview") instanceof LinkedTreeMap){
							LinkedTreeMap postpreview = (LinkedTreeMap) HTML.get("postpreview");
							if(postpreview.containsKey("errorlist")){
								if(postpreview.get("errorlist") instanceof LinkedTreeMap){
									LinkedTreeMap errorlist = (LinkedTreeMap) postpreview.get("errorlist");
									if(errorlist.containsKey("errors")){
										if(errorlist.get("errors") instanceof ArrayList){
											ArrayList errors = (ArrayList) errorlist.get("errors");
											if(errors.get(0) instanceof ArrayList){
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
				//errorlist
				else if(((LinkedTreeMap)response.get("response")).containsKey("errorlist")){
					ArrayList errorlist = (ArrayList) ((LinkedTreeMap)response.get("response")).get("errorlist");
					Base.Console.debug("Unknown Responses(errorlsit ->): "+errorlist.toString());
				}
				else{//has response..but not common
					Base.Console.debug("Unknown Responses: "+((LinkedTreeMap)response.get("response")).keySet().toString());
				}
			}
			else if(response.containsKey("custom")){
				theReturn = (String) response.get("custom");
			}
			//testing this:
			System.out.println("all ->");//XXX: for testing
			System.out.println(response.toString());
		}
		//Base.Console.debug("SC2Mafia API return error: "+theReturn);
		return theReturn;
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
	private HashMap<String, String> parseViewMember(LinkedTreeMap<String, Object> response){
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
		System.out.println(response.toString());
		return theReturn;
	}
	/**Returns list of PMs in the inbox
	 * @return
	 */
	public ArrayList<Message> pm_ListPMs(){
		String errorMsg;
		HashMap<String, String> params = new HashMap<String, String>();
		LinkedTreeMap<String,Object> linkmap = callMethod("private_messagelist", params, true);
		errorMsg = parseResponse(linkmap);
		System.out.println("parsed the link map for errors");
		if(errorMsg != null){
			if(errorMsg.equals("totalmessages")){//is the inbox
				System.out.println("contains totalmessages");
				ArrayList<Message> msgList = parseMessages(linkmap);
				System.out.println("parsed the link map for PM arrayList");
				System.out.println("has "+msgList.size()+" message(s)");
				for(Message msg:msgList){
					System.out.println("About to go through list for");
					msg.message = pm_ViewPM(msg.pmid);
				}
				System.out.println("done with for");
				return msgList;
			}
			else if(errorMsg.equals("nopermission_loggedout")||errorMsg.equals("invalid_accesstoken")){
				forum_Login();
				if(getConnected()){
					return pm_ListPMs();
				}
			}
			else if(errorMsg.equals("invalid_api_signature")){//XXX need to check this
				return pm_ListPMs();
			}
		}
		Base.Console.warning("SC2Mafia Forum API unable view messages! Reason: '"+errorMsg+"'");
		return null;
	}
	/**Sends a message to the 'user' using the saved Forum User Proxy(should be eMafia Game Master)
	 * @param user
	 * @param title subject
	 * @param message
	 * @return
	 */
	public String pm_SendNew(String user,String title,String message){
		return pm_SendNew( user, title, message, 0);
	}
	/**Sends a message to the 'user' using the saved Forum User Proxy(should be eMafia Game Master)
	 * @param user
	 * @param title subject
	 * @param message
	 * @return
	 */
	private String pm_SendNew(String user,String title,String message, int loop){
		loop++;
		String errorMsg;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("title", title);
		params.put("message", message);
		params.put("recipients", user);
		params.put("signature", "1");
		errorMsg = parseResponse(callMethod("private_insertpm", params, true));
		System.out.println("loop is "+loop);
		Base.Console.warning("loop is "+loop);
		if(loop < 5){//no inifinite loop by user
			if(errorMsg != null){
				if(errorMsg.equals("pm_messagesent")){
					return errorMsg;
				}
				else if(errorMsg.equals("nopermission_loggedout")||errorMsg.equals("invalid_accesstoken")||errorMsg.equals("invalid_api_signature")){
					//forum_Login();
					this.start();
					if(getConnected()){
						return pm_SendNew(user, title, message,loop);
					}
					return errorMsg;
				}
				else if(errorMsg.equals("invalid_api_signature")){
					return pm_SendNew( user, title, message,loop);
				}
			}
		}
		Base.Console.warning("SC2Mafia Forum API unable send message! Reason: '"+errorMsg+"'");
		return errorMsg;
	}
	/**Grabs the message from the PM specified by the pmID
	 * @param pmId
	 * @return
	 */
	public String pm_ViewPM(String pmId){
		return pm_ViewPM(pmId, 0);
	}
	/**Grabs the message from the PM specified by the pmID
	 * @param pmId
	 * @param loop increasing int to prevent inifinite loops
	 * @return
	 */
	private String pm_ViewPM(String pmId, int loop){
		String errorMsg = null;
		if(pmId != null){
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("pmid", pmId);
			errorMsg = parseResponse(callMethod("private_showpm", params, true));
			if(loop < 5){//no inifinite loop by user
				if(errorMsg != null){
					if(errorMsg.equals("nopermission_loggedout")||errorMsg.equals("invalid_accesstoken")){
						forum_Login();
						if(getConnected()){
							return pm_ViewPM(pmId, loop++);
						}
					}
					else if(errorMsg.equals("invalid_api_signature")){
						return pm_ViewPM(pmId, loop++);
					}
				}
			}
		}
		return errorMsg;
	}
	/**Attempts to edit a post based on the post id
	 * @param postid
	 * @param message
	 * @return true on successs
	 */
	public boolean post_Edit(String postid,String message){
		String errorMsg;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("postid", postid);
		params.put("message", message);
		params.put("signature", "1");
		errorMsg = parseResponse(callMethod("editpost_updatepost", params, true));
		boolean theReturn = false;
		if(errorMsg != null){
			if(errorMsg.equals("redirect_editthanks")){//success
				return true;
			}
			else if(errorMsg.equals("nopermission_loggedout")||errorMsg.equals("invalid_accesstoken")){
				forum_Login();
				if(getConnected()){
					return post_Edit(postid, message);
				}
				theReturn = false;
			}
			else if(errorMsg.equals("invalid_api_signature")){//XXX need ot check this
				return post_Edit(postid, message);
			}
			else{
				theReturn = false;
			}
		}
		Base.Console.warning("SC2Mafia Forum API unable edit post! Reason: '"+errorMsg+"'");
		return theReturn;
	}
	/**Attempts to post a new reply in said Thread
	 * @param threadid
	 * @param message
	 * @return true on success
	 */
	public boolean post_New(String threadid,String message){
		String errorMsg;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("threadid", threadid);
		params.put("message", message);
		params.put("signature", "1");
		errorMsg = parseResponse(callMethod("newreply_postreply", params, true));
		if(errorMsg != null){
			if(StringFunctions.isInteger(errorMsg.substring(0, 1))){//success
				return true;
			}
			else if(errorMsg.equals("nopermission_loggedout")||errorMsg.equals("invalid_accesstoken")){
				forum_Login();
				if(getConnected()){
					return post_New(threadid, message);
				}
			}
			else if(errorMsg.equals("invalid_api_signature")){//XXX need ot check this
				return post_New(threadid, message);
			}
		}
		Base.Console.warning("SC2Mafia Forum API unable post reply! Reason: '"+errorMsg+"'");
		return false;
	}
	public void run(){
		Properties props = System.getProperties();
		String errorMsg;
		//handshake with the forum
		if((errorMsg = parseResponse(init(clientname, clientversion, props.getProperty("os.name"),props.getProperty("os.version"),Integer.toString(props.hashCode()),false))) == null){
			Base.Console.config("SC2Mafia Forum API connected.");
			//attempt to login
			forum_Login();
		}
		else{
			Base.Console.warning("SC2Mafia Forum API unable to connect! Registration is disabled. Reason: '"+errorMsg+"'");
			setConnected(false);
		}
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
	 * Sets the API key.
	 *
	 * @param apikey
	 *            the new API key
	 */
	public void setAPIkey(String apikey) {
		this.apikey = apikey;
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
	 * Sets if the API successfully connected to the Forum
	 */
	public void setConnected(boolean arg){
		this.CONNECTED = arg;
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


	/**Attempts to post a new Thread in the forum, returns the posted Thread id and Post id for later use.
	 * Returns two numbers seperated by a space on success.'(threadid) (postid)'
	 * @param forumid
	 * @param subject
	 * @param message
	 * @return int(int String) on success, errormsg otherwise
	 */
	public String thread_New(String forumid,String subject,String message){
		String errorMsg;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("forumid", forumid);
		params.put("subject", subject);
		params.put("message", message);
		params.put("signature", "1");
		errorMsg = parseResponse(callMethod("newthread_postthread", params, true));
		if(errorMsg != null){
			if(StringFunctions.isInteger(errorMsg.substring(0, 1))){//success
				return errorMsg;
			}
			else if(errorMsg.equals("nopermission_loggedout")||errorMsg.equals("invalid_accesstoken")){
				forum_Login();
				if(getConnected()){
					return thread_New(forumid, subject, message);
				}
				return errorMsg;
			}
			else if(errorMsg.equals("invalid_api_signature")){//XXX need to check this
				return thread_New(forumid, subject, message);
			}
			else{
				Base.Console.warning("SC2Mafia Forum API unable submit Thread! Reason: '"+errorMsg+"'");
				return errorMsg;
			}
		}
		Base.Console.warning("SC2Mafia Forum API unable submit Thread! Reason: '"+errorMsg+"'");
		return errorMsg;
	}

}
