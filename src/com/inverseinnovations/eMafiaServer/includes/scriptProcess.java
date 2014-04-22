/* eMafiaServer - scriptProcess.java
GNU GENERAL PUBLIC LICENSE V3*/
package com.inverseinnovations.eMafiaServer.includes;

import java.util.concurrent.*;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;

import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Match;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Role;
import com.inverseinnovations.eMafiaServer.includes.classes.GameObjects.Team;
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

					//js.put("match", match.getERSClass());
					globalScope.put("match", globalScope, match.getERSClass());
					if(role != null){
						//js.put("self", role.getERSClass());
						globalScope.put("self", globalScope, role.getERSClass());
					}
					if(visitor != null){
						//js.put("visitor", visitor.getERSClass());
						globalScope.put("visitor", globalScope, visitor.getERSClass());
					}
					if(team != null){
						//js.put("team", team.getERSClass());
						globalScope.put("team", globalScope, team.getERSClass());
					}
					//js.eval(string);
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
				if(scriptDebugging){match.Game.Base.Console.debug("Script finished with completely");}
			}/*
			catch( Throwable t ){
				if( t instanceof ExecutionException ) {
		            t = t.getCause();
		        }
				if( t instanceof WrappedException) {
					WrappedException e = (WrappedException) t;
					String msg = "Script "+e.getClass().getName()+" from "+scriptName+"...: "+e.getMessage();e.g
					match.Game.Base.Console.warning(msg);
					e.printStackTrace();//Don't want this spamming the Console
					match.Game.Base.Console.warning("Caused by "+e.getCause().getClass().getName());
		            throw (RuntimeException)t;
		        }
			}*/
			/*catch (WrappedException e) {
				//String theScriptor = "Unknown";
				String msg = "Script "+e.getClass().getName()+" from "+scriptName+"...: "+e.getMessage();
				match.Game.Base.Console.warning(msg);
				match.Game.Base.Console.warning(".... on line "+e.lineNumber()+": "+e.lineSource());
				match.send(CmdCompile.genericPopup(msg));
				//TODO need to 'wrap' msg to fit window
				e.printStackTrace();//Don't want this spamming the Console
			}*/
			catch (ExecutionException t) {
				if( t.getCause() instanceof WrappedException) {
					WrappedException e = (WrappedException) t.getCause();
					String msg = "Script "+e.getClass().getName()+" from "+scriptName+"...: "+e.getMessage();
					match.Game.Base.Console.warning(msg);
					match.Game.Base.Console.warning(".... on line "+e.lineNumber()+": "+e.lineSource());
					match.Game.Base.Console.warning(string);
					match.send(CmdCompile.genericPopup(msg));
					//TODO need to 'wrap' msg to fit window
					e.printStackTrace();//Don't want this spamming the Console
				}
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
