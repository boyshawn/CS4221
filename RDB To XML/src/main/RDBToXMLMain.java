package main;

import ui.ChooseRootPanel;
import ui.MainPanel;
import ui.TranslatePanel;
import ui.UIController;

public class RDBToXMLMain {
	
	public static void main(String[] args) {
		
		TranslatePanel t = new TranslatePanel();
		ChooseRootPanel rt = new ChooseRootPanel(t);
		MainPanel m = new MainPanel(rt);
		RDBToXML r = new RDBToXML();
		new UIController(m, rt, t, r);
		m.getMainFrame().setVisible(true);
	}
}
