package com.developi.ic14.extensions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;

import com.developi.toolbox.DevelopiUtils;
import com.ibm.commons.util.StringUtil;
import com.ibm.sbt.security.credential.store.BaseStore;
import com.ibm.sbt.security.credential.store.CredentialStoreException;
import com.ibm.xsp.extlib.util.ExtLibUtil;

public class MemoryStore extends BaseStore {

	/**
	 * This is a modified MemoryStore implementation for persistence, also featured in XSnippets.
	 * 
	 * See: http://openntf.org/XSnippets.nsf/snippet.xsp?id=nsf-cached-credential-store-for-ibm-social-business-toolkit
	 * 
	 */
	
	private Map<String,byte[]> map = new HashMap<String,byte[]>();
	
	public MemoryStore() {
		loadStore();
	}
	
	public Object load(String service, String type, String user) throws CredentialStoreException {
		String application = findApplicationName();
		String key = createKey(application, service, type, user);
		return deSerialize(map.get(key));
	}

	public void store(String service, String type, String user, Object credentials) throws CredentialStoreException {
		String application = findApplicationName();
		String key = createKey(application, service, type, user);

		map.put(key, serialize(credentials) );
		saveStore();
	}

	public void remove(String service, String type, String user) throws CredentialStoreException {
		String application = findApplicationName();
		String key = createKey(application, service, type, user);
		System.out.println("removing: "+key);
		map.remove(key);
		saveStore();
	}
	
	/**
	 * Create a key for the internal map.
	 */
	protected String createKey(String application, String service, String type, String user) throws CredentialStoreException {
		StringBuilder b = new StringBuilder(128);
		b.append(StringUtil.getNonNullString(application));
		b.append('|');
		b.append(StringUtil.getNonNullString(service));
		b.append('|');
		b.append(StringUtil.getNonNullString(type));
		b.append('|');
		b.append(StringUtil.getNonNullString(user));
		return b.toString();
	}
	
	@SuppressWarnings("unchecked")
	protected void loadStore() {
		Document doc=getStoreDoc();
		
		try {
			Object obj=DevelopiUtils.restoreState(doc, "CredStore");
			
			if(obj instanceof Map) {
				Iterator it = ((Map)obj).entrySet().iterator();
			    while (it.hasNext()) {
			        Map.Entry pairs = (Map.Entry)it.next();
			        if(pairs.getValue() instanceof byte[]) {
				        map.put((String)pairs.getKey(), (byte[])pairs.getValue());
			        }
			        it.remove(); // avoids a ConcurrentModificationException
			    }
			}
			
		} catch (Throwable t) {
			System.out.println("Error loading tokenstore: "+t.getMessage());
			t.printStackTrace(System.err);
		} finally {
			DevelopiUtils.recycleObject(doc);
		}
		
		System.out.println("Token store loaded...");
	}

	protected void saveStore() {
		Document doc=getStoreDoc();
		
		try {
			DevelopiUtils.saveState((HashMap<String,byte[]>)map, doc, "CredStore");
			doc.save();
		} catch(Throwable t) {
			System.out.println("Error saving tokenstore: "+t.getMessage());
			t.printStackTrace(System.err);
		} finally {
			DevelopiUtils.recycleObject(doc);
		}
		
	}	
	
	private Document getStoreDoc() {
		Database database=ExtLibUtil.getCurrentDatabase();
				
		Document doc=null;
		View view=null;

		try {
			view=database.getView("tokenstores");
			String serverName=database.getServer();
			
			doc=view.getDocumentByKey(serverName, true);
			
			if(doc==null) {
				doc=database.createDocument();
				doc.replaceItemValue("Form", "tokenstore");
				doc.replaceItemValue("ServerName", serverName);
				doc.computeWithForm(false, false);
				doc.save();
			}
			
		} catch (NotesException e) {
			e.printStackTrace();
		} finally {
			DevelopiUtils.recycleObject(view);
		}

		return doc;
	}
	
	
}
