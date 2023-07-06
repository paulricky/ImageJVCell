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

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.*;
import java.awt.*;

//import org.vcell.imagej.helper.VCellHelper;

import ij.gui.GenericDialog;
import net.imagej.ImageJ;


@SuppressWarnings("unused")
@Plugin(type = ContextCommand.class, menuPath = "Plugins>VCell>Clean Test Search")
public class WebSearchClean extends ContextCommand {
    @Parameter
    private UIService uiService;

  //  @Parameter
   // private VCellHelper vcellHelper;

    
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
    
    
    private static String[] extractFormat(Element table) {        
    	Elements headerCells = table.select("th");
        String[] format = new String[headerCells.size()];       
        for (int i = 0; i < headerCells.size(); i++) {          
        	format[i] = headerCells.get(i).text();        
        }
        return format;
    }

    private static String[][] extractTableData(Element table) {       
    	Elements rows = table.select("tr");      
    	String[][] data = new String[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {         
        	Elements cells = rows.get(i).select("td");          
        	data[i] = new String[cells.size()];
            for (int j = 0; j < cells.size(); j++) {           
            	data[i][j] = cells.get(j).text();            
            }
        }
        return data;
    }
    
    
    @SuppressWarnings("serial")
	private class JTextAreaCellRenderer extends JTextArea implements TableCellRenderer {
        public JTextAreaCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder()); 
            FontMetrics fm = getFontMetrics(getFont());
            int lineHeight = fm.getHeight();
            int ascent = fm.getAscent();
            int descent = fm.getDescent();
            int leading = fm.getLeading();
            int adjustedLineHeight = lineHeight - leading + descent;
            setLineHeight(adjustedLineHeight);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setText(value != null ? value.toString() : "");
            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }

        private void setLineHeight(int lineHeight) {
            String html = "<html><body style='line-height:" + lineHeight + "px;'>%s</body></html>";
            setText(String.format(html, getText()));
        }
    }
    
    private static void adjustRowHeight(JTable table) {
        for (int row = 0; row < table.getRowCount(); row++) {
            int rowHeight = table.getRowHeight();
            for (int column = 0; column < table.getColumnCount(); column++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
            }
            table.setRowHeight(row, rowHeight);
        }
    }
    
    
    @SuppressWarnings("serial")
	private static class QuotationCellRenderer extends JScrollPane implements TableCellRenderer {
        private JTextArea textArea;

        public QuotationCellRenderer() {
            textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            setViewportView(textArea);
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            setPreferredSize(new Dimension(300, 50));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            textArea.setText(value != null ? value.toString() : "");
            return this;
        }
    }



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
        
        boolean shouldContinue = true;
        
        while(shouldContinue) { 
        	
        	
        if (box.wasCanceled() && !box.isVisible()) {
           shouldContinue = false;
           break;
        }
        	
        if (box.wasOKed()) {
        	
        	
        	String modelName = box.getNextString();
            String modelID = box.getNextString();
            String modelOwner = box.getNextString();
            String beginDate = box.getNextString();
            String endDate = box.getNextString();
            
           
            
            JFrame frame = new JFrame("Search Results");
            
            
            url = "https://vcellapi-beta.cam.uchc.edu:8080/biomodel?bmName=" + modelName + "&bmId=" + modelID + "&category=all"
                + "&owner=" + modelOwner + "&savedLow=&savedHigh=&startRow=1&maxRows=100&orderBy=date_desc";
            
            
            try {
            	
            	
				 Document doc = Jsoup.connect(url).get();
				 doc.select("tbody").first().remove();
				 
				 //Element element = doc.selectFirst("tbody");
				 
				 String[][] tableData = extractTableData(doc);
				 
	             String[] columns = extractFormat(doc);
	             
	             DefaultTableModel finalFormat = new DefaultTableModel(tableData, columns);

                 JTable table = new JTable(finalFormat);
                 table.setDefaultRenderer(Object.class, new QuotationCellRenderer());
                 adjustRowHeight(table);
                // table.setDefaultEditor(Object.class, null);
	             
	             //StringBuilder builder = new StringBuilder();
	             
	             int num = 0;
	             for (int i = 0; i < tableData.length; i++) {
	            	 for (int x = 0; x < tableData[i].length; x++) {
	            		 for (int z = 0; z < tableData[i][x].length(); z++) {
	            			 
	            			 if (tableData[i][x].substring(z, z+1).equals("\"")) {
	            				 num++;
	            				 
	            				 if (num % 2 != 0 && num > 1) {
	            					 tableData[i][x] = tableData[i][x].substring(0, z) + "\n" + tableData[i][x].substring(z);
	            				 }
	            				// System.out.println(tableData[i][x]);
	            				 }
	            			 }
	            		 }
	            	 }
	            
	            
	             JScrollPane panel = new JScrollPane(table);
	             
	             panel.setPreferredSize(new Dimension(2000, 1000));

	             frame.add(panel);
	
	             frame.pack();
	             
	             frame.setVisible(true);
	          
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
           // System.out.println(url);  
        }
       
        }
        ui.add(box);
        ui.pack();
        ui.setVisible(true);
    }
}
