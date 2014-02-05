package com.developi.ic14.dots.util;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.Session;

import com.ibm.commons.util.StringUtil;

public final class Utilities {

	private static String UNIQUEID_LETTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static int UNIQUEID_DEFAULTSIZE = 6;
	
	public static String generateUniqueId() {
		StringBuilder sb = new StringBuilder(UNIQUEID_DEFAULTSIZE);
		Random random = new Random();
		for (int i=0; i < UNIQUEID_DEFAULTSIZE; i++) {
			sb.append(UNIQUEID_LETTERS.charAt(random.nextInt(UNIQUEID_LETTERS.length())));
		}
	    return sb.toString().toLowerCase(Locale.US);            
	}


	public static Calendar getDateField(Document doc, String fieldName) throws NotesException {
		
		Calendar result=null;
		
		Item someItem=null;
		DateTime someDate=null;
		
		try {
			someItem=doc.getFirstItem(fieldName);
			if(null != someItem && someItem.getType()==Item.DATETIMES) {
				someDate=someItem.getDateTimeValue();
				result=Calendar.getInstance();
				result.setTime(someDate.toJavaDate());
			}
			return result;
		} catch(NotesException ne) {
			throw ne;
		} finally {
			recycleObjects(someItem, someDate);
		}		
		
	}

	
	/**
	 * recycles a domino document instance
	 * 
	 * @param lotus.domino.Base 
	 *           obj to recycle
	 * @category Domino
	 * @author Sven Hasselbach
	 * @category Tools
	 * @version 1.1
	 */
	public static void recycleObject(lotus.domino.Base obj) {
		if (obj != null) {
			try {
				obj.recycle();
			} catch (Exception e) {}
		}
	}

	/**
	 * 	 recycles multiple domino objects. Inspired by Nathan T. Freeman
	 *		
	 * @param objs
	 * 
	 */
	public static void recycleObjects(lotus.domino.Base... objs) {
		for ( lotus.domino.Base obj : objs ) 
			recycleObject(obj);
	}


	/**
	 * by Jesse Gallegher
	 * 
	 */
	
	public static String strLeft(String input, String delimiter) {
		return input.substring(0, input.indexOf(delimiter));
	}
	public static String strRight(String input, String delimiter) {
		return input.substring(input.indexOf(delimiter) + delimiter.length());
	}
	public static String strLeftBack(String input, String delimiter) {
		return input.substring(0, input.lastIndexOf(delimiter));
	}
	public static String strLeftBack(String input, int chars) {
		return input.substring(0, input.length() - chars);
	}
	public static String strRightBack(String input, String delimiter) {
		return input.substring(input.lastIndexOf(delimiter) + delimiter.length());
	}
	public static String strRightBack(String input, int chars) {
		return input.substring(input.length() - chars);
	}
	
	public static boolean compareIgnoreCase(String str1, String str2) {
		return str1.toLowerCase(Locale.ENGLISH).equals(str2.toLowerCase(Locale.ENGLISH));
	}
	
	public static boolean isName(String valueStr) {
		return valueStr.matches("CN=.*\\/O=.*");
	}
	

	public static String toCommon(Session session, String anyName) {
		if(StringUtil.isEmpty(anyName)) return "";
		
		Name nn=null;
		try {
			nn = session.createName(anyName);
			return nn.getCommon();	
		} catch (NotesException e) {
			// Not supposed to be here
		} finally {
			recycleObject(nn);
		}

		return "";
	}

}
