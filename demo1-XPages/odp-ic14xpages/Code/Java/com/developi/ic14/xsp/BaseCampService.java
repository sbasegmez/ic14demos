package com.developi.ic14.xsp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import com.developi.ic14.bc.File;
import com.developi.ic14.bc.Person;
import com.developi.ic14.bc.Project;
import com.developi.toolbox.DevelopiUtils;
import com.developi.toolbox.RestUtils;
import com.ibm.commons.util.StringUtil;
import com.ibm.commons.util.io.json.JsonJavaArray;
import com.ibm.commons.util.io.json.JsonJavaObject;
import com.ibm.sbt.security.authentication.AuthenticationException;
import com.ibm.sbt.security.authentication.oauth.consumer.OAuth2Handler;
import com.ibm.sbt.services.client.ClientService;
import com.ibm.sbt.services.client.ClientServicesException;
import com.ibm.sbt.services.client.Response;
import com.ibm.sbt.services.client.ClientService.Content;
import com.ibm.sbt.services.client.ClientService.ContentJson;
import com.ibm.sbt.services.client.ClientService.ContentStream;
import com.ibm.sbt.services.client.connections.files.AccessType;
import com.ibm.sbt.services.client.connections.files.FileServiceURIBuilder;
import com.ibm.sbt.services.client.connections.files.ResultType;
import com.ibm.sbt.services.client.connections.files.SubFilters;
import com.ibm.sbt.services.client.connections.files.model.Headers;
import com.ibm.sbt.services.endpoints.Endpoint;
import com.ibm.sbt.services.endpoints.EndpointFactory;
import com.ibm.sbt.services.endpoints.OAuth2Endpoint;
import com.ibm.xsp.extlib.util.ExtLibUtil;
import com.ibm.xsp.http.UploadedFile;

public class BaseCampService implements Serializable {

	/**
	 * Quick and Dirty implementation for Basecamp services.
	 */
	
	private static final long serialVersionUID = 1L;

	private final static String DEFAULT_ENDPOINT_NAME="basecamp";

	private String endpointName;
	private String currentServer; // common name of the current server
	
	private Map<String, Person> userMap; // authenticated users cached in a map.

	public BaseCampService() {
		currentServer=DevelopiUtils.getCommonServerName();
		setEndpointName(DEFAULT_ENDPOINT_NAME);
		userMap=new HashMap<String, Person>();
	}

	/** 
	 * Endpoint name is tricky. We had multiple servers, one for development and another for demo. Since OAUTH2 
	 * needs a seperate/constant callback url, we need to define different endpoints for each servers.
	 *   
	 */
	
	public void setEndpointName(String endpointName) {
		this.endpointName = endpointName+"_"+currentServer;
	}
	
	public Endpoint getEndpoint() {
		return EndpointFactory.getEndpoint(endpointName);
	}

	public Map<String, Person> getUserMap() {
		return userMap;
	}

	public void setUserMap(Map<String, Person> userMap) {
		this.userMap = userMap;
	}

	public synchronized Person getMe() {
		String userName=DevelopiUtils.getEffectiveUserName();

		Person me=userMap.get(userName);
		
		if(me==null) {
			me=new Person();
			userMap.put(userName, me);
		}
		
		try {
			if(me.isEmpty() && getEndpoint().isAuthenticated()) {
				me.setData(RestUtils.xhrGetJson(getEndpoint(), "/people/me.json"));
			}
		} catch (ClientServicesException e) {
			// So we are not able to determine authentication. Forget it.
		}

		return me; 
	}
	
	public void authenticate(boolean force) {
		try {
			getEndpoint().authenticate(force);
		} catch (ClientServicesException e) {
			System.out.println("Unable to Authenticate: "+e.getMessage());
		}
	}
	
	public boolean isAuthenticated() {
		try {
			return getEndpoint().isAuthenticated();
		} catch (ClientServicesException e) {
			System.out.println("Unable to determine authentication state: "+e.getMessage());
			return false;
		}
	}
	
	/**
	 * Tricky: This is a problem with the current SBT. When you logout, it deletes tokens from the endpoint.
	 * But since the handler class continues to keep the old token, it doesn't lose the authentication.
	 * 
	 */
	public void logout() {
		String userName=DevelopiUtils.getEffectiveUserName();
		userMap.remove(userName);
		try {
			OAuth2Endpoint endpoint=(OAuth2Endpoint)getEndpoint();
			OAuth2Handler oaHandler=endpoint.getHandler();
			oaHandler.deleteToken();
			oaHandler.setAccessToken(null);
			oaHandler.setAccessTokenObject(null);
		} catch (AuthenticationException e) { 
			System.out.println("Unable to logout: "+e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public List<Project> getProjectsCached() {
		List<Project> pL=(List<Project>)ExtLibUtil.getSessionScope().get("projectList");

		if(pL==null) {
			pL=getProjects();
			ExtLibUtil.getSessionScope().put("projectList", pL);
		}

		return pL;
	}
	
	public List<String> getProjectsForCombo() {
		List<Project> pL=getProjectsCached();
		List<String> options=new ArrayList<String>();
		
		for(Project p: pL) {
			options.add(p.getName()+"|"+p.getId());
		}
		
		return options;
	}
	
	public List<Project> getProjects() {
		List<Project> projects=new ArrayList<Project>();

		List<Object> data=RestUtils.xhrGetJsonList(getEndpoint(), "/projects.json");
		if(data!=null) {
			for(Object o: data) {
				if(o instanceof JsonJavaObject) {
					JsonJavaObject p=(JsonJavaObject)o; // This is just a place holder. 

					// We might receive entire project, or placeholder might be enough?
					//String pUrl=p.getAsString("url");
					//projects.add(new Project(RestUtils.xhrGetJson(getEndpoint(), pUrl)));
					projects.add(new Project(p));
				}
			}
		}
		return projects;
	}

	public Project getProjectCached(String id) {
		Project p=(Project)ExtLibUtil.getSessionScope().get("project"+id);

		if(p==null) {
			p=getProject(id);
			ExtLibUtil.getSessionScope().put("project"+id, p);
		}

		return p;
	}

	public Project getProject(String id) {
		if(id==null) return null;

		return new Project(RestUtils.xhrGetJson(getEndpoint(), "/projects/"+id+".json"));
	}

	@SuppressWarnings("unchecked")
	public List<File> getFilesCached(String pid) {
		Map<String, Object> scope=ExtLibUtil.getViewScope();

		List<File> files=(List<File>)scope.get("files"+pid);

		if(files==null) {
			files=getFiles(pid);
			scope.put("files"+pid, files);
		}

		return files;
	}

	public List<File> getFiles(String pid) {
		String serviceUrl="/attachments.json";

		if(StringUtil.isNotEmpty(pid)) {
			serviceUrl="/projects/"+pid+"/attachments.json";
		}

		List<File> files=new ArrayList<File>();

		List<Object> data=RestUtils.xhrGetJsonList(getEndpoint(), serviceUrl);

		if(data!=null) {
			for(Object o: data) {
				if(o instanceof JsonJavaObject) {
					JsonJavaObject f=(JsonJavaObject)o; // This is just a place holder. 

					files.add(new File(f));
				}
			}
		}
		return files;

	}

	/**
	 * Tip for SBT developers: this url pattern below can be used to make authenticated requests for the end user. 
	 */
	public String getDownloadUrl(String url) {
		Endpoint endpoint=getEndpoint();

		String baseUrl="xsp/.sbtservice/proxy/"+endpointName;
		try {
			URL fileUrl=new URL(url);
			URL apiUrl = new URL(endpoint.getUrl());

			String fileUri=fileUrl.getPath().replace(apiUrl.getPath(), "");
			return baseUrl+fileUri;
		} catch (Exception e) {}

		return "";
	}

	public String getDownloadUri(String url) {
		Endpoint endpoint=getEndpoint();

		try {
			URL fileUrl=new URL(url);
			URL apiUrl = new URL(endpoint.getUrl());

			String fileUri=fileUrl.getPath().replace(apiUrl.getPath(), "");
			return fileUri;
		} catch (Exception e) {}

		return "";
	}

	/**
	 * Uploading file into Basecamp is a two-step process. This is because files might be a part of anything (project, discussion, etc)
	 * 
	 * First, upload the file and receive a token (uploadAttachment() method)
	 * Second, label the token with some metadata and submit. 
	 * 
	 */
	
	public File uploadFile(String pid, String name, InputStream inputStream, String contentType) {
		Endpoint endpoint=getEndpoint();

		String token=uploadAttachment(name, inputStream, contentType);

		if(StringUtil.isEmpty(token)) {
			System.out.println("Unable to attach file!");
		} else {
			String uploadUrl="/projects/"+pid+"/uploads.json";

			JsonJavaObject uploadJson=new JsonJavaObject();
			JsonJavaArray attachmentJsonArray=new JsonJavaArray();
			JsonJavaObject attachmentJson=new JsonJavaObject();

			uploadJson.put("content", "Proudly uploaded via SBT SDK!");
			attachmentJson.put("token", token);
			attachmentJson.put("name", name);
			attachmentJsonArray.add(attachmentJson);
			uploadJson.put("attachments", attachmentJsonArray);

			Content contentJson=new ContentJson(uploadJson);

			Object data=RestUtils.xhrPost(endpoint, uploadUrl, contentJson);

			if(data!=null && data instanceof JsonJavaObject) {
				JsonJavaArray arr=((JsonJavaObject)data).getAsArray("attachments");
				
				return new File((JsonJavaObject)arr.get(0));
			}
		}

		return null;
	}

	private String uploadAttachment(String name, InputStream inputStream, String contentType) {
		Endpoint endpoint=getEndpoint();

		String serviceUrl="/attachments.json";

		Content content=new ContentStream(inputStream, contentType);

		Object data=RestUtils.xhrPost(endpoint, serviceUrl, content);

		if(data!=null) {
			if(data instanceof JsonJavaObject) {
				return ((JsonJavaObject)data).getAsString("token");
			}
		}

		return "";
	}

	public void uploadConnectionsFile(com.ibm.sbt.services.client.connections.files.File file) {
	    FacesContext facesContext = FacesContext.getCurrentInstance();
	    String projectId=DevelopiUtils.strRight((String)ExtLibUtil.getViewScope().get("selectedProject"), "|");
	    
	    ConnectionsService ics=(ConnectionsService)ExtLibUtil.resolveVariable(facesContext, "ics");
	    
	    try {

	    	String accessType = AccessType.AUTHENTICATED.getAccessType();
			
	    	SubFilters downloadFilters = new SubFilters();
			downloadFilters.setFileId(file.getFileId());

			String resultType = ResultType.MEDIA.getResultType();
			String requestUrl = FileServiceURIBuilder.constructUrl(FileServiceURIBuilder.FILES.getBaseUrl(), accessType, null, null,
	                null, downloadFilters, resultType); 
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put(Headers.ContentType, Headers.BINARY);

			Response response = null;
			try {
				response = ics.getEndpoint().getClientService().get(requestUrl, null, headers, ClientService.FORMAT_INPUTSTREAM);
			} catch (ClientServicesException e) {
				throw new RuntimeException(e);
			} 

			InputStream is = (InputStream) response.getData();
	    	
			if(is==null) throw new RuntimeException("Stream null");

			File newFile=uploadFile(projectId, file.getLabel(), is, file.getContentType());
	    	
	    	if(newFile==null) {
		        facesContext.addMessage("messages1", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error detected during upload. Check logs...", ""));
		        return;
	    	}
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	        facesContext.addMessage("messages1", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error in uploading file.", ""));
	        return;
	    }

	    facesContext.addMessage("messages1", new FacesMessage(FacesMessage.SEVERITY_INFO, "File successfully uploaded ("+file.getLabel()+")", ""));
	}
	
	public void uploadLocalFile() {

	    FacesContext facesContext = FacesContext.getCurrentInstance();
	    ExternalContext externalContext = facesContext.getExternalContext();

	    HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();

	    String fileUploadID = ExtLibUtil.getClientId(facesContext, facesContext.getViewRoot(), "fileUpload1", false);
	    
	    UploadedFile uploadedFile = ((UploadedFile) request.getParameterMap().get(fileUploadID));

	    if (uploadedFile == null) {
	        facesContext.addMessage("messages1", new FacesMessage(FacesMessage.SEVERITY_ERROR, "No file uploaded. Use the file upload button to upload a file.", ""));
	        return;
	    }
    
	    java.io.File file = uploadedFile.getServerFile();
	    String contentType=uploadedFile.getContentType();
	    String fileName = uploadedFile.getClientFileName();

	    String projectId=DevelopiUtils.strRight((String)ExtLibUtil.getViewScope().get("selectedProject"), "|");
	    
	    try {

	    	File newFile=uploadFile(projectId, fileName, new FileInputStream(file), contentType);
	    	
	    	if(newFile==null) {
		        facesContext.addMessage("messages1", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error detected during upload. Check logs...", ""));
		        return;
	    	}
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	        facesContext.addMessage("messages1", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error in uploaded file.", ""));
	        return;
	    }

	    facesContext.addMessage("messages1", new FacesMessage(FacesMessage.SEVERITY_INFO, "File successfully uploaded", ""));
	}

}
