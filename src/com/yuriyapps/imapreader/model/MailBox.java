package com.yuriyapps.imapreader.model;

public class MailBox
{
	//private String attribute;
	//private String separator;
	private String name;
	private String displayName;
	
	public MailBox(/*String attribute, String separator,*/ String name, String displayName)
	{
		//this.attribute = attribute;
		//this.separator = separator;
		this.name = name;
		this.displayName = displayName;
	}
	
	/*public String getAttribute() {
		return attribute;
	}
	
	public String getSeparator() {
		return separator;
	}*/
	
	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}
}