/* eMafiaServer - Console.java
GNU GENERAL PUBLIC LICENSE V3
Copyright (C) 2012  Matthew 'Apocist' Davis */
package com.inverseinnovations.eMafiaServer.includes.classes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.inverseinnovations.eMafiaServer.*;
import com.inverseinnovations.eMafiaServer.includes.Constants;
/**Textpane to accept html syntex*/
class HtmlPane extends JTextPane {
	private static final long serialVersionUID = 1L;
	public HTMLEditorKit kit = new HTMLEditorKit();
	public HTMLDocument doc = new HTMLDocument();
	public String lastMsg;

	/**Textpane to accept html syntex*/
	public HtmlPane(){
		setEditorKit(kit);
		setDocument(doc);
		setBackground(new Color(240,240,240));
		DefaultCaret caret = (DefaultCaret)this.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);//this will make it auto scroll to bottom
	}
	private String calcDate(long millisecs) {
		SimpleDateFormat date_format = new SimpleDateFormat("yyMMdd HH:mm:ss");
		Date resultdate = new Date(millisecs);
		return date_format.format(resultdate);
	}
	public void saveLog(){
		saveLog("log.html");
	}
	public void saveLog(String location){
		FileWriter writer = null;
		try {
			writer = new FileWriter(location);
			kit.write(writer, doc, 0, doc.getLength());
		}
		catch (IOException | BadLocationException e) {
			printStackTrace(e);
		}
		finally {
			try {
				writer.close();
			}
			catch (IOException e) {
				printStackTrace(e);
			}
		}
	}
	public void append(String s){
		try {
			//lastMsg = s;
			//System.out.print(s);
			kit.insertHTML(doc, doc.getLength(), s+"<br>", 0, 0, null);
			//this.select(this.getHeight(),0);
		}
		catch (BadLocationException e) {}
		catch (IOException e) {}
	}
	public void printStackTrace(Exception e){
		//e.printStackTrace();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace();
		e.printStackTrace(pw);
		append("<font color=\"red\">&nbsp;&nbsp;&nbsp;&nbsp;"+(sw.getBuffer().toString()).replace("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"</font>");
	}
	public void severe(String s){
		String add = calcDate(System.currentTimeMillis())+" !! "+s+" !!";
		System.out.println(add);
		append("<font color =\"red\"><b>"+add+"</b></font>");
	}
	public void warning(String s){
		String add = calcDate(System.currentTimeMillis())+" "+s;
		System.out.println(add);
		append("<font color =\"orange\">"+add+"</font>");
	}
	public void config(String s){
		String add = calcDate(System.currentTimeMillis())+" === "+s+" ===";
		System.out.println(add);
		append("<font color =\"blue\">"+add+"</font>");
	}
	public void info(String s){
		String add = calcDate(System.currentTimeMillis())+" "+s;
		System.out.println(add);
		append(add);
	}
	public void fine(String s){
		String add = calcDate(System.currentTimeMillis())+" "+s;
		System.out.println(add);
		append("<font color =\"gray\">"+add+"</font>");
	}
	public void debug(String s){
		String add = calcDate(System.currentTimeMillis())+" DEBUG: "+s;
		System.out.println(add);
		append("<font color =\"gray\">"+add+"</font>");
	}
}
/**Server side display and logging module*/
public class Console extends Frame{
	private static final long serialVersionUID = 1L;
	public Base Base;
	public HtmlPane output = new HtmlPane();

	/**Server side display and logging module*/
	public Console(Base base){
		super("eMafia Server "+Constants.VERSION);//client title
		this.Base = base;
		setSize(800,600);//client window size
		//setLocationRelativeTo(null);
		JScrollPane scrollpane = new JScrollPane(output);
		add(scrollpane);

		addWindowListener(
			new WindowAdapter(){
				public void windowClosing(WindowEvent e){
					warning("Server closed insecurely by admin clicking X, be sure to use proper methods next time.");
					Base.shutdown();
				}
			}
		);
		setVisible(true);
	}
	/**
	 * Saves the current Console to an html file - log.html
	 */
	public void saveLog(){
		output.saveLog();
	}
	/**
	 * Saves the current Console to an html file named as (location)
	 * @param location filename
	 */
	public void saveLog(String location){
		output.saveLog(location);
	}
	/**Log stackTraces, replaces standard printStackTrace
	 * @param e Exception*/
	public void printStackTrace(Exception e){
		output.printStackTrace(e);
	}
	/**Log server breaking errors */
	public void severe(String s){
		output.severe(s);
	}
	/**Log potential problems*/
	public void warning(String s){
		output.warning(s);
	}
	/**Log major changes to server and server startup*/
	public void config(String s){
		output.config(s);
	}
	/**Log normal information*/
	public void info(String s){
		if(Base.Settings.LOGGING >= 1){output.info(s);}
	}
	/**Log detail information that is unessary*/
	public void fine(String s){
		if(Base.Settings.LOGGING >= 2){output.fine(s);}
	}
	/**Log senseitive info for development*/
	public void debug(String s){
		if(Base.Settings.LOGGING >= 3){output.debug(s);}
	}
}
