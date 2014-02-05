package com.developi.ic14.dots.tasklet;

import lotus.domino.NotesException;

import org.eclipse.core.runtime.IProgressMonitor;

import com.developi.ic14.dots.NotificationManager;
import com.ibm.dots.annotation.HungPossibleAfter;
import com.ibm.dots.annotation.RunEvery;
import com.ibm.dots.annotation.RunOnStart;
import com.ibm.dots.task.AbstractServerTaskExt;
import com.ibm.dots.task.RunWhen;
import com.ibm.dots.task.RunWhen.RunUnit;

public class Automation extends AbstractServerTaskExt {

	public Automation() {
	}

	@Override
	public void dispose() throws NotesException {
	}

	@Override
	protected void doRun(RunWhen runWhen, IProgressMonitor monitor) throws NotesException {
				
	}

	@RunOnStart
	public void initialize(String[] args, IProgressMonitor monitor) {
		NotificationManager nm=NotificationManager.INSTANCE;
		nm.init(getSession());	
	}

	@RunEvery( every=15, unit=RunUnit.second )
	@HungPossibleAfter( timeInMinutes=1 )
	public void monitorNewEntries(IProgressMonitor monitor) {
		NotificationManager nm=NotificationManager.INSTANCE;
		nm.monitorNewEntries(getSession());
	}

//	@RunOnStart
//	public void test(String[] args, IProgressMonitor monitor) {
//		NotificationManager nm=NotificationManager.INSTANCE;
//		nm.init(getSession());	
//		nm.monitorNewEntries(getSession());
//	}

}
