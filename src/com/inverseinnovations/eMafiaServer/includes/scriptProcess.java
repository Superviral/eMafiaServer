/* eMafiaServer - scriptProcess.java
GNU GENERAL PUBLIC LICENSE V3*/
package com.inverseinnovations.eMafiaServer.includes;

import java.util.concurrent.*;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;

import com.inverseinnovations.eMafiaServer.Base;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Match;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.MatchForum;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Role;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.RoleForum;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Team;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.TeamForum;

import org.mozilla.javascript.*;

public class scriptProcess {
	private boolean scriptDebugging = false;
	//static ScriptEngine js = new ScriptEngineManager().getEngineByName("javascript");

	public scriptProcess(final String event, final String string, final Match match){
		this("Match "+event, string, match, null, null, null);
	}
	public scriptProcess(final String event, final String string, final Team team){
		this("TEAM "+team.getName()+" "+event, string, team.getMatch(), null, null, team);
	}
	public scriptProcess(final String event, final String string, final Role role){
		this("ROLE "+role.getName()+" "+event, string, role.getMatch(), role, null, role.getTeam());
	}
	public scriptProcess(final String event, final String string, final Role role, final Role visitor){
		this("ROLE "+role.getName()+" "+event, string, role.getMatch(), role, visitor, role.getTeam());
	}
	public scriptProcess(final String scriptName, final String string, final Match match, final Role role, final Role visitor, final Team team){
		if(StringUtils.isNotBlank(string)){
			if(scriptDebugging){
				match.Game.Base.Console.debug("SCRIPT: doing script:");
				match.Game.Base.Console.debug(string);
			}
			Callable<Boolean> callable = new Callable<Boolean>() {
				public Boolean call() throws Exception {
					Context cx = createContext();
					Scriptable globalScope = new ImporterTopLevel(cx);
					globalScope.put("match", globalScope, match.getERSClass());
					if(role != null){
						globalScope.put("self", globalScope, role.getERSClass());
					}
					if(visitor != null){
						globalScope.put("visitor", globalScope, visitor.getERSClass());
					}
					if(team != null){
						globalScope.put("team", globalScope, team.getERSClass());
					}
					cx.evaluateString(globalScope, string, "Script", 1, null);
					return true;
				}
			};
			ExecutorService executorService = Executors.newCachedThreadPool();

			Future<Boolean> task = executorService.submit(callable);
			try{
				// ok, wait for 15 seconds max
				//Boolean result = task.get(15, TimeUnit.SECONDS);
				task.get(15, TimeUnit.SECONDS);
				if(scriptDebugging){match.Game.Base.Console.debug("Script is finished with completely");}
			}
			catch (ExecutionException t) {
				handleExecutionException(match.Game.Base, t, string, scriptName);
			}
			catch (TimeoutException e) {
				match.Game.Base.Console.warning("Script timeout...");
			}
			catch (InterruptedException e) {
				match.Game.Base.Console.warning("Script interrupted");
			}
		}
		return;
	}

	//Forum Variants
	public scriptProcess(final String event, final String string, final MatchForum match){
		this("Match "+event, string, match, null, null, null);
	}
	public scriptProcess(final String event, final String string, final TeamForum team){
		this("TEAM "+team.getName()+" "+event, string, team.getMatch(), null, null, team);
	}
	public scriptProcess(final String event, final String string, final RoleForum role){
		this("ROLE "+role.getName()+" "+event, string, role.getMatch(), role, null, role.getTeam());
	}
	public scriptProcess(final String event, final String string, final RoleForum role, final RoleForum visitor){
		this("ROLE "+role.getName()+" "+event, string, role.getMatch(), role, visitor, role.getTeam());
	}
	public scriptProcess(final String scriptName, final String string, final MatchForum match, final RoleForum role, final RoleForum visitor, final TeamForum team){
		if(StringUtils.isNotBlank(string)){
			if(scriptDebugging){
				match.Game.Base.Console.debug("SCRIPT: doing script:");
				match.Game.Base.Console.debug(string);
			}
			Callable<Boolean> callable = new Callable<Boolean>() {
				public Boolean call() throws Exception {
					Context cx = createContext();
					Scriptable globalScope = new ImporterTopLevel(cx);
					globalScope.put("match", globalScope, match.getERSClass());
					if(role != null){
						globalScope.put("self", globalScope, role.getERSClass());
					}
					if(visitor != null){
						globalScope.put("visitor", globalScope, visitor.getERSClass());
					}
					if(team != null){
						globalScope.put("team", globalScope, team.getERSClass());
					}
					cx.evaluateString(globalScope, string, "Script", 1, null);
					return true;
				}
			};
			ExecutorService executorService = Executors.newCachedThreadPool();

			Future<Boolean> task = executorService.submit(callable);
			try{
				// ok, wait for 15 seconds max
				//Boolean result = task.get(15, TimeUnit.SECONDS);
				task.get(15, TimeUnit.SECONDS);
				if(scriptDebugging){match.Game.Base.Console.debug("Script is finished with completely");}
			}
			catch (ExecutionException t) {
				handleExecutionException(match.Game.Base, t, string, scriptName);
			}
			catch (TimeoutException e) {
				match.Game.Base.Console.warning("Script timeout...");
			}
			catch (InterruptedException e) {
				match.Game.Base.Console.warning("Script interrupted");
			}
		}
		return;
	}

	private void handleExecutionException(Base Base, ExecutionException t, String script, String scriptName){
		if( t.getCause() instanceof WrappedException) {
			WrappedException e = (WrappedException) t.getCause();
			String msg = "Script "+e.getClass().getName()+" from "+scriptName+"...: "+e.getMessage();
			Base.Console.warning(msg);
			Base.Console.warning(".... on line "+e.lineNumber()+": "+e.lineSource());
			Base.Console.warning(script);
			//match.send(CmdCompile.genericPopup(msg));
			//TODO need to 'wrap' msg to fit window
			e.printStackTrace();//Don't want this spamming the Console
		}
		else{
			String msg = "Script "+t.getClass().getName()+" from "+scriptName+"...: "+t.getMessage();
			Base.Console.warning(msg);
			//match.Game.Base.Console.warning(msg);
			t.printStackTrace();//Don't want this spamming the Console
		}
	}
	private static Context createContext(){
		Context cx = Context.enter();
		cx.setClassShutter(new ClassShutter(){
			public boolean visibleToScripts(String className){
					if (className.startsWith("com.inverseinnovations.eMafiaServer.")|| className.startsWith("java.lang.")) {
						return true;
					}
					return false;
			}
		});
		return cx;
	}

}
