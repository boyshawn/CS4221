package ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import main.RDBToXML;

/**
 * 
 * @author Sarah
 * 
 */

public class UIController {
	private MainPanel main;
	private TranslatePanel translate;
	private RDBToXML r;
	private String dbname;

	public UIController(MainPanel main, TranslatePanel translate, RDBToXML r) { 
		
		this.main = main;
		this.translate = translate;
		this.r = r;

		this.main.addConnectListener(new ConnectListener());
		this.translate.addCancelListener(new CancelListener());
		this.translate.addBrowseListener(new BrowseListener());
		this.translate.addTranslateListener(new TranslateListener());
	}

	class ConnectListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			String name, address, port, username, password;
			InputFormatValidator v = new InputFormatValidator();

			name = main.getName();
			address = main.getAddress();
			port = main.getPort();
			username = main.getUsername();
			password = main.getPassword();

			if (name.isEmpty() || address.isEmpty() || port.isEmpty()
					|| username.isEmpty() || password.isEmpty()) {
				main.setErrorMsg("Please enter all the fields");
			} else {
				// validate name add port.
				if (v.validateName(name) && v.validateAdd(address)
						&& v.validatePort(port)) {
					try {
						r.connectToDB(address, port, name, username, password);
						dbname = name;
						main.emptiedField();
						main.setErrorMsg(" ");
						main.getMainFrame().setContentPane(
								main.getTranslatePane());
						main.getMainFrame().validate();
					} catch (Exception s) { // SQLException
						main.setErrorMsg("Database access unsuccessful. Please try again");
					}
				} else {
					if (!v.validateName(name)) {
						main.setErrorMsg("Invalid database name");
					} else if (!v.validateAdd(address)) {
						main.setErrorMsg("Invalid database address");
					} else if (!v.validatePort(port)) {
						main.setErrorMsg("Invalid port number");
					}
				}
			}

		}
	}

	class CancelListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			translate.emptiedField();
			translate.setErrorMsg(" ");
			main.showMainPane();
		}
	}

	class BrowseListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Save to");
			JFrame frame = new JFrame();
			int returnVal = chooser.showDialog(frame, "Select");

			chooser.setAcceptAllFileFilterUsed(false);
			try {
				File file = chooser.getSelectedFile();
				String fullPath = file.getAbsolutePath();
				translate.setPath(fullPath);
			} catch (Exception ex) {
				//System.out.println("User did not choose any directory");
			}
		}
	}

	class TranslateListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			String xmlName, path;
			InputFormatValidator v = new InputFormatValidator();

			xmlName = translate.getFilename();
			path = translate.getPath();

			if (xmlName.isEmpty() || path.isEmpty()) {
				translate
						.setErrorMsg("Please enter the XML file name and choose a directory");
			} else {
				if (v.validateFilename(xmlName)) {
					String xmlDataName = path + "/" + xmlName + "_data";
					String xmlSchemaName = path + "/" + xmlName + "_schema";

					r.translateToXML(dbname, xmlSchemaName, xmlDataName);
					translate.displaySuccessfulMsg();
					translate.emptiedField();
					translate.setErrorMsg(" ");
					main.showMainPane();
				} else {
					translate.setErrorMsg("Invalid file name");
				}
			}
		}
	}
}
