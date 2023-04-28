package cbit.util.kisao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class KisaoTermParser {
	private final static Logger lg = LogManager.getLogger(KisaoTermParser.class);
	
//	final String Kisao_OBO = "kisao_2020_12_13.obo";
	final String Kisao_OBO = "kisao_algs.obo";
	
	final String TERM_PATTERN = "\\[Term\\]";
	final Pattern ID_PATTERN = Pattern.compile("id:\\s*(.+)");
	final Pattern NAME_PATTERN = Pattern.compile("name:\\s*(.+)");
	final Pattern ISA_PATTERN = Pattern.compile("is_a:\\s*(\\S+)");
	
	KisaoOntology parse() {
		InputStream is = KisaoTermParser.class.getClassLoader().getResourceAsStream(Kisao_OBO);
		BufferedReader isr = new BufferedReader(new InputStreamReader(is));
		String line = null;
		boolean inState = false;
		KisaoOntology ontology = new KisaoOntology();
		KisaoTerm curr = null;

		try {
			while ((line = isr.readLine()) != null) {
				
				if (line.matches(TERM_PATTERN)) {
					inState = true;
					curr = new KisaoTerm();
				}
				if (line.matches("^$") && curr != null) {
					inState = false;
					ontology.addTerm(curr);
					curr = null;
				}
				
				if(inState) {
					Matcher matcher = ID_PATTERN.matcher(line);
					if (matcher.find()) {
						curr.setId(matcher.group(1));
					}

					matcher = NAME_PATTERN.matcher(line);
					if (matcher.find()) {
						curr.setName(matcher.group(1));
					}
					matcher = ISA_PATTERN.matcher(line);
					if (matcher.find()) {
						curr.addIsaRef(matcher.group(1));
					}
				}
			}
		} catch(Exception e) {
			lg.error(e.getMessage(), e);
		}
		
		ontology.createRelations();
		return ontology;
	}
}
