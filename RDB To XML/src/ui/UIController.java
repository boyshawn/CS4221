package ui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import main.MainException;
import main.RDBToXML;
import erd.ErdNode;

public class UIController {
	private MainPanel main;
	private TranslatePanel translate;
	private ChoicePanel choice;
	private RDBToXML r;
	private String dbname;
	private Map<JButton, JLabel> mapButtonLabel;
	private Map<JButton, Pair<JPanel, ArrayList<JComboBox>>> mapButtonCombo;
	private Map<JButton, List<String>> mapButtonNary;
	private Map<String, List<String>> nary;
	private Map<JButton, String> mapButtonRelationName;
	private List<JComboBox> cycleCombo;

	public UIController(MainPanel main, ChoicePanel choice,
			TranslatePanel translate, RDBToXML r) {

		mapButtonLabel = new HashMap<JButton, JLabel>();
		mapButtonCombo = new HashMap<JButton, Pair<JPanel, ArrayList<JComboBox>>>();
		mapButtonNary = new HashMap<JButton, List<String>>();
		mapButtonRelationName = new HashMap<JButton, String>();

		this.main = main;
		this.choice = choice;
		this.translate = translate;
		this.r = r;

		this.main.addConnectListener(new ConnectListener());
		this.choice.addNextListener(new NextListener());
		this.choice.addCancelListener(new CancelListener());
		this.translate.addPrevListener(new PrevListener());
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
				if (v.validateName(name) && v.validatePort(port)) {
					try {
						r.connectToDB(address, port, name, username, password);
						dbname = name;
						main.emptiedField();
						main.setErrorMsg(" ");

						r.translateToERD();
						List<List<String>> cycles = r.checkCycle();
						if (cycles.size() != 0) {
							cycleCombo = choice.addSplitCyclePanel(cycles);
						}

						r.translateToORASS();

						// set up the choice panel
						Map<String, ErdNode> rootMap = r.getERDEntityTypes();
						String[] root = rootMap.keySet().toArray(new String[0]);
						choice.setRootList(root);

						nary = r.getNaryRels();
						for (Map.Entry<String, List<String>> entry : nary
								.entrySet()) {
							String relName = entry.getKey();
							List<String> listOrder = entry.getValue();
							Pair<JButton, JLabel> n = choice.addChoicePanel(
									relName, listOrder);
							JButton b = n.getFirst();
							mapButtonRelationName.put(b, relName);
							mapButtonLabel.put(b, n.getSecond());
							mapButtonNary.put(b, listOrder);

							b.addMouseListener(new MouseAdapter() {

								@Override
								public void mouseReleased(MouseEvent e) {
									if (MouseEvent.BUTTON1 == e.getButton()) {
										JButton btemp = (JButton) e.getSource();
										Pair<JPanel, ArrayList<JComboBox>> pane;
										if (mapButtonCombo.containsKey(btemp)) {
											pane = mapButtonCombo.get(btemp);
										} else {
											pane = choice
													.getOptionPane(mapButtonNary
															.get(btemp));
											mapButtonCombo.put(btemp, pane);
										}
										int result = JOptionPane.showConfirmDialog(
												null, pane.getFirst(),
												"Specify the order",
												JOptionPane.OK_CANCEL_OPTION);
										if (result == JOptionPane.OK_OPTION) {
											JLabel l = mapButtonLabel
													.get(btemp);
											l.setText(""); // remove the current
															// text
											ArrayList<JComboBox> tcombo = pane
													.getSecond();
											List<String> newOrder = new ArrayList<String>();
											for (int i = 0; i < tcombo.size(); i++) {
												String s = tcombo.get(i)
														.getSelectedItem()
														.toString();
												newOrder.add(s);
												l.setText(l.getText() + s + " ");
											}
											mapButtonNary.put(btemp, newOrder);
											String reltemp = mapButtonRelationName
													.get(btemp);
											nary.put(reltemp, newOrder);
										}
									}
								}
							});

						}

						choice.addNextCancelButton();

						main.getMainFrame().setContentPane(
								main.getChoicePanel());
						main.getMainFrame().validate();
					} catch (MainException me) {
						main.setErrorMsg(me.getMessage());
						me.printStackTrace();
					}
				} else {
					if (!v.validateName(name)) {
						main.setErrorMsg("Invalid database name");
					} else if (!v.validatePort(port)) {
						main.setErrorMsg("Invalid port number");
					}
				}
			}

		}
	}

	class NextListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			// set the root, entity splitting, nary relation order
			String rootString = choice.getRootCombo().getSelectedItem()
					.toString();
			Map<String, ErdNode> rootMap = r.getERDEntityTypes();
			r.buildORASS(rootMap.get(rootString));
			
			if (cycleCombo.size() != 0) {
				for (int i = 0; i < cycleCombo.size(); i++) {
					r.setEntityToBeSplitted(cycleCombo.get(i).getSelectedItem()
							.toString(), i);
				}
			}

			r.setOrders(nary);

			main.getMainFrame().setContentPane(choice.getTranslatePane());
			main.getMainFrame().validate();
		}
	}

	class CancelListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			choice = new ChoicePanel(translate);
			main.showMainPane();
		}
	}

	class PrevListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			main.getMainFrame().setContentPane(choice.getTranslatePane());
			main.getMainFrame().validate();
		}
	}

	class BrowseListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Save to");
			// trial
			UIManager.put("FileChooser.FileNameLabelText", "hello");
			SwingUtilities.updateComponentTreeUI(chooser);
			JFrame frame = new JFrame();
			chooser.showDialog(frame, "Select");

			chooser.setAcceptAllFileFilterUsed(false);
			try {
				File file = chooser.getSelectedFile();
				String fullPath = file.getAbsolutePath();
				// for mac
				String OS = System.getProperty("os.name").toLowerCase();
				if (OS.indexOf("mac") >= 0) {
					System.out.println("MAC");
					System.out.println(fullPath);
					int last = fullPath.lastIndexOf("/");
					String macPath = fullPath.substring(0, last);
					System.out.println(macPath);
					translate.setPath(macPath);
				} else {
					translate.setPath(fullPath);
				}
			} catch (Exception ex) {
				System.out.println("User did not choose any directory");
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
					try {
						String fName = "";
						// for mac
						String OS = System.getProperty("os.name").toLowerCase();
						if (OS.indexOf("mac") >= 0) {
							fName = path + "/" + xmlName;
						} else {
							// for windows
							fName = path + "\\" + xmlName;
						}
						r.translateToXML(dbname, fName);
						translate.displaySuccessfulMsg();
						translate.emptiedField();
						translate.setErrorMsg(" ");
						choice = new ChoicePanel(translate);
						// open the files generated
						try {
							Desktop.getDesktop().open(new File(fName + ".xml"));
							Desktop.getDesktop().open(new File(fName + ".xsd"));
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						main.showMainPane();
					} catch (MainException me) {
						translate.setErrorMsg(me.getMessage());
						me.printStackTrace();
					}
				} else {
					translate.setErrorMsg("Invalid file name");
				}
			}
		}
	}
}
