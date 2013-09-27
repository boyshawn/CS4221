package main;

import ui.MainPanel;
import ui.TranslatePanel;
import ui.UIController;

public class RDBToXMLMain {
	
	public static void main(String[] args) {
		
		TranslatePanel t = new TranslatePanel();
		MainPanel m = new MainPanel(t);
		RDBToXML r = new RDBToXML();
		new UIController(m, t, r);
		m.getMainFrame().setVisible(true);
	}
}
