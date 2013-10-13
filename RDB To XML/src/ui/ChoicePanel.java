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

public class ChoicePanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private String[] rootList = { "" };
	private JComboBox rootCombo;
	private JButton nextButton;
	private JButton cancelButton;
	private TranslatePanel t;
	private GridBagConstraints c;
	private int currLine = 0;

	public ChoicePanel(TranslatePanel t) {
		super();
		this.t = t;
		setPreferredSize(new Dimension(600, 400));

		setLayout(new GridBagLayout());

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.insets = new Insets(3, 3, 3, 3);

		JLabel chooseLabel = new JLabel("Choose the most important entity");
		add(chooseLabel, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(getRootCombo(), c);

		currLine = 2;

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
			rootCombo
					.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXX");
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

	public Pair<JButton, JLabel> addChoicePanel(String relName,
			List<String> listS) {
		c.gridx = 0;
		c.gridy = ++currLine;
		JLabel relation = new JLabel("Relation name: " + relName);
		add(relation, c);

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

		Pair<JButton, JLabel> p = new Pair(changeOrderButton, currOrder);
		return p;
	}

	public void addNextCancelButton() {
		c.gridx = 2;
		currLine = currLine + 1;
		c.gridy = ++currLine;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		bottomPanel.add(getNextButton());
		bottomPanel.add(getCancelButton());
		add(bottomPanel, c);
	}

	// the pop up window
	public Pair<JPanel, ArrayList<JComboBox>> getOptionPane(List<String> listS) {
		JPanel panel = new JPanel();
		ArrayList<JComboBox> combolist = new ArrayList<JComboBox>();

		for (int i = 0; i < listS.size(); i++) {
			JComboBox combo = new JComboBox(listS.toArray(new String[listS
					.size()]));
			combo.setSelectedIndex(0);
			panel.add(combo);
			combolist.add(combo);
		}

		Pair<JPanel, ArrayList<JComboBox>> p = new Pair(panel, combolist);

		return p;
	}

	public List<JComboBox> addSplitCyclePanel(List<List<String>> listCycle) {
		c.gridx = 0;
		c.gridy = ++currLine;
		JLabel cycleLabel = new JLabel(
				"Cycle(s) detected! Choose which entity to split");
		add(cycleLabel, c);

		List<JComboBox> comboList = new ArrayList<JComboBox>();
		for (int i = 0; i < listCycle.size(); i++) {
			List<String> curr = listCycle.get(i);
			c.gridx = 0;
			c.gridy = ++currLine;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.WEST;
			JComboBox combo = new JComboBox(
					curr.toArray(new String[curr.size()]));
			
			int temp = i + 1;
			String currNum = Integer.toString(temp);
			JPanel split = new JPanel(new FlowLayout(FlowLayout.TRAILING));
			split.add(new JLabel(currNum + ". "));
			split.add(combo);
			add(split, c);

			comboList.add(combo);
		}
		return comboList;
	}
}
