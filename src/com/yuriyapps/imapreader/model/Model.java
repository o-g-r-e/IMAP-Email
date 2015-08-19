package com.yuriyapps.imapreader.model;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


public class Model extends Observable implements Observer, Serializable
{
	public static final String fileName = "accounts.data";
	private List<ServerData> serversData;
	private StringBuilder userInputString;
	
	private MailBox[] userBoxes;
	private Mail[] mails;
	
	private int lettersOnScreen;
	
	public Model()
	{
		userInputString = new StringBuilder();
		serversData = new ArrayList<ServerData>();
		lettersOnScreen = 10;
		userBoxes = null;
	}
	
	@Override
	public void notifyObservers(Object arg) {
        setChanged();
        super.notifyObservers(arg);
    }

	@Override
	public void update(Observable o, Object arg)
	{
		switch (o.getClass().getName())
		{
		case "ServerIO":
			notifyObservers(arg);
			break;

		default:
			break;
		}
	}

	public List<ServerData> getServersData() {
		return serversData;
	}

	public StringBuilder getUserInputString() {
		return userInputString;
	}

	public MailBox[] getUserBoxes() {
		return userBoxes;
	}

	public void setUserBoxes(MailBox[] userBoxes) {
		this.userBoxes = userBoxes;
	}

	public Mail[] getMails() {
		return mails;
	}

	public void setMails(Mail[] mails) {
		this.mails = mails;
	}

	public int getLettersOnScreen() {
		return lettersOnScreen;
	}
	
	public void serialize() throws IOException
	{
		OutputStream fos = new FileOutputStream(Model.fileName);
		OutputStream oos = new ObjectOutputStream(fos);
		try {
			this.userInputString.setLength(0);
			this.userInputString.trimToSize();
			((ObjectOutputStream) oos).writeObject(this);
			
		} finally {
			oos.flush();
			oos.close();
			fos.close();
		}
		
	}
}