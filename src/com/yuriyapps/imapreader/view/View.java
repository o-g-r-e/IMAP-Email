package com.yuriyapps.imapreader.view;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowListener;
import java.util.Observer;
import java.util.Observable;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


public class View implements Observer
{
	private JFrame mainWindow;
	//JTextArea textArea;
	private JTextPane textPane;
	
	private StyledDocument doc;
	private Style style;
	
	public View()
	{
		mainWindow = new JFrame(); 
		mainWindow.setExtendedState( mainWindow.getExtendedState()|JFrame.MAXIMIZED_BOTH );
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		mainWindow.setSize(800, 600); 
		
		mainWindow.setLayout(new BorderLayout());
		textPane = new JTextPane();
		JScrollPane scrollPane = new JScrollPane(textPane);
		Font BigFontTR = new Font("fantasy", Font.BOLD, 30);
		textPane.setFont(BigFontTR);
		textPane.setBackground(Color.black);
		textPane.setCaretColor(new Color(0, 200, 0));
		textPane.setSelectionColor(new Color(0, 200, 0));
		textPane.setSelectedTextColor(Color.black);
		
		textPane.setCaret(new MyCaret());
		//textPane.setFocusable(false);
		doc = (StyledDocument) textPane.getDocument();
		style = doc.addStyle("StyleName", null);
	    StyleConstants.setForeground(style, new Color(0, 200, 0));
	    StyleConstants.setBackground(style, Color.black);
	    
		mainWindow.add(scrollPane, BorderLayout.CENTER);
		
		mainWindow.setVisible(true);
	}
	
	private void renderScreen(String screen) throws BadLocationException
	{
		doc.remove(0, doc.getLength());
		doc.insertString(0, screen, style);
		textPane.setCaretPosition(doc.getLength());
	}
	
	public void caretToEnd()
	{
		textPane.setCaretPosition(doc.getLength());
	}
	
	public void showFirstScreen(String servers) throws BadLocationException
	{
		String choose = "";
		if(servers != null && servers.length() > 0)
		{
			choose = "Enter number to choose server, ";
		}
		
		String screen = "Hello user !\n\nAvalable servers:\n\n"+servers+"\n\nYou may: "+choose+"[a]dd new server, or [e]xit\n>";
		renderScreen(screen);
	}
	
	public void showAddServerScreen() throws BadLocationException
	{
		String screen = "Enter the IMAP server url, [b]ack to return\n\n>";
		renderScreen(screen);
	}
	
	public void showAddAccountScreen() throws BadLocationException
	{
		String screen = "Enter the login. Format: login@example.ru. [B]ack to return\n\n>";
		renderScreen(screen);
	}
	
	public void showChooseAccountScreen(String server, String accounts) throws BadLocationException
	{
		String choose = "";
		if(accounts != null && accounts.length() > 0)
		{
			choose = "Enter number to choose account, ";
		}
		
		String screen = "Server: "+server+"\n\nYour accounts:\n"+accounts+"\n\nYou may: "+choose+"[a]dd new account, [b]ack to return\n>";
		renderScreen(screen);
	}
	
	public void showLoginScreen(String login) throws BadLocationException
	{
		String screen = "Enter password for "+login+", [b]ack to return\n\n>";
		renderScreen(screen);
	}
	
	public void showChooseMailBoxScreen(String server, String mailBoxNames) throws BadLocationException
	{
		String screen = "Server: "+server+"\n\nYour mail boxes:\n"+mailBoxNames+"\n\nYou may: Enter number to choose mail box, [b]ack to return\n>";
		renderScreen(screen);
	}
	
	public void showChooseMailScreen(String server, String mailSubjects) throws BadLocationException
	{
		String screen = "Server: "+server+"\n\nYour last letters:\n"+mailSubjects+"\n\nYou may: Enter number to choose mail, [b]ack to return\n>";
		renderScreen(screen);
	}
	
	public void showMailScreen(String server, String mailText) throws BadLocationException
	{
		String screen = "Server: "+server+"\n\n"+mailText+"\n\nYou may: [b]ack to return\n>";
		renderScreen(screen);
	}
	
	public void addMessageInScreen(String message) throws BadLocationException
	{
		String scr1 = doc.getText(0, doc.getText(0, doc.getLength()).indexOf(">")-1);
		renderScreen(scr1+"\n"+message+"\n>");
	}

	@Override
	public void update(Observable o, Object arg)
	{
		switch (o.getClass().getName())
		{
		case "Model":
			//textArea.append((String)arg+"\n");
			
		    try {
				doc.insertString(doc.getLength(), (String)arg+"\n", style);
				
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			System.out.println((String)arg);
			break;

		default:
			break;
		}
	}
	
	public JFrame getMainWindow()
	{
		return mainWindow;
	}
	
	public void setMainWindowListener(WindowListener listener)
	{
		mainWindow.addWindowListener(listener);
	}

	public JTextPane getTextPane() {
		return textPane;
	}
}