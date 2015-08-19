package com.yuriyapps.imapreader.controller;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser
{
	public static String quotedPrintableDecode(String value, String charset) throws NumberFormatException, UnsupportedEncodingException
	{
		String hexAlpha = "0123456789ABCDEF";
		
		List<Integer> bytes = new ArrayList<Integer>();
		
		for (int i = 0; i < value.length(); i++)
		{
				if(value.charAt(i) == '=' && hexAlpha.contains(String.valueOf(value.charAt(i+1))) && hexAlpha.contains(String.valueOf(value.charAt(i+2))))
				{
					bytes.add(Integer.parseInt(String.valueOf(value.charAt(i+1))+String.valueOf(value.charAt(i+2)), 16));
					i+=2;
				}
				else
				{
					if(String.valueOf(value.charAt(i)).getBytes()[0] == 0xFFFFFFA0)
					{
						bytes.add(32);
					}
					else
					{
						bytes.add((int)String.valueOf(value.charAt(i)).getBytes()[0]);
					}
				}
		}
		
		byte[] resultBytes = new byte[bytes.size()];
		for (int i = 0; i < resultBytes.length; i++) {
			resultBytes[i] = (byte) bytes.get(i).intValue();
		}
		return new String(resultBytes, charset);
	}
	
	public static String base64Decode(String value, String charset) throws UnsupportedEncodingException
	{
		return new String(Base64.getDecoder().decode(value), charset);
	}
	
	public static TreeMap<String, String> parseParametersString(Pattern parameterNameTemplate, String data)
	{
		Map<String, String> map = new TreeMap<String, String>();
		int[] indices = findIndices(parameterNameTemplate, data);
		String[][] s = findWithRegExp(parameterNameTemplate, data);
		StringBuilder b = new StringBuilder(data);
		
		//int index = 0;
		for (int i = 0; i < s.length; i++)
		{
			String key = s[i][1];
			String value = null;
			int startIndex = indices[i]+s[i][0].length();
			if(i+1 == s.length)
			{
				value = b.substring(startIndex);
			}
			else
			{
				value = b.substring(startIndex, indices[i+1]);
				//b.delete(b.indexOf(key), b.indexOf(s[i+1][0]));
			}
			map.put(key, value);
			
		}
		return (TreeMap<String, String>) map;
	}
	
	public static String[][] findWithRegExp(Pattern pattern, String value)
	{
		//List<String> found = new ArrayList<String>();
		ArrayList<String[]> found = new ArrayList<String[]>();
		int groupCount = 0;
		
		if(pattern != null && value != null)
		{
			Matcher matcher = pattern.matcher(value);
			
			while(matcher.find())
			{
				groupCount = matcher.groupCount()+1;
				String[] groups = new String[groupCount];
				for(int i = 0; i < groups.length; i++)
				{
					groups[i] = matcher.group(i);
				}
				
				found.add(groups);
			}
		}
		
		return found.toArray(new String[found.size()][groupCount]);
	}
	
	public static boolean matchWithRegExp(Pattern pattern, String value)
	{
		if(pattern != null && value != null)
		{
			return pattern.matcher(value).find();
		}
		return false;
	}
	
	public static int[] findIndices(Pattern pattern, String value)
	{
		List<Integer> result = new ArrayList<Integer>();
		Matcher matcher = pattern.matcher(value);
		
		while(matcher.find())
		{
			result.add(matcher.start());
		}
		
		int[] resultIdices = new int[result.size()];
		for (int i = 0; i < resultIdices.length; i++)
		{
			resultIdices[i] = result.get(i).intValue();
		}
		
		return resultIdices;
	}
	
	/*public static String replaceEntires(String string, Pattern subStringPattern, String replaceTo)
	{
		StringBuilder s = new StringBuilder(string);
		int[] indices = fetchIndices(subStringPattern, string);
		String[][] foundIndices = Parser.findWithRegExp(subStringPattern, string);
		for (int i = indices.length - 1; i >= 0; i--) //while(matchWithRegExp(Pattern.compile("\\=\\n"), s.toString()))
		{
			int startReplaceIndex = indices[i];
			int endReplaceIndex = startReplaceIndex + foundIndices[i][0].length();
			s.replace(startReplaceIndex, endReplaceIndex, replaceTo);
		}
		
		return s.toString();
	}*/
}