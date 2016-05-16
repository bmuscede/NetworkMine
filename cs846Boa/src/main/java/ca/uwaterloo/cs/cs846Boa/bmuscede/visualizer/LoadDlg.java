package ca.uwaterloo.cs.cs846Boa.bmuscede.visualizer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Font;
import java.awt.Frame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;

public class LoadDlg extends JDialog {

	private static final long serialVersionUID = -3017900620988164800L;
	private final static String DB_LOC = "data/boa.db";
	private final static int TIMEOUT = 30;
	private final JPanel contentPanel = new JPanel();
	private JTable tblProj;
	private JLabel lblNoProjects;
	JButton btnGetMoreProjects;
	
	public static final String GET_MORE = "-1";
	public String selectedProject = null;
	
	/**
	 * Create the dialog.
	 */
	public LoadDlg(Frame parent) {
		super(parent, "Load Project...", true);
		
		setBounds(100, 100, 560, 380);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		{
			JLabel lblTitle = new JLabel("Select Project to Load:");
			lblTitle.setFont(new Font("Tahoma", Font.PLAIN, 20));
			lblTitle.setBounds(15, 16, 508, 20);
			contentPanel.add(lblTitle);
			{
				lblNoProjects = new JLabel("Hmmm, it appears there are no projects in the database...");
				lblNoProjects.setFont(new Font("Tahoma", Font.PLAIN, 18));
				lblNoProjects.setHorizontalAlignment(SwingConstants.CENTER);
				lblNoProjects.setBounds(15, 110, 508, 51);
				contentPanel.add(lblNoProjects);
			}
			{
				btnGetMoreProjects = new JButton("Get More Projects...");
				btnGetMoreProjects.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						selectedProject = GET_MORE;
						dispose();
					}
				});
				btnGetMoreProjects.setFont(new Font("Tahoma", Font.BOLD, 18));
				btnGetMoreProjects.setBounds(65, 164, 416, 42);
				contentPanel.add(btnGetMoreProjects);
			}
			
			JScrollPane scrProj = new JScrollPane();
			scrProj.setBounds(15, 52, 508, 217);
			contentPanel.add(scrProj);
			
			tblProj = new JTable();
			tblProj.setModel(new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
					"Project ID", "Project Name", "Contributors", "Files"
				}
			));
			tblProj.getColumnModel().getColumn(0).setPreferredWidth(202);
			tblProj.getColumnModel().getColumn(1).setPreferredWidth(189);
			tblProj.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			scrProj.setViewportView(tblProj);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//First, we get selected rows.
						int[] rows = tblProj.getSelectedRows();
						if (rows.length == 0){
						    JOptionPane.showMessageDialog(contentPanel, "You must"
						    		+ " select a project.", "Error",
						    		JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						//Gets selected rows.
						selectedProject = (String) tblProj.getModel()
								.getValueAt(rows[0], 0);
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		
		//Load in the projects.
		String sql = "SELECT Project.ProjectID AS ProjectID, " +
				"Project.Name AS Name, COUNT(*) AS Contributors, Files FROM " +
				"(Project INNER JOIN BelongsTo ON Project.ProjectID = BelongsTo.Project) " +
				"INNER JOIN (SELECT Project.ProjectID AS PID, COUNT(*) AS Files " +
				"FROM Project INNER JOIN File ON Project.ProjectID = File.ProjectID " +
				"GROUP BY Project.ProjectID) ON Project.ProjectID = PID GROUP BY " +
				"Project.ProjectID;";
				
		Connection conn = null;
		Statement state = null;
		ArrayList<String[]> results = new ArrayList<String[]>();
	    try {
	    	conn = DriverManager.getConnection("jdbc:sqlite:" + DB_LOC);
	    	state = conn.createStatement();
			state.setQueryTimeout(TIMEOUT);
			
			//Runs the query to get all project IDs
			ResultSet rs = state.executeQuery(sql);
			while (rs.next()){
				String currProj[] = {
				rs.getString("ProjectID"),
				rs.getString("Name"),
				rs.getString("Contributors"),
				rs.getString("Files")};
				results.add(currProj);
			}
		    conn.close();
	    } catch (SQLException e){
	    	e.printStackTrace();
	    }
	    
	    if (results.size() != 0) {
	    	//Removes the help hint.
	    	lblNoProjects.setVisible(false);
	    	btnGetMoreProjects.setVisible(false);
	    	
		    //Next, we build the table.
			DefaultTableModel model = (DefaultTableModel) tblProj.getModel();
			for (int i = 0; i < results.size(); i++){
				model.addRow(new Object[]{results.get(i)[0], 
						results.get(i)[1], results.get(i)[2], results.get(i)[3]});
			}
	    }
	}
}
