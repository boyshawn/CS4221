package ui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import erd.ErdNodeType;

public class UIController {
	private MainPanel main;
	private TranslatePanel translate;
	private ChoicePanel choice;
	private NaryPanel np;
	private RDBToXML r;
	private String dbname;
	private Map<JButton, JLabel> mapButtonLabel;
	private Map<JButton, Pair<JPanel, ArrayList<JComboBox>>> mapButtonCombo;
	private Map<JButton, List<String>> mapButtonNary;
	private Map<String, List<String>> nary;
	private Map<JButton, String> mapButtonRelationName;
	private List<JComboBox> cycleCombo;
	private List<List<String>> cycles;
	private String rootString;
	private boolean naryPanelExist = false;

	public UIController(MainPanel main, ChoicePanel choice, NaryPanel np,
			TranslatePanel translate, RDBToXML r) {

		mapButtonLabel = new HashMap<JButton, JLabel>();
		mapButtonCombo = new HashMap<JButton, Pair<JPanel, ArrayList<JComboBox>>>();
		mapButtonNary = new HashMap<JButton, List<String>>();
		mapButtonRelationName = new HashMap<JButton, String>();
		cycleCombo = new ArrayList<JComboBox>();
		nary = new HashMap<String, List<String>>();
		cycles = new ArrayList<List<String>>();

		this.main = main;
		this.choice = choice;
		this.translate = translate;
		this.r = r;
		this.np = np;
		
		this.main.addConnectListener(new ConnectListener());
		this.choice.addNextListener(new NextListener());
		this.choice.addCancelListener(new CancelListener());
		this.np.addNextListener(new NaryNextListener());
		this.np.addPrevListener(new NaryPrevListener());
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

			// make sure every map and list are empty
			mapButtonLabel.clear();
			mapButtonCombo.clear();
			mapButtonNary.clear();
			mapButtonRelationName.clear();
			cycleCombo.clear();
			nary.clear();
			cycles.clear();
			
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

						choice.cleanUp();
						
						r.translateToERD();
						cycles = r.checkCycle();
						
						// set up the choice panel
						Map<String, ErdNode> rootMap = r.getERDEntityTypes();
						List<String> rootTemp = new ArrayList<String>(rootMap.keySet());
						List<String> rootEntity = new ArrayList<String>();
						for (int i = 0; i < rootTemp.size(); i++) {
							if (rootMap.get(rootTemp.get(i)).getErdNodeType() == ErdNodeType.ENTITY_TYPE) {
								rootEntity.add(rootTemp.get(i));
							}
						}
						String[] root = rootEntity.toArray(new String[0]);
						choice.setRootList(root);
						
						if (cycles.size() != 0){
							cycleCombo = choice.addSplitCyclePanel(cycles);
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
			
			np.cleanUp();
			
			// set the root, entity splitting, nary relation order
			rootString = choice.getRootCombo().getSelectedItem()
					.toString();
			
			boolean check = true;
			List<String> c = new ArrayList<String>();
			for (int i = 0; i < cycleCombo.size(); i++) {
				String x = cycleCombo.get(i).getSelectedItem().toString();
				c.add(x);
				if (x.equals(rootString)) {
						check = false;
				}					
			}
			
			HashSet<String> hashSet = new HashSet<String>();
			boolean dupli = false;
			for (String s : c) {
				if (hashSet.contains(s))
					dupli = true; // contains
									// duplicates
				else
					hashSet.add(s);
			}
			
			if (check == false) {
				JOptionPane.showMessageDialog(choice,
						"Cannot split the root", "ERROR", JOptionPane.ERROR_MESSAGE);
			} else if (dupli == true) {
				JOptionPane.showMessageDialog(choice,
						"Duplicate(s) detected! Cannot choose 2 same entity to be split", "ERROR", JOptionPane.ERROR_MESSAGE);
			} else {
				if (cycleCombo.size() != 0) {
					for (int i = 0; i < cycleCombo.size(); i++) {
						r.setEntityToBeSplitted(cycleCombo.get(i).getSelectedItem()
								.toString(), i);
					}
				}
				
				try {
					r.translateToORASS();
				} catch (MainException me) {
					JOptionPane.showMessageDialog(choice,
							me.getMessage(),
							"ERROR",JOptionPane.ERROR_MESSAGE);
				}
				
				nary = r.getNaryRels();
				
				if (nary.size() != 0) {
					// go to NaryPanel
					naryPanelExist = true;
					for (Map.Entry<String, List<String>> entry : nary
							.entrySet()) {
						String relName = entry.getKey();
						List<String> listOrder = entry.getValue();
						Pair<JButton, JLabel> n = np.addChoicePanel(
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
										pane = np.getOptionPane(mapButtonNary.get(btemp));
										mapButtonCombo.put(btemp, pane);
									}

									boolean check = true;
									while (check) {
										int result = JOptionPane.showConfirmDialog(null, pane.getFirst(),
														"Specify the order",JOptionPane.OK_CANCEL_OPTION);
										if (result == JOptionPane.CANCEL_OPTION) {
											check = false;
										} else {
											if (result == JOptionPane.OK_OPTION) {
												// check if the choices are
												// different
												List<String> newOrder = new ArrayList<String>();
												List<JComboBox> temp2 = pane
														.getSecond();
												for (int i = 0; i < temp2
														.size(); i++) {
													String s = temp2
															.get(i)
															.getSelectedItem()
															.toString();
													newOrder.add(s);
												}
												HashSet<String> hashSet = new HashSet<String>();
												check = false;
												for (String s : newOrder) {
													if (hashSet.contains(s))
														check = true; // contains
																		// duplicates
													else
														hashSet.add(s);
												}
												if (check) {
													JOptionPane.showMessageDialog(pane.getFirst(),
																	"Duplicate(s) detected! Choose the correct order!",
																	"ERROR",JOptionPane.ERROR_MESSAGE);
												} else {
													JLabel l = mapButtonLabel.get(btemp);
													l.setText(""); // remove the current text				
													for (int i = 0; i < newOrder.size(); i++) {
														l.setText(l.getText()+ newOrder.get(i)+ " ");
													}
													mapButtonNary.put(btemp,newOrder);
													String reltemp = mapButtonRelationName.get(btemp);
													nary.put(reltemp,newOrder);
												}
											}
										}
									}
								}
							}
						});

					}
					np.addNextCancelButton();
					main.getMainFrame().setContentPane(choice.getNaryPane());
					main.getMainFrame().validate();
				} else {
					// go to TranslatePanel
					naryPanelExist = false;
					translate.emptiedField();
					main.getMainFrame().setContentPane(choice.getTranslatePane());
					main.getMainFrame().validate();
				}
			}
		}
	}

	class CancelListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			try {
				r.closeConnection();
			} catch (MainException me) {
				System.out.println(me.getMessage());
			}
			main.showMainPane();
		}
	}
	
	class NaryPrevListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			np.cleanUp();
			try {
				r.translateToERD();
				main.getMainFrame().setContentPane(main.getChoicePanel());
				main.getMainFrame().validate();
			} catch (MainException me) {
				System.out.println(me.getMessage());
			}
		}
	}
	
	class NaryNextListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			boolean check = true;
			List<List<String>> list = new ArrayList<List<String>>(nary.values());
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).contains(rootString))
					if (!list.get(i).get(0).equals(rootString))
						check = false; 
			}
			
			if (check == false) {
			JOptionPane.showMessageDialog(choice,
					rootString + " must be the root of the n-ary relation, because it is the most important entity",
					"ERROR",JOptionPane.ERROR_MESSAGE);
			} else {
				try {
					r.setOrders(nary);
					Map<String, ErdNode> rootMap = r.getERDEntityTypes();
					r.buildORASS(rootMap.get(rootString));
					main.getMainFrame().setContentPane(choice.getTranslatePane());
					main.getMainFrame().validate();
				} catch (MainException me) {
					JOptionPane.showMessageDialog(choice,
							me.getMessage(),
							"ERROR",JOptionPane.ERROR_MESSAGE);
				}
			}
			
		}
	}
	
	class PrevListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			try {
				translate.emptiedField();
				translate.setErrorMsg(" ");
				if (naryPanelExist) {
					main.getMainFrame().setContentPane(choice.getNaryPane());
					main.getMainFrame().validate();
				} else {
					r.translateToERD();
					main.getMainFrame().setContentPane(main.getChoicePanel());
					main.getMainFrame().validate();
				}
			} catch (MainException me) {
				System.out.println(me.getMessage());
			}
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
