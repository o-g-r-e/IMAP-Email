package com.yuriyapps.imapreader.controller;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.yuriyapps.imapreader.model.Mail;
import com.yuriyapps.imapreader.model.MailBox;


public class ServerIO extends Observable implements Runnable
{
	private final int imapPort = 993;
	
	private SocketFactory socketfactory;
	private Socket socket;
	
	private Writer bufferedwriter;
	private Reader bufferedreader;
	
	private boolean on;
	
	private enum States { NOT_AUTHENTICATE, AUTHENTICATED, SELECTED };
	private States state;
	
	private enum WaitingResponseStates { NOT_WAIT, WAITING_LIST, WAITING_EXAMINE, WAITING_FETCH };
	private WaitingResponseStates waitingResponseState;
	
	private enum FetchResponseStates { NOTHING, WAITING_FETCH_HEADERLIST, WAITING_FETCH_MAILBODY };
	private FetchResponseStates fetchResponseState;
	
	//private String[] NotAuthResponses = { "LOGIN" };
	//private String[] AuthorizedResponses = { "SELECT", "LIST" };
	//private String[] SelectedResponses = { "FETCH" };
	//private String waitingFor;
	
	private String loginCommandPrefix;
	private String listCommandPrefix;
	private String examineCommandPrefix;
	private String fetchCommandPrefix;
	
	
	//private Pattern responseStatePattern;
	private Pattern notMarkResponsePattern;
	private Pattern okResponsePattern;
	private Pattern inputCommandPattern;
	private Pattern okListDoneResponsePattern;
	private Pattern responseTagPattern;
	private Pattern responsePrefixPattern;
	private Pattern mailBoxAttributePattern;
	private Pattern mailBoxSeparatorPattern;
	private Pattern mailBoxNamePattern;
	private Pattern mailBoxLinePattern;
	private Pattern okExamineCompleteResponsePattern;
	private Pattern existsMailsPattern;
	private Pattern existsMailsCountPattern;
	private Pattern okFetchDonePattern;
	
	private String serverErrorExecutionTag;
	private String successfulExecutionTag;
	private String sintaxErrorExecutionTag;
	
	private StringBuilder response;
	
	public ServerIO()
	{
		socketfactory = null;
		socket = null;
		    
		bufferedwriter = null;
		bufferedreader = null;
		
		on = true;
		
		state = States.NOT_AUTHENTICATE;
		waitingResponseState = WaitingResponseStates.NOT_WAIT;
		fetchResponseState = FetchResponseStates.NOTHING;
		
		//waitingFor = "";
		loginCommandPrefix = "login_prefix";
		listCommandPrefix = "list_prefix";
		examineCommandPrefix = "examine_prefix";
		fetchCommandPrefix = "fetch_prefix";
		
		serverErrorExecutionTag = "NO";
		successfulExecutionTag = "OK";
		sintaxErrorExecutionTag = "BAD";
		
		//responseStatePattern = Pattern.compile("^.* (\\w+) .*");
		notMarkResponsePattern = Pattern.compile("^\\*\\s");
		okListDoneResponsePattern = Pattern.compile("^\\w+\\sOK\\sLIST\\sdone$");
		responseTagPattern = Pattern.compile("^\\w+\\s(\\w+)\\s");
		responsePrefixPattern = Pattern.compile("^(\\w+)\\s");
		mailBoxAttributePattern = Pattern.compile("^\\*\\sLIST\\s\\((.+)\\)");
		mailBoxSeparatorPattern = Pattern.compile("^\\*\\sLIST\\s\\(.+\\)\\s\"(.+)\"\\s");
		mailBoxNamePattern      = Pattern.compile("^\\*\\sLIST\\s\\(.+\\)\\s\".+\"\\s\"(.+)\"");
		mailBoxLinePattern      = Pattern.compile("(\\*\\sLIST\\s\\(.+\\)\\s\".+\"\\s\".+\")");
		okExamineCompleteResponsePattern = Pattern.compile("^\\w+\\sOK\\s\\[READ-ONLY\\]\\sEXAMINE\\scompleted$");
		existsMailsPattern = Pattern.compile("\\*\\s\\d+\\sEXISTS");
		existsMailsCountPattern = Pattern.compile("\\*\\s(\\d+)\\sEXISTS");
		okFetchDonePattern = Pattern.compile("^\\w+\\sOK\\sFETCH\\sdone");
		//okResponsePattern = Pattern.compile("^\\w+\\s"+successfulExecutionTag+"\\s");
		//inputCommandPattern = Pattern.compile("^.* (\\w+) .*");
		
		response = new StringBuilder();
	}
	
	
	
	@Override
	public void notifyObservers(Object arg) {
        setChanged();
        super.notifyObservers(arg);
    }
    
    public void sendCommand(String v) throws IOException
    {
    	//String comm = patternFind(inputCommandPattern, v);
    	//if(comm != null)
    		//waitingFor = comm.toUpperCase();
    	System.out.println("write: "+v);
    	bufferedwriter.write(v+"\n");
    	bufferedwriter.flush();
    }
    
	public Reader getReader()
	{
		return bufferedreader;
	}
	
	public Writer getWriter()
	{
		return bufferedwriter;
	}
	
	public void connect(String serverUrl) throws UnknownHostException, IOException
	{
		socketfactory = SSLSocketFactory.getDefault();
		socket = (SSLSocket) socketfactory.createSocket(serverUrl, this.imapPort);
		    
		bufferedwriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		bufferedreader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	public void imapLogin(String login, String password)
	{
		try {
			if(state == States.NOT_AUTHENTICATE)
				sendCommand(loginCommandPrefix+" login "+login+" "+password);
		} catch (IOException e) {
			notifyObservers(new Boolean(false));
			e.printStackTrace();
		}
	}
	
	public void imapList(String path, String name)
	{
		try {
			sendCommand(listCommandPrefix+" list \""+path+"\" \""+name+"\"");
			waitingResponseState = WaitingResponseStates.WAITING_LIST;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void imapExamine(String mailBoxName)
	{
		try {
			sendCommand(examineCommandPrefix+" examine "+mailBoxName);
			waitingResponseState = WaitingResponseStates.WAITING_EXAMINE;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void imapFetch(int from, int to, String request)
	{
		try {
			sendCommand(fetchCommandPrefix+" fetch "+from+":"+to+" "+request);
			waitingResponseState = WaitingResponseStates.WAITING_FETCH;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void fetchMailList(int start, int end)
	{
		this.imapFetch(start, end, "body.peek[header]");
		fetchResponseState = FetchResponseStates.WAITING_FETCH_HEADERLIST;
	}
	
	public void fetchMailBody(int number)
	{
		this.imapFetch(number, number, "body[text]");
		fetchResponseState = FetchResponseStates.WAITING_FETCH_MAILBODY;
	}
	
	public void setState(States newState)
	{
		this.state = newState;
	}
	
	public void obtainCert()
	{
//      SSLContext ctx = SSLContext.getInstance("TLS");
//      
//      X509TrustManager tm = new X509TrustManager() {
//          public void checkClientTrusted(X509Certificate[] chain,
//                          String authType)
//                          throws CertificateException {
//              //do nothing, you're the client
//          }
//
//          public X509Certificate[] getAcceptedIssuers() {
//				return null;
//              //also only relevant for servers
//          }
//
//          public void checkServerTrusted(X509Certificate[] chain,
//                          String authType)
//                          throws CertificateException {
//              /* chain[chain.length -1] is the candidate for the
//               * root certificate. 
//               * Look it up to see whether it's in your list.
//               * If not, ask the user for permission to add it.
//               * If not granted, reject.
//               * Validate the chain using CertPathValidator and 
//               * your list of trusted roots.
//               */
//          }
//      };
//      
//      KeyStore ks = KeyStore.getInstance("JKS");
//      FileInputStream in = new FileInputStream("c:\\Program Files\\Java\\jre1.8.0_25\\lib\\security\\cacerts");
//      ks.load(in, "changeit".toCharArray());
//      
//      FileInputStream fis = new FileInputStream("c:\\javakeystores\\mail.crt");
//      FileOutputStream fos = new FileOutputStream("c:\\Program Files\\Java\\jre1.8.0_25\\lib\\security\\cacerts");
//      BufferedInputStream bis = new BufferedInputStream(fis);
//      CertificateFactory cf = CertificateFactory.getInstance("X.509");
//      Certificate cert = cf.generateCertificate(bis);
//      
//      KeyStore.Entry newEntry = new KeyStore.TrustedCertificateEntry(cert);
//      ks.setEntry("someAlias", newEntry, null);
//      ks.store(fos, "changeit".toCharArray());
	}
	
	/*private String convertHTMLEntities(String html)
	{
		String[] htmlEntities = new String[]{"&nbsp;"};
		String[] xmlEntities = new String[]{"&#160;"};
		
		//String[][] found = null;
		StringBuilder s = new StringBuilder(html);
		
		for(int i = 0; i < htmlEntities.length; i++)
		{
			//found = findWithRegExp(Pattern.compile(htmlEntities[i]), html);
			while(matchWithRegExp(Pattern.compile(htmlEntities[i]), s.toString()))
			{
				s.replace(s.indexOf(htmlEntities[i]), s.indexOf(htmlEntities[i])+htmlEntities[i].length(), xmlEntities[i]);
			}
		}
		
		return s.toString();
	}*/
	
	private TreeMap<Integer, String> parseFetchResponse(String fetchResponse)
	{
		Map<Integer, String> result = new TreeMap<Integer, String>();
		Pattern p1 = Pattern.compile("\\*\\s(\\d+)\\sFETCH\\s\\(.*\\s\\{(\\d+)\\}");
		
		fetchResponse = fetchResponse.replaceAll("\\n", "\r\n");
		int[] foundIndices = Parser.findIndices(p1, fetchResponse);
		
		if(foundIndices.length == 0)
		{
			return (TreeMap<Integer, String>) result;
		}
		
		String[][] foundString = Parser.findWithRegExp(p1, fetchResponse);
		
		for(int i = 0; i < foundIndices.length; i++)
		{
			Integer mailNumber = Integer.parseInt(foundString[i][1]);
			int dataLength = Integer.parseInt(foundString[i][2]);
			int hLength = foundString[i][0].length();
			int fetchDataBlockStart = foundIndices[i] + hLength;
			int fetchDataBlockEnd = fetchDataBlockStart + dataLength;
			String fetchDataBlock = fetchResponse.substring(fetchDataBlockStart, fetchDataBlockEnd);
			fetchDataBlock = fetchDataBlock.replaceAll("\\r\\n", "\n");
			result.put(mailNumber, fetchDataBlock);
		}
		
		return (TreeMap<Integer, String>) result;
	}
	
	private String contentTransferDecode(String content) throws UnsupportedEncodingException
	{
		Pattern tagPattern = Pattern.compile("--.*-?-?");
		Pattern charsetPattern = Pattern.compile("Content-Type:\\s.*;\\scharset=(.*)");
		Pattern encodeTypePattern = Pattern.compile("Content-Transfer-Encoding:\\s(.*)");
		
		int[] foundIndices = Parser.findIndices(tagPattern, content);
		
		if(foundIndices.length == 0)
		{
			return content;
		}
		
		String[][] foundString = Parser.findWithRegExp(tagPattern, content);
		int transferEncodedDataStart = foundIndices[0];
		int transferEncodedDataEnd = foundIndices[foundIndices.length-1] + foundString[foundString.length-1][0].length();
		StringBuilder result = new StringBuilder(/*content.substring(transferEncodedDataStart, transferEncodedDataEnd)*/);
		
		for(int i = 0; i < foundIndices.length-1; i++)
		{
			int startIndex = foundIndices[i]+foundString[i][0].length();
			String charset = Parser.findWithRegExp(charsetPattern, content.substring(startIndex))[0][1];
			String encodeType = Parser.findWithRegExp(encodeTypePattern, content.substring(startIndex))[0][1];
			String s = null;
			do {
				try {
					s = content.substring(startIndex, startIndex+2);
				} catch (StringIndexOutOfBoundsException e) {
					System.out.println(startIndex);
					break;
				}
				
				
				startIndex++;
				
			} while(!s.equals("\n\n"));
			startIndex++;
			switch (encodeType)
			{
			case "base64":
				result.append(Parser.base64Decode(content.substring(startIndex, foundIndices[i + 1]), charset));
				break;

			default:
				result.append(content.substring(startIndex, foundIndices[i + 1]));
				break;
			}
		}
		
		return new StringBuilder(content).replace(transferEncodedDataStart, transferEncodedDataEnd, result.toString()).toString();
	}
	
	private void authenticatedStateProcessor(String responseLine) throws UnsupportedEncodingException
	{
		//if(responseLine.substring(responseLine.length()-3).equals("=\n"))
		//{
		//if(responseLine.length() > 4)
			//System.out.println(responseLine.substring(responseLine.length()-3));
		//}
		switch (waitingResponseState) 
		{
			case WAITING_LIST:
				if(Parser.matchWithRegExp(this.okListDoneResponsePattern, responseLine))
				{
					List<MailBox> userBoxes = new ArrayList<MailBox>();
					String[][] boxesResponses = Parser.findWithRegExp(this.mailBoxLinePattern, this.response.toString());
					for(String[] boxResponseLine : boxesResponses)
					{
						String attribute = Parser.findWithRegExp(mailBoxAttributePattern, boxResponseLine[0])[0][1];
						//String separator = findFirstWithRegExp(mailBoxSeparatorPattern, boxResponseLine);
						String name      = Parser.findWithRegExp(mailBoxNamePattern,      boxResponseLine[0])[0][1];
						
						String displayName = attribute.substring(1);
						userBoxes.add(new MailBox(/*attribute, separator,*/ name, displayName));
					}
					
					this.response.setLength(0);
					this.response.trimToSize();
					waitingResponseState = WaitingResponseStates.NOT_WAIT;
					notifyObservers(userBoxes.toArray(new MailBox[userBoxes.size()]));
				}
				break;
				
			case WAITING_EXAMINE:
				if(Parser.matchWithRegExp(this.okExamineCompleteResponsePattern, responseLine))
				{
					Integer existsMails = Integer.valueOf(Parser.findWithRegExp(existsMailsCountPattern, this.response.toString())[0][1]);
					
					this.response.setLength(0);
					this.response.trimToSize();
					waitingResponseState = WaitingResponseStates.NOT_WAIT;
					notifyObservers(existsMails);
				}
				break;
				
			case WAITING_FETCH:
				if(Parser.matchWithRegExp(this.okFetchDonePattern, responseLine))
				{
					Map<Integer, String> responseBlocks = parseFetchResponse(this.response.toString());
					responseBlocks = ((TreeMap<Integer, String>) responseBlocks).descendingMap();
					switch (fetchResponseState)
					{
					case WAITING_FETCH_HEADERLIST:
						Mail[] mails = new Mail[responseBlocks.size()];
						
						int i = 0;
						for(Map.Entry<Integer,String> fetchResponseBlock : responseBlocks.entrySet())
						{
							String value = fetchResponseBlock.getValue();
							Map<String, String> headerMap = Parser.parseParametersString(Pattern.compile("\\n?([a-zA-Z-]+):\\s", Pattern.MULTILINE), value);
							mails[i] = new Mail(fetchResponseBlock.getKey(), headerMap);
							mails[i].decodeParameters(new String[]{ "Subject", "From", "To" } );
							i++;
						}
						
						waitingResponseState = WaitingResponseStates.NOT_WAIT;
						fetchResponseState = FetchResponseStates.NOTHING;
						notifyObservers(mails);
						this.response.setLength(0);
						this.response.trimToSize();
						break;
						
					case WAITING_FETCH_MAILBODY:
						//String s = ((TreeMap<String, String>)responseBlocks).firstEntry().getValue();
						Integer firstKey = responseBlocks.keySet().iterator().next();
						String s = responseBlocks.get(firstKey);
						
						//s = s.substring(0, s.indexOf("\n)"));
						
						//s = contentTransferDecode(s);
						s = s.replaceAll("=\\n", "");
						Document doc = Jsoup.parse(s);
						s = doc.body().text();
						//HtmlToPlainText htmlToPlainText = new HtmlToPlainText();
						//s = htmlToPlainText.getPlainText(doc.body());
						try {
							s = Parser.quotedPrintableDecode(s, "utf-8");
						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						
						/* HTMLEditorKit kit = new HTMLEditorKit(); 
						    HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument(); 
						    doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
						try {
							doc.setInnerHTML(null, s);
						} catch (BadLocationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}*/
						/*SAXParserFactory factory = SAXParserFactory.newInstance(); 
						SAXParser parser;
						try {
							parser = factory.newSAXParser();
							SAXPars saxp = new SAXPars(); 
							s = quotedPrintableDecode(s, "utf-8");
							s = convertHTMLEntities(s);
							parser.parse(new InputSource(new ByteArrayInputStream(s.getBytes("utf-8"))), saxp); 
						} catch (ParserConfigurationException e) {
							e.printStackTrace();
						} catch (SAXException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} */
						
						
						waitingResponseState = WaitingResponseStates.NOT_WAIT;
						fetchResponseState = FetchResponseStates.NOTHING;
						/*try {
							notifyObservers(new String(resp.toString().getBytes(), "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}*/
						notifyObservers(s);
						this.response.setLength(0);
						this.response.trimToSize();
						break;

					default:
						break;
					}
					
				}
				break;
	
			default:
				break;
		}
	}
	
	private void notAuthenticateStateProcessor(String response)
	{
		String[][] founds = Parser.findWithRegExp(responsePrefixPattern, response);
		String responsePrefix = "";
		if( founds != null && founds.length > 0 && founds[0].length >=2 )
		{
			responsePrefix = founds[0][1];
		}
		if(this.loginCommandPrefix.equals(responsePrefix))
		{
			founds = Parser.findWithRegExp(responseTagPattern, response);
			String reultTag = "";
			if( founds != null && founds.length > 0 && founds[0].length >=2 )
			{
				reultTag = founds[0][1];
			}
			
			if(this.successfulExecutionTag.equals(reultTag))
			{
				this.state = States.AUTHENTICATED;
				notifyObservers(new Boolean(true));
				this.response.setLength(0);
				this.response.trimToSize();
			}
			else
			{
				notifyObservers(new Boolean(false));
			}
		}
	}
	
	private void responseProcessor(String value) throws UnsupportedEncodingException
	{
		switch (this.state)
		{
			case NOT_AUTHENTICATE:
				notAuthenticateStateProcessor(value);
				break;
				
			case AUTHENTICATED:
				authenticatedStateProcessor(value);
				break;
	
			case SELECTED:
				
				break;
	
			default:
				break;
		}
		
		//waitingFor = "";
	}

	@Override
	public void run()
	{
		try {
			while (on)
        	{
				System.out.println("readLine...");
				String responseLine = ((BufferedReader) bufferedreader).readLine();
				System.out.println(responseLine);
				response.append(responseLine+"\n");
				//if(!matchByRegExp(serverAdditionalResponsePattern, inputString))
				//{
					responseProcessor(responseLine);
				//}
				
				//notifyObservers(inputString);
        	}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bufferedreader.close();
				socket.close();
				bufferedwriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("ServerIO exit");
		//notifyObservers("connection down");
	}

	public void setOn(boolean on) {
		this.on = on;
		if(!this.on)
		{
			try {
				if(socket != null)socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
