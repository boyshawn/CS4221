package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 
 * @author Sarah
 *
 */

public class TranslatePanel extends JPanel {
	private JButton browseButton;
	private JButton translateButton;
	private JButton cancelButton;
	private JTextField pathField;
	private JTextField xmlField;
	private JLabel errorMsgLabel;

	public TranslatePanel() {
		super();
		setPreferredSize(new Dimension(600, 400));

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		errorMsgLabel = new JLabel(" ");
		errorMsgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		errorMsgLabel.setForeground(Color.RED);
		add(errorMsgLabel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		add(new JLabel("Enter the XML file name"), c);

		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;
		add(getXmlNameField(), c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		add(new JLabel("Save to                               "), c);

		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 2;
		add(getPathField(), c);

		c.gridx = 4;
		c.gridy = 2;
		c.gridwidth = 1;
		add(getBrowseButton(), c);

		c.gridx = 1;
		c.gridy = 4;
		c.gridwidth = 3;
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		bottomPanel.add(getTranslateButton());
		bottomPanel.add(getCancelButton());
		bottomPanel.add(new JLabel("              "));
		add(bottomPanel, c);

	}

	private JButton getTranslateButton() {
		if (translateButton == null) {
			translateButton = new JButton("Translate");
		}
		return translateButton;
	}

	private JButton getBrowseButton() {
		if (browseButton == null) {
			browseButton = new JButton("Browse");
		}
		return browseButton;
	}

	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton("Cancel");
		}
		return cancelButton;
	}

	private JTextField getPathField() {
		if (pathField == null) {
			pathField = new JTextField(30);
			pathField.setEditable(false);
		}
		return pathField;
	}

	private JTextField getXmlNameField() {
		if (xmlField == null) {
			xmlField = new JTextField(30);
		}
		return xmlField;
	}
	
	public String getFilename() {
		return xmlField.getText();
	}
	
	public String getPath() {
		return pathField.getText();
	}
	
	public void setPath(String path) {
		pathField.setText(path);
	}
	
	void addTranslateListener(ActionListener listenForTranslateButton) {
		translateButton.addActionListener(listenForTranslateButton);
	}
	
	void addCancelListener(ActionListener listenForCancelButton) {
		cancelButton.addActionListener(listenForCancelButton);
	}
	
	void addBrowseListener(ActionListener listenForBrowseButton) {
		browseButton.addActionListener(listenForBrowseButton);
	}
	
	void setErrorMsg(String msg) {
		errorMsgLabel.setText(msg);
	}
	
	void displaySuccessfulMsg() {
		JOptionPane.showMessageDialog(this, "Translation successful!", "Message",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	void emptiedField() {
		xmlField.setText("");
		pathField.setText("");
	}
}
