package com.developi.ic14.bc;

/**
 * Representation of a project within Basecamp.
 */

import java.io.Serializable;

import com.ibm.commons.util.io.json.JsonJavaObject;

public class Project implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;
	private String name;
	private String description;
	
	private int attachmentCount; //attachments.count
	private int calendarCount; //calendar_events.count
	private int documentCount; //documents.count
	private int topicCount; //topics.count
	private int todolistCount; //todolists.remaining_count
	
	
	public Project() {
		
	}

	/**
	 * Projects can be constructed from a JSON data. In Basecamp, JSON data about entities would be different according to 
	 * what you are really looking for. So it's always safe to avoid assumptions on the exact format of JSON.
	 * 
	 */
	public Project(JsonJavaObject data) {
		this();
		if(data!=null) {
			Long idL=data.getAsLong("id");
			
			this.id=idL.toString();
			this.name = data.getAsString("name");
			this.description = data.getAsString("description");
			
			if(data.containsKey("attachments")) {
				this.attachmentCount=data.getJsonObject("attachments").getAsInt("count");
			}

			if(data.containsKey("calendar_events")) {
				this.calendarCount=data.getJsonObject("calendar_events").getAsInt("count");
			}
			
			if(data.containsKey("documents")) {
				this.documentCount=data.getJsonObject("documents").getAsInt("count");
			}

			if(data.containsKey("topics")) {
				this.topicCount=data.getJsonObject("topics").getAsInt("count");
			}

			if(data.containsKey("todolists")) {
				this.todolistCount=data.getJsonObject("todolists").getAsInt("remaining_count");
			}

		}
	}

	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public int getAttachmentCount() {
		return attachmentCount;
	}

	public void setAttachmentCount(int attachmentCount) {
		this.attachmentCount = attachmentCount;
	}

	public int getCalendarCount() {
		return calendarCount;
	}

	public void setCalendarCount(int calendarCount) {
		this.calendarCount = calendarCount;
	}

	public int getDocumentCount() {
		return documentCount;
	}

	public void setDocumentCount(int documentCount) {
		this.documentCount = documentCount;
	}

	public int getTopicCount() {
		return topicCount;
	}

	public void setTopicCount(int topicCount) {
		this.topicCount = topicCount;
	}

	public int getTodolistCount() {
		return todolistCount;
	}

	public void setTodolistCount(int todolistCount) {
		this.todolistCount = todolistCount;
	}

	@Override
	public String toString() {
		return "Project #"+id+" - "+name;
	}

	
}
