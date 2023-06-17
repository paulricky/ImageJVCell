package org.vcell.sedml;

import cbit.util.xml.XmlRdfUtil;
import cbit.util.xml.XmlUtil;
import cbit.vcell.biomodel.BioModel;
import cbit.vcell.biomodel.ModelUnitConverter;
import cbit.vcell.geometry.GeometryClass;
import cbit.vcell.mapping.*;
import cbit.vcell.mapping.SimulationContext.Application;
import cbit.vcell.mapping.SpeciesContextSpec.SpeciesContextSpecParameter;
import cbit.vcell.mapping.StructureMapping.StructureMappingParameter;
import cbit.vcell.math.Constant;
import cbit.vcell.math.MathUtilities;
import cbit.vcell.model.Kinetics.KineticsParameter;
import cbit.vcell.model.*;
import cbit.vcell.model.Model.ModelParameter;
import cbit.vcell.model.Model.ReservedSymbol;
import cbit.vcell.model.Structure.StructureSize;
import cbit.vcell.parser.*;
import cbit.vcell.publish.ITextWriter;
import cbit.vcell.resource.NativeLib;
import cbit.vcell.solver.Simulation;
import cbit.vcell.solver.*;
import cbit.vcell.solver.MathOverridesResolver.SymbolReplacement;
import cbit.vcell.xml.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jlibsedml.Model;
import org.jlibsedml.*;
import org.jlibsedml.UniformRange.UniformType;
import org.jlibsedml.modelsupport.SBMLSupport;
import org.jlibsedml.modelsupport.SBMLSupport.CompartmentAttribute;
import org.jlibsedml.modelsupport.SBMLSupport.ParameterAttribute;
import org.jlibsedml.modelsupport.SBMLSupport.SpeciesAttribute;
import org.jlibsedml.modelsupport.SUPPORTED_LANGUAGE;
import org.jmathml.ASTNode;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.libcombine.CombineArchive;
import org.sbml.libcombine.KnownFormats;
import org.sbpax.impl.HashGraph;
import org.sbpax.schemas.util.DefaultNameSpaces;
import org.sbpax.schemas.util.OntUtil;
import org.sbpax.util.SesameRioUtil;
import org.vcell.sbml.SbmlException;
import org.vcell.sbml.SimSpec;
import org.vcell.sbml.vcell.SBMLExporter;
import org.vcell.util.FileUtils;
import org.vcell.util.ISize;
import org.vcell.util.Pair;
import org.vcell.util.TokenMangler;
import org.vcell.util.document.BioModelInfo;
import org.vcell.util.document.PublicationInfo;
import org.vcell.util.document.Version;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class SEDMLExporter {
	private final static Logger logger = LogManager.getLogger(SEDMLExporter.class);

	private int sedmlLevel = 1;
	private int sedmlVersion = 2;
	private  SedML sedmlModel = null;
	private cbit.vcell.biomodel.BioModel vcBioModel = null;
	private String jobId = null;
	private ArrayList<String> modelFilePathStrAbsoluteList = new ArrayList<String>();
	private ArrayList<String> sedmlFilePathStrAbsoluteList = new ArrayList<String>();
	private List<String> simsToExport = new ArrayList<String>();

	private static String DATAGENERATOR_TIME_NAME = "time";
	private static String DATAGENERATOR_TIME_SYMBOL = "t";
	
	private String sbmlLanguageURN = SUPPORTED_LANGUAGE.SBML_GENERIC.getURN();
	private String vcmlLanguageURN = SUPPORTED_LANGUAGE.VCELL_GENERIC.getURN();
	
	private SEDMLRecorder sedmlRecorder = null;
	private int simCount;
	private int overrideCount;

	private SBMLSupport sbmlSupport = new SBMLSupport();


	public SEDMLExporter(String argJobId, BioModel argBiomodel, int argLevel, int argVersion, List<Simulation> argSimsToExport) {
		this(argJobId, argBiomodel, argLevel, argVersion, argSimsToExport, null);
	}

	public SEDMLExporter(String argJobId, BioModel argBiomodel, int argLevel, int argVersion, List<Simulation> argSimsToExport, String jsonFilePath) {

		super();
		
		this.jobId = argJobId;
		this.vcBioModel = argBiomodel;
		this.sedmlLevel = argLevel;
		this.sedmlVersion = argVersion;

		this.sedmlRecorder = new SEDMLRecorder(argJobId, SEDMLConversion.EXPORT, jsonFilePath);
        // we need to collect simulation names to be able to match sims in BioModel clone
		if (argSimsToExport != null && argSimsToExport.size() > 0) {
	        for (Simulation sim : argSimsToExport) {
	        	simsToExport.add(sim.getName());
	        }
		} else {
			simsToExport = null;
		}
	}

	public SEDMLDocument getSEDMLDocument(String sPath, String sBaseFileName, ModelFormat modelFormat,
				boolean bFromCLI, boolean bRoundTripSBMLValidation) {
		
		double start = System.currentTimeMillis();

		// Create an SEDMLDocument and create the SEDMLModel from the document, so that other details can be added to it in translateBioModel()
		SEDMLDocument sedmlDocument = new SEDMLDocument(this.sedmlLevel, this.sedmlVersion);

		final String VCML_NS = "http://sourceforge.net/projects/vcell/vcml";
		final String VCML_NS_PREFIX = "vcml";
		
		List<Namespace> nsList = new ArrayList<>();
		Namespace ns = Namespace.getNamespace(SEDMLTags.MATHML_NS_PREFIX, SEDMLTags.MATHML_NS);
		nsList.add(ns);
		ns = Namespace.getNamespace(VCML_NS_PREFIX, VCML_NS);
		nsList.add(ns);

		if (modelFormat.equals(ModelFormat.SBML) || modelFormat.equals(ModelFormat.SBML_VCML)) {
			final String SBML_NS = "http://www.sbml.org/sbml/level3/version2/core";
			final String SBML_NS_PREFIX = "sbml";
			final String SPATIAL_NS = "https://sbml.org/documents/specifications/level-3/version-1/spatial";
			final String SPATIAL_NS_PREFIX = "spatial";
			ns = Namespace.getNamespace(SBML_NS_PREFIX, SBML_NS);
			nsList.add(ns);
			SimulationContext[] simContexts = vcBioModel.getSimulationContexts();
			for (SimulationContext sc : simContexts) {
				if (sc.getGeometry() != null && sc.getGeometry().getDimension() > 0) {
					ns = Namespace.getNamespace(SPATIAL_NS_PREFIX, SPATIAL_NS);
					nsList.add(ns);
					break;
				}
			} 
		}
		sedmlModel = sedmlDocument.getSedMLModel();
		sedmlModel.setAdditionalNamespaces(nsList);
		
		this.translateBioModelToSedML(sPath, sBaseFileName, modelFormat, bFromCLI, bRoundTripSBMLValidation);
		
		double stop = System.currentTimeMillis();
		Exception timer = new Exception(Double.toString((stop-start)/1000)+" seconds");
		// update overall status
		if (bFromCLI) {
			if (sedmlRecorder.hasErrors()) {
				sedmlRecorder.addTaskRecord(vcBioModel.getName(), TaskType.BIOMODEL, TaskResult.FAILED, timer);
			} else {
				sedmlRecorder.addTaskRecord(vcBioModel.getName(), TaskType.BIOMODEL, TaskResult.SUCCEEDED, timer);
			}
		}
		// should never bomb out just because we fail to export to json...
		try {
			this.sedmlRecorder.exportToJSON();
		} catch (Exception e) {
			logger.error("Failed to export to JSON", e);
		}
		return sedmlDocument;
	}

	private void translateBioModelToSedML(String savePath, String sBaseFileName, ModelFormat modelFormat,
				boolean bFromCLI, boolean bRoundTripSBMLValidation) {		// true if invoked for omex export, false if for sedml
		modelFilePathStrAbsoluteList.clear();
		try {

			if (modelFormat == ModelFormat.SBML_VCML) {
				// TODO
				throw new RuntimeException("Hybrid SBML_VCML export not yet implemented");
			}
			if (modelFormat == ModelFormat.VCML) {
				BioModel prunedBM = XmlHelper.cloneBioModel(vcBioModel);
				for (Simulation sim : prunedBM.getSimulations()) {
					prunedBM.removeSimulation(sim);
				}
				String vcmlString = XmlHelper.bioModelToXML(prunedBM);
				String modelFileNameRel = sBaseFileName+"_sedml.vcml";
				String modelFileNameAbs = Paths.get(savePath,modelFileNameRel).toString();
				XmlUtil.writeXMLStringToFile(vcmlString, modelFileNameAbs, false);
				modelFilePathStrAbsoluteList.add(modelFileNameRel);
				for (int i = 0; i < vcBioModel.getSimulationContexts().length; i++) {
					writeModelVCML(modelFileNameRel, vcBioModel.getSimulationContext(i));
					sedmlRecorder.addTaskRecord(vcBioModel.getSimulationContext(i).getName(), TaskType.SIMCONTEXT, TaskResult.SUCCEEDED, null);
					exportSimulations(i, vcBioModel.getSimulationContext(i), null, null, bFromCLI, vcmlLanguageURN);
				}
			}
			if (modelFormat == ModelFormat.SBML) {
				try {
//					// TODO: uncomment the for loop below to only export non-spatial
//					for(Simulation sim : vcBioModel.getSimulations()) {
//						if(sim.isSpatial()) {
//							sedmlRecorder.addTaskLog(vcBioModel.getName(), TaskType.MODEL, TaskResult.FAILED, new RuntimeException("spatial"));
//							return;
//						}
//					}
					
					// convert to SBML units; this also ensures we will use a clone
					vcBioModel = ModelUnitConverter.createBioModelWithSBMLUnitSystem(vcBioModel);
					sedmlRecorder.addTaskRecord(vcBioModel.getName(), TaskType.UNITS, TaskResult.SUCCEEDED, null);
				} catch (Exception e1) {
					String msg = "unit conversion failed for BioModel '"+vcBioModel.getName()+"': " + e1.getMessage();
					logger.error(msg, e1);
					sedmlRecorder.addTaskRecord(vcBioModel.getName(), TaskType.UNITS, TaskResult.FAILED, e1);
					if (bFromCLI) {
						return;
					} else {
						throw e1;
					}
				}
				SimulationContext[] simContexts = vcBioModel.getSimulationContexts();

				if (simContexts.length == 0) {
					sedmlRecorder.addTaskRecord(vcBioModel.getName(), TaskType.MODEL, TaskResult.FAILED, new Exception("Model has no Applications"));
				} else {
					int simContextCnt = 0;	// for model count, task subcount
					for (SimulationContext simContext : simContexts) {
						// Export the application itself to SBML, with default values (overrides will become model changes or repeated tasks)
						String sbmlString = null;
						Map<Pair <String, String>, String> l2gMap = null;		// local to global translation map
						boolean sbmlExportFailed = false;
						Exception simContextException = null;
						try {
							SBMLExporter.validateSimulationContextSupport(simContext);
							boolean isSpatial = simContext.getGeometry().getDimension() > 0 ? true : false;
							Pair <String, Map<Pair <String, String>, String>> pair = XmlHelper.exportSBMLwithMap(vcBioModel, 3, 2, 0, isSpatial, simContext, bRoundTripSBMLValidation);
							sbmlString = pair.one;
							l2gMap = pair.two;
							writeModelSBML(savePath, sBaseFileName, sbmlString, simContext);
							sedmlRecorder.addTaskRecord(simContext.getName(), TaskType.SIMCONTEXT, TaskResult.SUCCEEDED, null);
						} catch (Exception e) {
							String msg = "SBML export failed for simContext '"+simContext.getName()+"': " + e.getMessage();
							logger.error(msg, e);
							sbmlExportFailed = true;
							simContextException = e;
							sedmlRecorder.addTaskRecord(simContext.getName(), TaskType.SIMCONTEXT, TaskResult.FAILED, e);
						}
	
						if (!sbmlExportFailed) {
							// simContext was exported succesfully, now we try to export its simulations
							exportSimulations(simContextCnt, simContext, sbmlString, l2gMap, bFromCLI, sbmlLanguageURN);
						} else {
							if (bFromCLI) {
								continue;
							} else {
								System.err.println(sedmlRecorder.getRecordsAsCSV());
								throw new Exception ("SimContext '"+simContext.getName()+"' could not be exported to SBML :" +simContextException.getMessage(), simContextException);
							}
						}			
						simContextCnt++;
					}
				}
			}
	       	if(sedmlModel.getModels() != null && sedmlModel.getModels().size() > 0) {
	       		logger.trace("Number of models in the sedml is " + sedmlModel.getModels().size());
	       	}
	       	if (sedmlRecorder.hasErrors()) {
				System.err.println(sedmlRecorder.getRecordsAsCSV());       		
	       	} else {
	       		System.out.println(sedmlRecorder.getRecordsAsCSV());
	       	}
		} catch (Exception e) {
			// this only happens if not from CLI, we need to pass this down the calling thread
			throw new RuntimeException("Error adding model to SEDML document : " + e.getMessage(), e);
		}
	}

	private void writeModelVCML(String filePathStrRelative, SimulationContext simContext) {
		String simContextName = simContext.getName();
		String simContextId = TokenMangler.mangleToSName(simContextName);
		sedmlModel.addModel(new Model(simContextId, simContextName, vcmlLanguageURN, filePathStrRelative+"#"+VCMLSupport.getXPathForSimContext(simContextName)));
	}

	private void exportSimulations(int simContextCnt, SimulationContext simContext,
			String sbmlString, Map<Pair<String, String>, String> l2gMap, boolean bFromCLI, String languageURN) throws Exception {
		// -------
		// create sedml objects (simulation, task, datagenerators, report, plot) for each simulation in simcontext 
		// -------	
		String simContextName = simContext.getName();
		String simContextId = TokenMangler.mangleToSName(simContextName);
		simCount = 0;
		overrideCount = 0;
		if (simContext.getSimulations().length == 0) return;
		for (Simulation vcSimulation : simContext.getSimulations()) {
			try {
				// if we have a hash containing a subset of simulations to export
				// skip simulations not present in hash
				if (simsToExport != null && !simsToExport.contains(vcSimulation.getName())) continue;

				// 1 -------> check compatibility
				// if simContext is non-spatial stochastic, check if sim is histogram; if so, skip it, it can't be encoded in sedml 1.x
				SolverTaskDescription simTaskDesc = vcSimulation.getSolverTaskDescription();
				if (simContext.getGeometry().getDimension() == 0 && simContext.isStoch()) {
					long numOfTrials = simTaskDesc.getStochOpt().getNumOfTrials();
					if (numOfTrials > 1) {
						String msg = simContextName + " ( " + vcSimulation.getName() + " ) : export of non-spatial stochastic simulation with histogram option to SEDML not supported at this time.";
						throw new Exception(msg);
					}
				}
				
				// 2 ------->
				// create sedmlSimulation (UniformTimeCourse) with specs and algorithm parameters
				UniformTimeCourse utcSim = createSEDMLsim(simTaskDesc);

				// 3 ------->
				// create Tasks
				Set<String> dataGeneratorTasksSet = new LinkedHashSet<>();	// tasks not referenced as subtasks by any other (repeated) task; only these will have data generators
				MathOverrides mathOverrides = vcSimulation.getMathOverrides(); // need to clone so we can manipulate expressions
				createSEDMLtasks(simContextCnt, l2gMap, simContextName, simContextId,
						vcSimulation, utcSim, dataGeneratorTasksSet, mathOverrides, languageURN);
				
				// 4 ------->
				// Create DataGenerators

				List<DataGenerator> dataGeneratorsOfSim = createSEDMLdatagens(sbmlString, simContext, dataGeneratorTasksSet);

				// 5 ------->
				// create Report and Plot

				for(String taskRef : dataGeneratorTasksSet) {
					createSEDMLoutputs(simContext, vcSimulation, dataGeneratorsOfSim, taskRef);
				}
				sedmlRecorder.addTaskRecord(vcSimulation.getName(), TaskType.SIMULATION, TaskResult.SUCCEEDED, null);
			} catch (Exception e) {
				String msg = "SEDML export failed for simulation '"+ vcSimulation.getName() + "': " + e.getMessage();
				logger.error(msg, e);
				sedmlRecorder.addTaskRecord(vcSimulation.getName(), TaskType.SIMULATION, TaskResult.FAILED, e);
	        	if (bFromCLI) {
	        		continue;
	        	} else {
					System.err.println(sedmlRecorder.getRecordsAsCSV());
	        		throw e;
	        	}
			}
			simCount++;
		}
		return;
	}

	private void createSEDMLoutputs(SimulationContext simContext, Simulation vcSimulation,
			List<DataGenerator> dataGeneratorsOfSim, String taskRef) {
		// add output to sedml Model : 1 plot2d for each non-spatial simulation with all vars (species/output functions) vs time (1 curve per var)
		// ignoring output for spatial deterministic (spatial stochastic is not exported to SEDML) and non-spatial stochastic applications with histogram
		if (!(simContext.getGeometry().getDimension() > 0)) {
			String plot2dId = "plot2d_" + TokenMangler.mangleToSName(vcSimulation.getName());
			String reportId = "report_" + TokenMangler.mangleToSName(vcSimulation.getName());
			//								String reportId = "__plot__" + plot2dId;
			String plotName = simContext.getName() + "_" + vcSimulation.getName() + "_plot";
			Plot2D sedmlPlot2d = new Plot2D(plot2dId, plotName);
			Report sedmlReport = new Report(reportId, plotName);

			sedmlPlot2d.setNote(createNotesElement("Plot of all variables and output functions from application '" + simContext.getName() + "' ; simulation '" + vcSimulation.getName() + "' in VCell model"));
			sedmlReport.setNote(createNotesElement("Report of all variables and output functions from application '" + simContext.getName() + "' ; simulation '" + vcSimulation.getName() + "' in VCell model"));
			DataGenerator dgtime = sedmlModel.getDataGeneratorWithId(DATAGENERATOR_TIME_NAME + "_" + taskRef);
			String xDataRef = dgtime.getId();
			String xDatasetXId = "__data_set__" + plot2dId + dgtime.getId();
			DataSet dataSet = new DataSet(xDatasetXId, DATAGENERATOR_TIME_NAME, xDataRef, xDataRef);	// id, name, label, data generator reference
			sedmlReport.addDataSet(dataSet);

			// add a curve for each dataGenerator in SEDML model
			int curveCnt = 0;
			// String id, String name, ASTNode math
			for (DataGenerator dg : dataGeneratorsOfSim) {
				// no curve for time, since time is xDateReference
				if (dg.getId().equals(xDataRef)) {
					continue;
				}
				String curveId = "curve_" + plot2dId + "_" + dg.getId();
				String datasetYId = "__data_set__" + plot2dId + dg.getId();
				Curve curve = new Curve(curveId, dg.getName(), false, false, xDataRef, dg.getId());
				sedmlPlot2d.addCurve(curve);
				//									// id, name, label, dataRef
				//									// dataset id    <- unique id
				//									// dataset name  <- data generator name
				//									// dataset label <- dataset id
				DataSet yDataSet = new DataSet(datasetYId, dg.getName(), dg.getId(), dg.getId());
				sedmlReport.addDataSet(yDataSet);
				curveCnt++;
			}
			sedmlModel.addOutput(sedmlPlot2d);
			sedmlModel.addOutput(sedmlReport);
		} else {		// spatial deterministic
			if(simContext.getApplicationType().equals(Application.NETWORK_DETERMINISTIC)) {	// we ignore spatial stochastic (Smoldyn)
				// TODO: add curves/surfaces to the plots
				String plot3dId = "plot3d_" + TokenMangler.mangleToSName(vcSimulation.getName());
				String reportId = "report_" + TokenMangler.mangleToSName(vcSimulation.getName());
				String plotName = simContext.getName() + "plots";
				Plot3D sedmlPlot3d = new Plot3D(plot3dId, plotName);
				Report sedmlReport = new Report(reportId, plotName);

				sedmlPlot3d.setNote(createNotesElement("Plot of all variables and output functions from application '" + simContext.getName() + "' ; simulation '" + vcSimulation.getName() + "' in VCell model"));
				sedmlReport.setNote(createNotesElement("Report of all variables and output functions from application '" + simContext.getName() + "' ; simulation '" + vcSimulation.getName() + "' in VCell model"));
				DataGenerator dgtime = sedmlModel.getDataGeneratorWithId(DATAGENERATOR_TIME_NAME + "_" + taskRef);
				String xDataRef = dgtime.getId();
				String xDatasetXId = "__data_set__" + plot3dId + dgtime.getId();
				DataSet dataSet = new DataSet(xDatasetXId, DATAGENERATOR_TIME_NAME, xDataRef, xDataRef);	// id, name, label, data generator reference
				sedmlReport.addDataSet(dataSet);

				// add a curve for each dataGenerator in SEDML model
				int curveCnt = 0;
				// String id, String name, ASTNode math
				for (DataGenerator dg : dataGeneratorsOfSim) {
					// no curve for time, since time is xDateReference
					if (dg.getId().equals(xDataRef)) {
						continue;
					}
					String curveId = "curve_" + plot3dId + "_" + dg.getId();
					String datasetYId = "__data_set__" + plot3dId + dg.getId();

					DataSet yDataSet = new DataSet(datasetYId, dg.getName(), dg.getId(), dg.getId());
					sedmlReport.addDataSet(yDataSet);
					curveCnt++;
				}
				sedmlModel.addOutput(sedmlReport);
			}
		}
	}

	private List<DataGenerator> createSEDMLdatagens(String sbmlString, SimulationContext simContext, Set<String> dataGeneratorTasksSet)
			throws IOException, SbmlException, XMLStreamException {
		List<DataGenerator> dataGeneratorsOfSim = new ArrayList<> ();					
		for(String taskRef : dataGeneratorTasksSet) {
			// add one DataGenerator for 'time'
			String timeDataGenPrefix = DATAGENERATOR_TIME_NAME + "_" + taskRef;
			DataGenerator timeDataGen = sedmlModel.getDataGeneratorWithId(timeDataGenPrefix);
			org.jlibsedml.Variable timeVar = new org.jlibsedml.Variable(DATAGENERATOR_TIME_SYMBOL + "_" + taskRef, DATAGENERATOR_TIME_SYMBOL, taskRef, VariableSymbol.TIME);
			ASTNode math = Libsedml.parseFormulaString(DATAGENERATOR_TIME_SYMBOL + "_" + taskRef);
			timeDataGen = new DataGenerator(timeDataGenPrefix, timeDataGenPrefix, math);
			timeDataGen.addVariable(timeVar);
			sedmlModel.addDataGenerator(timeDataGen);
			dataGeneratorsOfSim.add(timeDataGen);

			String dataGenIdPrefix = "dataGen_" + taskRef;

			// add dataGenerators for species
			// get species list from SBML model.
			ArrayList<String> varNamesList = new ArrayList<String>();
			if (sbmlString != null) {
				String[] sbmlVars = SimSpec.fromSBML(sbmlString).getVarsList();
				for (String sbmlVar : sbmlVars) {
					varNamesList.add(sbmlVar);
				}
			} else {
				SpeciesContextSpec[] scSpecs = simContext.getReactionContext().getSpeciesContextSpecs();
				for (SpeciesContextSpec scs : scSpecs) {
					varNamesList.add(scs.getSpeciesContext().getName());
				}
			}
			for (String varName : varNamesList) {
				String varId = TokenMangler.mangleToSName(varName) + "_" + taskRef;
				org.jlibsedml.Variable sedmlVar = null;
				if (sbmlString != null) {
					sedmlVar = new org.jlibsedml.Variable(varId, varName, taskRef, sbmlSupport.getXPathForSpecies(varName));
				} else {
					sedmlVar = new org.jlibsedml.Variable(varId, varName, taskRef, VCMLSupport.getXPathForSpeciesContextSpec(simContext.getName(), varName));
				}
				ASTNode varMath = Libsedml.parseFormulaString(varId);
				String dataGenId = dataGenIdPrefix + "_" + TokenMangler.mangleToSName(varName); //"dataGen_" + varCount; - old code
				DataGenerator dataGen = new DataGenerator(dataGenId, varName, varMath);
				dataGen.addVariable(sedmlVar);
				sedmlModel.addDataGenerator(dataGen);
				dataGeneratorsOfSim.add(dataGen);
			}
			// add dataGenerators for output functions
			// get output function list from SBML model
			varNamesList = new ArrayList<String>();
			if (sbmlString != null) {
				SBMLDocument sbmlDoc = new SBMLReader().readSBMLFromString(sbmlString);
				List<Parameter> listofGlobalParams = sbmlDoc.getModel().getListOfParameters();
				for (int i = 0; i < listofGlobalParams.size(); i++) {
					Parameter sbmlGlobalParam = listofGlobalParams.get(i);
					// check whether it is a vcell-exported output function
					Annotation paramAnnotation = sbmlGlobalParam.getAnnotation();
					if (paramAnnotation != null && paramAnnotation.getNonRDFannotation() != null) {
						XMLNode paramElement = paramAnnotation.getNonRDFannotation()
								.getChildElement(XMLTags.SBML_VCELL_OutputFunctionTag, "*");
						if (paramElement != null) {
							String varName = sbmlGlobalParam.getId();
							varNamesList.add(varName);
						}
					}
				} 
			} else {
				List<AnnotatedFunction> ofs = simContext.getOutputFunctionContext().getOutputFunctionsList();
				for (AnnotatedFunction of : ofs) {
					varNamesList.add(of.getName());
				}
			}
			for (String varName : varNamesList) {
				String varId = TokenMangler.mangleToSName(varName) + "_" + taskRef;
				org.jlibsedml.Variable sedmlVar;
				if (sbmlString != null) {
					sedmlVar = new org.jlibsedml.Variable(varId, varName, taskRef, sbmlSupport.getXPathForGlobalParameter(varName));
				} else {
					sedmlVar = new org.jlibsedml.Variable(varId, varName, taskRef, VCMLSupport.getXPathForOutputFunction(simContext.getName(),varName));					
				}
				ASTNode varMath = Libsedml.parseFormulaString(varId);
				String dataGenId = dataGenIdPrefix + "_" + TokenMangler.mangleToSName(varName); //"dataGen_" + varCount; - old code
				DataGenerator dataGen = new DataGenerator(dataGenId, varName, varMath);
				dataGen.addVariable(sedmlVar);
				sedmlModel.addDataGenerator(dataGen);
				dataGeneratorsOfSim.add(dataGen);
			}
		}
		return dataGeneratorsOfSim;
	}

	private UniformTimeCourse createSEDMLsim(SolverTaskDescription simTaskDesc) {
		// list of kisao terms in vcell-core/src/main/resources/kisao_algs.obo
		SolverDescription vcSolverDesc = simTaskDesc.getSolverDescription();
		String kiSAOIdStr = vcSolverDesc.getKisao();
		Algorithm sedmlAlgorithm = new Algorithm(kiSAOIdStr);
		Notes an = createNotesElement("");	// we show the description of kisao terms for AlgorithmParameters as notes
											// for L1V4 and up, AlgorithmParameters has a "name" field we can use instead
		sedmlAlgorithm.setNote(an);
		TimeBounds vcSimTimeBounds = simTaskDesc.getTimeBounds();
		double startingTime = vcSimTimeBounds.getStartingTime();
		String simName = simTaskDesc.getSimulation().getName();
		UniformTimeCourse utcSim = new UniformTimeCourse(TokenMangler.mangleToSName(simName), simName, startingTime, startingTime, 
				vcSimTimeBounds.getEndingTime(), (int) simTaskDesc.getExpectedNumTimePoints(), sedmlAlgorithm);

		boolean enableAbsoluteErrorTolerance;		// --------- deal with error tolerance
		boolean enableRelativeErrorTolerance;
		if (vcSolverDesc.isSemiImplicitPdeSolver() || vcSolverDesc.isChomboSolver()) {
			enableAbsoluteErrorTolerance = false;
			enableRelativeErrorTolerance = true;
		} else if (vcSolverDesc.hasErrorTolerance()) {
			enableAbsoluteErrorTolerance = true;
			enableRelativeErrorTolerance = true;
		} else {
			enableAbsoluteErrorTolerance = false;
			enableRelativeErrorTolerance = false;
		}	
		if(enableAbsoluteErrorTolerance) {
			ErrorTolerance et = simTaskDesc.getErrorTolerance();
			String kisaoStr = ErrorTolerance.ErrorToleranceDescription.Absolute.getKisao();
			String kisaoDesc = ErrorTolerance.ErrorToleranceDescription.Absolute.getDescription();
			AlgorithmParameter sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, et.getAbsoluteErrorTolerance()+"");
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);
		}
		if(enableRelativeErrorTolerance) {
			ErrorTolerance et = simTaskDesc.getErrorTolerance();
			String kisaoStr = ErrorTolerance.ErrorToleranceDescription.Relative.getKisao();
			String kisaoDesc = ErrorTolerance.ErrorToleranceDescription.Relative.getDescription();
			AlgorithmParameter sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, et.getRelativeErrorTolerance()+"");
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);
		}

		boolean enableDefaultTimeStep;		// ---------- deal with time step (code adapted from TimeSpecPanel.refresh()
		boolean enableMinTimeStep;
		boolean enableMaxTimeStep;
		if (vcSolverDesc.compareEqual(SolverDescription.StochGibson)) { // stochastic time
			enableDefaultTimeStep = false;
			enableMinTimeStep = false;
			enableMaxTimeStep = false;
		} else if(vcSolverDesc.compareEqual(SolverDescription.NFSim)) {
			enableDefaultTimeStep = false;
			enableMinTimeStep = false;
			enableMaxTimeStep = false;
		} else {
			// fixed time step solvers and non spatial stochastic solvers only show default time step.
			if (!vcSolverDesc.hasVariableTimestep() || vcSolverDesc.isNonSpatialStochasticSolver()) {
				enableDefaultTimeStep = true;
				enableMinTimeStep = false;
				enableMaxTimeStep = false;
			} else {
				// variable time step solvers shows min and max, but sundials solvers don't show min
				enableDefaultTimeStep = false;
				enableMinTimeStep = true;
				enableMaxTimeStep = true;			
				if (vcSolverDesc.hasSundialsTimeStepping()) {
					enableMinTimeStep = false;
				}
			}
		}
		if (vcSolverDesc == SolverDescription.SundialsPDE) {
			String kisaoStr = SolverDescription.AlgorithmParameterDescription.PDEMeshSize.getKisao();
			String kisaoDesc = SolverDescription.AlgorithmParameterDescription.PDEMeshSize.getDescription();
			ISize meshSize = simTaskDesc.getSimulation().getMeshSpecification().getSamplingSize();
			AlgorithmParameter sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, meshSize.toTemporaryKISAOvalue());
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);
		}
		TimeStep ts = simTaskDesc.getTimeStep();
		if(enableDefaultTimeStep) {
			String kisaoStr = TimeStep.TimeStepDescription.Default.getKisao();
			String kisaoDesc = TimeStep.TimeStepDescription.Default.getDescription();
			AlgorithmParameter sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, ts.getDefaultTimeStep()+"");
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);
		}
		if(enableMinTimeStep) {
			String kisaoStr = TimeStep.TimeStepDescription.Minimum.getKisao();
			String kisaoDesc = TimeStep.TimeStepDescription.Minimum.getDescription();
			AlgorithmParameter sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, ts.getMinimumTimeStep()+"");
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);
		}
		if(enableMaxTimeStep) {
			String kisaoStr = TimeStep.TimeStepDescription.Maximum.getKisao();
			String kisaoDesc = TimeStep.TimeStepDescription.Maximum.getDescription();
			AlgorithmParameter sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, ts.getMaximumTimeStep()+"");
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);
		}

		if(simTaskDesc.getSimulation().getMathDescription().isNonSpatialStoch()) {	// ------- deal with seed
			NonspatialStochSimOptions nssso = simTaskDesc.getStochOpt();
			if(nssso.isUseCustomSeed()) {
				String kisaoStr = SolverDescription.AlgorithmParameterDescription.Seed.getKisao();	// 488
				String kisaoDesc = SolverDescription.AlgorithmParameterDescription.Seed.getDescription();
				AlgorithmParameter sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, nssso.getCustomSeed()+"");
				sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
				addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);
			}
		} else {
			;	// (... isRuleBased(), isSpatial(), isMovingMembrane(), isSpatialHybrid() ...
		}

		if(vcSolverDesc == SolverDescription.HybridEuler || 						// -------- deal with hybrid solvers (non-spatial)
				vcSolverDesc == SolverDescription.HybridMilAdaptive ||
				vcSolverDesc == SolverDescription.HybridMilstein) {
			NonspatialStochHybridOptions nssho = simTaskDesc.getStochHybridOpt();

			String kisaoStr = SolverDescription.AlgorithmParameterDescription.Epsilon.getKisao();
			String kisaoDesc = SolverDescription.AlgorithmParameterDescription.Epsilon.getDescription();
			AlgorithmParameter sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, nssho.getEpsilon()+"");
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);

			kisaoStr = SolverDescription.AlgorithmParameterDescription.Lambda.getKisao();
			kisaoDesc = SolverDescription.AlgorithmParameterDescription.Lambda.getDescription();
			sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, nssho.getLambda()+"");
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);

			kisaoStr = SolverDescription.AlgorithmParameterDescription.MSRTolerance.getKisao();
			kisaoDesc = SolverDescription.AlgorithmParameterDescription.MSRTolerance.getDescription();
			sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, nssho.getMSRTolerance()+"");
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);
		}
		if(vcSolverDesc == SolverDescription.HybridMilAdaptive) {					// --------- one more param for hybrid-adaptive
			NonspatialStochHybridOptions nssho = simTaskDesc.getStochHybridOpt();

			String kisaoStr = SolverDescription.AlgorithmParameterDescription.SDETolerance.getKisao();
			String kisaoDesc = SolverDescription.AlgorithmParameterDescription.SDETolerance.getDescription();
			AlgorithmParameter sedmlAlgorithmParameter = new AlgorithmParameter(kisaoStr, nssho.getSDETolerance()+"");
			sedmlAlgorithm.addAlgorithmParameter(sedmlAlgorithmParameter);
			addNotesChild(an, TokenMangler.mangleToSName(kisaoStr), kisaoDesc);
		}

		// add a note to utcSim to indicate actual solver name
		String simNotesStr = "Actual Solver Name : '" + vcSolverDesc.getDisplayLabel() + "'.";  
		utcSim.setNote(createNotesElement(simNotesStr));
		sedmlModel.addSimulation(utcSim);
		return utcSim;
	}
	
	private void createSEDMLtasks(int simContextCnt, Map<Pair<String, String>, String> l2gMap, String simContextName,
			String simContextId, Simulation vcSimulation, UniformTimeCourse utcSim,
			Set<String> dataGeneratorTasksSet, MathOverrides mathOverrides, String languageURN)
			throws ExpressionBindingException, ExpressionException, DivideByZeroException, MappingException {
		if(mathOverrides != null && mathOverrides.hasOverrides()) {
			String[] overridenConstantNames = mathOverrides.getOverridenConstantNames();
			String[] scannedConstantsNames = mathOverrides.getScannedConstantNames();
			HashMap<String, String> scannedParamHash = new HashMap<String, String>();
			HashMap<String, String> unscannedParamHash = new HashMap<String, String>();
			
			VariableSymbolTable varST = new VariableSymbolTable();
			String[] constantNames = mathOverrides.getAllConstantNames();
			final HashMap<String, Expression> substitutedConstants = new HashMap<>();
			{
				final ArrayList<Constant> overrides = new ArrayList<>();
				for (String constantName : constantNames) {
					overrides.add(new Constant(constantName, new Expression(mathOverrides.getActualExpression(constantName, 0))));
				}
				for (Constant override : overrides) {
					 varST.addVar(override);
				}
				for (Constant override : overrides) {
					 override.bind(varST);
				}
				for (Constant override : overrides) {
					 Expression flattened = MathUtilities.substituteFunctions(override.getExpression(), varST, true);
					 substitutedConstants.put(override.getName(), new Expression(flattened));
				}
			}
			
			// need to check for "leftover" overrides from parameter renaming or other model editing
			HashMap<String, String> missingParamHash = new HashMap<String, String>();
			for (String name : scannedConstantsNames) {
				if (!mathOverrides.isUnusedParameter(name)) {
					scannedParamHash.put(name, name);
				} else {
					missingParamHash.put(name, name);
				}
			}
			for (String name : overridenConstantNames) {
				if (!scannedParamHash.containsKey(name)) {
					if (!mathOverrides.isUnusedParameter(name)) {
						unscannedParamHash.put(name, name);
					} else {
						missingParamHash.put(name, name);
					}
				}
			}
			if (!missingParamHash.isEmpty()) {
				for (String missingParamName : missingParamHash.values()) {
					logger.error("ERROR: there is an override entry for non-existent parameter "+missingParamName);
					throw new MappingException("MathOverrides has entry for non-existent parameter "+missingParamName);
				}
			}
			
			SimulationContext simContext = (SimulationContext)vcSimulation.getSimulationOwner();
			MathSymbolMapping mathSymbolMapping = (MathSymbolMapping) simContext.getMathDescription().getSourceSymbolMapping();
			if (!unscannedParamHash.isEmpty() && scannedParamHash.isEmpty()) {
				// only parameters with simple overrides (numeric/expression) no scans
				// create new model with change for each parameter that has override; add simple task
				String overriddenSimContextId = simContextId + "_" + overrideCount;
				String overriddenSimContextName = simContextName + " modified";
				Model sedModel = new Model(overriddenSimContextId, overriddenSimContextName, languageURN, "#"+simContextId);
				overrideCount++;

				int variableCount = 0;
				for (String unscannedParamName : unscannedParamHash.values()) {
					SymbolTableEntry ste = getSymbolTableEntryForModelEntity(mathSymbolMapping, unscannedParamName);
					Expression unscannedParamExpr = mathOverrides.getActualExpression(unscannedParamName, 0);
					unscannedParamExpr = adjustIfRateParam(vcSimulation, ste, unscannedParamExpr);
					if(unscannedParamExpr.isNumeric()) {
						// if expression is numeric, add ChangeAttribute to model created above
						XPathTarget targetXpath = getTargetAttributeXPath(ste, l2gMap, simContext);
						ChangeAttribute changeAttribute = new ChangeAttribute(targetXpath, unscannedParamExpr.infix());
						sedModel.addChange(changeAttribute);
					} else {
						
						Map<String, XPathTarget> symbolToTargetMap = new LinkedHashMap<>();
						// create setValue for unscannedParamName (which contains a scanned param in its expression)
						String[] symbols = unscannedParamExpr.getSymbols();
						for(String symbol : symbols) {
							SymbolTableEntry entry = getSymbolTableEntryForModelEntity(mathSymbolMapping, symbol);
							XPathTarget target = getTargetAttributeXPath(entry, l2gMap, simContext);
							symbolToTargetMap.put(symbol, target);
						}

						// non-numeric expression : add 'computeChange' to modified model
						XPathTarget targetXpath = getTargetAttributeXPath(ste, l2gMap, simContext);
						ComputeChange computeChange = new ComputeChange(targetXpath);
						
						Expression expr = new Expression(unscannedParamExpr);
						String[] exprSymbols = expr.getSymbols();
						for (String symbol : exprSymbols) {
							String varName = overriddenSimContextId + "_" + symbol + "_" + variableCount;
							Variable sedmlVar = new Variable(varName, varName, overriddenSimContextId, symbolToTargetMap.get(symbol).toString());
							expr.substituteInPlace(new Expression(symbol), new Expression(varName));
							computeChange.addVariable(sedmlVar);
							variableCount++;
						}
						ASTNode math = Libsedml.parseFormulaString(expr.infix());
						computeChange.setMath(math);
						sedModel.addChange(computeChange);
					}
				}
				sedmlModel.addModel(sedModel);

				String taskId = "tsk_" + simContextCnt + "_" + simCount;
				Task sedmlTask = new Task(taskId, vcSimulation.getName(), sedModel.getId(), utcSim.getId());
				dataGeneratorTasksSet.add(sedmlTask.getId());
				sedmlModel.addTask(sedmlTask);
				
			} else if (!scannedParamHash.isEmpty() && unscannedParamHash.isEmpty()) {
				// only parameters with scans
				String taskId = "tsk_" + simContextCnt + "_" + simCount;
				String ownerTaskId = taskId;
				Task sedmlTask = new Task(taskId, vcSimulation.getName(), simContextId, utcSim.getId());
				dataGeneratorTasksSet.add(sedmlTask.getId());
				sedmlModel.addTask(sedmlTask);

				int repeatedTaskIndex = 0;
				for (String scannedConstName : scannedConstantsNames) {
					String repeatedTaskId = "repTsk_" + simContextCnt + "_" + simCount + "_" + repeatedTaskIndex;
					String rangeId = "range_" + simContextCnt + "_" + simCount + "_" + scannedConstName;
					RepeatedTask rt = createSEDMLreptask(rangeId, l2gMap, simContext,
							dataGeneratorTasksSet, mathOverrides, ownerTaskId, scannedConstName, repeatedTaskId, simContextId);
					ownerTaskId = repeatedTaskId;
					repeatedTaskIndex++;
					sedmlModel.addTask(rt);
				}

			} else {
				// both scanned and simple parameters : create new model with change for each simple override; add RepeatedTask
				Map<String, RepeatedTask> rangeToRepeatedTaskHash = new LinkedHashMap<> ();
				List<RepeatedTask> repeatedTasksList = new ArrayList<> ();
				
				// create new model with change for each unscanned parameter that has override
				String overriddenSimContextId = simContextId + "_" + overrideCount;
				String overriddenSimContextName = simContextName + " modified";
				Model sedModel = new Model(overriddenSimContextId, overriddenSimContextName, languageURN, "#"+simContextId);
				overrideCount++;

				String taskId = "tsk_" + simContextCnt + "_" + simCount;
				String ownerTaskId = taskId;
				Task sedmlTask = new Task(taskId, vcSimulation.getName(), overriddenSimContextId, utcSim.getId());
				dataGeneratorTasksSet.add(sedmlTask.getId());
				sedmlModel.addTask(sedmlTask);

				// scanned parameters
				int repeatedTaskIndex = 0;
				int variableCount = 0;
				for (String scannedConstName : scannedConstantsNames) {
					String repeatedTaskId = "repTsk_" + simContextCnt + "_" + simCount + "_" + repeatedTaskIndex;
					String rangeId = "range_" + simContextCnt + "_" + simCount + "_" + scannedConstName;
					RepeatedTask rt = createSEDMLreptask(rangeId, l2gMap, simContext,
							dataGeneratorTasksSet, mathOverrides, ownerTaskId, scannedConstName, repeatedTaskId, overriddenSimContextId);
					ownerTaskId = repeatedTaskId;
					repeatedTaskIndex++;

					// use scannedParamHash to store rangeId for that param, since it might be needed if unscanned param has a scanned param in expr.
					if (scannedParamHash.get(scannedConstName).equals(scannedConstName)) {
						// the hash was originally populated as <scannedParamName, scannedParamName>. Replace 'value' with rangeId for scannedParam
						scannedParamHash.put(scannedConstName, rangeId);
						rangeToRepeatedTaskHash.put(rangeId, rt);		// we'll need the right repeated task for this range later on, in the unscanned loop
					}
					// add to local list; will be added to sedml doc later
					repeatedTasksList.add(rt);
				}

				// for unscanned parameter overrides
				for (String unscannedParamName : unscannedParamHash.values()) {
					SymbolTableEntry ste = getSymbolTableEntryForModelEntity(mathSymbolMapping, unscannedParamName);
					Expression unscannedParamExpr = mathOverrides.getActualExpression(unscannedParamName, 0);
					unscannedParamExpr = adjustIfRateParam(vcSimulation, ste, unscannedParamExpr);
					if(unscannedParamExpr.isNumeric()) {
						// if expression is numeric, add ChangeAttribute to model created above
						XPathTarget targetXpath = getTargetAttributeXPath(ste, l2gMap, simContext);
						ChangeAttribute changeAttribute = new ChangeAttribute(targetXpath, unscannedParamExpr.infix());
						sedModel.addChange(changeAttribute);
					} else {
						// check for any scanned parameter in unscanned parameter expression
						String[] exprSymbols = unscannedParamExpr.getSymbols();
						boolean bHasScannedParameter = false;
						List<String> scannedParamNameInUnscannedParamExpList = new ArrayList<> ();
						for (String symbol : exprSymbols) {
							if (scannedParamHash.get(symbol) != null) {
								bHasScannedParameter = true;
								scannedParamNameInUnscannedParamExpList.add(new String(symbol));
							}
						}
						// (scanned parameter in expr) ? (add setValue for unscanned param in repeatedTask) : (add computeChange to modifiedModel)
						Map<String, XPathTarget> symbolToTargetMap = new LinkedHashMap<>();
						String[] symbols = unscannedParamExpr.getSymbols();
						for(String symbol : symbols) {
							SymbolTableEntry entry = getSymbolTableEntryForModelEntity(mathSymbolMapping, symbol);
							XPathTarget target = getTargetAttributeXPath(entry, l2gMap, simContext);
							symbolToTargetMap.put(symbol, target);
						}
						if (bHasScannedParameter) {
							// create setValue for unscannedParamName (which contains a scanned param in its expression)
							SymbolTableEntry entry = getSymbolTableEntryForModelEntity(mathSymbolMapping, unscannedParamName);
							XPathTarget target = getTargetAttributeXPath(entry, l2gMap, simContext);
							Set<String> rangeIdSet = new HashSet<>();
							for(String scannedParamNameInUnscannedParamExp : scannedParamNameInUnscannedParamExpList) {
								String rangeId = scannedParamHash.get(scannedParamNameInUnscannedParamExp);
								rangeIdSet.add(rangeId);	// all the ranges referred in the scannedParamNameInUnscannedParamExpList
							}
							for(String rangeId : rangeIdSet) {
								SetValue setValue = new SetValue(target, rangeId, overriddenSimContextId);	// @TODO: we have no range??
								Expression expr = new Expression(unscannedParamExpr);
								for(String symbol : symbols) {
									String symbolName = rangeId + "_" + symbol + "_" + variableCount;
									Variable sedmlVar = new Variable(symbolName, symbolName, overriddenSimContextId, symbolToTargetMap.get(symbol).toString());	// sbmlSupport.getXPathForSpecies(symbol));
									setValue.addVariable(sedmlVar);
									expr.substituteInPlace(new Expression(symbol), new Expression(symbolName));
									variableCount++;
								}
								ASTNode math = Libsedml.parseFormulaString(expr.infix());
								setValue.setMath(math);
								RepeatedTask rtRecovered = rangeToRepeatedTaskHash.get(rangeId);
								rtRecovered.addChange(setValue);
							}
						} else {
							// non-numeric expression : add 'computeChange' to modified model
							XPathTarget targetXpath = getTargetAttributeXPath(ste, l2gMap, simContext);
							ComputeChange computeChange = new ComputeChange(targetXpath);
							Expression expr = new Expression(unscannedParamExpr);
							for (String symbol : exprSymbols) {
								String varName = overriddenSimContextId + "_" + symbol + "_" + variableCount;
								Variable sedmlVar = new Variable(varName, varName, overriddenSimContextId, symbolToTargetMap.get(symbol).toString());
								expr.substituteInPlace(new Expression(symbol), new Expression(varName));
								computeChange.addVariable(sedmlVar);
								variableCount++;
							}
							ASTNode math = Libsedml.parseFormulaString(expr.infix());
							computeChange.setMath(math);
							sedModel.addChange(computeChange);
						}
					}
				}
				sedmlModel.addModel(sedModel);
				for(RepeatedTask rt : repeatedTasksList) {
					sedmlModel.addTask(rt);
				}
			}
		} else {						// no math overrides, add basic task.
			String taskId = "tsk_" + simContextCnt + "_" + simCount;
			Task sedmlTask = new Task(taskId, vcSimulation.getName(), simContextId, utcSim.getId());
			dataGeneratorTasksSet.add(sedmlTask.getId());
			sedmlModel.addTask(sedmlTask);
//						taskRef = taskId;		// to be used later to add dataGenerators : one set of DGs per model (simContext).
		}
	}

	private Expression adjustIfRateParam(Simulation vcSimulation, SymbolTableEntry ste, Expression unscannedParamExpr)
			throws ExpressionException {
		if (ste instanceof KineticsParameter) {
			KineticsParameter kp = (KineticsParameter)ste;
			if (kp.getKinetics().getAuthoritativeParameter() == kp) {
				SimulationContext simulationContext = (SimulationContext) vcSimulation.getSimulationOwner();
				boolean bSpatial = simulationContext.getGeometry().getDimension() > 0;
				boolean bLumped = kp.getKinetics() instanceof LumpedKinetics;
				if (!bLumped && !bSpatial) {
					MathSymbolMapping msm = (MathSymbolMapping) simulationContext.getMathDescription().getSourceSymbolMapping();
					cbit.vcell.math.Variable structSize = msm.getVariable(simulationContext.getGeometryContext().getStructureMapping(kp.getKinetics().getReactionStep().getStructure()).getSizeParameter());
					unscannedParamExpr = Expression.mult(unscannedParamExpr, new Expression(structSize.getName()));
				}
			}
		}
		return unscannedParamExpr;
	}

	private RepeatedTask createSEDMLreptask(String rangeId, Map<Pair<String, String>, String> l2gMap,
			SimulationContext simContext, Set<String> dataGeneratorTasksSet,
			MathOverrides mathOverrides, String ownerTaskId, String scannedConstName, String repeatedTaskId, String modelReferenceId)
			throws ExpressionException, DivideByZeroException, MappingException {
		RepeatedTask rt = new RepeatedTask(repeatedTaskId, mathOverrides.getSimulation().getName(), true, rangeId);
		dataGeneratorTasksSet.add(rt.getId());
		SubTask subTask = new SubTask("0", ownerTaskId);
		dataGeneratorTasksSet.remove(ownerTaskId);
		rt.addSubtask(subTask);
		ConstantArraySpec constantArraySpec = mathOverrides.getConstantArraySpec(scannedConstName);
		MathSymbolMapping mathSymbolMapping = (MathSymbolMapping)simContext.getMathDescription().getSourceSymbolMapping();
		// list of Ranges, if sim is parameter scan.
		Range r = createSEDMLrange(rangeId, rt, constantArraySpec, scannedConstName, simContext, l2gMap, modelReferenceId, mathOverrides.getSimulation());
		// list of Changes
		SymbolTableEntry ste = getSymbolTableEntryForModelEntity(mathSymbolMapping, scannedConstName);
		XPathTarget target = getTargetAttributeXPath(ste, l2gMap, simContext);
		//ASTNode math1 = new ASTCi(r.getId());		// was scannedConstName
		ASTNode math1 = Libsedml.parseFormulaString(r.getId());		// here the math is always the range id expression
		SetValue setValue = new SetValue(target, r.getId(), modelReferenceId);
		setValue.setMath(math1);
		rt.addChange(setValue);
		return rt;
	}

	private Range createSEDMLrange(String rangeId, RepeatedTask rt, ConstantArraySpec constantArraySpec, String scannedConstantName, SimulationContext simContext, Map<Pair<String, String>, String> l2gMap, String modelReferenceId, Simulation vcSim)
			throws ExpressionException, DivideByZeroException, MappingException {
		Range r = null;
		SimulationContext sc = (SimulationContext)vcSim.getSimulationOwner();
		SymbolReplacement sr = sc.getMathOverridesResolver().getSymbolReplacement(scannedConstantName, true);
		String cName = sr != null ? sr.newName : scannedConstantName;
		MathSymbolMapping msm = (MathSymbolMapping)simContext.getMathDescription().getSourceSymbolMapping();
		SymbolTableEntry ste = msm.getBiologicalSymbol(vcSim.getMathOverrides().getConstant(cName))[0];
		if(constantArraySpec.getType() == ConstantArraySpec.TYPE_INTERVAL) {
			// ------ Uniform Range
			UniformType type = constantArraySpec.isLogInterval() ? UniformType.LOG : UniformType.LINEAR;
			if (constantArraySpec.getMinValue().isNumeric() && constantArraySpec.getMaxValue().isNumeric()) {
				r = new UniformRange(rangeId, constantArraySpec.getMinValue().evaluateConstant(),
						constantArraySpec.getMaxValue().evaluateConstant(), constantArraySpec.getNumValues(), type);
				rt.addRange(r);
				return r;
			} else {
				r = new UniformRange(rangeId, 1, 2, constantArraySpec.getNumValues(), type);
				rt.addRange(r);
				// now make a FunctionalRange with expressions
				FunctionalRange fr = new FunctionalRange("fr_"+rangeId, rangeId);
				Expression expMin = constantArraySpec.getMinValue();
				expMin = adjustIfRateParam(vcSim, ste, expMin);
				Expression expMax = constantArraySpec.getMaxValue();
				expMax = adjustIfRateParam(vcSim, ste, expMax);
				Expression trans = Expression.add(new Expression(rangeId), new Expression("-1"));
				Expression func = Expression.add(expMax, Expression.negate(expMin));
				func = Expression.mult(func, trans);
				func = Expression.add(expMin, func);
				createFunctionalRangeElements(fr, func, simContext, l2gMap, modelReferenceId);
				rt.addRange(fr);
				return fr;
			}
		} else {
			// ----- Vector Range
			// we try to preserve symbolic values coming from unit transforms...
			cbit.vcell.math.Constant[] cs = constantArraySpec.getConstants();
			ArrayList<Double> values = new ArrayList<Double>();
			Expression expFact = null;
			for (int i = 0; i < cs.length; i++){
				if (!(cs[i].getExpression().evaluateConstant() == 0)) {
					expFact = cs[i].getExpression();
					break;
				}
			}
			// compute list of numeric multipliers
			for (int i = 0; i < cs.length; i++){
				Expression exp = cs[i].getExpression();
				exp = Expression.div(exp, expFact).simplifyJSCL();
				values.add(new Double(exp.evaluateConstant()));
			}
			r = new VectorRange(rangeId, values);
			rt.addRange(r);
			// now make a FunctionalRange with expressions
			FunctionalRange fr = new FunctionalRange("fr_"+rangeId, rangeId);
			expFact = Expression.mult(new Expression(rangeId), expFact);
			expFact = adjustIfRateParam(vcSim, ste, expFact);
			createFunctionalRangeElements(fr, expFact, simContext, l2gMap, modelReferenceId);
			rt.addRange(fr);
			return fr;
		}
	}

	private void createFunctionalRangeElements(FunctionalRange fr, Expression func, SimulationContext simContext,
			Map<Pair<String, String>, String> l2gMap, String modelReferenceId) throws ExpressionException, MappingException {
		String[] symbols = func.getSymbols();
		MathSymbolMapping msm = (MathSymbolMapping)simContext.getMathDescription().getSourceSymbolMapping();
		for(String symbol : symbols) {
			if (symbol.equals(fr.getRange())) continue;
			SymbolTableEntry entry = getSymbolTableEntryForModelEntity(msm, symbol);
			XPathTarget target = getTargetAttributeXPath(entry, l2gMap, simContext);
			String symbolName = fr.getRange() + "_" + symbol;
			Variable sedmlVar = new Variable(symbolName, symbolName, modelReferenceId, target.toString());	// sbmlSupport.getXPathForSpecies(symbol));
			fr.addVariable(sedmlVar);
			func.substituteInPlace(new Expression(symbol), new Expression(symbolName));
		}
		ASTNode math = Libsedml.parseFormulaString(func.infix());
		fr.setMath(math);
	}

	private void writeModelSBML(String savePath, String sBaseFileName, String sbmlString, SimulationContext simContext) throws IOException {
		String simContextName = simContext.getName();
		String simContextId = TokenMangler.mangleToSName(simContextName);
		String filePathStrAbsolute = Paths.get(savePath, sBaseFileName + "_" + simContextId + ".xml").toString();
		String filePathStrRelative = sBaseFileName + "_" +  simContextId + ".xml";
		XmlUtil.writeXMLStringToFile(sbmlString, filePathStrAbsolute, true);
		modelFilePathStrAbsoluteList.add(filePathStrRelative);
		sedmlModel.addModel(new Model(simContextId, simContextName, sbmlLanguageURN, filePathStrRelative));
	}

	private Notes createNotesElement(String notesStr) {
		// create some xhtml. E.g.,
		org.jdom.Element para = new org.jdom.Element("p");
		para.setText(notesStr);
		// create a notes element
		Notes note = new Notes(para);
		return note;
	}
	private void addNotesChild(Notes note, String kisao, String desc) {
		Element sub = new Element("AlgoritmParameter", "http://www.biomodels.net/kisao/KISAO_FULL#");
		sub.setAttribute(TokenMangler.mangleToSName(kisao), desc);
		note.getNotesElement().addContent(sub);
	}

	public static SymbolTableEntry getSymbolTableEntryForModelEntity(MathSymbolMapping mathSymbolMapping, String paramName) throws MappingException {
		cbit.vcell.math.Variable mathVar = mathSymbolMapping.findVariableByName(paramName);
		if (mathVar == null) {
			throw new MappingException("No variable found for parameter: " + paramName);
		}
		SymbolTableEntry[] stEntries = mathSymbolMapping.getBiologicalSymbol(mathVar);
		if (stEntries == null) {
			throw new MappingException("No matching biological symbol for variable: " + mathVar);
		}
		
		// if the extra stes in the array are KineticsProxyParameters/ModelQuantities, remove them from array. Should be left with only one entry for overriddenConstantName
		if (stEntries.length > 1) {
			//
			// If there are more than one stEntries, usually, it is a regular ste (species, global parameter, structure, etc) together with 
			// kineticsProxyParameters (that have the regular ste as target) or Model quantities (structure size, membrane voltage). 
			// So filtering out the kinticProxyParametes should leave only the regular parameter, 
			// which is what we want. If there are more, then there is a problem.
			//
			ArrayList<SymbolTableEntry> steList = new ArrayList<SymbolTableEntry>(Arrays.asList(stEntries));
			for (int i = 0; i < stEntries.length; i++) {
				if (stEntries[i] instanceof ProxyParameter) {
					SymbolTableEntry ppTargetSte = ((ProxyParameter)stEntries[i]).getTarget();
					if (steList.contains(ppTargetSte) || ppTargetSte instanceof ModelQuantity) {
						steList.remove(stEntries[i]);
					}
				}
				if (stEntries[i] instanceof ModelQuantity) {
					if (steList.contains(stEntries[i])) {
						steList.remove(stEntries[i]);
					}
				}
			}
			// after removing proxy parameters, cannot have more than one ste in list
			if (steList.size() == 0) {
				throw new MappingException("No mapping entry for constant : '" + paramName + "'.");
			}
			if (steList.size() > 1) {
				throw new MappingException("Cannot have more than one mapping entry for constant : '" + paramName + "'.");
			}
			SymbolTableEntry[] stes = (SymbolTableEntry[])steList.toArray(new SymbolTableEntry[0]);
			return stes[0];
		} else {
			return stEntries[0];
		}
	}

	private XPathTarget getTargetAttributeXPath(SymbolTableEntry ste, Map<Pair <String, String>, String> l2gMap, SimulationContext simContext) {
		// VCML model format
		if (l2gMap == null) return getVCMLTargetXPath(ste, simContext);
		// SBML model format
		SBMLSupport sbmlSupport = new SBMLSupport();		// to get Xpath string for variables.
		XPathTarget targetXpath = null;
		if (ste instanceof SpeciesContext || ste instanceof SpeciesContextSpecParameter) {
			String speciesId = TokenMangler.mangleToSName(ste.getName());
			// can change species initial concentration or amount 
			String speciesAttr = "";
			if (ste instanceof SpeciesContextSpecParameter) {
				SpeciesContextSpecParameter scsp = (SpeciesContextSpecParameter)ste;
				speciesId = TokenMangler.mangleToSName((scsp).getSpeciesContext().getName());
				int role = scsp.getRole();
				if (role == SpeciesContextSpec.ROLE_InitialConcentration) {
					speciesAttr = scsp.getName(); 
				}
				if (role == SpeciesContextSpec.ROLE_InitialCount) {
					speciesAttr = scsp.getName(); 
				}
				if(role == SpeciesContextSpec.ROLE_DiffusionRate) {
					speciesAttr = scsp.getName(); 
				}
			}
			if (speciesAttr.length() < 1) {
				targetXpath = new XPathTarget(sbmlSupport.getXPathForCompartment(speciesId));
			} else if (speciesAttr.equalsIgnoreCase("initialConcentration") || speciesAttr.equalsIgnoreCase("initConc")) {
				targetXpath = new XPathTarget(sbmlSupport.getXPathForSpecies(speciesId, SpeciesAttribute.initialConcentration));
			} else if (speciesAttr.equalsIgnoreCase("initialCount") || speciesAttr.equalsIgnoreCase("initCount")) {
				targetXpath = new XPathTarget(sbmlSupport.getXPathForSpecies(speciesId, SpeciesAttribute.initialAmount));
			} else if (speciesAttr.equalsIgnoreCase("diff")) {
				targetXpath = new XPathTarget(sbmlSupport.getXPathForGlobalParameter(speciesId + "_" + speciesAttr, ParameterAttribute.value));
			} else {
				throw new RuntimeException("Unknown species attribute '" + speciesAttr + "'; cannot get xpath target for species '" + speciesId + "'.");
			}

		} else if (ste instanceof ModelParameter || ste instanceof ReservedSymbol) {
			// can only change parameter value. 
			targetXpath = new XPathTarget(sbmlSupport.getXPathForGlobalParameter(ste.getName(), ParameterAttribute.value));
			// use Ion's sample 3, with spatial app
		}  else if (ste instanceof Structure || ste instanceof Structure.StructureSize || ste instanceof StructureMappingParameter) {
			String compartmentId = TokenMangler.mangleToSName(ste.getName());
			// can change compartment size or spatial dimension, but in vcell, we cannot change compartment dimension. 
			String compartmentAttr = "";
			String mappingId = "";
			if (ste instanceof Structure.StructureSize) { 
				compartmentId = TokenMangler.mangleToSName(((StructureSize)ste).getStructure().getName());
				compartmentAttr = ((StructureSize)ste).getName();
			}
			if (ste instanceof StructureMappingParameter) {
				StructureMappingParameter smp = (StructureMappingParameter)ste;
				compartmentId = TokenMangler.mangleToSName(smp.getStructure().getName());
				int role = ((StructureMappingParameter)ste).getRole();
				if (role == StructureMapping.ROLE_Size) {
					compartmentAttr = smp.getName();
				} else if(role == StructureMapping.ROLE_AreaPerUnitArea || role == StructureMapping.ROLE_VolumePerUnitVolume) {
					compartmentAttr = smp.getName();
					Structure structure = smp.getStructure();
					GeometryClass gc = smp.getSimulationContext().getGeometryContext().getStructureMapping(structure).getGeometryClass();
					mappingId = TokenMangler.mangleToSName(gc.getName() + structure.getName());
				}
			}
			if (compartmentAttr.length() < 1) {
				targetXpath = new XPathTarget(sbmlSupport.getXPathForCompartment(compartmentId));
			} else if (compartmentAttr.equalsIgnoreCase("size")) {
				targetXpath = new XPathTarget(sbmlSupport.getXPathForCompartment(compartmentId, CompartmentAttribute.size));
			} else if(compartmentAttr.equalsIgnoreCase("AreaPerUnitArea") || compartmentAttr.equalsIgnoreCase("VolPerUnitVol")) {
				targetXpath = new XPathTarget(sbmlSupport.getXPathForCompartmentMapping(compartmentId, mappingId, CompartmentAttribute.unitSize));
			} else {
				throw new RuntimeException("Unknown compartment attribute '" + compartmentAttr + "'; cannot get xpath target for compartment '" + compartmentId + "'.");
			}
		} else if (ste instanceof KineticsParameter) {
			KineticsParameter kp = (KineticsParameter)ste;
			String reactionID = kp.getKinetics().getReactionStep().getName();
			String parameterID = kp.getName();
			Pair<String, String> key = new Pair(reactionID, parameterID);
			String value = l2gMap.get(key);
			if(value == null) {
				// stays as local parameter
				targetXpath = new XPathTarget(sbmlSupport.getXPathForKineticLawParameter(reactionID, parameterID, ParameterAttribute.value));
			} else {
				// became a global in SBML, we need to refer to that global
				targetXpath = new XPathTarget(sbmlSupport.getXPathForGlobalParameter(value, ParameterAttribute.value));
			}
		} else if (ste instanceof Membrane.MembraneVoltage) {
			// they are exported as globals
			targetXpath = new XPathTarget(sbmlSupport.getXPathForGlobalParameter(TokenMangler.mangleToSName(ste.getName()), ParameterAttribute.value));
		} else {
			logger.error("redundant error log: "+"Entity should be SpeciesContext, Structure, ModelParameter, ReserverdSymbol, KineticsParameter, or MembraneVoltage : " + ste.getClass());
			throw new RuntimeException("Unsupported entity in SBML model export: "+ste.getClass());
		}
		return targetXpath;
	}
	

	private XPathTarget getVCMLTargetXPath(SymbolTableEntry ste, SimulationContext simContext) {
		XPathTarget targetXpath = null;
		if (ste instanceof SpeciesContextSpecParameter) {
			String paramXpath = "";
			SpeciesContextSpecParameter scsp = (SpeciesContextSpecParameter)ste;
			String baseXpath = VCMLSupport.getXPathForSpeciesContextSpec(simContext.getName(), scsp.getSpeciesContextSpec().getSpeciesContext().getName());
			int role = scsp.getRole();
			if (role == SpeciesContextSpec.ROLE_InitialConcentration) {
				paramXpath = "/vcml:InitialConcentration"; 
			}
			if (role == SpeciesContextSpec.ROLE_InitialCount) {
				paramXpath = "/vcml:InitialCount"; 
			}
			if(role == SpeciesContextSpec.ROLE_DiffusionRate) {
				paramXpath = "/vcml:Diffusion"; 
			}
			targetXpath = new XPathTarget(baseXpath+paramXpath);
		} else if (ste instanceof ModelParameter) {
			// can only change parameter value. 
			targetXpath = new XPathTarget(VCMLSupport.getXPathForModelParameter(ste.getName()));
		}  else if (ste instanceof Structure || ste instanceof Structure.StructureSize || ste instanceof StructureMappingParameter) {
			// can change compartment size or spatial dimension, but in vcell, we cannot change compartment dimension. 
			String attributeName = "Size";
			Structure struct = null;
			if (ste instanceof Structure) {
				struct = (Structure) ste;
			}
			if (ste instanceof Structure.StructureSize) { 
				struct = ((StructureSize)ste).getStructure();
			}
			if (ste instanceof StructureMappingParameter) {
				StructureMappingParameter smp = (StructureMappingParameter)ste;
				struct = smp.getStructure();
				int role = smp.getRole();
				if(role == StructureMapping.ROLE_AreaPerUnitArea) {
					attributeName = "AreaPerUnitArea";
				} else if (role == StructureMapping.ROLE_VolumePerUnitVolume) {
					attributeName = "VolumePerUnitVolume";
				}
			}
			if (struct instanceof Feature) {
				targetXpath = new XPathTarget(VCMLSupport.getXPathForFeatureMappingAttribute(simContext.getName(),
						struct.getName(), attributeName));
			} else {
				targetXpath = new XPathTarget(VCMLSupport.getXPathForMembraneMappingAttribute(simContext.getName(),
						struct.getName(), attributeName));
			}
		} else if (ste instanceof KineticsParameter) {
			KineticsParameter kp = (KineticsParameter) ste;
			targetXpath = new XPathTarget(VCMLSupport.getXPathForKineticLawParameter(kp.getKinetics().getReactionStep().getName(), kp.getName()));
		} else if (ste instanceof Membrane.MembraneVoltage) {
			targetXpath = new XPathTarget(VCMLSupport.getXPathForMembraneMappingAttribute(simContext.getName(),
					((Membrane.MembraneVoltage)ste).getMembrane().getName(), "InitialVoltage"));
		} else {
			logger.error("redundant error log: "+"Entity should be SpeciesContext, Structure, ModelParameter, KineticsParameter, or MembraneVoltage : " + ste.getClass());
			throw new RuntimeException("Unsupported entity in VCML model export: "+ste.getClass());
		}
		return targetXpath;
	}

	public void addSedmlFileToList(String sedmlFileName) {
		if(sedmlFileName != null && !sedmlFileName.isEmpty()) {
			sedmlFilePathStrAbsoluteList.add(sedmlFileName);
		}
	}
	

    public boolean createOmexArchive(String srcFolder, String sFileName) {
    try {
		//System.loadLibrary("combinej");
		CombineArchive archive = new CombineArchive();

    	
		for (String sd : sedmlFilePathStrAbsoluteList) {
			String s = Paths.get(srcFolder, sd).toString();
			archive.addFile(
					s,
					"./" + sd, // target file name
					KnownFormats.lookupFormat("sedml"),
					true // mark file as master
			);
    	}
		for (String sd : modelFilePathStrAbsoluteList) {
			archive.addFile(
					Paths.get(srcFolder, sd).toString(),
					"./" + sd, // target file name
					KnownFormats.lookupFormat("sbml"),
					false // mark file as master
			);
    	}

		archive.addFile(
				Paths.get(srcFolder, sFileName + ".vcml").toString(),
				"./" + sFileName + ".vcml",
				"http://purl.org/NET/mediatypes/application/vcml+xml",
				false
		);
		
        String[] files;
        File dir = new File(srcFolder);
        files = dir.list();
        for(String sd : files) {
        	if (sd.endsWith(".rdf")) {
        		archive.addFile(	
        				Paths.get(srcFolder, sd).toString(),
        				"./" + sd,
        				"http://identifiers.org/combine.specifications/omex-metadata",
        				false
        		);
            } else if(sd.endsWith(".png")) {
                archive.addFile(
                        Paths.get(srcFolder, sd).toString(),
                        "./" + sd,
                        "http://purl.org/NET/mediatypes/image/png",
                        false
                );
        	}
        }

		archive.writeToFile(Paths.get(srcFolder, sFileName + ".omex").toString());
        removeOtherFiles(srcFolder, files);

    } catch (Exception e) {
    	throw new RuntimeException("createZipArchive threw exception: " + e.getMessage());        
    }
    return true;
}
    
	private static void removeOtherFiles(String outputDir, String[] files) {
    	boolean isDeleted = false;
        for (String sd : files) {
            if (sd.endsWith(".sedml") || sd.endsWith(".sbml") || sd.endsWith("xml") || sd.endsWith("vcml") || sd.endsWith("rdf") || sd.endsWith("png")) {
                isDeleted = Paths.get(outputDir, sd).toFile().delete();
                if (!isDeleted) {
                	throw new RuntimeException("Unable to remove intermediate file '" + sd + "'.");        
               }
            }
        }
    }

	public static List<SEDMLTaskRecord> writeBioModel(BioModel bioModel,
													  File exportFileOrDirectory,
													  ModelFormat modelFormat,
													  boolean bFromCLI,
													  boolean bRoundTripSBMLValidation,
													  boolean bCreateOmexArchive
	) throws Exception {

		String resultString;
		// export the entire biomodel to a SEDML file (all supported applications)
		int sedmlLevel = 1;
		int sedmlVersion = 2;
		String sPath = FileUtils.getFullPathNoEndSeparator(exportFileOrDirectory.getAbsolutePath());
		String sFile = FileUtils.getBaseName(exportFileOrDirectory.getAbsolutePath());

		Predicate<Simulation> simulationExportFilter = s -> true;
		List<Simulation> simsToExport = Arrays.stream(bioModel.getSimulations()).filter(simulationExportFilter).collect(Collectors.toList());

		// we replace the obsolete solver with the fully supported equivalent
		for (Simulation simulation : simsToExport) {
			if (simulation.getSolverTaskDescription().getSolverDescription().equals(SolverDescription.FiniteVolume)) {
				try {
					simulation.getSolverTaskDescription().setSolverDescription(SolverDescription.SundialsPDE);
				} catch (PropertyVetoException e) {
					logger.error("Failed to replace obsolete solver", e);
				}
			}
		}

		SEDMLExporter sedmlExporter;
		if (bioModel != null) {
			sedmlExporter = new SEDMLExporter(sFile, bioModel, sedmlLevel, sedmlVersion, simsToExport);
			resultString = sedmlExporter.getSEDMLDocument(sPath, sFile, modelFormat, bFromCLI, bRoundTripSBMLValidation).writeDocumentToString();

			// convert biomodel to vcml and save to file.
			String vcmlString = XmlHelper.bioModelToXML(bioModel);
			String vcmlFileName = Paths.get(sPath, sFile + ".vcml").toString();
			File vcmlFile = new File(vcmlFileName);
			XmlUtil.writeXMLStringToFile(vcmlString, vcmlFile.getAbsolutePath(), true);
		} else {
			throw new RuntimeException("unsupported Document Type " + Objects.requireNonNull(bioModel).getClass().getName() + " for SedML export");
		}
		if (bCreateOmexArchive) {
			String sedmlFileName = Paths.get(sPath, sFile + ".sedml").toString();
			XmlUtil.writeXMLStringToFile(resultString, sedmlFileName, true);
			sedmlExporter.addSedmlFileToList(sFile + ".sedml");

			String diagramName = XmlRdfUtil.diagramBaseName + XmlRdfUtil.diagramExtension;
			String destinationPath = Paths.get(sPath, diagramName).toString();
			File diagramFile = new File(destinationPath);
			Path diagramPath = Paths.get(sPath, diagramName);
			XmlRdfUtil.writeModelDiagram(bioModel, diagramPath.toFile());
			
			BioModelInfo bioModelInfo = null;
			String rdfString = XmlRdfUtil.getMetadata(sFile, bioModel, diagramFile, bioModelInfo);
			XmlUtil.writeXMLStringToFile(rdfString, String.valueOf(Paths.get(sPath, "metadata.rdf")), true);

			sedmlExporter.createOmexArchive(sPath, sFile);
		} else {
			XmlUtil.writeXMLStringToFile(resultString, exportFileOrDirectory.getAbsolutePath(), true);
		}
		return sedmlExporter.sedmlRecorder.getRecords();
	}

	public SEDMLRecorder getSedmlLogger() {
		return sedmlRecorder;
	}

	public static boolean vcmlToOmexConversion(String inputFilePath,
											   BioModelInfo bioModelInfo,
											   String outputBaseDir,
											   String outputDir,
											   Predicate<Simulation> simulationExportFilter,
											   ModelFormat modelFormat,
											   SEDMLEventLog eventLogWriter,
											   boolean bValidate
	) throws IOException {

		int sedmlLevel = 1;
		int sedmlVersion = 2;

		String inputVcmlFile = inputFilePath;

		// get VCML name from VCML path
		String vcmlName = FilenameUtils.getBaseName(inputVcmlFile);		// platform independent, strips extension too
		String jsonFullyQualifiedName = Paths.get(outputBaseDir, "json_reports" ,vcmlName + ".json").toString();

		File vcmlFilePath = new File(inputVcmlFile);
		eventLogWriter.writeEntry(vcmlName);

		// Create biomodel
		BioModel bioModel = null;
		try {
			bioModel = XmlHelper.XMLToBioModel(new XMLSource(vcmlFilePath));
			bioModel.updateAll(false);
			bioModel.refreshDependencies();
			eventLogWriter.writeEntry(vcmlName + ",VCML,SUCCEEDED\n");
		} catch (XmlParseException | MappingException e1) {
			logger.error(vcmlName + " VCML failed to parse and generate math: "+e1.getMessage(), e1);
			eventLogWriter.writeEntry(vcmlName + ",VCML,FAILED"+e1.getMessage() + "\n");
			return false;
		}

		List<Simulation> simsToExport = Arrays.stream(bioModel.getSimulations()).filter(simulationExportFilter).collect(Collectors.toList());

		// we replace the obsolete solver with the fully supported equivalent
		for (Simulation simulation : simsToExport) {
			if (simulation.getSolverTaskDescription().getSolverDescription().equals(SolverDescription.FiniteVolume)) {
				try {
					simulation.getSolverTaskDescription().setSolverDescription(SolverDescription.SundialsPDE);
				} catch (PropertyVetoException e) {
					logger.error("Failed to replace obsolete solver", e);
				}
			}
		}

		Path diagramPath = Paths.get(outputDir, "diagram.png");
		writeModelDiagram(bioModel, diagramPath.toFile());

		SEDMLExporter sedmlExporter = new SEDMLExporter(vcmlName, bioModel, sedmlLevel, sedmlVersion, simsToExport, jsonFullyQualifiedName);

		SEDMLDocument sedmlDocument = sedmlExporter.getSEDMLDocument(outputDir, vcmlName,
				modelFormat, true, bValidate);

		eventLogWriter.writeEntry(sedmlExporter.sedmlRecorder.getRecordsAsCSV());

		if (sedmlExporter.sedmlRecorder.hasErrors()) {
			File dir = new File(outputDir);
			String[] files = dir.list();
			removeOtherFiles(outputDir, files);
			return false;
		} else {
			// write summary of successful export
			boolean hasSpatial = false;
			for (SimulationContext sc : bioModel.getSimulationContexts()) {
				if (sc.getGeometry().getDimension() > 0) {
					hasSpatial = true;
					break;
				}
			}
			int numModels = sedmlDocument.getSedMLModel().getModels().size();
			int numTasks = sedmlDocument.getSedMLModel().getTasks().size();
			String summary = vcmlName+",EXPORTED,hasSpatial="+hasSpatial+",numModels="+numModels+",numTasks="+numTasks+"\n";
			eventLogWriter.writeEntry(summary);
		}

		String sedmlString = sedmlDocument.writeDocumentToString();
		XmlUtil.writeXMLStringToFile(sedmlString, String.valueOf(Paths.get(outputDir, vcmlName + ".sedml")), true);

		String rdfString = getMetadata(vcmlName, bioModel, diagramPath.toFile(), bioModelInfo);
		XmlUtil.writeXMLStringToFile(rdfString, String.valueOf(Paths.get(outputDir, "metadata.rdf")), true);
		try {
			NativeLib.combinej.load();
		} catch (UnsatisfiedLinkError ex) {
			logger.error("Unable to link to native 'libCombine' lib, check native lib: " + ex.getMessage());
			throw ex;
		} catch (Exception ex) {
			String msg = "Error occurred while importing libCombine: " + ex.getMessage();
			logger.error(msg, ex);
			throw new RuntimeException(msg, ex);
		}

		boolean isCreated;
		String failureMessage = null;
		try {
			CombineArchive archive = new CombineArchive();

			String[] files;

			// TODO: try-catch if no files
			File dir = new File(outputDir);
			files = dir.list();

			for (String sd : files) {
				if (sd.endsWith(".sedml")) {
					archive.addFile(
							Paths.get(outputDir, sd).toString(),
							"./" + sd, // target file name
							KnownFormats.lookupFormat("sedml"),
							true // mark file as master
					);
				} else if (sd.endsWith(".sbml") || sd.endsWith(".xml")) {
					archive.addFile(
							Paths.get(outputDir, sd).toString(),
							"./" + sd,
							KnownFormats.lookupFormat("sbml"),
							false
					);
				} else if (sd.endsWith(".rdf")) {
					archive.addFile(
							Paths.get(outputDir, sd).toString(),
							"./" + sd,
							"http://identifiers.org/combine.specifications/omex-metadata",
//                            KnownFormats.lookupFormat("xml"),
							false
					);
				} else if(sd.endsWith(".png")) {
					archive.addFile(
							Paths.get(outputDir, sd).toString(),
							"./" + sd,
							"http://purl.org/NET/mediatypes/image/png",		// was "https://www.iana.org/assignments/media-types/image/png"
							false
					);
				}
			}

			archive.addFile(
					Paths.get(String.valueOf(vcmlFilePath)).toString(),
					"./" + vcmlName + ".vcml",
					"http://purl.org/NET/mediatypes/application/vcml+xml",
					false
			);

			// writing into combine archive
			String omexPath = Paths.get(outputDir, vcmlName + ".omex").toString();
			File omexFile = new File(omexPath);

			// Deleting file if already exists with same name
			if(omexFile.exists()) {
				omexFile.delete();
			}
			isCreated = archive.writeToFile(omexPath);

			if (bValidate){
				logger.warn("skipping VcmlOmexConverter.validation until verify math override round-trip (relying on SBMLExporter.bRoundTripSBMLValidation)");
			}
			// Removing all other files(like SEDML, XML, SBML) after archiving
			removeOtherFiles(outputDir, files);

			if (failureMessage != null){
				throw new RuntimeException(failureMessage);
			}
		} catch (Exception e) {
			throw new RuntimeException("createZipArchive threw exception: " + e.getMessage(), e);
		}
		return isCreated;
	}

	private static void writeModelDiagram(BioModel bioModel, File destination) {
		Integer imageWidthInPixels = 1000;
		try {
			BufferedImage bufferedImage = ITextWriter.generateDocReactionsImage(bioModel.getModel(), imageWidthInPixels);
			FileOutputStream imageOutputStream = new FileOutputStream(destination);
			ImageIO.write(bufferedImage, "png", imageOutputStream);
		} catch (Exception e) {
			logger.error("Failed to generate diagram image for BioModel "+bioModel.getName(), e);
			throw new RuntimeException("Failed to generate diagram image for BioModel "+bioModel.getName(), e);
		}
	}

	private static void removeOtherFiles(String outputDir, String[] files) {
		boolean isDeleted = false;
		for (String sd : files) {
			if (sd.endsWith(".sedml") || sd.endsWith(".sbml") || sd.endsWith("xml") || sd.endsWith("vcml") || sd.endsWith("rdf") || sd.endsWith("png")) {
				isDeleted = Paths.get(outputDir, sd).toFile().delete();
			}
		}
		if (isDeleted) logger.trace("Removed intermediary files in "+outputDir);
	}


	private static String getMetadata(String vcmlName, BioModel bioModel, File diagram, BioModelInfo bioModelInfo) {
		String ret = "";
		String ns = DefaultNameSpaces.EX.uri;

		Graph graph = new HashGraph();
		Graph schema = new HashGraph();

		if(bioModelInfo == null) {								// perhaps it's not public, in which case is not in the map
			ret = XmlRdfUtil.getMetadata(vcmlName);
			return ret;
		}
		PublicationInfo[] publicationInfos = bioModelInfo.getPublicationInfos();
		if(publicationInfos == null || publicationInfos.length == 0) {				// we may not have PublicationInfo
			ret = XmlRdfUtil.getMetadata(vcmlName);
			return ret;
		}

		PublicationInfo publicationInfo = publicationInfos[0];
		String bioModelName = bioModel.getName();
		Version version = bioModelInfo.getVersion();
		String[] creators = publicationInfo.getAuthors();
		String citation = publicationInfo.getCitation();
		String doi = publicationInfo.getDoi();
		Date pubDate = publicationInfo.getPubDate();
		String pubmedid = publicationInfo.getPubmedid();
		String sTitle = publicationInfo.getTitle();
		String url = publicationInfo.getUrl();
		List<String> contributors = new ArrayList<>();
		contributors.add("Dan Vasilescu");
		contributors.add("Michael Blinov");
		contributors.add("Ion Moraru");

		String description = "http://omex-library.org/" + vcmlName + ".omex";	// "http://omex-library.org/biomodel_12345678.omex";
		URI descriptionURI = ValueFactoryImpl.getInstance().createURI(description);
		Literal descTitle = OntUtil.createTypedString(schema, sTitle);
		graph.add(descriptionURI, RDF.TYPE, PubMet.Description);		// <rdf:Description rdf:about='http://omex-library.org/Monkeyflower_pigmentation_v2.omex'>
		graph.add(descriptionURI, PubMet.Title, descTitle);

		try {
			Map<String, String> nsMap = DefaultNameSpaces.defaultMap.convertToMap();
			ret = SesameRioUtil.writeRDFToString(graph, nsMap, RDFFormat.RDFXML);
//			SesameRioUtil.writeRDFToStream(System.out, graph, nsMap, RDFFormat.RDFXML);
		} catch (RDFHandlerException e) {
			logger.error(e.getMessage(), e);
		}

		String end = "\n\n" + ret.substring(ret.indexOf(PubMet.EndDescription0));
		ret = ret.substring(0, ret.indexOf(PubMet.EndDescription0));

		// https://vcellapi-beta.cam.uchc.edu:8080/biomodel/200301683/diagram
		// <collex:thumbnail rdf:resource="http://omex-library.org/Monkeyflower_pigmentation_v2.omex/Figure1.png"/>
		if(diagram.exists()) {
			ret += PubMet.StartDiagram;
			ret += description;
			ret += "/diagram.png";
			ret += PubMet.EndDiagram;
		}

		ret += PubMet.CommentTaxon;

		ret += PubMet.CommentOther;
		ret += PubMet.StartIs;
		ret += PubMet.StartDescription;
		ret += PubMet.StartIdentifier;
		ret += PubMet.ResourceIdentifier;
		String isLabel = "vcell:" + version.getVersionKey();
		ret += isLabel;
		ret += PubMet.EndIdentifier;
		ret += PubMet.StartLabel;
		ret += isLabel;
		ret += PubMet.EndLabel;
		ret += PubMet.EndDescription;
		ret += PubMet.EndIs;

		ret += PubMet.StartIsDescribedBy;
		ret += PubMet.StartDescription;
		ret += PubMet.StartIdentifier;
		ret += PubMet.ResourceIdentifier;
		String pubmed = "pubmed:" + pubmedid;
		ret += pubmed;
		ret += PubMet.EndIdentifier;
		ret += PubMet.StartLabel;
		ret += pubmed;
		ret += PubMet.EndLabel;
		ret += PubMet.EndDescription;
		ret += PubMet.EndIsDescribedBy;

		ret += PubMet.CommentCreator;
		for(String creator : creators) {
			ret += PubMet.StartCreator;
			ret += PubMet.StartDescription;
			ret += PubMet.StartName;
			ret += creator;
			ret += PubMet.EndName;
			ret += PubMet.StartLabel;
			ret += creator;
			ret += PubMet.EndLabel;
			ret += PubMet.EndDescription;
			ret += PubMet.EndCreator;
		}

		ret += PubMet.CommentContributor;
		for(String contributor : contributors) {
			ret += PubMet.StartContributor;
			ret += PubMet.StartDescription;
			ret += PubMet.StartName;
			ret += contributor;
			ret += PubMet.EndName;
			ret += PubMet.StartLabel;
			ret += contributor;
			ret += PubMet.EndLabel;
			ret += PubMet.EndDescription;
			ret += PubMet.EndContributor;
		}

		ret += PubMet.CommentCitations;
		ret += PubMet.StartIsDescribedBy;
		ret += PubMet.StartDescription;
		ret += PubMet.StartIdentifier;
		ret += PubMet.ResourceIdentifier;
		String sdoi = "doi:" + doi;
		ret += sdoi;
		ret += PubMet.EndIdentifier;
		ret += PubMet.StartLabel;
		ret += citation;
		ret += PubMet.EndLabel;
		ret += PubMet.EndDescription;
		ret += PubMet.EndIsDescribedBy;

//		ret += PubMet.CommentLicense;
//		ret += PubMet.StartLicense;
//		ret += PubMet.StartDescription;
//		ret += PubMet.StartIdentifier;
//		ret += PubMet.ResourceIdentifier;
//		String lic = "spdx:" + "CC0-1.0";
//		ret += lic;
//		ret += PubMet.EndIdentifier;
//		ret += PubMet.StartLabel;
//		ret += "CC0-1.0";
//		ret += PubMet.EndLabel;
//		ret += PubMet.EndDescription;
//		ret+= PubMet.EndLicense;

		String sPubDate = new SimpleDateFormat("yyyy-MM-dd").format(pubDate);
		ret += PubMet.CommentCreated;
		ret += PubMet.StartCreated;
		ret += PubMet.StartDescription;
		ret += PubMet.PrefixCreated;
		ret += new SimpleDateFormat("yyyy-MM-dd").format(pubDate);
		ret += PubMet.SuffixCreated;
		ret += PubMet.EndDescription;
		ret += PubMet.EndCreated;

		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		ret += PubMet.CommentModified;
		ret += PubMet.StartModified;
		ret += PubMet.StartDescription;
		ret += PubMet.PrefixModified;
		ret += new SimpleDateFormat("yyyy-MM-dd").format(today);
		ret += PubMet.SuffixModified;
		ret += PubMet.EndDescription;
		ret += PubMet.EndModified;

		ret += end;
		logger.trace(ret);
		return(ret);
	}

}


