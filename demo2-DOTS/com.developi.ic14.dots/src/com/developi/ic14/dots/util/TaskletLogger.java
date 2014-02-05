package com.developi.ic14.dots.util;

import com.ibm.dots.task.ServerConsole;

// TODO: Add OpenLog functionality

public class TaskletLogger extends Logger {

	ServerConsole console;
	String appName="";
	
	public TaskletLogger(String appName, int mode) {
		super("", mode);
		this.appName=appName;
		console=new ServerConsole(appName+":");
	}

	@Override
	public void log(Object obj) {
		if(checkMode(LOG_MESSAGES)) {
			console.logMessage("(LOG) "+formatMessage(obj.toString()));
		}
	}

	@Override
	public void error(Object obj) {
		if(checkMode(ERROR_MESSAGES)) {
			console.logMessage("(ERROR) "+formatMessage(obj.toString()));
		}
	}
	
	@Override
	public void debug(Object obj) {
		if(checkMode(DEBUG_MESSAGES)) {
			console.logMessage("(DEBUG) "+formatMessage(obj.toString()));
		}
	}

	public void printStack(Throwable t) {
		console.logException(t);
	}

	public String formatMessage(String message) {
		return message;
	}
	
}
