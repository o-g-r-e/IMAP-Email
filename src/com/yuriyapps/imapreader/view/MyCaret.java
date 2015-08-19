package com.yuriyapps.imapreader.view;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

public class MyCaret extends DefaultCaret
{
	protected synchronized void damage(Rectangle r)
	{
		if (r == null)
	      return;

	    // give values to x,y,width,height (inherited from java.awt.Rectangle)
	    x = r.x;
	    y = r.y;
	    height = r.height;
	    // A value for width was probably set by paint(), which we leave alone.
	    // But the first call to damage() precedes the first call to paint(), so
	    // in this case we must be prepared to set a valid width, or else
	    // paint()
	    // will receive a bogus clip area and caret will not get drawn properly.
	    if (width <= 0)
	      width = getComponent().getWidth();

	    repaint(); // calls getComponent().repaint(x, y, width, height)
	}
	
	public void paint(Graphics g)
	{
		JTextComponent comp = getComponent();
	    
	    if (comp == null)
	    	return;

	    int dot = getDot();
	    Rectangle r = null;
	    char dotChar;
	    
	    try {
	    	r = comp.modelToView(dot);
	    	if (r == null)
	    		return;
	    	dotChar = comp.getText(dot, 1).charAt(0);
	    } catch (BadLocationException e) {
	    	return;
	    }

	    if ((x != r.x) || (y != r.y))
	    {
	    	// paint() has been called directly, without a previous call to
	    	// damage(), so do some cleanup. (This happens, for example, when
	    	// the
	    	// text component is resized.)
	    	repaint(); // erase previous location of caret
	    	x = r.x; // Update dimensions (width gets set later in this method)
	    	y = r.y;
	    	height = r.height;
	    }

	    g.setColor(comp.getCaretColor());
	    g.setXORMode(comp.getBackground()); // do this to draw in XOR mode

	    if (dotChar == '\n')
	    {
	    	int diam = r.height;
	    	if (isVisible())
	    		g.fillRect(r.x, r.y, width, r.height);
	    	//width = diam / 2 + 2;
	    	width=2;
	    	return;
	    }

	    if (dotChar == '\t')
	    {
	    	try {
	    		Rectangle nextr = comp.modelToView(dot + 1);
	    		if ((r.y == nextr.y) && (r.x < nextr.x))
	    		{
	    			//width = nextr.x - r.x;
	    			width=5;
	    			if (isVisible())
	    				g.fillRoundRect(r.x, r.y, width, r.height, 12, 12);
	          
	    			return;
	        
	    		}
	    		else
	    		{
	    			dotChar = ' ';
	    		}
	      
	    	} catch (BadLocationException e) {
	    		dotChar = ' ';
	    	}
	    }
	    // width = g.getFontMetrics().charWidth(dotChar);
	    width=2;
	    if (isVisible())
	    	g.fillRect(r.x, r.y, width, r.height);
	  }

	  /*public static void main(String args[]) {
	    JFrame frame = new JFrame("FancyCaret demo");
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    JTextArea area = new JTextArea(8, 32);
	    area.setCaret(new FancyCaret());
	    area.setText("VI\tVirgin Islands \nVA      Virginia\nVT\tVermont");
	    frame.getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
	    frame.pack();
	    frame.setVisible(true);
	  }*/
}
