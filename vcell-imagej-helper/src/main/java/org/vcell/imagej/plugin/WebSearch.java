package org.vcell.imagej.plugin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
//import org.vcell.imagej.helper.VCellHelper;

import ij.gui.GenericDialog;
import net.imagej.ImageJ;

@Plugin(type = ContextCommand.class, menuPath = "Plugins>VCell> ...")
public class WebSearch extends ContextCommand {
    @Parameter
    private UIService uiService;

  //  @Parameter
   // private VCellHelper vcellHelper;

    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }

   /* public static void removeLineBreaks(Elements elements) {
        for (Element element : elements) {
            for (int i = 0; i < element.childNodeSize(); i++) {
                org.jsoup.nodes.Node childNode = element.childNode(i);

                if (childNode instanceof TextNode) {
                    TextNode textNode = (TextNode) childNode;
                    String text = textNode.text();
                    text = text.replace("\n", "").replace("\r", "");
                    textNode.text(text);
                }
            }
            
            removeLineBreaks(element.children());
        }
    } */
    
    public void loadWebsite(String url, JEditorPane editorPane) {
        try {
            editorPane.setPage(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class WebsiteDisplayFrame extends JFrame {
        public WebsiteDisplayFrame(String url) {
            setTitle("Model Search");
            setSize(1000, 800);

            JEditorPane editorPane = new JEditorPane();
            editorPane.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(editorPane);
            add(scrollPane);

            loadWebsite(url, editorPane);
        }
    }

    @SuppressWarnings("unlikely-arg-type")
	public void run() {
        String string = new String();
        String url = new String();
        JFrame ui = new JFrame();
        GenericDialog box = new GenericDialog("Web Model Search");
        box.addStringField("Model Name:", string, 45);
        box.addStringField("Model ID:", string, 45);
        box.addStringField("Model Owner:", string, 45);
        box.addStringField("Begin Date:", string, 45);
        box.addStringField("End Date:", string, 45);
        box.showDialog(); 
        if (box.wasOKed()) {
        	String modelName = box.getNextString();
            String modelID = box.getNextString();
            String modelOwner = box.getNextString();
            String beginDate = box.getNextString();
            String endDate = box.getNextString();
           // WebsiteDisplayFrame frame = new WebsiteDisplayFrame(url);
           // System.out.println(modelOwner);    
            JFrame frame = new JFrame();
            url = "https://vcellapi-beta.cam.uchc.edu:8080/biomodel?bmName=" + modelName + "&bmId=" + modelID + "&category=all"
                + "&owner=" + modelOwner + "&savedLow=&savedHigh=&startRow=1&maxRows=10&orderBy=date_desc";
            try {
				 Document doc = Jsoup.connect(url).get();
				 Elements elements = doc.select("tbody");
				 //System.out.println(element);
				 
				/* for (Element tdElements : element) {
					 removeLineBreaks(element);
				 } 
				 
				 
				 for (Element tdElements : element) {
					 tdElements.append("<br>");
					 element.equals(tdElements);
				 } */
				 
				 //System.out.println(element.html());
				 
				 for (Element individualElements : elements) {
		                String text = individualElements.text();
		               // System.out.println(element1.getElementsByTag("<td>"));
		               // JLabel label = new JLabel(text);
		                /* JPanel gridPanel = new JPanel(new GridBagLayout());
		                gridPanel.setPreferredSize(new Dimension(1000, 1000));
		    			GridBagConstraints c = new GridBagConstraints(); */
		    			JEditorPane pane = new JEditorPane(text);
		    			pane.setContentType("text/html");
		                JScrollPane panel = new JScrollPane(pane);
		                panel.setVisible(true);
		                frame.add(panel);
		            }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
           // System.out.println(url);
            frame.setVisible(true);
            
            
        }
        ui.add(box);
        ui.setVisible(true);
    }
}
