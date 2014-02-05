package com.developi.ic14.dots;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.Item;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;

import com.developi.ic14.dots.util.Logger;
import com.developi.ic14.dots.util.TaskletLogger;
import com.developi.ic14.dots.util.Utilities;
import com.ibm.commons.runtime.Application;
import com.ibm.commons.runtime.Context;
import com.ibm.commons.runtime.RuntimeFactory;
import com.ibm.commons.runtime.impl.app.RuntimeFactoryStandalone;
import com.ibm.commons.runtime.util.ParameterProcessor;
import com.ibm.commons.util.StringUtil;
import com.ibm.commons.util.io.json.JsonException;
import com.ibm.commons.util.io.json.JsonGenerator;
import com.ibm.commons.util.io.json.JsonJavaFactory;
import com.ibm.commons.util.io.json.JsonJavaObject;
import com.ibm.commons.util.io.json.JsonParser;
import com.ibm.sbt.services.client.connections.activitystreams.ASApplication;
import com.ibm.sbt.services.client.connections.activitystreams.ASGroup;
import com.ibm.sbt.services.client.connections.activitystreams.ASUser;
import com.ibm.sbt.services.client.connections.activitystreams.ActivityStreamService;
import com.ibm.sbt.services.client.connections.activitystreams.ActivityStreamServiceException;
import com.ibm.sbt.services.client.connections.profiles.Profile;
import com.ibm.sbt.services.client.connections.profiles.ProfileList;
import com.ibm.sbt.services.client.connections.profiles.ProfileService;
import com.ibm.sbt.services.client.connections.profiles.ProfileServiceException;
import com.ibm.sbt.services.endpoints.BasicEndpoint;
import com.ibm.sbt.services.endpoints.ConnectionsBasicEndpoint;


public enum NotificationManager {
	INSTANCE;

	private Logger logger;

	private String dbName;

	private boolean ready;
	private boolean socialized;
	
	private Map<String, String> templates;   // Mapping template codes >>> templates
	private Map<String, String> templateCommunities;   // Mapping template codes >>> Community IDs
	private Properties config;

	private BasicEndpoint endpoint;
	RuntimeFactory runtimeFactory;
	Context context;
	Application application;

	private Map<String, String> profileIds; // Mapping common names >>> profile ids for cache 
	
	public void setLogger(Logger _logger) {
		logger=_logger;
	}

	NotificationManager() {
		
		// We have two types of configuration here. Some can be reloaded.
		// In case of reload, we don't want to clear the cache.
		// This will not be cleared throughoutt the lifetime.
		
		profileIds=new HashMap<String, String>();
		ready=false;
		socialized=false;
		dbName="";
		logger=new TaskletLogger("NM", Logger.ALL_MESSAGES);
		
		endpoint=null;
		runtimeFactory=new RuntimeFactoryStandalone();
		context=null;
		application=null;
	}
	
	/**
	 * @return the ready
	 */
	public boolean isReady() {
		return ready;
	}
	
	public BasicEndpoint getEndpoint() {
		return endpoint;
	}

	/**
	 * Staging database can be determined by a notes.ini parameter. If not defined, the default one will be used 	
	 * from Constants class.
	 */
	private Database getStagingDB(Session session) {
		try {
			if(dbName.equals("")) {
				dbName = session.getEnvironmentString(Constants.NOTES_INI_PARAMETER_NAME, true);
				if(null == dbName || dbName.equals("")) {
					dbName=Constants.DATABASE_FILENAME;
				}
				logger.log("Using database '" + dbName + "'");
			}
			
			Database stagingDB = session.getDatabase("", dbName );
			
			if(! stagingDB.isOpen()) {
				stagingDB.open();
			}
			
			return stagingDB;
			
		} catch (NotesException e) {
			logger.printStack(e);
			throw new RuntimeException("Unable to open News database!");
		}
		
	}
	
	public void init(Session session, Logger logger) {
		setLogger(logger);
		init(session);
	}
	
	public void init(Session session) {
		clearConfig();
		
		Database stagingDb=null;		
		View configView=null;
		DocumentCollection templateDocs=null;
		Document configDoc=null;
		
		try {
			stagingDb=getStagingDB(session);
			configView=stagingDb.getView(Constants.VIEW_CONFIG);
			
			configDoc=configView.getDocumentByKey(Constants.FORM_PARAMS, true);
			
			if(configDoc==null) {
				throw new RuntimeException("No configuration document found!");
			}
			
			config.setProperty("cnxServerURL", configDoc.getItemValueString("cnxServerURL"));
			config.setProperty("cnxUserName", configDoc.getItemValueString("cnxUserName"));
			config.setProperty("cnxPassword", configDoc.getItemValueString("cnxPassword"));
			
			templateDocs=configView.getAllDocumentsByKey(Constants.FORM_TEMPLATES, true);
			
			Document templateDoc=templateDocs.getFirstDocument();
			
			while(templateDoc!=null) {
				
				String code=templateDoc.getItemValueString("Code");
				String communityId=templateDoc.getItemValueString("CommunityId");
				String content=templateDoc.getItemValueString("Content");
				
				templates.put(code, content);

				if(StringUtil.isNotEmpty(communityId)) {
					templateCommunities.put(code, communityId);
				}
				
				Document tmpDoc=templateDocs.getNextDocument(templateDoc);
				Utilities.recycleObject(templateDoc);
				templateDoc=tmpDoc;
			}
			
			this.ready=true;
		} catch(NotesException ne) {
			logger.printStack(ne);
			logger.error("Unable to initialize Notification Manager");
		} finally {
			Utilities.recycleObjects(stagingDb, configView, templateDocs, configDoc);
		}
		
	}
	
	public void monitorNewEntries(Session session) {
		if(!isReady()) {
			logger.error("Monitor New Entries will not work until all set!");
			return;
		}
		
		Database stagingDb=null;		
		View pendingView=null;
		ViewEntryCollection entries=null;
		
		try {
			startSocial();

			stagingDb=getStagingDB(session);
			pendingView=stagingDb.getView(Constants.VIEW_PENDING);
			
			entries=pendingView.getAllEntries();
			
			int count=entries.getCount();
			
			for(int i=1; i<=count; i++) {
				ViewEntry entry=entries.getNthEntry(i);
				Document doc=entry.getDocument();
				
				if(entry.isDocument() && !entry.isConflict()) {
					processDocument(session, doc);
				}
				
				Utilities.recycleObjects(entry, doc);
			}
			
			if(count>0) logger.log(count + " documents processed in the pending requests.");
			
		} catch(NotesException ne) {
			logger.printStack(ne);
			logger.error("Unable to process pending requests...");
		} finally {
			Utilities.recycleObjects(stagingDb, pendingView, entries);
			stopSocial();
		}
		
	}
	
	private void processDocument(Session session, Document doc) {

		try {
			String templateCode=doc.getItemValueString("templateCode");
	
			Properties props=new Properties();
			
			Vector<?> items=doc.getItems(); 
			
			for(Object itemObj: items) {
				if(! (itemObj instanceof Item)) continue; //Paranoid check 
				
				Item item=(Item)itemObj;
				
				String name=item.getName();
				String value="";
				
				if(! name.startsWith("$")) { //eliminate special fields
					
					switch(item.getType()) {
					
					// XXX: Multivalue support?
					// XXX: Dangerous characters?
					case Item.TEXT:
						value=item.getText();
						break;
					
					case Item.DATETIMES:
						DateTime dt=item.getDateTimeValue();
						Date dateValue=dt.toJavaDate();
						try {
							value=JsonGenerator.dateToString(dateValue); // What about timezone?
						} catch (IOException e) { } 
						Utilities.recycleObject(dt);
						break;
						
					case Item.NUMBERS:
						value=String.valueOf(item.getValueInteger()); // What about double?
						break;
						
					case Item.NAMES:
						value=Utilities.toCommon(session, item.getValueString());
						if(StringUtil.isNotEmpty(value)) {
							String userId=getProfileId(value);
							props.setProperty(name+".id", userId);
							name+=".displayName"; // any names value will generate two properties: XYZ.id and XYZ.displayName
						}
						break;
						
						
					default: 
						value=item.getValueString();
					}
					
					props.setProperty(name, value);
				}
			}
			
			doc.recycle(items);
			
			String jsonTemplate=createJsonEntry(templateCode, props);

			logger.debug(props);
			
			String result=postActivityStream(templateCode, jsonTemplate);
			
			if(result.startsWith("Error:")) {
				doc.replaceItemValue("$ErrorMessage", result);
			} else {
				doc.replaceItemValue("$Result", result);
			}

			doc.replaceItemValue("Pending", "0");

			doc.save();
			
		} catch(NotesException ne) {
			logger.printStack(ne);
			logger.error("Unable process a pending request.");
		} 
	}

	private String postActivityStream(String templateCode, String jsonTemplate) {
		
		String result="";
		if(socialized) {

			ActivityStreamService asService=new ActivityStreamService(endpoint);

			try {
				JsonJavaObject data = (JsonJavaObject)JsonParser.fromJson(JsonJavaFactory.instanceEx, jsonTemplate);

				String communityId=templateCommunities.get(templateCode);
								
				String user=StringUtil.isEmpty(communityId)? ASUser.ME.getUserType() : ASUser.COMMUNITY.getUserType()+communityId;
				String group=ASGroup.ALL.getGroupType();
				String app=ASApplication.ALL.getApplicationType();
				
				logger.debug(user+"/"+group+"/"+app);
				logger.debug(jsonTemplate);
				
				return asService.postEntry(user, group, app, data);
			
			} catch (ActivityStreamServiceException e) {
				result="Unable to post entry: "+e.getMessage();
				logger.debug(jsonTemplate);
			} catch (JsonException e) {
				result="Unable to parse JSON: "+e.getMessage();
				logger.debug(jsonTemplate);
			}

		} else {
			result="Unable to post the entry... Not socialized yet!";
		}

		logger.error(result);
		return "Error: "+result;
	}

	private String getProfileId(String userName) {
		String userId=profileIds.get(userName);
		
		if(StringUtil.isEmpty(userId)) {
			logger.debug("UserId for "+userName+ " not in the cache... Searching...");
			userId=receiveProfileId(userName);			
		}

		return userId;
	}

	private String receiveProfileId(String userName) {
		String userId="";
		
		if(socialized) {
			ProfileService profileService=new ProfileService(endpoint);

			Map<String, String> parameters=new HashMap<String, String>();
			parameters.put("name", userName);

			try {
				ProfileList profiles=profileService.searchProfiles(parameters);

				if(profiles != null && ! profiles.isEmpty()) {
					Profile profile=profiles.get(0);
					userId=profile.getUserid();
					logger.debug("Found UserId for "+userName+ ": "+userId);
					profileIds.put(userName, userId);
				} else {
					logger.error("Unable to receive profile for "+userName+"... No match found!");
				}
			} catch (ProfileServiceException e) {
				logger.error("Unable to receive profile for "+userName+"... Error from the service!");
				logger.printStack(e);
			}
			
		} else {
			logger.error("Unable to receive profile for "+userName+"... Not socialized yet!");
		}
		
		return userId;
	}

	private String createJsonEntry(String templateCode, Properties props) {
		String jsonTemplate=getJsonTemplate(templateCode);
		
		if(StringUtil.isEmpty(jsonTemplate)) return null;
		
		jsonTemplate = ParameterProcessor.process(jsonTemplate, props);

		return jsonTemplate;		
	}

	private String getJsonTemplate(String templateCode) {
		if(templates.containsKey(templateCode)) {
			return templates.get(templateCode);
		} 
		
		return templates.get(Constants.DEFAULT_ASTEMPLATE_CODE);
	}

	private void clearConfig() {
		ready=false;
		templates=new HashMap<String, String>();
		templateCommunities=new HashMap<String, String>();
		config=new Properties();
	}
	
	public void startSocial() {

		application=runtimeFactory.initApplication(null);
		context = Context.init(application, null, null);
		
		if(endpoint==null) {
			endpoint= new ConnectionsBasicEndpoint();

			endpoint.setUrl(config.getProperty("cnxServerURL"));
			endpoint.setForceTrustSSLCertificate(true);

			try {
				// SDK has a bug here. Within DOTS, it does throw NoClassDefFoundError.
				// Because it tries to redirect the client to the login form.
				// We can't catch this error, so pray for a correct password!

				String userName=config.getProperty("cnxUserName");
				String password=config.getProperty("cnxPassword");

				endpoint.login(userName, password);
			} catch (Throwable t) {
				// This will not happen...
				t.printStackTrace();
			}
		}
		
		socialized=true;
	}
	
	public void stopSocial() {
		if (context != null)
			Context.destroy(context);
		if (application != null)
			Application.destroy(application);
		
		socialized=false;
	}
	
	
}
