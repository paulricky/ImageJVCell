package org.vcell.imagej.plugin;

import java.io.IOException;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.vcell.imagej.helper.VCellHelper;
import net.imagej.ImageJ;

@Plugin(type = ContextCommand.class, menuPath = "Plugins>VCell> Full HTML Import")
public class OnlineModelSearch extends ContextCommand {
	@Parameter
	private UIService uiService;

	@Parameter
	private VCellHelper vcellHelper;
	//static JOptionPane pannel = new JOptionPane();
	//static JEditorPane pane1 = new JEditorPane();

//	JFrame ui = new JFrame();

	public static void main(String[] args) {

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
	
	 public void loadWebsite(String url, JEditorPane editorPane) {
		        try {
		            editorPane.setPage(new URL(url));
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }
	 
	 public class websiteDisplayFrame extends JFrame {
		// private JEditorPane editorPane;

		    public websiteDisplayFrame(String url) {
		       setTitle("Model Search");
		       setSize(1000, 800);
		      // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		        
		      /*  editorPane.addHyperlinkListener(new HyperlinkListener() {
		            @Override
		            public void hyperlinkUpdate(HyperlinkEvent e) {
		                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
		                    loadWebsite(e.getURL().toString(), editorPane);
		                }
		            }
		        }); */
		       //   
		    }
	 }
	 
	 
		    
	 
	public void run() {
		
		String url = "https://vcellapi-beta.cam.uchc.edu:8080/biomodel";
        websiteDisplayFrame frame = new websiteDisplayFrame(url);
		JEditorPane editorPane = new JEditorPane();
		loadWebsite(url, editorPane);
		editorPane.setVisible(true);
		editorPane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(editorPane); 
		scrollPane.setVisible(true);
		frame.add(scrollPane);
		frame.setVisible(true);
	
        
	       /* editorPane.addHyperlinkListener(new HyperlinkListener() {
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
						try {
							URI url = null;
							try {
								url = new URI("https://vcellapi-beta.cam.uchc.edu:8080/biomodel");
							} catch (URISyntaxException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							Desktop.getDesktop().browse(url);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				
			}); */
	        
		/* try {

		    String url = "https://vcellapi-beta.cam.uchc.edu:8080/biomodel";
		    Document doc = Jsoup.connect(url).get();
		    

		    Element fragment = doc.select("css-selector").first();


		    String htmlContent = fragment.html();

		    
		    ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		    ui.setSize(400, 300);


		    JLabel label = new JLabel(htmlContent);
		    ui.add(label);


		    ui.setVisible(true);
		} catch (Exception e) {
		    e.printStackTrace();
		} */
		
		
	}
}