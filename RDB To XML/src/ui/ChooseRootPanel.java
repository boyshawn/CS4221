package ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ChooseRootPanel extends JPanel {
	
	private String[] rootList = {"root1", "root2", "root3"};
	private JComboBox rootCombo;
	private JButton nextButton;
	private JButton cancelButton;
	private TranslatePanel t;
	
	public ChooseRootPanel(TranslatePanel t) {
		super();
		this.t = t;
		setPreferredSize(new Dimension(600, 400));
		
		BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(layout);
		
		add(Box.createRigidArea(new Dimension(0,150)));
		
		JLabel chooseLabel = new JLabel("Choose the most important entity");
		chooseLabel.setAlignmentX(RIGHT_ALIGNMENT);
		add(chooseLabel, layout);
		
		JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		comboPanel.add(getRootCombo());	
		comboPanel.add(new JLabel("                                   "));
		add(comboPanel, layout);
		
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		bottomPanel.add(getNextButton());
		bottomPanel.add(getCancelButton());
		bottomPanel.add(new JLabel("                                     "));
		bottomPanel.add(new JLabel("                                     "));
		bottomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(bottomPanel, layout);
		
		add(Box.createRigidArea(new Dimension(0,130)));
	}
	
	public void setRootList(String[] rlist) {
		rootList = rlist;
	}
	
	public JPanel getTranslatePane() {
		return t;
	}
	
	private JComboBox getRootCombo() {
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
}
