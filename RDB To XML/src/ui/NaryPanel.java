package ui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class NaryPanel extends JPanel {

	private TranslatePanel t;
	private List<JPanel> panellist = new ArrayList<JPanel>();
	private List<JLabel> labellist = new ArrayList<JLabel>();
	private int currLine = 0;
	private GridBagConstraints c;
	private JButton nextButton;
	private JButton prevButton;
	
	public NaryPanel(TranslatePanel t) {
		super();
		this.t = t;
		
		setPreferredSize(new Dimension(600, 400));

		setLayout(new GridBagLayout());

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.insets = new Insets(3, 3, 3, 3);

		JLabel chooseLabel = new JLabel("Specify the order for n-ary relationship");
		add(chooseLabel, c);
		
	    currLine = 0;
	    getNextButton();
		getPrevButton();
	}
	
	public Pair<JButton, JLabel> addChoicePanel(String relName,
			List<String> listS) {
		c.gridx = 0;
		c.gridy = ++currLine;
		c.anchor = GridBagConstraints.WEST;
		JLabel relation = new JLabel("Relation name: " + relName);
		add(relation, c);
		labellist.add(relation);

		JButton changeOrderButton = new JButton("Change Order");
		JLabel currOrder = new JLabel("");
		for (int i = 0; i < listS.size(); i++) {
			currOrder.setText(currOrder.getText() + listS.get(i) + " ");
		}

		c.gridx = 0;
		c.gridy = ++currLine;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		JPanel orderPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		orderPanel.add(changeOrderButton);
		orderPanel.add(currOrder);
		add(orderPanel, c);

		panellist.add(orderPanel);
		Pair<JButton, JLabel> p = new Pair(changeOrderButton, currOrder);
		return p;
	}
	
	// the pop up window
	public Pair<JPanel, ArrayList<JComboBox>> getOptionPane(List<String> listS) {
		JPanel panel = new JPanel();
		ArrayList<JComboBox> combolist = new ArrayList<JComboBox>();

		for (int i = 0; i < listS.size(); i++) {
			JComboBox combo = new JComboBox(listS.toArray(new String[listS
					.size()]));
			combo.setSelectedIndex(i);
			panel.add(combo);
			combolist.add(combo);
		}

		Pair<JPanel, ArrayList<JComboBox>> p = new Pair(panel, combolist);

		return p;
	}	
	
	public void cleanUp() {
		currLine = 0;
		for (int i = 0; i < labellist.size(); i++) {
			remove(labellist.get(i));
			revalidate();
			repaint();
		}
		for (int i = 0; i < panellist.size(); i++) {
			remove(panellist.get(i));
			revalidate();
			repaint();
		}
	}

	private JButton getPrevButton() {
		if (prevButton == null) {
			prevButton = new JButton("Cancel");
		}
		return prevButton;
	}

	private JButton getNextButton() {
		if (nextButton == null) {
			nextButton = new JButton("Next");
		}
		return nextButton;
	}
	
	void addPrevListener(ActionListener listenForCancelButton) {
		prevButton.addActionListener(listenForCancelButton);
	}

	void addNextListener(ActionListener listenForNextButton) {
		nextButton.addActionListener(listenForNextButton);
	}
	
	void addChangeOrderListener(ActionListener listenForOrderButton, JButton b) {
		b.addActionListener(listenForOrderButton);
	}
	
	public JPanel getTranslatePane() {
		return t;
	}
	
	public void addNextCancelButton() {
		c.gridx = 2;
		c.gridy = 30;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		bottomPanel.add(getNextButton());
		bottomPanel.add(getPrevButton());
		add(bottomPanel, c);
	}
}
