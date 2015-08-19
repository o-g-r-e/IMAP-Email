package com.yuriyapps.imapreader.model;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.yuriyapps.imapreader.controller.Parser;

public class Mail
{
	private int number;
	private Map<String, String> parameters;
	
	public Mail(int number, Map<String, String> parameters)
	{
		this.number = number;
		this.parameters = parameters;
	}
	
	public void decodeParameters(String[] parameterNames)
	{
		for(String key : parameterNames)
		{
			if(this.parameters.containsKey(key))
			{
				String decodedValue = this.headerValueDecoder(this.parameters.get(key));
				this.parameters.put(key, decodedValue);
			}
		}
	}
	
	private String headerValueDecoder(String string)
	{
		//string = Parser.replaceEntires(string, Pattern.compile("\\n"), "");
		string = string.replaceAll("\\n", "");
		//Pattern tagPattern = Pattern.compile("\\??\\=?\\n?\\s*\\=\\?([a-zA-Z0-9-]+)\\?(\\w{1})\\?");
		Pattern tagPattern = Pattern.compile("\\=\\?([a-zA-Z0-9-]+)\\?(\\w{1})\\?");
		int[] foundIndices = Parser.findIndices(tagPattern, string);
		
		if(foundIndices.length == 0)
		{
			return string;
		}
		
		List<Integer> foundIndicesList = new ArrayList<Integer>();
		for(int i = 0; i < foundIndices.length; i++)
		{
			foundIndicesList.add(new Integer(foundIndices[i]));
		}
		
		String[][] foundString = Parser.findWithRegExp(tagPattern, string);
		StringBuilder resultString = new StringBuilder();
		//int tagIndex = 0;
		for (int i = 0; i < string.length(); i++)
		{
			Integer boxingI = Integer.valueOf(i);
			if(foundIndicesList.contains(boxingI))
			{
				int tagIndex = foundIndicesList.indexOf(boxingI);
				String charset = foundString[tagIndex][1];
				String charsetType = foundString[tagIndex][2].toUpperCase();
				String encodedString = null;
				int startDataIndex = foundIndices[tagIndex]+foundString[tagIndex][0].length();
				int endDataIndex = string.indexOf("?=", startDataIndex);
				encodedString = string.substring(startDataIndex, endDataIndex);
				i = endDataIndex + 2;
				//tagIndex++;
				String decodedString = null;
				try {
					if(charsetType.equals("B"))
					{
						decodedString = Parser.base64Decode(encodedString, charset);
					}
					else
					{
						decodedString = Parser.quotedPrintableDecode(encodedString, charset);
					}
					resultString.append(decodedString);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
			}
			else
			{
				resultString.append(string.charAt(i));
			}	
		}
		
		return resultString.toString();
	}
	
	public int getNumber() {
		return this.number;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	
	public String param(String paramName) {
		if(this.parameters.containsKey(paramName))
			return this.parameters.get(paramName);
		
		return "";
	}
}