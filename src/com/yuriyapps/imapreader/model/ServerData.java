package com.yuriyapps.imapreader.model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServerData implements Serializable
{
	private String serverUrl;
	private List<String> userLogins;
	
	public ServerData(String serverUrl)
	{
		this.serverUrl = serverUrl;
		this.userLogins = new ArrayList<String>();
	}
	
	public String getServerUrl() {
		return serverUrl;
	}
	
	public List<String> getUserLogins() {
		return userLogins;
	}
}