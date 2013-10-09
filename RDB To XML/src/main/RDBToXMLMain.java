package main;

import ui.ChoicePanel;
import ui.MainPanel;
import ui.TranslatePanel;
import ui.UIController;

public class RDBToXMLMain {
	
	public static void main(String[] args) {
		
		TranslatePanel t = new TranslatePanel();
		ChoicePanel rt = new ChoicePanel(t);
		MainPanel m = new MainPanel(rt);
		RDBToXML r = new RDBToXML();
		new UIController(m, rt, t, r);
		m.getMainFrame().setVisible(true);
	}
}
