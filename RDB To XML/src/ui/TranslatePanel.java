package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class TranslatePanel extends JPanel {


	private static final long serialVersionUID = 1L;
	private JButton browseButton;
	private JButton translateButton;
	private JButton prevButton;
	private JTextField pathField;
	private JTextField xmlField;
	private JLabel errorMsgLabel;

	public TranslatePanel() {
		super();
		setPreferredSize(new Dimension(600, 400));
		
		BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
		//setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setLayout(layout);
		
		add(Box.createRigidArea(new Dimension(0,130)));
		
		errorMsgLabel = new JLabel(" ");
		errorMsgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		errorMsgLabel.setForeground(Color.RED);
		add(errorMsgLabel);
		
		JPanel xmlNamePane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		xmlNamePane.add(new JLabel("Enter the XML file name"));
		xmlNamePane.add(getXmlNameField());
		xmlNamePane.add(new JLabel("                                "));
		xmlNamePane.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(xmlNamePane, layout);
		
		JPanel directoryPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		directoryPane.add(new JLabel("Save to"));
		directoryPane.add(getPathField());
		directoryPane.add(new JLabel(" "));
		directoryPane.add(getBrowseButton());
		directoryPane.add(new JLabel("  "));
		directoryPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(directoryPane, layout);
		
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		bottomPanel.add(getTranslateButton());
		bottomPanel.add(getPrevButton());
		bottomPanel.add(new JLabel("                                "));
		bottomPanel.add(new JLabel("                                "));
		bottomPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(bottomPanel, layout);
		
		add(Box.createRigidArea(new Dimension(0,130)));
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

	private JButton getPrevButton() {
		if (prevButton == null) {
			prevButton = new JButton("Previous");
		}
		return prevButton;
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
	
	void addPrevListener(ActionListener listenForPrevButton) {
		prevButton.addActionListener(listenForPrevButton);
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

