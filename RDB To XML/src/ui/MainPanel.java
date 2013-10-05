package ui;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;


public class MainPanel {
	private JFrame mainFrame;
	private JPanel mainPane;
	
	private JButton connectButton;
	private JTextField dbNameField;
	private JTextField dbAddressField;
	private JTextField portNumField;
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JLabel errorMsgLabel;
	private ChooseRootPanel rt;
	
	public MainPanel(ChooseRootPanel rt) {
		this.rt = rt;
		getMainFrame().setContentPane(getMainPane());
		getMainFrame().pack();
		getMainFrame().setLocationRelativeTo(null);
	}
	
	private JPanel getMainPane() {
		if (mainPane == null) {
			mainPane = new JPanel();
			mainPane.setPreferredSize(new Dimension(600, 400));
			
			BoxLayout layout = new BoxLayout(mainPane, BoxLayout.Y_AXIS);
			mainPane.setLayout(layout);
			
			//mainPane.add(Box.createRigidArea(new Dimension(0,40)));
			
			errorMsgLabel = new JLabel(" ");
			errorMsgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			errorMsgLabel.setForeground(Color.RED);
			mainPane.add(errorMsgLabel);
			
			JLabel title = new JLabel(
				new javax.swing.ImageIcon(getClass().getResource(
							"/resource/title.png")));
			title.setAlignmentX(Component.CENTER_ALIGNMENT);
			mainPane.add(title);
			
			JPanel dbNamePane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
			dbNamePane.add(new JLabel("Database name "));
			dbNamePane.add(getDbNameField());
			dbNamePane.add(new JLabel("                "));
			dbNamePane.setAlignmentX(Component.CENTER_ALIGNMENT);
			mainPane.add(dbNamePane,layout);
			
			JPanel dbAddPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
			dbAddPane.add(new JLabel("Database address "));
			dbAddPane.add(getDbAddressField());
			dbAddPane.add(new JLabel("                "));
			dbAddPane.setAlignmentX(Component.CENTER_ALIGNMENT);
			mainPane.add(dbAddPane,layout);
			
			JPanel portNumPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
			portNumPane.add(new JLabel("Port number "));
			portNumPane.add(getPortNumField());
			portNumPane.add(new JLabel("                "));
			portNumPane.setAlignmentX(Component.CENTER_ALIGNMENT);
			mainPane.add(portNumPane,layout);
			
			JPanel usernamePane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
			usernamePane.add(new JLabel("Username "));
			usernamePane.add(getUsernameField());
			usernamePane.add(new JLabel("                "));
			usernamePane.setAlignmentX(Component.CENTER_ALIGNMENT);
			mainPane.add(usernamePane,layout);
			
			JPanel passwordPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
			passwordPane.add(new JLabel("Password "));
			passwordPane.add(getPasswordField());
			passwordPane.add(new JLabel("                "));
			passwordPane.setAlignmentX(Component.CENTER_ALIGNMENT);
			mainPane.add(passwordPane,layout);
			
			JButton con = getConnectButton();
			con.setAlignmentX(Component.CENTER_ALIGNMENT);
			mainPane.add(con);
			
			mainPane.add(Box.createRigidArea(new Dimension(0,40)));
		}
		generateInput(); //testing
		return mainPane;
	}
		
	private JButton getConnectButton() {
		if (connectButton == null) {
			connectButton = new JButton("Connect");
		}
		return connectButton;
	}
	
	private JTextField getDbNameField() {
		if (dbNameField == null) {
			dbNameField = new JTextField(35);
		}
		return dbNameField;
	}
	
	private JTextField getDbAddressField() {
		if (dbAddressField== null) {
			dbAddressField = new JTextField(35);
		}
		return dbAddressField;
	}
	
	private JTextField getPortNumField() {
		if (portNumField== null) {
			portNumField = new JTextField(35);
		}
		return portNumField;
	}
	
	private JTextField getUsernameField() {
		if (usernameField == null) {
			usernameField = new JTextField(35);
		}
		return usernameField;
	}

	private JPasswordField getPasswordField() {
		if (passwordField == null) {
			passwordField = new JPasswordField(35);
		}
		return passwordField;
	}	
	
	public void showMainPane() {
		getMainFrame().setContentPane(getMainPane());
		getMainFrame().validate();
	}

	public JFrame getMainFrame() {
		if (mainFrame == null) {
			mainFrame = new JFrame("RDB to XML Translator");
			mainFrame.setResizable(false);
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} 
		return mainFrame;
	}
	
	public String getName() {
		return dbNameField.getText();
	}
	
	public String getAddress() {
		return dbAddressField.getText();
	}
	
	public String getPort() {
		return portNumField.getText();
	}
	
	public String getUsername() {
		return usernameField.getText();
	}
	
	public String getPassword() {
		return String.valueOf(passwordField.getPassword());
	}
	
	void addConnectListener(ActionListener listenForConnectButton) {
		connectButton.addActionListener(listenForConnectButton);
	}
	
	void setErrorMsg(String msg) {
		errorMsgLabel.setText(msg);
	}
	
	void emptiedField() {
		dbAddressField.setText("");
		dbNameField.setText("");
		portNumField.setText("");
		usernameField.setText("");
		passwordField.setText("");
	}
	
	public JPanel getChooseRootPane() {
		return rt;
	}
	
	//for testing purpose
	void generateInput() {
		dbNameField.setText("acebrain_francisjanice");
		dbAddressField.setText("www.solvith.com");
		portNumField.setText("3306");
		usernameField.setText("acebrain_francis");
		passwordField.setText("nus1234");
	}
}
