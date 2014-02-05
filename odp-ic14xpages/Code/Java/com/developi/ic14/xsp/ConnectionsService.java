package com.developi.ic14.xsp;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import com.developi.ic14.bc.File;
import com.developi.toolbox.DevelopiUtils;
import com.developi.toolbox.RestUtils;
import com.ibm.commons.util.StringUtil;
import com.ibm.sbt.security.authentication.AuthenticationException;
import com.ibm.sbt.services.client.ClientServicesException;
import com.ibm.sbt.services.client.connections.communities.Community;
import com.ibm.sbt.services.client.connections.communities.CommunityList;
import com.ibm.sbt.services.client.connections.communities.CommunityService;
import com.ibm.sbt.services.client.connections.communities.CommunityServiceException;
import com.ibm.sbt.services.client.connections.files.FileList;
import com.ibm.sbt.services.client.connections.files.FileService;
import com.ibm.sbt.services.client.connections.files.FileServiceException;
import com.ibm.sbt.services.client.connections.profiles.Profile;
import com.ibm.sbt.services.client.connections.profiles.ProfileService;
import com.ibm.sbt.services.client.connections.profiles.ProfileServiceException;
import com.ibm.sbt.services.endpoints.Endpoint;
import com.ibm.sbt.services.endpoints.EndpointFactory;
import com.ibm.xsp.extlib.util.ExtLibUtil;

public class ConnectionsService implements Serializable {

	/**
	 * Quick and Dirty helper for Connections services.
	 */
	
	private static final long serialVersionUID = 1L;

	private final static String DEFAULT_ENDPOINT_NAME="connections";

	private String endpointName;

	public ConnectionsService() {
		setEndpointName(DEFAULT_ENDPOINT_NAME);
	}
	
	public void setEndpointName(String endpointName) {
		this.endpointName = endpointName;
	}
	
	public Endpoint getEndpoint() {
		return EndpointFactory.getEndpoint(endpointName);
	}

	public synchronized Profile getMe() {
		try {
			if(getEndpoint().isAuthenticated()) {
				ProfileService ps=new ProfileService(getEndpoint());
				return ps.getMyProfile();
			}
		} catch (ClientServicesException e) {
			// So we are not able to determine authentication. Forget it.
		} catch (ProfileServiceException e) {
			// We can't get profile
			System.out.println("I can't find the user profile! "+e.getMessage());
		}

		return null;
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
	
	public void logout() {
		try {
			getEndpoint().logout();
		} catch (AuthenticationException e) { 
			System.out.println("Unable to logout: "+e.getMessage());
		}
	}
	
	public FileList getFiles() {
		
		FileService fs=new FileService(getEndpoint());
		
		try {
			FileList fileList=fs.getMyFiles();
			return fileList;
		} catch (FileServiceException e) {
			System.out.println("Unable to get files: "+e.getMessage());
		}
		
		
		return null;
	}

	public FileList getCommunityFiles(String id) {
		
		FileService fs=new FileService(getEndpoint());
		
		try {
			return fs.getCommunityFiles(id);
		} catch (FileServiceException e) {
			System.out.println("Unable to get community files("+id+"): "+e.getMessage());
		}
		
		
		return null;
	}

	public List<String> getCommunitiesForCombo() {
		CommunityList cL=getCommunities();
		List<String> options=new ArrayList<String>();
		
		for(Community c : cL) {
			options.add(c.getTitle()+"|"+c.getCommunityUuid());
		}

		return options;
	}
	
	public CommunityList getCommunities() {
		CommunityService cs=new CommunityService(getEndpoint());
		
		try {
			return cs.getMyCommunities();
		} catch (CommunityServiceException e) {
			System.out.println("Unable to get communities: "+e.getMessage());
		}
		
		return null;
	}
	
	public String getIcon(String fileName, String type) {
		
		if(RestUtils.ICONMAP.containsKey(type)) return RestUtils.ICONMAP.get(type);

		type=DevelopiUtils.strLeft(type, "/");
		if(RestUtils.ICONMAP.containsKey(type)) return RestUtils.ICONMAP.get(type);
		
		
		String ext=DevelopiUtils.strRightBack(fileName, ".");
		if(RestUtils.ICONMAP.containsKey(ext)) return RestUtils.ICONMAP.get(ext);
		
		return "ct-default";
	}
	
	public void uploadBCFile(File file, String target) {
		String communityId="";
		
		if(StringUtil.isEmpty(target) || target.equals("myFiles")) {
			// To My Files
		} else {
			communityId=DevelopiUtils.strRight(target, "|");
		}
		
		FileService fs=new FileService(getEndpoint());
		FacesContext facesContext = FacesContext.getCurrentInstance();
		BaseCampService bcs=(BaseCampService)ExtLibUtil.resolveVariable(facesContext, "bcs");

		String errorMessage="";
		
		try {

			InputStream is=RestUtils.xhrGetStream(bcs.getEndpoint(), bcs.getDownloadUri(file.getUrl()));
			
			if(StringUtil.isEmpty(communityId)) {
				fs.uploadFile(is, file.getName(), -1);
			} else {
				fs.uploadCommunityFile(is, communityId, file.getName(), -1);
			}
		
		    facesContext.addMessage("messages1", new FacesMessage(FacesMessage.SEVERITY_INFO, file.getName()+" successfully transferred...", ""));
		    return;
				
		} catch (FileServiceException e) {
			errorMessage="Error uploading file to the FileService...";
			e.printStackTrace();
		}

        facesContext.addMessage("messages1", new FacesMessage(FacesMessage.SEVERITY_ERROR, errorMessage, ""));

	}

}
