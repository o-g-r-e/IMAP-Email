package com.yuriyapps.imapreader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import com.yuriyapps.imapreader.controller.Controller;
import com.yuriyapps.imapreader.model.Model;
import com.yuriyapps.imapreader.view.View;

public class Main
{
	private static Model model;
	private static View view;
	private static Controller controller;
	
	public static void main(String[] args)
	{
		InputStream fis = null;
		InputStream oin = null;
		try {
			fis = new FileInputStream(Model.fileName);
			oin = new ObjectInputStream(fis);
			model = (Model) ((ObjectInputStream) oin).readObject();
		} catch (FileNotFoundException e) {
			model = new Model();
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(oin != null)oin.close();
				if(fis != null)fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		view = new View();
		controller = new Controller(model, view);
		model.addObserver(view);
		view.getTextPane().addKeyListener(controller);
		view.getTextPane().addMouseListener(controller);
//		sslClient = new ServerIO("imap.mail.ru", 993);
//		new Thread(sslClient).start();
//		keyboardReader = new KeyboardReader();
//		new Thread(keyboardReader).start();
//        
//		while(true)
//		{
//			String s = sslClient.getInputString();
//			if((s != null) && (!s.equals("")))
//			{
//				System.out.println(s);
//				sslClient.setInputString("");
//			}
//			
//			s = keyboardReader.getInputString();
//			if(!"".equals(s))
//			{
//				if("exit".equals(s))
//					break;
//				
//				try {
//					sslClient.writeOut(s);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				keyboardReader.setInputString("");
//			}
//		}
//		
//		sslClient.threadExit();
//		keyboardReader.threadExit();
	}
}
