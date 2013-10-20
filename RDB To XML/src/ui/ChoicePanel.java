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
	private NaryPanel np;
	private GridBagConstraints c;
	private int currLine = 0;
	private List<JPanel> cycleCombolist = new ArrayList<JPanel>();
	private List<JLabel> cyclelabel = new ArrayList<JLabel>();

	public ChoicePanel(TranslatePanel t, NaryPanel np) {
		super();
		this.t = t;
		this.np = np;
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
		// rootCombo.setSelectedIndex(0);
	}

	public JPanel getTranslatePane() {
		return t;
	}
	
	public JPanel getNaryPane() {
		return np;
	}

	public JComboBox getRootCombo() {
		if (rootCombo == null) {
			rootCombo = new JComboBox(rootList);
			rootCombo.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXX");
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

	public void addNextCancelButton() {
		c.gridx = 2;
		c.gridy = 30;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		bottomPanel.add(getNextButton());
		bottomPanel.add(getCancelButton());
		add(bottomPanel, c);
	}
	
	
	
	public List<JComboBox> addSplitCyclePanel(List<List<String>> listCycle) {
		c.gridx = 0;
		c.gridy = ++currLine;
		c.anchor = GridBagConstraints.WEST;
		JLabel cycleLabel = new JLabel(
				"Cycle(s) detected! Choose which entity to split");
		add(cycleLabel, c);
		cyclelabel.add(cycleLabel);

		List<JComboBox> comboList = new ArrayList<JComboBox>();
		for (int i = 0; i < listCycle.size(); i++) {
			List<String> curr = listCycle.get(i);

			if (curr.size() > 2) {
				for (int j = 0; j < curr.size() - 1; j++) {
					c.gridx = 0;
					c.gridy = ++currLine;
					c.fill = GridBagConstraints.NONE;
					c.anchor = GridBagConstraints.WEST;
					JComboBox combo = new JComboBox(
							curr.toArray(new String[curr.size()]));
					int temp = i + 1;
					String currNum = Integer.toString(temp);
					JPanel split = new JPanel(new FlowLayout(
							FlowLayout.TRAILING));
					int temp2 = j+1;
					String currSubNum = Integer.toString(temp2);
					split.add(new JLabel(currNum + "." + currSubNum));
					split.add(combo);
					add(split, c);
					comboList.add(combo);
					cycleCombolist.add(split);
				}
			} else {
				c.gridx = 0;
				c.gridy = ++currLine;
				c.fill = GridBagConstraints.NONE;
				c.anchor = GridBagConstraints.WEST;
				JComboBox combo = new JComboBox(curr.toArray(new String[curr
						.size()]));

				int temp = i + 1;
				String currNum = Integer.toString(temp);
				JPanel split = new JPanel(new FlowLayout(FlowLayout.TRAILING));
				split.add(new JLabel(currNum + ". "));

				split.add(combo);
				add(split, c);
				
				comboList.add(combo);
				cycleCombolist.add(split);
			}
		}
		return comboList;
	}

	public void cleanUp() {
		currLine = 2;
		for (int i = 0; i < cycleCombolist.size(); i++) {
			remove(cycleCombolist.get(i));
			revalidate();
			repaint();
		}
		for (int i = 0; i < cyclelabel.size(); i++) {
			remove(cyclelabel.get(i));
			revalidate();
			repaint();
		}
	}
	
}
