package com.developi.ic14.bc;

/**
 * File class is a representation of a single File object within Basecamp...
 */

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.developi.toolbox.DevelopiUtils;
import com.developi.toolbox.RestUtils;
import com.ibm.commons.util.io.json.JsonJavaObject;

public class File implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String id;
	private String key;
	private String name;
	private long size; //byte_size
	private Date createdAt; //created_at
	private String type; //content_type
	private String url; 
	
	private String creatorName; //creator.name
	private String creatorAvatar; //creator.avatar_url
	
	public File() {
		
	}

	/**
	 * Most of the time, we have a JSON data returned from Basecamp API. We use this constructor to build a new File
	 * out of JSON.	It's quick and dirty, so we have not spent much time on date time issues or data type checks etc.
	 * 
	 */
	public File(JsonJavaObject data) {
		this();
		
		if(data!=null) {
			Long idL=data.getAsLong("id");
			
			this.id=idL.toString();
			this.name = data.getAsString("name");
			this.key = data.getAsString("key");
			this.size = data.getAsLong("byte_size");
			
			// FIXME Timezone bug
			String datePart=DevelopiUtils.strLeft(data.getAsString("created_at"), "T");
			DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
			try {
				this.createdAt=df.parse(datePart);
			} catch (ParseException e1) {}
			
			this.type = data.getAsString("content_type");
			this.url = data.getAsString("url");
			
			if(data.containsKey("creator")) {
				JsonJavaObject obj=data.getJsonObject("creator");
				this.creatorName=obj.getAsString("name");
				this.creatorAvatar=obj.getAsString("avatar_url");
			}

		}

	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public String getCreatedAtFormatted() {
		DateFormat df=DateFormat.getDateInstance();
		return df.format(getCreatedAt());
	}
	public String getType() {
		return type;
	}

	public String getIcon() {
		String type=getType();
		if(RestUtils.ICONMAP.containsKey(type)) return RestUtils.ICONMAP.get(type);

		type=DevelopiUtils.strLeft(type, "/");
		if(RestUtils.ICONMAP.containsKey(type)) return RestUtils.ICONMAP.get(type);
		
		
		String ext=DevelopiUtils.strRightBack(getName(), ".");
		if(RestUtils.ICONMAP.containsKey(ext)) return RestUtils.ICONMAP.get(ext);
		
		return "ct-default";
	}
	
	public void setType(String type) {
		this.type = type;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getCreatorName() {
		return creatorName;
	}
	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}
	public String getCreatorAvatar() {
		return creatorAvatar;
	}
	public void setCreatorAvatar(String creatorAvatar) {
		this.creatorAvatar = creatorAvatar;
	}

	@Override
	public String toString() {
		return "#"+id+": "+name+" ("+type+" - "+size+" bytes)";
	}
	
	
}
