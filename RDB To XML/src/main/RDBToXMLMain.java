package main;

import ui.ChoicePanel;
import ui.MainPanel;
import ui.NaryPanel;
import ui.TranslatePanel;
import ui.UIController;

public class RDBToXMLMain {
	
	public static void main(String[] args) {
		
		TranslatePanel t = new TranslatePanel();
		NaryPanel np = new NaryPanel(t);
		ChoicePanel rt = new ChoicePanel(t, np);
		MainPanel m = new MainPanel(rt);
		RDBToXML r = new RDBToXML();
		new UIController(m, rt, np, t, r);
		m.getMainFrame().setVisible(true);
	}
}
