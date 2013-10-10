package ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ChoicePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private String[] rootList = {""};
	private JComboBox rootCombo;
	private JButton nextButton;
	private JButton cancelButton;
	private TranslatePanel t;
	private BoxLayout layout;
	
	public ChoicePanel(TranslatePanel t) {
		super();
		this.t = t;
		setPreferredSize(new Dimension(600, 400));
		
		layout = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(layout);
		
		add(Box.createRigidArea(new Dimension(0,50)));
		
		JLabel chooseLabel = new JLabel("Choose the most important entity");
		chooseLabel.setAlignmentX(RIGHT_ALIGNMENT);
		add(chooseLabel, layout);
		
		JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		comboPanel.add(getRootCombo());	
		comboPanel.add(new JLabel("                                   "));
		add(comboPanel, layout);
		
		getNextButton();
		getCancelButton();
	}
	
	public void setRootList(String[] rlist) {
		rootList = rlist;
		rootCombo.removeAllItems();
		for (int i = 0; i < rootList.length; i++) {
			rootCombo.addItem(rootList[i]);
		}
		rootCombo.setSelectedIndex(0);
	}
	
	public JPanel getTranslatePane() {
		return t;
	}
	
	public JComboBox getRootCombo() {
		if (rootCombo == null) {
			rootCombo = new JComboBox(rootList);
			rootCombo.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			rootCombo.setSelectedIndex(0);
		}
		return rootCombo;
	}
	
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton("Cancel");
		}
		return cancelButton;
	}
	
	private JButton getNextButton() {
		if (nextButton == null) {
			nextButton = new JButton("Next");
		}
		return nextButton;
	}
	
	void addCancelListener(ActionListener listenForCancelButton) {
		cancelButton.addActionListener(listenForCancelButton);
	}
	
	void addNextListener(ActionListener listenForNextButton) {
		nextButton.addActionListener(listenForNextButton);
	}
	
	void addChangeOrderListener(ActionListener listenForOrderButton, JButton b) {
		b.addActionListener(listenForOrderButton);
	}
	
	public Pair<JButton, JLabel> addChoicePanel(String relName, List<String> listS) {
		JLabel relation = new JLabel("Relation name: " + relName);
		add(relation, layout);
		
		JButton changeOrderButton = new JButton("Change Order");
		JLabel currOrder = new JLabel("");
		for (int i = 0; i < listS.size(); i++) {
			currOrder.setText(currOrder.getText() + listS.get(i) + " ");
		}
		
		JPanel orderPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		orderPanel.add(changeOrderButton);
		orderPanel.add(currOrder);
		add(orderPanel, layout);
		
		Pair<JButton, JLabel> p = new Pair(changeOrderButton, currOrder);
		return p;
	}
	
	public void addNextCancelButton() {
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		bottomPanel.add(getNextButton());
		bottomPanel.add(getCancelButton());
		bottomPanel.add(new JLabel("                                     "));
		bottomPanel.add(new JLabel("                                     "));
		bottomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(bottomPanel, layout);
	}
	
	// the pop up window
	public Pair<JPanel, ArrayList<JComboBox>> getOptionPane(List<String> listS) {
		JPanel panel = new JPanel();
		ArrayList<JComboBox> combolist = new ArrayList<JComboBox>();
		
		for (int i = 0; i < listS.size(); i++) {
			JComboBox combo = new JComboBox(listS.toArray(new String[listS.size()]));
			combo.setSelectedIndex(0);
			panel.add(combo);
			combolist.add(combo);
		}
		
		Pair<JPanel, ArrayList<JComboBox>> p = new Pair(panel, combolist);
		
		return p;
	}
	
	public List<JComboBox> addSplitCyclePanel(List<List<String>> c) {
		add(new JLabel("Cycle(s) detected! Choose which entity to split"), layout);
		List<JComboBox> comboList = new ArrayList<JComboBox>();
		for (int i = 0; i < c.size(); i++) {
			List<String> curr = c.get(i);
			for (int j = 0; j < curr.size(); j++) {
				JComboBox combo = new JComboBox(curr.toArray(new String[curr.size()]));
				add(combo, layout);
				comboList.add(combo);
			}
		}
		return comboList;
	}
}
