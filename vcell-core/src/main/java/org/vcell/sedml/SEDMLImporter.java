
package org.vcell.sedml;

import java.beans.PropertyVetoException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import cbit.vcell.parser.ExpressionMathMLParser;
import cbit.vcell.parser.ExpressionUtils;
import cbit.vcell.parser.SymbolTableEntry;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.DOMBuilder;
import org.jdom.output.DOMOutputter;
import org.jlibsedml.AbstractIdentifiableElement;
import org.jlibsedml.AbstractTask;
import org.jlibsedml.Algorithm;
import org.jlibsedml.AlgorithmParameter;
import org.jlibsedml.ArchiveComponents;
import org.jlibsedml.Change;
import org.jlibsedml.ChangeAttribute;
import org.jlibsedml.ComputeChange;
import org.jlibsedml.DataGenerator;
import org.jlibsedml.FunctionalRange;
import org.jlibsedml.Libsedml;
import org.jlibsedml.Model;
import org.jlibsedml.Output;
import org.jlibsedml.Parameter;
import org.jlibsedml.Range;
import org.jlibsedml.RepeatedTask;
import org.jlibsedml.SedML;
import org.jlibsedml.SetValue;
import org.jlibsedml.SubTask;
import org.jlibsedml.Task;
import org.jlibsedml.UniformRange;
import org.jlibsedml.UniformRange.UniformType;
import org.jlibsedml.UniformTimeCourse;
import org.jlibsedml.VectorRange;
import org.jlibsedml.XMLException;
import org.jlibsedml.XPathEvaluationException;
import org.jlibsedml.execution.ArchiveModelResolver;
import org.jlibsedml.execution.FileModelResolver;
import org.jlibsedml.execution.ModelResolver;
import org.jlibsedml.modelsupport.SBMLSupport;
import org.jlibsedml.modelsupport.SUPPORTED_LANGUAGE;
import org.jmathml.ASTNode;
import org.sbml.jsbml.SBase;
import org.vcell.sbml.vcell.SBMLImporter;
import org.vcell.sbml.vcell.SBMLSymbolMapping;
import org.vcell.sbml.vcell.SymbolContext;
import org.vcell.util.FileUtils;

import cbit.util.xml.VCLogger;
import cbit.util.xml.XmlUtil;
import cbit.vcell.biomodel.BioModel;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.mapping.MathMapping;
import cbit.vcell.mapping.MathMappingCallbackTaskAdapter;
import cbit.vcell.mapping.MathSymbolMapping;
import cbit.vcell.mapping.ReactionSpec;
import cbit.vcell.mapping.SimulationContext;
import cbit.vcell.mapping.SimulationContext.Application;
import cbit.vcell.mapping.SimulationContext.MathMappingCallback;
import cbit.vcell.mapping.SimulationContext.NetworkGenerationRequirements;
import cbit.vcell.mapping.SpeciesContextSpec;
import cbit.vcell.mapping.SpeciesContextSpec.SpeciesContextSpecParameter;
import cbit.vcell.math.Constant;
import cbit.vcell.math.MathDescription;
import cbit.vcell.math.MathException;
import cbit.vcell.math.Variable;
import cbit.vcell.matrix.MatrixException;
import cbit.vcell.model.Model.ModelParameter;
import cbit.vcell.model.Model.ReservedSymbol;
import cbit.vcell.model.ModelException;
import cbit.vcell.model.ReactionParticipant;
import cbit.vcell.model.ReactionStep;
import cbit.vcell.model.SpeciesContext;
import cbit.vcell.parser.Expression;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.solver.ConstantArraySpec;
import cbit.vcell.solver.DefaultOutputTimeSpec;
import cbit.vcell.solver.ErrorTolerance;
import cbit.vcell.solver.MathOverrides;
import cbit.vcell.solver.NonspatialStochHybridOptions;
import cbit.vcell.solver.NonspatialStochSimOptions;
import cbit.vcell.solver.OutputTimeSpec;
import cbit.vcell.solver.Simulation;
import cbit.vcell.solver.SolverDescription;
import cbit.vcell.solver.SolverDescription.AlgorithmParameterDescription;
import cbit.vcell.solver.SolverTaskDescription;
import cbit.vcell.solver.SolverUtilities;
import cbit.vcell.solver.TimeBounds;
import cbit.vcell.solver.TimeStep;
import cbit.vcell.solver.UniformOutputTimeSpec;
import cbit.vcell.xml.ExternalDocInfo;
import cbit.vcell.xml.XMLSource;
import cbit.vcell.xml.XmlHelper;
import cbit.vcell.xml.XmlParseException;
import cbit.vcell.xml.Xmlproducer;

public class SEDMLImporter {

	private final static Logger logger = LogManager.getLogger(SEDMLImporter.class);
	
	private SedML sedml;
	private ExternalDocInfo externalDocInfo;
	private boolean exactMatchOnly;
	
	private VCLogger transLogger;
	private List<BioModel> docs;
	private String bioModelBaseName;
	private ArchiveComponents ac;
	private ModelResolver resolver;
	private SBMLSupport sbmlSupport;
	
	private HashMap<BioModel, SBMLImporter> importMap = new HashMap<BioModel, SBMLImporter>();

	
	public  SEDMLImporter(VCLogger transLogger, ExternalDocInfo externalDocInfo, SedML sedml, boolean exactMatchOnly) throws FileNotFoundException, XMLException {
		this.transLogger = transLogger;
		this.externalDocInfo = externalDocInfo;
		this.sedml = sedml;
		this.exactMatchOnly = exactMatchOnly;
		
		initialize();
	}
	
	private void initialize() throws FileNotFoundException, XMLException {
		bioModelBaseName = FileUtils.getBaseName(externalDocInfo.getFile().getAbsolutePath());		// extract bioModel name from sedx (or sedml) file
		if(externalDocInfo.getFile().getPath().toLowerCase().endsWith("sedx") || externalDocInfo.getFile().getPath().toLowerCase().endsWith("omex")) {
			ac = Libsedml.readSEDMLArchive(new FileInputStream(externalDocInfo.getFile().getPath()));
		}
		resolver = new ModelResolver(sedml);
		if(ac != null) {
			ArchiveModelResolver amr = new ArchiveModelResolver(ac);
			amr.setSedmlPath(sedml.getPathForURI());
			resolver.add(amr);
		} else {
			resolver.add(new FileModelResolver()); // assumes absolute paths
			String sedmlRelativePrefix = externalDocInfo.getFile().getParent() + File.separator;
			resolver.add(new RelativeFileModelResolver(sedmlRelativePrefix)); // in case model URIs are relative paths
		}
		sbmlSupport = new SBMLSupport();
	}

	public  List<BioModel> getBioModels() throws Exception {
		docs = new ArrayList<BioModel>();
		try {
	        // iterate through all the elements and show them at the console
	        List<org.jlibsedml.Model> mmm = sedml.getModels();
	        if (mmm.isEmpty()) {
	        	// nothing to import
	        	return docs;
	        }
	        List<org.jlibsedml.Simulation> sss = sedml.getSimulations();
	        List<AbstractTask> ttt = sedml.getTasks();
	        List<DataGenerator> ddd = sedml.getDataGenerators();
	        List<Output> ooo = sedml.getOutputs();
	        printSEDMLSummary(mmm, sss, ttt, ddd, ooo);
	        
			// We don't know how many BioModels we'll end up with as some model changes may be translatable as simulations with overrides
	        // The HashMap below has entries for all SEDML Models where some may reference the same BioModel
	        // The docs field will have a List of all unique BioModels
			HashMap<String, BioModel> bmMap = new HashMap<String, BioModel>();
			createBioModels(mmm, bmMap);
 	        
			// We will parse all tasks and create Simulations in BioModels
			// Creating one VCell Simulation for each SED-ML actual Task (RepeatedTasks get added as parameter scan overrides)
	    	org.jlibsedml.Simulation sedmlSimulation = null;	// this will become the vCell simulation
	    	org.jlibsedml.Model sedmlModel = null;		// the "original" model referred to by the task
	    	String sedmlOriginalModelName = null;				// this will be used in the BioModel name
	    	String sedmlOriginalModelLanguage = null;			// can be sbml or vcml
			
			HashMap<String, Simulation> vcSimulations = new HashMap<String, Simulation>();
			for (AbstractTask selectedTask : ttt) {
				if(selectedTask instanceof Task) {
					sedmlModel = sedml.getModelWithId(selectedTask.getModelReference());
					sedmlSimulation = sedml.getSimulation(selectedTask.getSimulationReference());
					sedmlOriginalModelLanguage = sedmlModel.getLanguage();
				} else if(selectedTask instanceof RepeatedTask) {
					// Repeated tasks refer to regular tasks
					// We need simulations to be created for all regular tasks before we can process repeated tasks
					continue;
				} else {
					throw new RuntimeException("Unexpected task " + selectedTask);
				}
				// only UTC sims supported
				if(!(sedmlSimulation instanceof UniformTimeCourse)) {
					logger.error("task '" + selectedTask.getName() + "' is being skipped, it references an unsupported simulation type: "+sedmlSimulation);
					continue;
				}

				// at this point we assume that the sedml simulation, algorithm and kisaoID are all valid

				// identify the vCell solvers that would match best the sedml solver kisao id
				String kisaoID = sedmlSimulation.getAlgorithm().getKisaoID();
				// try to find a match in the ontology tree
				SolverDescription solverDescription = SolverUtilities.matchSolverWithKisaoId(kisaoID, exactMatchOnly);
				if (solverDescription != null) {
					logger.info("Task (id='"+selectedTask.getId()+"') is compatible, solver match found in ontology: '" + kisaoID + "' matched to " + solverDescription);
				} else {
					// give it a try anyway with our deterministic default solver
					solverDescription = SolverDescription.CombinedSundials;
					logger.error("Task (id='"+selectedTask.getId()+")' is not compatible, no equivalent solver found in ontology for requested algorithm '"+kisaoID + "'; trying with deterministic default solver "+solverDescription);
				}
				// find out everything else we need about the application we're going to use,
				// some of the info will be needed when we parse the sbml file
				boolean bSpatial = false;
				Application appType = Application.NETWORK_DETERMINISTIC;
				Set<SolverDescription.SolverFeature> sfList = solverDescription.getSupportedFeatures();
				for(SolverDescription.SolverFeature sf : sfList) {
					switch(sf) {
						case Feature_Rulebased:
							appType = Application.RULE_BASED_STOCHASTIC;
							break;
						case Feature_Stochastic:
							appType = Application.NETWORK_STOCHASTIC;
							break;
						case Feature_Deterministic:
							appType = Application.NETWORK_DETERMINISTIC;
							break;
						case Feature_Spatial:
							bSpatial = true;
							break;
						default:
							break;
					}
				}

				BioModel bioModel = bmMap.get(sedmlModel.getId());
				
				// if language is VCML, we don't need to create Applications and Simulations in BioModel
				// we allow a subset of SED-ML Simulation settings (may have been edited) to override existing BioModel Simulation settings
				
				if(sedmlOriginalModelLanguage.contentEquals(SUPPORTED_LANGUAGE.VCELL_GENERIC.getURN())) {
					Simulation theSimulation = null;
					for (Simulation sim : bioModel.getSimulations()) {
						if (sim.getName().equals(selectedTask.getName())) {
							logger.trace(" --- selected task - name: " + selectedTask.getName() + ", id: " + selectedTask.getId());
							sim.setImportedTaskID(selectedTask.getId());
							theSimulation = sim;
							break;	// found the one, no point to continue the for loop
						}
					}if(theSimulation == null) {
						logger.error("Couldn't match sedml task '" + selectedTask.getName() + "' with any biomodel simulation");
						// TODO: should we throw an exception?
						continue;	// should never happen
					}
					
					SolverTaskDescription simTaskDesc = theSimulation.getSolverTaskDescription();
					translateTimeBounds(simTaskDesc, sedmlSimulation);
					continue;
				}
				
				// if language is SBML, we must create Simulations
				// we may need to also create Applications, since the default one from SBML import may not be the right type)
				
				// see first if there is a suitable application type for the specified kisao
				// if not, we add one by doing a "copy as" to the right type
				SimulationContext[] existingSimulationContexts = bioModel.getSimulationContexts();
				SimulationContext matchingSimulationContext = null;
				for (SimulationContext simContext : existingSimulationContexts) {
					if (simContext.getApplicationType().equals(appType) && ((simContext.getGeometry().getDimension() > 0) == bSpatial)) {
						matchingSimulationContext = simContext;
						break;
					}
				}
				if (matchingSimulationContext == null) {
					// this happens if we need a NETWORK_STOCHASTIC application
					matchingSimulationContext = SimulationContext.copySimulationContext(bioModel.getSimulationContext(0), sedmlOriginalModelName+"_"+existingSimulationContexts.length, bSpatial, appType);
					bioModel.addSimulationContext(matchingSimulationContext);
					try {
						String importedSCName = bioModel.getSimulationContext(0).getName();
						bioModel.getSimulationContext(0).setName("original_imported_"+importedSCName);
						matchingSimulationContext.setName(importedSCName);
					} catch (PropertyVetoException e) {
						// we should never bomb out just for trying to set a pretty name
						logger.warn("could not set pretty name on application from name of model "+sedmlModel);
					}
				}
				matchingSimulationContext.refreshDependencies();
				MathMappingCallback callback = new MathMappingCallbackTaskAdapter(null);
				matchingSimulationContext.refreshMathDescription(callback, NetworkGenerationRequirements.ComputeFullStandardTimeout);

				// making the new vCell simulation based on the sedml simulation
				Simulation newSimulation = new Simulation(matchingSimulationContext.getMathDescription(), matchingSimulationContext);
				if (selectedTask instanceof Task) {
					String newSimName = selectedTask.getId();
					if(SEDMLUtil.getName(selectedTask) != null) {
						newSimName += "_" + SEDMLUtil.getName(selectedTask);
					}
					newSimulation.setName(newSimName);
					newSimulation.setImportedTaskID(selectedTask.getId());
					vcSimulations.put(selectedTask.getId(), newSimulation);
				} else {
					newSimulation.setName(SEDMLUtil.getName(sedmlSimulation)+"_"+SEDMLUtil.getName(selectedTask));
				}
				
				// we identify the type of sedml simulation (uniform time course, etc)
				// and set the vCell simulation parameters accordingly
				SolverTaskDescription simTaskDesc = newSimulation.getSolverTaskDescription();
				if(solverDescription != null) {
					simTaskDesc.setSolverDescription(solverDescription);
				}

				translateTimeBounds(simTaskDesc, sedmlSimulation);
				translateAlgorithmParams(simTaskDesc, sedmlSimulation);
				
				newSimulation.setSolverTaskDescription(simTaskDesc);
				newSimulation.setDescription(SEDMLUtil.getName(selectedTask));
				bioModel.addSimulation(newSimulation);
				newSimulation.refreshDependencies();
				
				// finally, add MathOverrides if referenced model has specified compatible changes
				if (!sedmlModel.getListOfChanges().isEmpty() && canTranslateToOverrides(bioModel, sedmlModel)) {
					createOverrides(newSimulation, sedmlModel.getListOfChanges());
				}
				
			}
			// now process repeated tasks, if any
			addRepeatedTasks(ttt, vcSimulations);
			// finally try to pretty up simulation names
			for (BioModel bm : docs) {
				Simulation[] sims = bm.getSimulations();
				for (int i = 0; i < sims.length; i++) {
					String taskId = sims[i].getImportedTaskID();
					AbstractTask task = sedml.getTaskWithId(taskId);
					if (task.getName() != null) {
						try {
							sims[i].setName(task.getName());
						} catch (PropertyVetoException e) {
							// we should never bomb out just for trying to set a pretty name
							logger.warn("could not set pretty name for simulation "+sims[i].getDisplayName()+" from task "+task);
						}
					}
				}
			}
			// purge unused biomodels and applications
			Iterator<BioModel> docIter = docs.iterator();
			while (docIter.hasNext()) {
				BioModel doc = docIter.next();
				for (int i = 0; i < doc.getSimulationContexts().length; i++) {
					if (doc.getSimulationContext(i).getSimulations().length == 0) {
						doc.removeSimulationContext(doc.getSimulationContext(i));
						i--;
					}
				}
				if (doc.getSimulations().length == 0) docIter.remove();
			}
			// finally try to consolidate SimContexts into fewer (posibly just one) BioModels
			// unlikely to happen from SEDMLs not originating from VCell, but very useful for roundtripping if so
			// TODO: maybe try to detect that and only try if of VCell origin
			mergeBioModels(docs);
			return docs;
		} catch (Exception e) {
			throw new RuntimeException("Unable to initialize bioModel for the given selection\n"+e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private void mergeBioModels(List<BioModel> bioModels) {
		if (bioModels.size() <=1) return;
		// for now just try if they *ALL* have matchable model
		// this should be the case if dealing with exported SEDML/OMEX from VCell BioModel with multiple applications
		
		// temporary kludge for dealing with BioModels that have Applications with disabled reactions
		// TODO export disabled reactions with specific zero kinetic law multiplier
		BioModel bmMax = bioModels.get(0);
		boolean differentRxNo = false;
		for (int i = 1; i < bioModels.size(); i++) {
			if (bioModels.get(i).getModel().getReactionSteps().length != bmMax.getModel().getReactionSteps().length) {
				differentRxNo = true;				
			}
			if (bioModels.get(i).getModel().getReactionSteps().length > bmMax.getModel().getReactionSteps().length) {
				bmMax = bioModels.get(i);
			}
		}
		if (differentRxNo) {
			for (int i = 0; i < bioModels.size(); i++) {
				BioModel currentBM = bioModels.get(i);
				if (currentBM.getModel().getReactionSteps().length < bmMax.getModel().getReactionSteps().length) {
					try {
						BioModel clonedBM = XmlHelper.cloneBioModel(bmMax);
						// try to add missing reactions
						// these may use unique global params, so try to first add missing model params, if any
						for (int l = 0; l < clonedBM.getModel().getModelParameters().length; l++) {
							if (currentBM.getModel().getModelParameter(clonedBM.getModel().getModelParameters()[l].getName()) == null) {
								currentBM.getModel().addModelParameter(clonedBM.getModel().getModelParameters()[l]);
							}
						}
						// now try to add the missing reactions
						for (int j = 0; j < clonedBM.getModel().getReactionSteps().length; j++) {
							if (currentBM.getModel().getReactionStep(clonedBM.getModel().getReactionSteps()[j].getName()) == null) {
								ReactionStep rxToAdd = clonedBM.getModel().getReactionSteps()[j];
								// must update pointers to objects in current model
								ReactionParticipant[] rxPart = rxToAdd.getReactionParticipants();
								for (int k = 0; k < rxPart.length; k++) {
									String spcn = rxPart[k].getSpeciesContext().getName();
									SpeciesContext sc = currentBM.getModel().getSpeciesContext(spcn);
									rxPart[k].setSpeciesContext(sc);
								}
								rxToAdd.setStructure(currentBM.getModel().getStructure(rxToAdd.getStructure().getName()));
								rxToAdd.setModel(currentBM.getModel());
								currentBM.getModel().addReactionStep(rxToAdd);
								// set the added reactions as disabled in simcontext(s)
								for (int k = 0; k < bioModels.get(i).getSimulationContexts().length; k++) {
									currentBM.getSimulationContexts()[k].getReactionContext().getReactionSpec(clonedBM.getModel().getReactionSteps()[j]).setReactionMapping(ReactionSpec.EXCLUDED);
								}
							}
						}
						bioModels.get(i).refreshDependencies();
					} catch (XmlParseException | PropertyVetoException e) {
						e.printStackTrace();
						return;
					}
				}
			}
		}
		
		BioModel bm0 = bioModels.get(0);
		for (int i = 1; i < bioModels.size(); i++) {
			System.out.println("----comparing model from----"+bioModels.get(i)+" with model from "+bm0);
			boolean matchable = bioModels.get(i).getModel().compareEqual(bm0.getModel());
			System.out.println(matchable);
			if (!matchable) return;
		}
		// all have matchable model, try to merge by pooling SimContexts
		Document dom = null;
		Xmlproducer xmlProducer = new Xmlproducer(false);
		try {
			dom = XmlHelper.bioModelToXMLDocument(bm0,false);
			Element root = dom.getRootElement();
			Element bmel = root.getChild("BioModel", root.getNamespace());
			for (int i = 1; i < bioModels.size(); i++) {
				// get XML of SimContext here and insert into baseXML
				SimulationContext[] simContexts = bioModels.get(i).getSimulationContexts();
				for (SimulationContext sc : simContexts) {
					Element scElement = xmlProducer.getXML(sc, bioModels.get(i));
					XmlUtil.setDefaultNamespace(scElement, root.getNamespace());
					bmel.addContent(scElement);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		// re-read XML into a single BioModel and replace docs List
		BioModel mergedBM = null;
		try {
			String mergedXML = XmlUtil.xmlToString(dom, true);
			mergedBM = XmlHelper.XMLToBioModel(new XMLSource(mergedXML));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		// merge succeeded, replace the list
		docs = new ArrayList<BioModel>();
		docs.add(mergedBM);
		return;
		// TODO more work here if we want to generalize
	}

	private void createOverrides(Simulation newSimulation, List<Change> changes) {
		for (Change change : changes) {
			String targetID = sbmlSupport.getIdFromXPathIdentifer(change.getTargetXPath().getTargetAsString());
			SimulationContext importedSC = null; SimulationContext convertedSC = null;
			SimulationContext currentSC = (SimulationContext)newSimulation.getSimulationOwner();
			if (((SimulationContext)newSimulation.getSimulationOwner()).getApplicationType() == Application.NETWORK_STOCHASTIC) {
				importedSC = currentSC.getBioModel().getSimulationContext(0);
				convertedSC = (SimulationContext)newSimulation.getSimulationOwner();
			} else {
				importedSC = (SimulationContext)newSimulation.getSimulationOwner();
				convertedSC = null;
			}
			String vcConstantName = resolveConstant(importedSC, convertedSC, targetID);
			if (vcConstantName == null) {
				logger.error("target in change "+change+" could not be resolved to Constant, overrides not applied");
				continue;
			}
			try {
				Expression exp = null;
				if (change.isChangeAttribute()) {
					exp = new Expression(((ChangeAttribute)change).getNewValue());
				} else if (change.isComputeChange()) {
					ComputeChange cc = (ComputeChange)change;
					ASTNode math = cc.getMath();
					exp = new ExpressionMathMLParser(null).fromMathML(math, "t");
					
					// Substitute SED-ML parameters
					List<Parameter> params = cc.getListOfParameters();
					System.out.println(params);
					for (Parameter param : params) {
						exp.substituteInPlace(new Expression(param.getId()), new Expression(param.getValue()));
					}
					
					// Substitute SED-ML variables (which reference SBML entities)
					List<org.jlibsedml.Variable> vars = cc.getListOfVariables();
					System.out.println(vars);
					for (org.jlibsedml.Variable var : vars) {
						String sbmlID = sbmlSupport.getIdFromXPathIdentifer(var.getTarget());
						String vcmlName = resolveConstant(importedSC, convertedSC, sbmlID);
						exp.substituteInPlace(new Expression(var.getId()), new Expression(vcmlName));
					}
				} else {
					logger.error("unsupported change "+change+" encountered, overrides not applied");
					continue;
				}
				Constant constant = new Constant(vcConstantName,exp);
				newSimulation.getMathOverrides().putConstant(constant);
			} catch (ExpressionException e) {
				logger.error("expression in change "+change+" could not be resolved to Constant, overrides not applied");
				continue;
			}
		}
	}

	private String resolveConstant(SimulationContext importedSimContext, SimulationContext convertedSimContext, String SBMLtargetID) {
		// finds name of math-side Constant corresponding to SBML entity, if there is one
		// returns null if there isn't

		// check ReservedSymbols first
		// TODO this is crude implementation
		ReservedSymbol rs = importedSimContext.getModel().getReservedSymbolByName(SBMLtargetID);
		if (rs != null) return SBMLtargetID;
		
		// now check mapping from importer
		MathSymbolMapping msm = (MathSymbolMapping)importedSimContext.getMathDescription().getSourceSymbolMapping();
		SBMLImporter sbmlImporter = importMap.get(importedSimContext.getBioModel());
		SBMLSymbolMapping sbmlMap = sbmlImporter.getSymbolMapping();
		SBase targetSBase = sbmlMap.getMappedSBase(SBMLtargetID);
		if (targetSBase == null){
			logger.error("couldn't find SBase with sid="+SBMLtargetID+" in SBMLSymbolMapping");
			return null;
		}
		SymbolTableEntry ste =sbmlMap.getSte(targetSBase, SymbolContext.INITIAL);
		Variable var = msm.getVariable(ste);
		if (var instanceof Constant) {
			String constantName = var.getName();
			// if simcontext was converted to stochastic then species init constants use different names
			if (convertedSimContext != null && ste instanceof SpeciesContextSpecParameter) {
				SpeciesContextSpecParameter scsp = (SpeciesContextSpecParameter)ste;
				if (scsp.getRole() == SpeciesContextSpec.ROLE_InitialConcentration || scsp.getRole() == SpeciesContextSpec.ROLE_InitialCount) {
					String spcName = scsp.getSpeciesContext().getName();
					SpeciesContextSpec scs = null;
					for (int i = 0; i < convertedSimContext.getReactionContext().getSpeciesContextSpecs().length; i++) {
						scs = convertedSimContext.getReactionContext().getSpeciesContextSpecs()[i];
						if (scs.getSpeciesContext().getName().equals(spcName)) {
							break;
						}
					}
					SpeciesContextSpecParameter convertedSCSP = scs.getInitialConditionParameter();
					var = ((MathSymbolMapping)convertedSimContext.getMathDescription().getSourceSymbolMapping()).getVariable(convertedSCSP);
					constantName = var.getName();
				}
			}
			return constantName; 
		} else {
			logger.error("couldn't find Constant target with sid="+SBMLtargetID+" in symbol mapping, var = "+var);
			return null;
		}
	}

	private void addRepeatedTasks(List<AbstractTask> ttt, HashMap<String, Simulation> vcSimulations)
			throws ExpressionException {
		for (AbstractTask selectedTask : ttt) {
			if (selectedTask instanceof RepeatedTask) {
				RepeatedTask rt = (RepeatedTask)selectedTask;
				if (!rt.getResetModel() || rt.getSubTasks().size() != 1) {
					logger.error("sequential RepeatedTask not yet supported, task "+SEDMLUtil.getName(selectedTask)+" is being skipped");
					continue;
				}
				AbstractTask referredTask;
				// find the actual Task, which can be directly referred or indirectly through a chain of RepeatedTasks (in case of multiple parameter scans)
				do {
					SubTask st = rt.getSubTasks().entrySet().iterator().next().getValue(); // single subtask
					String taskId = st.getTaskId();
					referredTask = sedml.getTaskWithId(taskId);
					if (referredTask instanceof RepeatedTask) rt = (RepeatedTask)referredTask;
				} while (referredTask instanceof RepeatedTask);
				rt = (RepeatedTask)selectedTask; // need to reset if we had a chain above
				Task actualTask = (Task)referredTask;
				Simulation simulation = vcSimulations.get(actualTask.getId());
				SimulationContext importedSC = null; SimulationContext convertedSC = null;
				SimulationContext currentSC = (SimulationContext)simulation.getSimulationOwner();
				if (((SimulationContext)simulation.getSimulationOwner()).getApplicationType() == Application.NETWORK_STOCHASTIC) {
					importedSC = currentSC.getBioModel().getSimulationContext(0);
					convertedSC = (SimulationContext)simulation.getSimulationOwner();
				} else {
					importedSC = (SimulationContext)simulation.getSimulationOwner();
					convertedSC = null;
				}
				List<SetValue> changes = rt.getChanges();
				List<Change> functions = new ArrayList<Change>();
				for (SetValue change : changes) {
					Range range = rt.getRange(change.getRangeReference());
					ASTNode math = change.getMath();
					Expression exp = new ExpressionMathMLParser(null).fromMathML(math, "t");
					if (exp.infix().equals(range.getId())) {
						// add a parameter scan to the simulation referred by the actual task
						ConstantArraySpec scanSpec = null;
						String targetID = sbmlSupport
								.getIdFromXPathIdentifer(change.getTargetXPath().getTargetAsString());
						String constant = resolveConstant(importedSC, convertedSC, targetID);
						if (range instanceof UniformRange) {
							UniformRange ur = (UniformRange)range;
							scanSpec = ConstantArraySpec.createIntervalSpec(constant,
									""+Math.min(ur.getStart(), ur.getEnd()), ""+Math.max(ur.getStart(), ur.getEnd()),
									ur.getNumberOfPoints(), ur.getType().equals(UniformType.LOG));
						} else if (range instanceof VectorRange) {
							VectorRange vr = (VectorRange)range;
							String[] values = new String[vr.getNumElements()];
							for (int i = 0; i < values.length; i++) {
								values[i] = Double.toString(vr.getElementAt(i));
							}
							scanSpec = ConstantArraySpec.createListSpec(constant, values);
						} else if (range instanceof FunctionalRange){
							FunctionalRange fr = (FunctionalRange)range;
							Range index = rt.getRange(fr.getRange());
							if (index instanceof UniformRange) {
								UniformRange ur = (UniformRange)index;
								ASTNode frMath = fr.getMath();
								Expression frExpMin = new ExpressionMathMLParser(null).fromMathML(frMath, "t");
								Expression frExpMax = new ExpressionMathMLParser(null).fromMathML(frMath, "t");

								// Substitute Range values
								frExpMin.substituteInPlace(new Expression(ur.getId()), new Expression(ur.getStart()));
								frExpMax.substituteInPlace(new Expression(ur.getId()), new Expression(ur.getEnd()));

								// Substitute SED-ML parameters
								Map<String, AbstractIdentifiableElement> params = fr.getParameters();
								System.out.println(params);
								for (String paramId : params.keySet()) {
									frExpMin.substituteInPlace(new Expression(params.get(paramId).getId()), new Expression(((Parameter)params.get(paramId)).getValue()));
									frExpMax.substituteInPlace(new Expression(params.get(paramId).getId()), new Expression(((Parameter)params.get(paramId)).getValue()));
								}
								
								// Substitute SED-ML variables (which reference SBML entities)
								Map<String, AbstractIdentifiableElement> vars = fr.getVariables();
								System.out.println(vars);
								for (String varId : vars.keySet()) {
									String sbmlID = sbmlSupport.getIdFromXPathIdentifer(((org.jlibsedml.Variable)vars.get(varId)).getTarget());
									String vcmlName = resolveConstant(importedSC, convertedSC, sbmlID);
									frExpMin.substituteInPlace(new Expression(varId), new Expression(vcmlName));
									frExpMax.substituteInPlace(new Expression(varId), new Expression(vcmlName));
								}
								frExpMin = frExpMin.simplifyJSCL();
								frExpMax = frExpMax.simplifyJSCL();
								String minValueExpStr = frExpMin.infix();
								String maxValueExpStr = frExpMax.infix();
								scanSpec = ConstantArraySpec.createIntervalSpec(constant, minValueExpStr, maxValueExpStr, ur.getNumberOfPoints(), ur.getType().equals(UniformType.LOG));
							} else if (index instanceof VectorRange) {
								VectorRange vr = (VectorRange)index;
								ASTNode frMath = fr.getMath();
								Expression expFact = new ExpressionMathMLParser(null).fromMathML(frMath, "t");
								// Substitute SED-ML parameters
								Map<String, AbstractIdentifiableElement> params = fr.getParameters();
								System.out.println(params);
								for (String paramId : params.keySet()) {
									expFact.substituteInPlace(new Expression(params.get(paramId).getId()), new Expression(((Parameter)params.get(paramId)).getValue()));
								}
								// Substitute SED-ML variables (which reference SBML entities)
								Map<String, AbstractIdentifiableElement> vars = fr.getVariables();
								System.out.println(vars);
								for (String varId : vars.keySet()) {
									String sbmlID = sbmlSupport.getIdFromXPathIdentifer(((org.jlibsedml.Variable)vars.get(varId)).getTarget());
									String vcmlName = resolveConstant(importedSC, convertedSC, sbmlID);
									expFact.substituteInPlace(new Expression(varId), new Expression(vcmlName));
								}
								expFact = expFact.simplifyJSCL();
								String[] values = new String[vr.getNumElements()];
								for (int i = 0; i < values.length; i++) {
									Expression expFinal = new Expression(expFact);
									// Substitute Range values
									expFinal.substituteInPlace(new Expression(vr.getId()), new Expression(vr.getElementAt(i)));
									expFinal = expFinal.simplifyJSCL();
									values[i] = expFinal.infix();
								}
								scanSpec = ConstantArraySpec.createListSpec(constant, values);
							} else {
								// we only support FunctionalRange with intervals and lists
								logger.error("FunctionalRange does not reference UniformRange or VectorRange, task " + SEDMLUtil.getName(selectedTask)
								+ " is being skipped");
								continue;								
							}
						} else {
							logger.error("unsupported Range class found, task " + SEDMLUtil.getName(selectedTask)
									+ " is being skipped");
							continue;
						}
						MathOverrides mo = simulation.getMathOverrides();
						mo.putConstantArraySpec(scanSpec);
					} else {
						functions.add(change);
					}
				}
				createOverrides(simulation, functions);
				// we didn't bomb out, so update the simulation
				simulation.setImportedTaskID(selectedTask.getId());
			}
		}
	}

	private void createBioModels(List<org.jlibsedml.Model> mmm, HashMap<String, BioModel> bmMap) throws Exception {
		// first go through models without changes which are unique and must be imported as new BioModel/SimContext
		for(Model mm : mmm) {
			if (mm.getListOfChanges().isEmpty()) {
				String id = mm.getId();
				bmMap.put(id, importModel(mm));
			}
		}
		// now go through models with changes and see if they refer to another model within this sedml
		for(Model mm : mmm) {
			if (!mm.getListOfChanges().isEmpty()) {
				String refID = null;
				if (mm.getSource().startsWith("#")) {
					refID = mm.getSource().substring(1);
					// direct reference within sedml
				} else {
					// need to check if it is an indirect reference (another model using same source URI)
					for (Model model : sedml.getModels()) {
						if (model != mm && model.getSource().equals(mm.getSource())) {
							refID = model.getId();
						}
					}
				}
				if (refID != null) {
					BioModel refBM = bmMap.get(refID);
					if (refBM == null) {
						//TODO at some point we need to check for sequential application of changes and apply in chain
						throw new RuntimeException("Model " + mm
								+ " refers to either a non-existent model (invalid SED-ML) or to another model with changes (not supported yet)");
					}
					boolean translateToOverrides = canTranslateToOverrides(refBM, mm);
					if (!translateToOverrides) {
						bmMap.put(mm.getId(), importModel(mm));
					} else {
						bmMap.put(mm.getId(), refBM);
					} 
				} else {
					bmMap.put(mm.getId(), importModel(mm));
				}
			}
		}
	}

	private boolean canTranslateToOverrides(BioModel refBM, Model mm) {
		List<Change> changes = mm.getListOfChanges();
		// XML changes can't be translated to math overrides, only attribute changes and compute changes can
		for (Change change : changes) {
			if (change.isAddXML() || change.isChangeXML() || change.isRemoveXML()) return false;			
		}
		// check whether all targets have addressable Constants on the Math side
		for (Change change : changes) {
			String sbmlID = sbmlSupport.getIdFromXPathIdentifer(change.getTargetXPath().toString());
			if (resolveConstant(refBM.getSimulationContext(0), null, sbmlID) == null) {
				logger.warn("could not map changeAttribute for ID "+sbmlID+" to a VCell Constant");
				return false;
			}
		}
		return true;
	}

	private BioModel importModel(Model mm) throws Exception {
		BioModel bioModel = null;
		String sedmlOriginalModelName = mm.getId();
		String sedmlOriginalModelLanguage = mm.getLanguage();
		String modelXML = resolver.getModelString(mm); // source with all the changes applied
		if (modelXML == null) {
			throw new Exception("Resolver could not find model: "+mm.getId());
		}
		String bioModelName = bioModelBaseName + "_" + sedml.getFileName() + "_" + sedmlOriginalModelName;
		try {
			if(sedmlOriginalModelLanguage.contentEquals(SUPPORTED_LANGUAGE.VCELL_GENERIC.getURN())) {	// vcml
				XMLSource vcmlSource = new XMLSource(modelXML);
				bioModel = (BioModel)XmlHelper.XMLToBioModel(vcmlSource);
				bioModel.setName(bioModelName);
				try {
					bioModel.getVCMetaData().createBioPaxObjects(bioModel);
				} catch (Exception e) {
					logger.error("failed to make BioPax objects", e);
				}
				docs.add(bioModel);
			} else {				// we assume it's sbml, if it's neither import will fail
				InputStream sbmlSource = IOUtils.toInputStream(modelXML, Charset.defaultCharset());
				boolean bValidateSBML = false;
				SBMLImporter sbmlImporter = new SBMLImporter(sbmlSource,transLogger,bValidateSBML);
				bioModel = (BioModel)sbmlImporter.getBioModel();
				bioModel.setName(bioModelName);
				bioModel.getSimulationContext(0).setName(mm.getName());
				bioModel.updateAll(false);
				docs.add(bioModel);
				importMap.put(bioModel, sbmlImporter);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			throw e;
		}
		return bioModel;
	}

	private void printSEDMLSummary(List<org.jlibsedml.Model> mmm, List<org.jlibsedml.Simulation> sss,
			List<AbstractTask> ttt, List<DataGenerator> ddd, List<Output> ooo) {
		for(Model mm : mmm) {
		    logger.trace("sedml model: "+mm.toString());
		    List<Change> listOfChanges = mm.getListOfChanges();
		    logger.debug("There are " + listOfChanges.size() + " changes in model "+mm.getId());
		}
		for(org.jlibsedml.Simulation ss : sss) {
		    logger.trace("sedml simulaton: "+ss.toString());
		}
		for(AbstractTask tt : ttt) {
		    logger.trace("sedml task: "+tt.toString());
		}
		for(DataGenerator dd : ddd) {
		    logger.trace("sedml dataGenerator: "+dd.toString());
		}
		for(Output oo : ooo) {
		    logger.trace("sedml output: "+oo.toString());
		}
	}

	private void translateTimeBounds(SolverTaskDescription simTaskDesc, org.jlibsedml.Simulation sedmlSimulation) throws PropertyVetoException {
		TimeBounds timeBounds = new TimeBounds();
		TimeStep timeStep = new TimeStep();
		double outputTimeStep = 0.1;
		int outputNumberOfPoints = 1;
		// we translate initial time to zero, we provide output for the duration of the simulation
		// because we can't select just an interval the way the SEDML simulation can
		double initialTime = ((UniformTimeCourse) sedmlSimulation).getInitialTime();
		double outputStartTime = ((UniformTimeCourse) sedmlSimulation).getOutputStartTime();
		double outputEndTime = ((UniformTimeCourse) sedmlSimulation).getOutputEndTime();
		outputNumberOfPoints = ((UniformTimeCourse) sedmlSimulation).getNumberOfPoints();
		outputTimeStep = (outputEndTime - outputStartTime) / outputNumberOfPoints;
		timeBounds = new TimeBounds(0, outputEndTime - initialTime);

		OutputTimeSpec outputTimeSpec = new UniformOutputTimeSpec(outputTimeStep);
		simTaskDesc.setTimeBounds(timeBounds);
		simTaskDesc.setTimeStep(timeStep);
		if (simTaskDesc.getSolverDescription().supports(outputTimeSpec)) {
			simTaskDesc.setOutputTimeSpec(outputTimeSpec);
		} else {
			simTaskDesc.setOutputTimeSpec(new DefaultOutputTimeSpec(1,Integer.max(DefaultOutputTimeSpec.DEFAULT_KEEP_AT_MOST, outputNumberOfPoints)));
		}

	}
	
	private void translateAlgorithmParams(SolverTaskDescription simTaskDesc, org.jlibsedml.Simulation sedmlSimulation) throws PropertyVetoException {
		TimeStep timeStep = simTaskDesc.getTimeStep();
		Algorithm algorithm = sedmlSimulation.getAlgorithm();
		String kisaoID = algorithm.getKisaoID();
		ErrorTolerance errorTolerance = new ErrorTolerance();
		List<AlgorithmParameter> sedmlAlgorithmParameters = algorithm.getListOfAlgorithmParameters();
		for(AlgorithmParameter sedmlAlgorithmParameter : sedmlAlgorithmParameters) {

			String apKisaoID = sedmlAlgorithmParameter.getKisaoID();
			String apValue = sedmlAlgorithmParameter.getValue();
			if(apKisaoID == null || apKisaoID.isEmpty()) {
				logger.error("Undefined KisaoID algorithm parameter for algorithm '" + kisaoID + "'");
			}

			// we don't check if the recognized algorithm parameters are valid for our algorithm
			// we just use any parameter we find for the solver task description we have, assuming the exporting code did all the checks
			// WARNING: if our algorithm is the result of a guess, this may be wrong
			// TODO: use the proper ontology for the algorithm parameters kisao id
			if(apKisaoID.contentEquals(ErrorTolerance.ErrorToleranceDescription.Absolute.getKisao())) {
				double value = Double.parseDouble(apValue);
				errorTolerance.setAbsoluteErrorTolerance(value);
			} else if(apKisaoID.contentEquals(ErrorTolerance.ErrorToleranceDescription.Relative.getKisao())) {
				double value = Double.parseDouble(apValue);
				errorTolerance.setRelativeErrorTolerance(value);
			} else if(apKisaoID.contentEquals(TimeStep.TimeStepDescription.Default.getKisao())) {
				double value = Double.parseDouble(apValue);
				timeStep.setDefaultTimeStep(value);
			} else if(apKisaoID.contentEquals(TimeStep.TimeStepDescription.Maximum.getKisao())) {
				double value = Double.parseDouble(apValue);
				timeStep.setMaximumTimeStep(value);
			} else if(apKisaoID.contentEquals(TimeStep.TimeStepDescription.Minimum.getKisao())) {
				double value = Double.parseDouble(apValue);
				timeStep.setMinimumTimeStep(value);
			} else if(apKisaoID.contentEquals(AlgorithmParameterDescription.Seed.getKisao())) {		// custom seed
				if(simTaskDesc.getSimulation().getMathDescription().isNonSpatialStoch()) {
					NonspatialStochSimOptions nssso = simTaskDesc.getStochOpt();
					int value = Integer.parseInt(apValue);
					nssso.setCustomSeed(value);
				} else {
					logger.error("Algorithm parameter '" + AlgorithmParameterDescription.Seed.getDescription() +"' is only supported for nonspatial stochastic simulations");
				}
				// some arguments used only for non-spatial hybrid solvers
			} else if(apKisaoID.contentEquals(AlgorithmParameterDescription.Epsilon.getKisao())) {
				NonspatialStochHybridOptions nssho = simTaskDesc.getStochHybridOpt();
				nssho.setEpsilon(Double.parseDouble(apValue));
			} else if(apKisaoID.contentEquals(AlgorithmParameterDescription.Lambda.getKisao())) {
				NonspatialStochHybridOptions nssho = simTaskDesc.getStochHybridOpt();
				nssho.setLambda(Double.parseDouble(apValue));
			} else if(apKisaoID.contentEquals(AlgorithmParameterDescription.MSRTolerance.getKisao())) {
				NonspatialStochHybridOptions nssho = simTaskDesc.getStochHybridOpt();
				nssho.setMSRTolerance(Double.parseDouble(apValue));
			} else if(apKisaoID.contentEquals(AlgorithmParameterDescription.SDETolerance.getKisao())) {
				NonspatialStochHybridOptions nssho = simTaskDesc.getStochHybridOpt();
				nssho.setSDETolerance(Double.parseDouble(apValue));
			} else {
				logger.error("Algorithm parameter with kisao id '" + apKisaoID + "' not supported at this time, skipping.");
			}
		}
		simTaskDesc.setErrorTolerance(errorTolerance);
	}
}