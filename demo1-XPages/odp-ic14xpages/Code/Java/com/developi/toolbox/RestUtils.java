package com.developi.toolbox;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.commons.util.io.json.JsonJavaObject;
import com.ibm.sbt.services.client.ClientService;
import com.ibm.sbt.services.client.ClientServicesException;
import com.ibm.sbt.services.client.Response;
import com.ibm.sbt.services.client.ClientService.Args;
import com.ibm.sbt.services.client.ClientService.Content;
import com.ibm.sbt.services.endpoints.Endpoint;

public class RestUtils {

	/**
	 * Quick and Dirty utilities for different REST communications.
	 */

	private static final String USER_AGENT = "IBM SBT SDK Demo (someone@somewhere.com)";

	public static final Map<String, String> ICONMAP=new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			put("text", "ct-txt");
			put("pdf", "ct-pdf");
			put("image", "ct-image");
			put("doc", "ct-doc");
			put("xls", "ct-xls");
			put("ppt", "ct-ppt");
			put("docx", "ct-doc");
			put("xlsx", "ct-xls");
			put("pptx", "ct-ppt");
			put("odt", "ct-doc");
			put("ods", "ct-xls");
			put("odp", "ct-ppt");
			put("zip", "ct-zip");
		}
	};

	public final static JsonJavaObject xhrGetJson(Endpoint endpoint, String serviceUrl) {
		return xhrGetJson(endpoint, serviceUrl, null);
	}

	public final static JsonJavaObject xhrGetJson(Endpoint endpoint, String serviceUrl, Map<String, String> parameters) {
		Object result=xhrGet(endpoint, serviceUrl, parameters);

		if(result!=null) {
			if(result instanceof JsonJavaObject) {
				return (JsonJavaObject) result;
			} else {
				System.out.println("Unexpected Response: "+serviceUrl+" > "+result.getClass().getName());
			}
		}

		return null;
	}

	public final static List<Object> xhrGetJsonList(Endpoint endpoint, String serviceUrl) {
		return xhrGetJsonList(endpoint, serviceUrl, null);
	}

	@SuppressWarnings("unchecked")
	public final static List<Object> xhrGetJsonList(Endpoint endpoint, String serviceUrl, Map<String, String> parameters) {
		Object result=xhrGet(endpoint, serviceUrl, parameters);

		if(result!=null) {
			if(result instanceof List) {
				return (List<Object>) result;
			} else {
				System.out.println("Unexpected Response: "+serviceUrl+" > "+result.getClass().getName());
			}
		}

		return null;
	}

	public final static Object xhrGet(Endpoint endpoint, String serviceUrl, Map<String, String> parameters) {
		try {
			if(endpoint.isAuthenticated()) {
				Response response = endpoint.xhrGet(serviceUrl, parameters);

				if(response!=null) {
					return response.getData();
				} else {
					System.out.println("Null Response: ("+endpoint.getUrl()+serviceUrl+")");
				}
			} else {
				System.out.println("Not authenticated...");
			}
		} catch (ClientServicesException e1) {
			System.out.println("Unable to receive data: "+e1.getMessage());
		}

		return null;
	}

	public final static InputStream xhrGetStream(Endpoint endpoint, String url) {
		try {
			if(endpoint.isAuthenticated()) {
				Args args=new Args();
				
				args.setServiceUrl(url);
				args.addHeader("User-Agent", USER_AGENT);
				args.setHandler(ClientService.FORMAT_INPUTSTREAM);
				
				Response response = endpoint.xhrGet(args);

				if(response!=null) {
					return (InputStream)response.getData();
				} else {
					System.out.println("Null Response: ("+url+")");
				}
			} else {
				System.out.println("Not authenticated...");
			}
		} catch (ClientServicesException e1) {
			System.out.println("Unable to receive stream data: "+e1.getMessage());
		}		
		return null;
	}
	
	public final static Object xhrPost(Endpoint endpoint, String serviceUrl, Content content) {
		return xhrPost(endpoint, serviceUrl, content, null);
	}
	
	public final static Object xhrPost(Endpoint endpoint, String serviceUrl, Content content, Map<String, String> parameters) {
		try {
			if(endpoint.isAuthenticated()) {
				Args args=new Args();
				
				args.setServiceUrl(serviceUrl);
				args.addHeader("User-Agent", USER_AGENT);
				
				if(parameters!=null) args.setParameters(parameters);
				
				Response response = endpoint.xhrPost(args, content);

				if(response!=null) {
					return response.getData();
				} else {
					System.out.println("Null Response: ("+endpoint.getUrl()+serviceUrl+")");
				}
			} else {
				System.out.println("Not authenticated...");
			}
		} catch (ClientServicesException e1) {
			System.out.println("Unable to receive data: "+e1.getMessage());
		}

		return null;
	}

}
