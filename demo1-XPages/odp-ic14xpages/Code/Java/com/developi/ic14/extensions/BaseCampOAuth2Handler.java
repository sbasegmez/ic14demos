package com.developi.ic14.extensions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import com.ibm.commons.util.io.StreamUtil;
import com.ibm.sbt.core.configuration.Configuration;
import com.ibm.sbt.security.authentication.oauth.OAuthException;
import com.ibm.sbt.security.authentication.oauth.consumer.OAuth2Handler;

@SuppressWarnings("deprecation")
public class BaseCampOAuth2Handler extends OAuth2Handler {

	/**
	 * Basecamp OAUTH2 implementation needs a modified URL for OAUTH operations. So we actually extended the standard OAUTH2
	 * implementation of SBT and built our own URLs from scratch (see two methods below).
	 * 
	 */
	
	public static final String BASECAMP_OAUTH2_TYPE = "type";
	public static final String BASECAMP_OAUTH2_REDIRECTURI = "redirect_uri";
	
	public static final String DEFAULT_APPLICATION_TYPE = "web_server";
	
	private String applicationType;
	
	public BaseCampOAuth2Handler() {
		super();
		setApplicationType(DEFAULT_APPLICATION_TYPE);
	}

	@Override
	public String getAuthorizationNetworkUrl() {
		StringBuilder url = new StringBuilder();
		try {
			url.append(getAuthorizationURL());
			url.append('?');
			url.append(BASECAMP_OAUTH2_TYPE);
			url.append('=');
			url.append(applicationType);
			url.append('&');
			url.append(Configuration.OAUTH2_CLIENT_ID);
			url.append('=');
			url.append(URLEncoder.encode(getConsumerKey(), "UTF-8"));
			url.append('&');
			url.append(BASECAMP_OAUTH2_REDIRECTURI);
			url.append('=');
			url.append(URLEncoder.encode(getClient_uri(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
		}
		return url.toString();	
	}

	public void getAccessTokenForAuthorizedUser() throws Exception {
		HttpPost method = null;
		int responseCode = HttpStatus.SC_OK;
		String responseBody = null;
		InputStream content = null;
		try {
			HttpClient client = new DefaultHttpClient();

			StringBuffer url = new StringBuffer(2048);
			url.append(getAccessTokenURL()).append("?");
			url.append(BASECAMP_OAUTH2_TYPE).append('=').append(getApplicationType());
			url.append('&');
			url.append(BASECAMP_OAUTH2_REDIRECTURI).append('=').append(URLEncoder.encode(getClient_uri(), "UTF-8"));
			url.append('&');
			url.append(Configuration.OAUTH2_CLIENT_ID).append('=').append(URLEncoder.encode(getConsumerKey(), "UTF-8"));
			url.append('&');
			url.append(Configuration.OAUTH2_CLIENT_SECRET).append('=').append(URLEncoder.encode(getConsumerSecret(), "UTF-8"));
			url.append('&');
			url.append(Configuration.OAUTH2_CODE).append('=').append(URLEncoder.encode(getAuthorization_code(), "UTF-8"));

			method = new HttpPost(url.toString());
			HttpResponse httpResponse =client.execute(method);
			responseCode = httpResponse.getStatusLine().getStatusCode();

			content = httpResponse.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(content));
			try {
				responseBody = StreamUtil.readString(reader);
			} finally {
				StreamUtil.close(reader);
			}
		} catch (Exception e) {
			throw new OAuthException(e, "getAccessToken failed with Exception: <br>" + e);
		} finally {
			if(content != null) {
				content.close(); 
			}
		}
		if (responseCode != HttpStatus.SC_OK) {
			getAccessTokenForAuthorizedUsingPOST();
			 return;
//			if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
//				throw new Exception("getAccessToken failed with Response Code: Unauthorized (401),<br>Msg: " + responseBody);
//			} else if (responseCode == HttpStatus.SC_BAD_REQUEST) {
//				throw new Exception("getAccessToken failed with Response Code: Bad Request (400),<br>Msg: " + responseBody);
//			} else if (responseCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
//				throw new Exception("getAccessToken failed with Response Code: Internal Server error (500),<br>Msg: " + responseBody);
//			} else {
//				throw new Exception("getAccessToken failed with Response Code: (" + responseCode + "),<br>Msg: " + responseBody);
//			}
		} else {
			setOAuthData(responseBody); //save the returned data
		}
	
		}

	
	
	
	public void setApplicationType(String applicationType) {
		this.applicationType = applicationType;
	}

	public String getApplicationType() {
		return applicationType;
	}

	
}
