package com.developi.toolbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.MIMEEntity;
import lotus.domino.MIMEHeader;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.Stream;

import com.ibm.commons.util.StringUtil;
import com.ibm.xsp.application.ApplicationEx;
import com.ibm.xsp.extlib.util.ExtLibUtil;

public final class DevelopiUtils {

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
	 * 	 recycles multiple domino objects (thx Nathan T. Freeman)
	 *		
	 * @param objs
	 * 
	 */
	public static void recycleObjects(lotus.domino.Base... objs) {
		for ( lotus.domino.Base obj : objs ) 
			recycleObject(obj);
	}

	
	// MIMEBean methods

	/**
	 * Restore state. Imported from org.openntf.domino
	 * 
	 * @param doc
	 *            the doc
	 * @param itemName
	 *            the item name
	 * @return the serializable
	 * @throws Throwable
	 *             the throwable
	 */
	@SuppressWarnings("unchecked")
	public static Object restoreState(Document doc, String itemName) throws Throwable {
		Session session=doc.getParentDatabase().getParent();
		boolean convertMime = session.isConvertMime();
		session.setConvertMime(false);

		Object result = null;
		MIMEEntity entity = doc.getMIMEEntity(itemName);

		if(null==entity) return null;
		
		Stream mimeStream = session.createStream();
		entity.getContentAsBytes(mimeStream);

		ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
		mimeStream.getContents(streamOut);
		recycleObject(mimeStream);

		byte[] stateBytes = streamOut.toByteArray();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(stateBytes);
		ObjectInputStream objectStream;
		if (entity.getHeaders().toLowerCase().contains("content-encoding: gzip")) {
			GZIPInputStream zipStream = new GZIPInputStream(byteStream);
			objectStream = new ObjectInputStream(zipStream);
		} else {
			objectStream = new ObjectInputStream(byteStream);
		}

		// There are three potential storage forms: Externalizable, Serializable, and StateHolder, distinguished by type or header
		if(entity.getContentSubType().equals("x-java-externalized-object")) {
			Class<Externalizable> externalizableClass = (Class<Externalizable>)Class.forName(entity.getNthHeader("X-Java-Class").getHeaderVal());
			Externalizable restored = externalizableClass.newInstance();
			restored.readExternal(objectStream);
			result = restored;
		} else {
			Object restored = (Serializable) objectStream.readObject();

			// But wait! It might be a StateHolder object or Collection!
			MIMEHeader storageScheme = entity.getNthHeader("X-Storage-Scheme");
			MIMEHeader originalJavaClass = entity.getNthHeader("X-Original-Java-Class");
			if(storageScheme != null && storageScheme.getHeaderVal().equals("StateHolder")) {
				Class<?> facesContextClass = Class.forName("javax.faces.context.FacesContext");
				Method getCurrentInstance = facesContextClass.getMethod("getCurrentInstance");

				Class<?> stateHoldingClass = (Class<?>)Class.forName(originalJavaClass.getHeaderVal());
				Method restoreStateMethod = stateHoldingClass.getMethod("restoreState", facesContextClass, Object.class);
				result = stateHoldingClass.newInstance();
				restoreStateMethod.invoke(result, getCurrentInstance.invoke(null), restored);
			} else {
				result = restored;
			}
		}


		recycleObject(entity);

		session.setConvertMime(convertMime);

		return result;
	}

	/**
	 * Save state. Imported from org.openntf.domino
	 * 
	 * @param object
	 *            the object
	 * @param doc
	 *            the doc
	 * @param itemName
	 *            the item name
	 * @throws Throwable
	 *             the throwable
	 */
	public static void saveState(Serializable object, Document doc, String itemName) throws Throwable {
		saveState(object, doc, itemName, true, null);
	}

	/**
	 * Save state. Imported from org.openntf.domino
	 * 
	 * @param object
	 *            the object
	 * @param doc
	 *            the doc
	 * @param itemName
	 *            the item name
	 * @param compress
	 *            the compress
	 * @throws Throwable
	 *             the throwable
	 */
	public static void saveState(Serializable object, Document doc, String itemName, boolean compress, Map<String, String> headers) throws Throwable {
		Session session=doc.getParentDatabase().getParent();
		boolean convertMime = session.isConvertMime();
		session.setConvertMime(false);

		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = compress ? new ObjectOutputStream(new GZIPOutputStream(byteStream)) : new ObjectOutputStream(
				byteStream);
		String contentType = null;

		// Prefer externalization if available
		if(object instanceof Externalizable) {
			((Externalizable)object).writeExternal(objectStream);
			contentType = "application/x-java-externalized-object";
		} else {
			objectStream.writeObject(object);
			contentType = "application/x-java-serialized-object";
		}

		objectStream.flush();
		objectStream.close();

		Stream mimeStream = session.createStream();
		MIMEEntity previousState = doc.getMIMEEntity(itemName);
		MIMEEntity entity = previousState == null ? doc.createMIMEEntity(itemName) : previousState;
		ByteArrayInputStream byteIn = new ByteArrayInputStream(byteStream.toByteArray());
		mimeStream.setContents(byteIn);
		entity.setContentFromBytes(mimeStream, contentType, MIMEEntity.ENC_NONE);
		MIMEHeader contentEncoding = entity.getNthHeader("Content-Encoding");
		if (compress) {
			if (contentEncoding == null) {
				contentEncoding = entity.createHeader("Content-Encoding");
			}
			contentEncoding.setHeaderVal("gzip");
			contentEncoding.recycle();
		} else {
			if (contentEncoding != null) {
				contentEncoding.remove();
				contentEncoding.recycle();
			}
		}
		MIMEHeader javaClass = entity.getNthHeader("X-Java-Class");
		if (javaClass == null) {
			javaClass = entity.createHeader("X-Java-Class");
		}
		javaClass.setHeaderVal(object.getClass().getName());
		javaClass.recycle();

		if(headers != null) {
			for(Map.Entry<String, String> entry : headers.entrySet()) {
				MIMEHeader paramHeader = entity.getNthHeader(entry.getKey());
				if(paramHeader == null) {
					paramHeader = entity.createHeader(entry.getKey());
				}
				paramHeader.setHeaderVal(entry.getValue());
				paramHeader.recycle();
			}
		}

		entity.recycle();
		mimeStream.recycle();

		session.setConvertMime(convertMime);
	}

	@SuppressWarnings("unchecked")
	public static Vector<?> toVector(Collection<?> collection) {
		Vector v=new Vector();
		v.addAll(collection);
		return v;
	}
	
	public static <T> List<T> uniqueList(List<T> list) {
		Vector<T> v=new Vector<T>();
		for(T obj: list ) {
			if(! v.contains(obj)) {
				v.add((T) obj);
			}
		}
		return v;
	}

	public static String getXspProperty(String propertyName, String defaultValue) {
		String retVal = ApplicationEx.getInstance().getApplicationProperty(propertyName, defaultValue);
		return retVal;
	}

	public static int getXspProperty(String propertyName, int defaultValue) {
		String xspValue=DevelopiUtils.getXspProperty(propertyName, "");
		int value=0;
		try {
			value=Integer.parseInt(xspValue);
		} catch(NumberFormatException e) {
			value=defaultValue;
		}
		return value;
	}

	/**
	 * Check if the list contains the member. For objects, it uses standard .equals() method. 
	 * For strings, it compares but ignores the case.
	 * 
	 * @param list
	 * @param member
	 * @return
	 */
	
	public static boolean contains(List<? extends Object> list, Object member) {
		if(list.isEmpty() || member==null) return false;
		
		for(Object obj:list) {
			if(obj instanceof String && member instanceof String) {
				if(compareIgnoreCase((String)obj, (String)member)) return true;
			} else {
				if(member.equals(obj)) return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Compares two lists. Caution: Lists can hold same values multiple times.
	 * 
	 * @param list1
	 * @param list2
	 * @return -1 if no common elements or any of lists are empty/null.
	 * 			0 if two lists have the same contents.
	 * 			x (x>0) if there are x elements in common. 
	 */
	
	public static int compareLists(List<? extends Object> list1, List<? extends Object> list2) {
		if(null==list1 || null==list2 || list1.isEmpty() || list2.isEmpty()) {
			return -1;
		}
		
		int count=0;
		
		for(Object o: list1) {
			if(list2.contains(o)) count++;
		}
		
		return count;
		
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

	public static String getEffectiveUserName() {
		String userName="";
		
		try {
			userName=ExtLibUtil.getCurrentSession().getEffectiveUserName();
		} catch (NotesException e) {
			// Not supposed to be here!
			e.printStackTrace();
		}

		return userName;
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

	public static String getCommonServerName() {
		Session session=ExtLibUtil.getCurrentSession();
		Database db=ExtLibUtil.getCurrentDatabase();
		
		String serverName="";

		try {
			serverName = db.getServer();
		} catch (NotesException e) { }
		
		return toCommon(session, serverName);
		
	}


	
	
}
