package com.developi.ic14.extensions;

import com.ibm.sbt.services.endpoints.OAuth2Endpoint;

public class BaseCampEndpoint extends OAuth2Endpoint {

	/**
	 * The tricky part about Basecamp is that they don't exactly follow the standard OAUTH2 protocol.
	 * So we have to change specific parts, especially URLs.
	 * 
	 * This is a very common problem for Social App Developers. When we need to extend certain OAUTH endpoint type, here is the way.
	 * 
	 * The actual magic is in the handler. But we need to extend the endpoint to change the handler. We also added a new parameter below.
	 *  
	 */
	
	public BaseCampEndpoint() {
		super(new BaseCampOAuth2Handler());
	}

	// This can be given within Managed Bean definitions.
	// BaseCamp only supports "web_server" (default) and "user_agent"
	public void setApplicationType(String applicationType) {
		((BaseCampOAuth2Handler)oAuthHandler).setApplicationType(applicationType);
	}

	public String getApplicationType() {
		return ((BaseCampOAuth2Handler)oAuthHandler).getApplicationType();
	}

}
