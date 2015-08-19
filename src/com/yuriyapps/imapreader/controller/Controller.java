package com.yuriyapps.imapreader.controller;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.text.BadLocationException;

import com.yuriyapps.imapreader.model.Mail;
import com.yuriyapps.imapreader.model.MailBox;
import com.yuriyapps.imapreader.model.Model;
import com.yuriyapps.imapreader.model.ServerData;
import com.yuriyapps.imapreader.view.View;


public class Controller implements KeyListener, Observer, MouseListener, MouseMotionListener
{
	private Model model;
	private View view;
	
	private enum States { CHOOSE_SERVER, ADD_SERVER, CHOOSE_ACCOUNT, ADD_ACCOUNT, ENTER_PASSWORD, CHOOSE_MAILBOX, CHOOSE_MAIL, IN_MAIL,
						  WAITING_LOGIN_RESPONSE, WAITING_LIST_RESPONSE, WAITING_EXAMINE_RESPONSE, WAITING_MAILS_LIST_RESPONSE, WAITING_MAIL };
	private States state;
	private int currentServerIndex;
	private int currentAccountIndex;
	private int currentMailNumber;
	private ServerIO serverIO;

	public Controller(Model model, View view)
	{
		this.model = model;
		this.view = view;
		this.currentServerIndex = -1;
		this.currentAccountIndex = -1;
		this.currentMailNumber = -1;
		this.serverIO = new ServerIO();
		this.serverIO.addObserver(this);
		try {
			this.view.showFirstScreen(fetchServerNamesList(model.getServersData()));
		} catch (BadLocationException e) {
			
		}
		state = States.CHOOSE_SERVER;
	}
	
	private String buildList(String[] strings)
	{
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < strings.length; i++)
		{
			result.append("[");
			result.append(i);
			result.append("] ");
			result.append(strings[i]);
			result.append("\n");
		}
		
		return result.toString();
	}
	
	private String[] prepareServerNames(List<ServerData> serversData)
	{
		List<String> serversArray = new ArrayList<String>();
		for (ServerData serverData : serversData) {
			serversArray.add(serverData.getServerUrl());
		}
		
		return serversArray.toArray(new String[serversArray.size()]);
	}
	
	private String fetchServerNamesList(List<ServerData> serversData)
	{
		String[] serversArray =  prepareServerNames(serversData);
		return buildList(serversArray);
	}
	
	private void processor(String command) throws BadLocationException, IOException, NumberFormatException
	{
		switch(state)
		{
		
		case CHOOSE_SERVER:
			switch (command) {
			case "a":
				view.showAddServerScreen();
				state = States.ADD_SERVER;
				break;
				
			case "e":
				System.exit(0);
				break;

			default:
				int n = Integer.parseInt(command);
				if(n >= 0 && n < model.getServersData().size())
				{
					this.currentServerIndex = n;
					view.addMessageInScreen("Connection...");
							
					this.serverIO.connect(model.getServersData().get(this.currentServerIndex).getServerUrl());
						
					new Thread(this.serverIO).start();
					this.changeToChooseAccount();
				}
				break;
			}
			break;
			
		case ADD_SERVER:
			if(command.equals("b"))
			{
				changeToChooseServer();
			}
			else
			{
				model.getServersData().add(new ServerData(command));
				model.serialize();
				changeToChooseServer();
			}
			break;
			
		case CHOOSE_ACCOUNT:
			if(command.equals("b"))
			{
				this.currentServerIndex = -1;
				this.serverIO.setOn(false);
				this.serverIO = new ServerIO();
				changeToChooseServer();
			}
			else
			{
				if(command.equals("a"))
				{
					this.view.showAddAccountScreen();
					state = States.ADD_ACCOUNT;
				}
				else
				{
					int n = Integer.parseInt(command);
					if(n >= 0 && n < model.getServersData().get(this.currentServerIndex).getUserLogins().size())
					{
						this.currentAccountIndex = n;
						String login = model.getServersData().get(this.currentServerIndex).getUserLogins().get(this.currentAccountIndex);
						this.view.showLoginScreen(login);
						state = States.ENTER_PASSWORD;
					}
				}
			}
			break;
			
		case ADD_ACCOUNT:
			if(command.equals("b"))
			{
				this.changeToChooseAccount();
			}
			else
			{
				model.getServersData().get(this.currentServerIndex).getUserLogins().add(command);
				model.serialize();
				this.changeToChooseAccount();
			}
			break;
			
		case ENTER_PASSWORD:
			if(command.equals("b"))
			{
				this.currentAccountIndex = -1;
				changeToChooseAccount();
			}
			else
			{
				String login = model.getServersData().get(this.currentServerIndex).getUserLogins().get(this.currentAccountIndex);
				this.serverIO.imapLogin(login, command);
				state = States.WAITING_LOGIN_RESPONSE;
			}
			break;
			
		case CHOOSE_MAILBOX:
			if(command.equals("b"))
			{
				reset();
			}
			else
			{
				int n = Integer.parseInt(command);
				if(n >= 0 && n < model.getUserBoxes().length)
				{
					this.serverIO.imapExamine(this.model.getUserBoxes()[n].getName());
					this.state = States.WAITING_EXAMINE_RESPONSE;
				}
			}
			break;
			
		case CHOOSE_MAIL:
			if(command.equals("b"))
			{
				changeToChooseMailBox();
			}
			else
			{
				int n = Integer.parseInt(command);
				if(n >= 0 && n < model.getMails().length)
				{
					this.currentMailNumber = n;
					this.serverIO.fetchMailBody(model.getMails()[this.currentMailNumber].getNumber());
					state = States.WAITING_MAIL;
				}
			}
			break;
			
		case IN_MAIL:
			if(command.equals("b"))
			{
				changeToChooseMail();
			}
			break;

		default:
			break;
		}
	}
	
	private void reset() throws BadLocationException
	{
		this.currentAccountIndex = -1;
		this.currentServerIndex = -1;
		this.serverIO.setOn(false);
		this.serverIO = new ServerIO();
		this.serverIO.addObserver(this);
		changeToChooseServer();
	}
	
	private void changeToChooseAccount() throws BadLocationException
	{
		String accounts = buildList(this.model.getServersData().get(this.currentServerIndex).getUserLogins().toArray(new String[this.model.getServersData().get(this.currentServerIndex).getUserLogins().size()]));
		this.view.showChooseAccountScreen(this.model.getServersData().get(this.currentServerIndex).getServerUrl(), accounts);
		this.state = States.CHOOSE_ACCOUNT;
	}
	
	private void changeToChooseServer() throws BadLocationException
	{
		this.view.showFirstScreen(fetchServerNamesList(model.getServersData()));
		this.state = States.CHOOSE_SERVER;
	}
	
	private void changeToChooseMailBox() throws BadLocationException
	{
		List<String> mailBoxesNames = new ArrayList<String>();
		for(MailBox mailBox : model.getUserBoxes())
		{
			mailBoxesNames.add(mailBox.getDisplayName());
		}
		view.showChooseMailBoxScreen(model.getServersData().get(this.currentServerIndex).getServerUrl(), buildList(mailBoxesNames.toArray(new String[mailBoxesNames.size()])));
		state = States.CHOOSE_MAILBOX;
	}
	
	private void changeToChooseMail() throws BadLocationException
	{
		String[] subjects = new String[model.getMails().length];
		Map<String, String> mailParameters = null;
		for(int i = 0; i < subjects.length; i++)
		{
			mailParameters = model.getMails()[i].getParameters();
			if(mailParameters.containsKey("Subject"))
			{
				subjects[i] = mailParameters.get("Subject");
			}
			else
			{
				subjects[i] = "Empty";
			}
		}
		
		view.showChooseMailScreen(model.getServersData().get(this.currentServerIndex).getServerUrl(), buildList(subjects));
		state = States.CHOOSE_MAIL;
	}

	@Override
	public void keyPressed(KeyEvent keyEvent)
	{
		StringBuilder userInputString = model.getUserInputString();
		
		switch(keyEvent.getKeyCode())
		{
		case 37: case 38: case 39: case 40: case 16: case 33: case 34: case 35: case 36:
			keyEvent.consume();
			break;
			
		case 10: //Enter
			keyEvent.consume();
			try {
				view.getTextPane().getDocument().remove(view.getTextPane().getCaretPosition()-userInputString.length(), userInputString.length());
				processor(userInputString.toString());
				
			} catch (BadLocationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} finally {
				userInputString.setLength(0);
				userInputString.trimToSize();
			}
			break;
			
		case 8: //Backspace
			if(userInputString.length() > 0)
			{
				userInputString.deleteCharAt(userInputString.length()-1);
			}
			else
			{
				keyEvent.consume();
			}
			break;

		default:
			userInputString.append(keyEvent.getKeyChar());
			
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		
	}

	@Override
	public void update(Observable arg0, Object object)
	{
		if(arg0.getClass().getSimpleName().equals("ServerIO"))
		{
			try {
				switch(this.state)
				{
				case WAITING_LOGIN_RESPONSE:
					Boolean authenticated = (Boolean) object;
					if(authenticated.booleanValue())
					{
						this.serverIO.imapList("", "*");
						state = States.WAITING_LIST_RESPONSE;
					}
					else
					{
						view.addMessageInScreen("Failed password");
						state = States.ENTER_PASSWORD;
					}
					break;
					
				case WAITING_LIST_RESPONSE:
					MailBox[] mailBoxes = (MailBox[]) object;
					model.setUserBoxes(mailBoxes);
					changeToChooseMailBox();
					break;
					
				case WAITING_EXAMINE_RESPONSE:
					Integer existsMails = (Integer) object;
					
					if(model.getLettersOnScreen() > existsMails.intValue())
					{
						this.serverIO.fetchMailList(1, existsMails);
					}
					else
					{
						this.serverIO.fetchMailList((existsMails - model.getLettersOnScreen()) + 1, existsMails);
					}
						
					state = States.WAITING_MAILS_LIST_RESPONSE;
					break;
					
				case WAITING_MAILS_LIST_RESPONSE:
					Mail[] mails = (Mail[]) object;
					model.setMails(mails);
					changeToChooseMail();
					break;
					
				case WAITING_MAIL:
					String mailContent = (String) object;
					String subject = model.getMails()[this.currentMailNumber].param("Subject");
					String from = model.getMails()[this.currentMailNumber].param("From");
					String to = model.getMails()[this.currentMailNumber].param("To");
					String date = model.getMails()[this.currentMailNumber].param("Date");
					
					String header = "Subject: "+subject+"\nFrom: "+from+"\nTo: "+to+"\nDate: "+date+"\n\n";
					
					view.showMailScreen(model.getServersData().get(this.currentServerIndex).getServerUrl(), header+mailContent);
					state = States.IN_MAIL;
					break;
					
				default:
					break;
				}
			} catch (ClassCastException e) {
				try {
					changeToChooseServer();
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		view.caretToEnd();
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		
	}
}