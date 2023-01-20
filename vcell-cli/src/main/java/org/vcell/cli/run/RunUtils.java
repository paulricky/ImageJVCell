package org.vcell.cli.run;

import cbit.vcell.biomodel.BioModel;
import cbit.vcell.client.data.SimResultsViewer;
import cbit.vcell.export.server.*;
import cbit.vcell.mapping.SimulationContext;
import cbit.vcell.math.VariableType;
import cbit.vcell.parser.Expression;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.parser.SimpleSymbolTable;
import cbit.vcell.parser.SymbolTable;
import cbit.vcell.simdata.*;
import cbit.vcell.solver.*;
import cbit.vcell.solver.ode.ODESolverResultSet;
import cbit.vcell.util.ColumnDescription;
import com.google.common.io.Files;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jlibsedml.*;
import org.jlibsedml.DataSet;
import org.jlibsedml.Simulation;
import org.jlibsedml.execution.IXPathToVariableIDResolver;
import org.jlibsedml.modelsupport.SBMLSupport;
import org.vcell.cli.CLIUtils;
import org.vcell.cli.run.hdf5.*;
import org.vcell.stochtest.TimeSeriesMultitrialData;
import org.vcell.util.BeanUtils;
import org.vcell.util.DataAccessException;
import org.vcell.util.document.User;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RunUtils {

    public final static String VCELL_TEMP_DIR_PREFIX = "vcell_temp_";
    private final static Logger logger = LogManager.getLogger(RunUtils.class);

    public static ODESolverResultSet interpolate(ODESolverResultSet odeSolverResultSet, UniformTimeCourse sedmlSim) throws ExpressionException {
        double outputStart = sedmlSim.getOutputStartTime();
        double outputEnd = sedmlSim.getOutputEndTime();

        int numPoints = sedmlSim.getNumberOfPoints() + 1;


        ColumnDescription[] columnDescriptions = odeSolverResultSet.getColumnDescriptions();
        String[] columnNames = new String[columnDescriptions.length];

        for (int i = 0; i < columnDescriptions.length; i++) {
            columnNames[i] = columnDescriptions[i].getDisplayName();
        }

        // need to construct a new RowColumnResultSet instance
        ODESolverResultSet finalResultSet = new ODESolverResultSet();


        // use same column descriptions
        for (ColumnDescription cd : columnDescriptions) {
            finalResultSet.addDataColumn(cd);
        }


        double deltaTime = ((outputEnd - outputStart) / (numPoints - 1));
        double[] timepoints = new double[numPoints];

        timepoints[0] = outputStart;
        for (int i = 1; i < numPoints; i++) {
            timepoints[i] = timepoints[i - 1] + deltaTime;
        }

        double[] originalTimepoints = odeSolverResultSet.extractColumn(0);


        double[][] columnValues = new double[columnDescriptions.length][];
        columnValues[0] = timepoints;
        for (int i = 1; i < columnDescriptions.length; i++) {
            // each row uses the time index based on the params above and for each column descriptions interpolate the value from the original result set
            columnValues[i] = interpLinear(originalTimepoints, odeSolverResultSet.extractColumn(i), timepoints);
        }


        double[][] rowValues = new double[numPoints][columnDescriptions.length];

        for (int rowCount = 0; rowCount < numPoints; rowCount++) {
            for (int colCount = 0; colCount < columnDescriptions.length; colCount++) {
                rowValues[rowCount][colCount] = columnValues[colCount][rowCount];
            }
        }


        // add a numPoints number of rows one by one as double[]
        for (int rowCount = 0; rowCount < numPoints; rowCount++) {
            finalResultSet.addRow(rowValues[rowCount]);
        }

        return finalResultSet;
    }

    public static double[] interpLinear(double[] x, double[] y, double[] xi) throws IllegalArgumentException {

        if (x.length != y.length) {
            throw new IllegalArgumentException("X and Y must be the same length");
        }
        if (x.length == 1) {
            throw new IllegalArgumentException("X must contain more than one value");
        }
        double[] dx = new double[x.length - 1];
        double[] dy = new double[x.length - 1];
        double[] slope = new double[x.length - 1];
        double[] intercept = new double[x.length - 1];

        // Calculate the line equation (i.e. slope and intercept) between each point
        for (int i = 0; i < x.length - 1; i++) {
            dx[i] = x[i + 1] - x[i];
            if (dx[i] == 0) {
                throw new IllegalArgumentException("X must be montotonic. A duplicate " + "x-value was found");
            }
            if (dx[i] < 0) {
                throw new IllegalArgumentException("X must be sorted");
            }
            dy[i] = y[i + 1] - y[i];
            slope[i] = dy[i] / dx[i];
            intercept[i] = y[i] - x[i] * slope[i];
        }

        // Perform the interpolation here
        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            if ((xi[i] > x[x.length - 1]) || (xi[i] < x[0])) {
                yi[i] = Double.NaN;
            } else {
                int loc = Arrays.binarySearch(x, xi[i]);
                if (loc < -1) {
                    loc = -loc - 2;
                    yi[i] = slope[loc] * xi[i] + intercept[loc];
                } else {
                    yi[i] = y[loc];
                }
            }
        }

        return yi;
    }

    public static void exportPDE2HDF5(SimulationJob simJob, File userDir, File hdf5OutputFile) throws DataAccessException, IOException, Exception {

        cbit.vcell.solver.Simulation sim = simJob.getSimulation();
    	SimulationContext sc = (SimulationContext)sim.getSimulationOwner();
        BioModel bm = sc.getBioModel();
        int jobIndex = simJob.getJobIndex();


//        VCSimulationIdentifier vcSimID = new VCSimulationIdentifier(sim.getKey(), sim.getSimulationInfo().getVersion().getOwner());
        User user = new User(userDir.getName(), null);
        VCSimulationIdentifier vcSimID = new VCSimulationIdentifier(sim.getKey(), user);

        VCSimulationDataIdentifier vcId = new VCSimulationDataIdentifier(vcSimID, jobIndex);

        DataSetControllerImpl dsControllerImpl = new DataSetControllerImpl(null, userDir.getParentFile(), null);
        ExportServiceImpl exportServiceImpl = new ExportServiceImpl();
        DataServerImpl dataServerImpl = new DataServerImpl(dsControllerImpl, exportServiceImpl);
        OutputContext outputContext = new OutputContext(sc.getOutputFunctionContext().getOutputFunctionsList().toArray(new AnnotatedFunction[0]));
        PDEDataContext pdedc = new ServerPDEDataContext(outputContext, user, dataServerImpl, vcId);
        DataIdentifier[] dataIDArr = pdedc.getDataIdentifiers();
        ArrayList<String> variableNames = new ArrayList<String>();
        for(int i = 0; i<dataIDArr.length; i++) {
        	if (dataIDArr[i].getVariableType().getType() == VariableType.VOLUME.getType()) variableNames.add(dataIDArr[i].getName());
        }
        VariableSpecs variableSpecs = new VariableSpecs(variableNames.toArray(new String[0]), ExportConstants.VARIABLE_MULTI);

        double[] dataSetTimes = dsControllerImpl.getDataSetTimes(vcId);
        TimeSpecs timeSpecs = new TimeSpecs(0,dataSetTimes.length-1, dataSetTimes, ExportConstants.TIME_RANGE);

        int geoMode = ExportConstants.GEOMETRY_FULL;
        int axis = 2;
        int sliceNumber = 0;
        GeometrySpecs geometrySpecs = new GeometrySpecs(null, axis, sliceNumber, geoMode);

        ExportConstants.DataType dataType = ExportConstants.DataType.PDE_VARIABLE_DATA;
        boolean switchRowsColumns = false;

        // String simulationName,VCSimulationIdentifier vcSimulationIdentifier,ExportParamScanInfo exportParamScanInfo
        ExportSpecs.ExportParamScanInfo exportParamScanInfo = SimResultsViewer.getParamScanInfo(sim,jobIndex);
        ExportSpecs.SimNameSimDataID snsdi= new ExportSpecs.SimNameSimDataID(sim.getName(), vcSimID, exportParamScanInfo);
        ExportSpecs.SimNameSimDataID[] simNameSimDataIDs = { snsdi };
        int[] exportMultipleParamScans = null;
        boolean isHDF5 = true;
        FormatSpecificSpecs formatSpecificSpecs = new ASCIISpecs(ExportFormat.CSV, dataType, switchRowsColumns, simNameSimDataIDs, exportMultipleParamScans, ASCIISpecs.csvRoiLayout.var_time_val, isHDF5);

        ASCIIExporter ae = new ASCIIExporter(exportServiceImpl);
        String contextName = bm.getName() + ":" + sc.getName();
        ExportSpecs exportSpecs = new ExportSpecs(vcId, ExportFormat.HDF5, variableSpecs, timeSpecs, geometrySpecs, formatSpecificSpecs, sim.getName(), contextName);
        FileDataContainerManager fileDataContainerManager = new FileDataContainerManager();

        JobRequest jobRequest = JobRequest.createExportJobRequest(vcId.getOwner());

        Collection<ExportOutput > eo;
		try {
			eo = ae.makeASCIIData(outputContext, jobRequest, vcId.getOwner(), dataServerImpl, exportSpecs,
					fileDataContainerManager);
		} catch (Exception e) {
			throw e;
		}
        Iterator<ExportOutput> iterator = eo.iterator();
        ExportOutput aaa = iterator.next();


        if(((ASCIISpecs)exportSpecs.getFormatSpecificSpecs()).isHDF5()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            aaa.writeDataToOutputStream(baos, fileDataContainerManager);//Get location of temp HDF5 file
            File tempHDF5File = new File(baos.toString());
            Files.copy(tempHDF5File, hdf5OutputFile);
            tempHDF5File.delete();
        }
    }

    public static HashMap<String, File> generateReportsAsCSV(SedML sedml, Map<TaskJob, ODESolverResultSet> resultsHash, File outDirForCurrentSedml, String outDir, String sedmlLocation) throws DataAccessException, IOException {
        // finally, the real work
        HashMap<String, File> reportsHash = new HashMap<>();
        List<Output> ooo = sedml.getOutputs();
        for (Output oo : ooo) {
            // We only want Reports
            if (!(oo instanceof Report)) {
                logger.info("Ignoring unsupported output `" + oo.getId() + "` while CSV generation.");
                continue;
            }

            StringBuilder sb = new StringBuilder();

            logger.info("Generating report `" + oo.getId() +"`.");
            /*
             * we go through each entry (dataset) in the list of datasets
             * for each dataset, we use the data reference to obtain the data generator
             * we get the list of variables associated with the data reference
             * * each variable has an id (which is the data reference above, the task and the sbml symbol urn
             * * for each variable we recover the task, from the task we get the sbml model
             * * we search the sbml model to find the vcell variable name associated with the urn
             */
            try {
                List<DataSet> datasets = ((Report) oo).getListOfDataSets();
                for (DataSet dataset : datasets) {
                    DataGenerator datagen = sedml.getDataGeneratorWithId(dataset.getDataReference()); assert datagen != null;
                    List<String> varIDs = new ArrayList<>();     
                    List<Variable> vars = new ArrayList<>(datagen.getListOfVariables());
                    Map<Variable, List<double[]> > values = new HashMap<>();
                    int mxlen = 0;
//                        boolean supportedDataset = true;
                    
                    // get target values
                    for (Variable var : vars) {
                        AbstractTask task = sedml.getTaskWithId(var.getReference());
                        Simulation sedmlSim = null;
                        Task actualTask = null;

                        if(task instanceof RepeatedTask) { // We assume that we can never have a sequential repeated task at this point, we check for that in SEDMLImporter
                            RepeatedTask rt = (RepeatedTask)task;
                            
                            if (!rt.getResetModel() || rt.getSubTasks().size() != 1) {
                                logger.warn("Sequential RepeatedTask not yet supported, task " + task.getElementName() + " is being skipped");
                                continue;
                   			}
                            
                            AbstractTask referredTask;
                            do { // find the actual Task and extract the simulation
                                SubTask st = rt.getSubTasks().entrySet().iterator().next().getValue(); // single subtask
                                String taskId = st.getTaskId();
                                referredTask = sedml.getTaskWithId(taskId);
                                if (referredTask instanceof RepeatedTask) rt = (RepeatedTask)referredTask;
                            } while (referredTask instanceof RepeatedTask);
                            actualTask = (Task)referredTask;
                            sedmlSim = sedml.getSimulation(actualTask.getSimulationReference());
                        } else {
                            actualTask = (Task)task;
                            sedmlSim = sedml.getSimulation(task.getSimulationReference());
                        }

                        // Confirm uniform time
                        if (!(sedmlSim instanceof UniformTimeCourse)){
                            logger.error("only uniform time course simulations are supported");
                            continue;
                        }

                        IXPathToVariableIDResolver variable2IDResolver = new SBMLSupport(); // must get variable ID from SBML model
                        String sbmlVarId = "";
                        if (var.getSymbol() != null) { // it is a predefined symbol
                            // translate SBML official symbols
                            // TODO: check spec for other symbols

                            // Time
                            sbmlVarId = var.getSymbol().name();      
                            if ("TIME".equals(sbmlVarId)) 
                                sbmlVarId = "t";// this is VCell reserved symbold for time
                        } else {// it is an XPATH target in model
                            String target = var.getTarget();
                            sbmlVarId = variable2IDResolver.getIdFromXPathIdentifer(target);
                        }

                        // Get task job(s)
                        List<TaskJob> taskJobs = new ArrayList<>();
                        for (Map.Entry<TaskJob, ODESolverResultSet> entry : resultsHash.entrySet()) {
                            TaskJob taskJob = entry.getKey();
                            ODESolverResultSet value = entry.getValue();
                            if(value != null && taskJob.getTaskId().equals(task.getId())) {
                                taskJobs.add(taskJob);
                                if (!(task instanceof RepeatedTask)) break; // Only have one entry for non-repeated tasks
                            }
                        }
                        if (taskJobs.isEmpty()) continue; 
                        
                        varIDs.add(var.getId());
                        
                        // we want to keep the last outputNumberOfPoints only
                        int outputNumberOfPoints = ((UniformTimeCourse) sedmlSim).getNumberOfPoints();
                        double outputStartTime = ((UniformTimeCourse) sedmlSim).getOutputStartTime();
                        List<double[]> variablesList = new ArrayList<>();

                        for (TaskJob taskJob : taskJobs){
                            // key format in resultsHash is taskId + "_" + simJobId
                            // ex: task_0_0_0 where the last 0 is the simJobId (always 0 when no parameter scan)
                            double[] data;
                            ODESolverResultSet results = resultsHash.get(taskJob); // hence the added "_0"
                            if (results==null) continue;
                            int column = results.findColumn(sbmlVarId);

                            if (outputStartTime > 0){
                                data = new double[outputNumberOfPoints+1];
                                double[] tmpData = results.extractColumn(column);
                                for(int i=tmpData.length-outputNumberOfPoints-1, j=0; i<tmpData.length; i++, j++) {
                                    data[j] = tmpData[i];
                                }
                            } else {
                                data = results.extractColumn(column);
                            }

                            mxlen = Integer.max(mxlen, data.length);
                            if(!values.containsKey(var)) {		// this is the first double[]
                                variablesList.add(data);
                                values.put(var, variablesList);
                            } else {
                                List<double[]> variablesListTemp = values.get(var);
                                variablesListTemp.add(data);
                                values.put(var, variablesListTemp);
                            }
                        }
                    }
                    if (varIDs.isEmpty()) continue;
                    
                    String mathMLStr = datagen.getMathAsString(); //get math
                    Expression expr = new Expression(mathMLStr);
                    SymbolTable st = new SimpleSymbolTable(varIDs.toArray(new String[vars.size()]));
                    expr.bindExpression(st);

                    Variable firstVar = vars.get(0);
                    List<double[]> v = values.get(firstVar);
                    int overridesCount = v!=null ? v.size() : 0;
                    for(int k=0; k < overridesCount; k++) {

                        if(k>0 && dataset.getId().contains("time_")) {
                            continue;
                        }
                        //compute and write result, padding with NaN if unequal length or errors
                        double[] row = new double[vars.size()];

                        // Handling row labels that contains ","
                        if (dataset.getId().startsWith("__vcell_reserved_data_set_prefix__")) {		// also used in cli.py
                            if (dataset.getLabel().contains(",")) sb.append("\"" + dataset.getLabel() + "\"").append(",");
                            else sb.append(dataset.getLabel()).append(",");
                        } else {
                            if (dataset.getId().contains(",")) sb.append("\"" + dataset.getId() + "\"").append(",");
                            else sb.append(dataset.getId()).append(",");
                        }

                        if (dataset.getLabel().contains(",")) sb.append("\"" + dataset.getLabel() + "\"").append(",");
                        else sb.append(dataset.getLabel()).append(",");

                        DataGenerator dg = sedml.getDataGeneratorWithId(dataset.getDataReference());
                        if(dg != null && dg.getName() != null && !dg.getName().isEmpty()) {
                            // name may contain spaces or other things
                            sb.append("\"" + dg.getName() + "\"").append(",");
                        } else {			// dg may be null, name may be null
                            sb.append("").append(",");
                        }

                        // TODO: here was the for(k : overridesCount)
                        for (int i = 0; i < mxlen; i++) {
                            for (int j = 0; j < vars.size(); j++) {
                                //                                double[] varVals = ((double[]) values.get(vars.get(j)));
                                Variable var = vars.get(j);
                                List<double[]> variablesList = values.get(var);
                                double[] varVals = variablesList.get(k);

                                if (i < varVals.length) {
                                    row[j] = varVals[i];
                                } else {
                                    row[j] = Double.NaN;
                                }
                            }
                            double computed = Double.NaN;
                            try {
                                computed = expr.evaluateVector(row);
                            } catch (Exception e) {
                                // do nothing, we leave NaN and don't warn/log since it could flood
                            }
                            sb.append(computed).append(",");
                        }
                        //	}	// here would have been the close bracket for the loop over k
                        sb.deleteCharAt(sb.lastIndexOf(","));
                        sb.append("\n");

                    }	//end of k loop
                    PythonCalls.updateDatasetStatusYml(sedmlLocation, oo.getId(), dataset.getId(), Status.SUCCEEDED, outDir);

                } // end of dataset			

                if (sb.length() > 0) {
                    File f = new File(outDirForCurrentSedml, oo.getId() + ".csv");
                    PrintWriter out = new PrintWriter(f);
                    out.print(sb.toString());
                    out.flush();
                    out.close();
                    logger.info("created csv file for report " + oo.getId() + ": " + f.getAbsolutePath());
                    reportsHash.put(oo.getId(), f);
                } else {
                    logger.info("no csv file for report " + oo.getId());
                }
            } catch (Exception e) {
                logger.error("Encountered exception: " + e.getMessage(), e);
                reportsHash.put(oo.getId(), null);
            }
        }
        return reportsHash;
    }

    private static class NonspatialValueHolder {
        List<double[]> values = new ArrayList<>();
        final cbit.vcell.solver.Simulation vcSimulation;

        public NonspatialValueHolder(cbit.vcell.solver.Simulation simulation) {
            this.vcSimulation = simulation;
        }

        public int getNumJobs() {
            return values.size();
        }

        public int[] getJobCoordinate(int index){
            String[] names = vcSimulation.getMathOverrides().getScannedConstantNames();
            java.util.Arrays.sort(names); // must do things in a consistent way
            int[] bounds = new int[names.length]; // bounds of scanning matrix
            for (int i = 0; i < names.length; i++){
                bounds[i] = vcSimulation.getMathOverrides().getConstantArraySpec(names[i]).getNumValues() - 1;
            }
            int[] coordinates = BeanUtils.indexToCoordinate(index, bounds);
            return coordinates;
        }
    }

    public static List<Hdf5DatasetWrapper> prepareNonspatialHdf5(SedML sedml, Map<TaskJob, ODESolverResultSet> nonspatialResultsHash, Map<AbstractTask, cbit.vcell.solver.Simulation> taskToSimulationMap, String sedmlLocation) throws DataAccessException, IOException, HDF5Exception, ExpressionException {

        List<Hdf5DatasetWrapper> datasetWrappers = new ArrayList<>();
        List<Report> reports = new LinkedList<>();

        for (Output out : sedml.getOutputs()) {
            if (out instanceof Report){
                reports.add((Report)out);
            } else {
                logger.info("Ignoring unsupported output `" + out.getId() + "` while CSV generation.");
            }
        } 
        
        for (Report report : reports){
            Map<DataSet, Map<Variable, NonspatialValueHolder>> dataSetValues = new LinkedHashMap<>();

            logger.info("Generating report `" + report.getId() + "`.");

            // we go through each entry (dataset) in the list of datasets
            // for each dataset, we use the data reference to obtain the data generator
            // we get the list of variables associated with the data reference
            //   each variable has an id (which is the data reference above, the task and the sbml symbol urn
            //   for each variable we recover the task, from the task we get the sbml model
            //   we search the sbml model to find the vcell variable name associated with the urn

            for (DataSet dataset : report.getListOfDataSets()) { // we go through each entry (dataset)
                DataGenerator datagen;
                List<String> varIDs = new ArrayList<>();
                Map<Variable, NonspatialValueHolder> values = new HashMap<>();
                int maxLengthOfAllData = 0; // We have to pad up to this value

                // use the data reference to obtain the data generator
                datagen = sedml.getDataGeneratorWithId(dataset.getDataReference()); 
                assert datagen != null;
                
                // get the list of variables associated with the data reference
                for (Variable var : datagen.getListOfVariables()) {
                    AbstractTask initialTask = sedml.getTaskWithId(var.getReference());
                    Simulation sedmlSim = null;

                    AbstractTask referredTask = initialTask;
                    while (referredTask instanceof RepeatedTask) { // We need to find the original task burried beneath.
                        // We assume that we can never have a sequential repeated task at this point, we check for that in SEDMLImporter
                        SubTask st = ((RepeatedTask)referredTask).getSubTasks().entrySet().iterator().next().getValue(); // single subtask
                        referredTask = sedml.getTaskWithId(st.getTaskId());
                    }
                    sedmlSim = sedml.getSimulation(((Task)referredTask).getSimulationReference());

                    
                    // must get variable ID from SBML model
                    String sbmlVarId = "";
                    if (var.getSymbol() != null) { // it is a predefined symbol
                        // translate SBML official symbols
                        switch(var.getSymbol().name()){
                            case "TIME": { // TIME is t, etc
                                sbmlVarId = "t"; // this is VCell reserved symbold for time
                            }
                            // etc, TODO: check spec for other symbols (CSymbols?)
                            // Delay? Avogadro? rateOf?
                        }
                    } else { // it is an XPATH target in model
                        String target = var.getTarget();
                        IXPathToVariableIDResolver resolver = new SBMLSupport();
                        sbmlVarId = resolver.getIdFromXPathIdentifer(target);
                    }


                    boolean bFoundTaskInNonspatial = nonspatialResultsHash.keySet().stream().anyMatch(taskJob -> taskJob.getTaskId().equals(initialTask.getId()));
                    if (!bFoundTaskInNonspatial){
                        break;
                    }

                    // ==================================================================================
                    
                    ArrayList<TaskJob> taskJobs = new ArrayList<>();

                    for (Map.Entry<TaskJob, ODESolverResultSet> entry : nonspatialResultsHash.entrySet()) {
                        TaskJob taskJob = entry.getKey();
                        if (entry.getValue() != null && taskJob.getTaskId().equals(initialTask.getId())) {
                            taskJobs.add(taskJob);
                            if (!(initialTask instanceof RepeatedTask)) 
                                break; // No need to keep looking if its not a repeated task
                        }
                    }

                    if (taskJobs.isEmpty()) continue;

                    varIDs.add(var.getId());

                    if (!(sedmlSim instanceof UniformTimeCourse)){
                        logger.error("only uniform time course simulations are supported");
                        continue;
                    }

                    // we want to keep the last outputNumberOfPoints only
                    int outputNumberOfPoints = ((UniformTimeCourse) sedmlSim).getNumberOfPoints();
                    double outputStartTime = ((UniformTimeCourse) sedmlSim).getOutputStartTime();
                    NonspatialValueHolder variablesList;

                    for (TaskJob taskJob : taskJobs) {
                        ODESolverResultSet results = nonspatialResultsHash.get(taskJob);
                        int column = results.findColumn(sbmlVarId);
                        double[] data = results.extractColumn(column);
                        
                        if (outputStartTime > 0){
                            double[] correctiveData = new double[outputNumberOfPoints + 1];
                            for (int i = data.length - outputNumberOfPoints - 1, j = 0; i < data.length; i++, j++) {
                                correctiveData[j] = data[i];
                            }
                            data = correctiveData;
                        }

                        maxLengthOfAllData = Integer.max(maxLengthOfAllData, data.length);
                        if (initialTask instanceof RepeatedTask && values.containsKey(var)) { // double[] exists
                            variablesList = values.get(var);
                        } else { // this is the first double[]
                            variablesList = new NonspatialValueHolder(taskToSimulationMap.get(initialTask));
                        }
                        variablesList.values.add(data);
                        values.put(var, variablesList);
                    }
                }
                dataSetValues.put(dataset, values);

            } // end of dataset

            Hdf5DatasetWrapper hdf5DatasetWrapper = new Hdf5DatasetWrapper();
            hdf5DatasetWrapper.datasetMetadata._type = report.getKind();
            hdf5DatasetWrapper.datasetMetadata.sedmlId = report.getId();
            hdf5DatasetWrapper.datasetMetadata.sedmlName = report.getName();
            hdf5DatasetWrapper.datasetMetadata.uri = sedmlLocation;

            Map<Variable, NonspatialValueHolder> firstValues = dataSetValues.entrySet().iterator().next().getValue();
            if (firstValues.size()==0){
                continue;
            }
            NonspatialValueHolder valuesForFirstVar = firstValues.entrySet().iterator().next().getValue();
            int numJobs = valuesForFirstVar.getNumJobs();

            Hdf5DataSourceNonspatial dataSourceNonspatial = new Hdf5DataSourceNonspatial();
            hdf5DatasetWrapper.dataSource = dataSourceNonspatial;
            for (int i=0; i<numJobs; i++) {
                dataSourceNonspatial.jobData.add(new Hdf5DataSourceNonspatial.Hdf5JobData());
            }

            for (Map.Entry<DataSet, Map<Variable, NonspatialValueHolder>> dataSetEntry : dataSetValues.entrySet()){
                DataSet dataSet = dataSetEntry.getKey();
                Map<Variable, NonspatialValueHolder> values = dataSetEntry.getValue();

                for (Map.Entry<Variable, NonspatialValueHolder> varEntry : values.entrySet()){
                    Variable var = varEntry.getKey();
                    NonspatialValueHolder dataArrays = varEntry.getValue();
                    dataSourceNonspatial.scanBounds = dataArrays.vcSimulation.getMathOverrides().getScanBounds();
                    dataSourceNonspatial.scanParameterNames = dataArrays.vcSimulation.getMathOverrides().getScannedConstantNames();
                    for (int jobIndex=0; jobIndex<numJobs; jobIndex++){
                        double[] data = dataArrays.values.get(jobIndex);
                        Hdf5DataSourceNonspatial.Hdf5JobData hdf5JobData = dataSourceNonspatial.jobData.get(jobIndex);
                        hdf5JobData.varData.put(var,data);
                    }
                }
                hdf5DatasetWrapper.datasetMetadata.sedmlDataSetDataTypes.add("float64");
                hdf5DatasetWrapper.datasetMetadata.sedmlDataSetIds.add(dataSet.getId());
                hdf5DatasetWrapper.datasetMetadata.sedmlDataSetLabels.add(dataSet.getLabel());
                hdf5DatasetWrapper.datasetMetadata.sedmlDataSetNames.add(dataSet.getName());
            }
            hdf5DatasetWrapper.datasetMetadata.sedmlDataSetShapes = null;
            datasetWrappers.add(hdf5DatasetWrapper);
        } // outputs/reports
        return datasetWrappers;
    }


    public static List<Hdf5DatasetWrapper> prepareSpatialHdf5(SedML sedml, Map<TaskJob, File> spatialResultsHash, Map<AbstractTask, cbit.vcell.solver.Simulation> taskToSimulationMap, String sedmlLocation) throws DataAccessException, IOException, HDF5Exception, ExpressionException {

        List<Hdf5DatasetWrapper> datasetWrappers = new ArrayList<>();
        // finally, the real work
        List<Output> outputs = sedml.getOutputs();
        for (Output out : outputs) {
            if (!(out instanceof Report)) {
                logger.info("Ignoring unsupported output `" + out.getId() + "` during HDF5 generation.");
                continue;
            }
            Report report = (Report)out;
            boolean bNotSpatial = false;
            Hdf5DataSourceSpatial hdf5DataSourceSpatial = new Hdf5DataSourceSpatial();

            logger.info("Generating report `" + out.getId() + "`.");
            // we go through each entry (dataset) in the list of datasets
            // for each dataset, we use the data reference to obtain the data generator
            // ve get the list of variables associated with the data reference
            //   each variable has an id (which is the data reference above, the task and the sbml symbol urn
            //   for each variable we recover the task, from the task we get the sbml model
            //   we search the sbml model to find the vcell variable name associated with the urn
            List<DataSet> datasets = report.getListOfDataSets();
            for (DataSet dataset : datasets) {

                DataGenerator datagen = sedml.getDataGeneratorWithId(dataset.getDataReference());
                assert datagen != null;
                ArrayList<Variable> vars = new ArrayList<>(datagen.getListOfVariables());
                for (Variable var : vars) {
                    AbstractTask task = sedml.getTaskWithId(var.getReference());

                    Simulation sedmlSim = null;
                    Task actualTask = null;
                    if (task instanceof RepeatedTask) {
                        RepeatedTask rt = (RepeatedTask) task;
                        // We assume that we can never have a sequential repeated task at this point, we check for that in SEDMLImporter
//                				if (!rt.getResetModel() || rt.getSubTasks().size() != 1) {
//                					logger.error("sequential RepeatedTask not yet supported, task "+SEDMLUtil.getName(selectedTask)+" is being skipped");
//                					continue;
//                				}
                        AbstractTask referredTask;
                        // find the actual Task and extract the simulation
                        do {
                            SubTask st = rt.getSubTasks().entrySet().iterator().next().getValue(); // single subtask
                            String taskId = st.getTaskId();
                            referredTask = sedml.getTaskWithId(taskId);
                            if (referredTask instanceof RepeatedTask) rt = (RepeatedTask) referredTask;
                        } while (referredTask instanceof RepeatedTask);
                        actualTask = (Task) referredTask;
                        sedmlSim = sedml.getSimulation(actualTask.getSimulationReference());
                    } else {
                        actualTask = (Task) task;
                        sedmlSim = sedml.getSimulation(task.getSimulationReference());
                    }

                    IXPathToVariableIDResolver variable2IDResolver = new SBMLSupport();
                    // must get variable ID from SBML model
                    String sbmlVarId = "";
                    if (var.getSymbol() != null) {
                        // it is a predefined symbol
                        sbmlVarId = var.getSymbol().name();
                        // translate SBML official symbols
                        // TIME is t, etc.
                        if ("TIME".equals(sbmlVarId)) {
                            // this is VCell reserved symbold for time
                            sbmlVarId = "t";
                            continue;
                        }
                        // TODO
                        // check spec for other symbols
                    } else {
                        // it is an XPATH target in model
                        String target = var.getTarget();
                        sbmlVarId = variable2IDResolver.getIdFromXPathIdentifer(target);
                    }
                    boolean bFoundTaskInSpatial = spatialResultsHash.keySet().stream().anyMatch(taskJob -> taskJob.getTaskId().equals(task.getId()));
                    if (!bFoundTaskInSpatial){
                        bNotSpatial = true;
                        break;
                    }

                    if (task instanceof RepeatedTask) {
                        ArrayList<TaskJob> taskJobs = new ArrayList<>();
                        for (Map.Entry<TaskJob, File> entry : spatialResultsHash.entrySet()) {
                            TaskJob taskJob = entry.getKey();
                            File value = entry.getValue();
                            if (value != null && taskJob.getTaskId().equals(task.getId())) {
                                taskJobs.add(taskJob);
                            }
                        }
                        if (sedmlSim instanceof UniformTimeCourse) {
                            int outputNumberOfPoints = ((UniformTimeCourse) sedmlSim).getNumberOfPoints();
                            double outputStartTime = ((UniformTimeCourse) sedmlSim).getOutputStartTime();
                            int jobIndex=0;
                            for (TaskJob taskJob : taskJobs) {
                                File spatialH5File = spatialResultsHash.get(taskJob);
                                if (spatialH5File!=null) {
                                    Hdf5DataSourceSpatialVarDataItem job = new Hdf5DataSourceSpatialVarDataItem(
                                            report, dataset, var, jobIndex, spatialH5File, outputStartTime, outputNumberOfPoints);
                                    hdf5DataSourceSpatial.varDataItems.add(job);
                                    hdf5DataSourceSpatial.scanBounds = taskToSimulationMap.get(task).getMathOverrides().getScanBounds();
                                    hdf5DataSourceSpatial.scanParameterNames = taskToSimulationMap.get(task).getMathOverrides().getScannedConstantNames();
                                }
                                jobIndex++;
                            }
                        } else {
                            logger.error("only uniform time course simulations are supported");
                        }
                    } else {
                        TaskJob taskJob = new TaskJob(actualTask.getId(), 0);
                        if (sedmlSim instanceof UniformTimeCourse) {
                            // we want to keep the last outputNumberOfPoints only
                            int outputNumberOfPoints = ((UniformTimeCourse) sedmlSim).getNumberOfPoints();
                            double outputStartTime = ((UniformTimeCourse) sedmlSim).getOutputStartTime();
                            File spatialH5File = spatialResultsHash.get(taskJob);
                            if (spatialH5File!=null) {
                                Hdf5DataSourceSpatialVarDataItem job = new Hdf5DataSourceSpatialVarDataItem(
                                        report, dataset, var, 0, spatialH5File, outputStartTime, outputNumberOfPoints);
                                hdf5DataSourceSpatial.varDataItems.add(job);
                                hdf5DataSourceSpatial.scanBounds = new int[0];
                                hdf5DataSourceSpatial.scanParameterNames = new String[0];
                            }
                        } else {
                            logger.error("only uniform time course simulations are supported");
                        }
                    }
                }
            } // end of dataset
            if (bNotSpatial || hdf5DataSourceSpatial.varDataItems.size()==0){
                continue;
            }

            Hdf5DatasetWrapper hdf5DatasetWrapper = new Hdf5DatasetWrapper();
            hdf5DatasetWrapper.dataSource = hdf5DataSourceSpatial;
            hdf5DatasetWrapper.datasetMetadata._type = report.getKind();
            hdf5DatasetWrapper.datasetMetadata.sedmlId = report.getId();
            hdf5DatasetWrapper.datasetMetadata.sedmlName = report.getName();
            hdf5DatasetWrapper.datasetMetadata.uri = sedmlLocation;

            hdf5DatasetWrapper.dataSource = hdf5DataSourceSpatial;
            for (Hdf5DataSourceSpatialVarDataItem job : hdf5DataSourceSpatial.varDataItems){
                DataSet dataSet = job.sedmlDataset;
                hdf5DatasetWrapper.datasetMetadata.sedmlDataSetDataTypes.add("float64");
                hdf5DatasetWrapper.datasetMetadata.sedmlDataSetIds.add(dataSet.getId());
                hdf5DatasetWrapper.datasetMetadata.sedmlDataSetLabels.add(dataSet.getLabel());
                hdf5DatasetWrapper.datasetMetadata.sedmlDataSetNames.add(dataSet.getName());
            }
            hdf5DatasetWrapper.datasetMetadata.sedmlDataSetShapes = null;
            datasetWrappers.add(hdf5DatasetWrapper);
        } // outputs/reports
        return datasetWrappers;
    }


    public static String generateIdNamePlotsMap(SedML sedml, File outDirForCurrentSedml) {
        StringBuilder sb = new StringBuilder();
        List<Output> ooo = sedml.getOutputs();
        for (Output oo : ooo) {
            if (!(oo instanceof Report)) {
                logger.info("Ignoring unsupported output `" + oo.getId() + "` while generating idNamePlotsMap.");
            } else {
                String id = oo.getId();
                sb.append(id).append("|");	// hopefully no vcell name contains '|', so I can use it as separator
                sb.append(oo.getName()).append("\n");
                if(id.startsWith("__plot__")) {
                    id = id.substring("__plot__".length());
                    sb.append(id).append("|");	// the creation of the csv files is whimsical, we also use an id with __plot__ removed
                    sb.append(oo.getName()).append("\n");
                }
            }
        }

        File f = new File(outDirForCurrentSedml, "idNamePlotsMap.txt");
        try {
            PrintWriter out = new PrintWriter(f);
            out.print(sb.toString());
            out.flush();
            out.close();
        } catch(Exception e) {
            logger.error("Unable to create the idNamePlotsMap; " + e.getMessage(), e);
        }
        return f.toString();
    }



    public static void zipResFiles(File dirPath) throws IOException {

        FileInputStream fileInputstream;
        FileOutputStream fileOutputStream;
        ZipOutputStream zipOutputStream;
        ArrayList<File> srcFiles;
        String relativePath;
        ZipEntry zipEntry;

        // TODO: Add SED-ML name as base dirPath to avoid zipping all available CSV, PDF
        // Map for naming to extension
        Map<String, String> extensionListMap = new HashMap<String, String>() {{
            put("csv", "reports.zip");
            put("pdf", "plots.zip");
        }};

        for (String ext : extensionListMap.keySet()) {
            srcFiles = listFilesForFolder(dirPath, ext);

            if (srcFiles.size() == 0) {
                logger.error("No " + ext.toUpperCase() + " files found, skipping archiving `" + extensionListMap.get(ext) + "` files");
            } else {
                fileOutputStream = new FileOutputStream(Paths.get(dirPath.toString(), extensionListMap.get(ext)).toFile());
                zipOutputStream = new ZipOutputStream(fileOutputStream);
                if (srcFiles.size() != 0) logger.info("Archiving resultant " + ext.toUpperCase() + " files to `" + extensionListMap.get(ext) + "`.");
                for (File srcFile : srcFiles) {

                    fileInputstream = new FileInputStream(srcFile);

                    // get relative path
                    relativePath = dirPath.toURI().relativize(srcFile.toURI()).toString();
                    zipEntry = new ZipEntry(relativePath);
                    zipOutputStream.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fileInputstream.read(bytes)) >= 0) {
                        zipOutputStream.write(bytes, 0, length);
                    }
                    fileInputstream.close();
                }
                zipOutputStream.close();
                fileOutputStream.close();
            }
        }
    }

    public static String getTempDir() throws IOException {
        String tempPath = String.valueOf(java.nio.file.Files.createTempDirectory(
            RunUtils.VCELL_TEMP_DIR_PREFIX + UUID.randomUUID().toString()).toAbsolutePath());
        logger.info("TempPath Created: " + tempPath);
        return tempPath;
    }

    public static ArrayList<File> listFilesForFolder(File dirPath, String extensionType) {
        File dir = new File(String.valueOf(dirPath));
        String[] extensions = new String[]{extensionType};
        List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
        return new ArrayList<>(files);
    }


    // Breakline
    public static void drawBreakLine(String breakString, int times) {
        if (logger.isInfoEnabled()) System.out.println(breakString + StringUtils.repeat(breakString, times));
        //logger.info(breakString + StringUtils.repeat(breakString, times));
    }


    public static boolean removeAndMakeDirs(File f) {
        if (f.exists()) {
            boolean isRemoved = CLIUtils.removeDirs(f);
            if (!isRemoved)
                return false;
        }
        return f.mkdirs();
    }

    public static void createCSVFromODEResultSet(ODESolverResultSet resultSet, File f) throws ExpressionException {
        ColumnDescription[] descriptions = resultSet.getColumnDescriptions();
        StringBuilder sb = new StringBuilder();


        int numberOfColumns = descriptions.length;
        int numberOfRows = resultSet.getRowCount();

        double[][] dataPoints = new double[numberOfColumns][];
        // Write headers
        for (ColumnDescription description : descriptions) {
            sb.append(description.getDisplayName());
            sb.append(",");
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append("\n");


        // Write rows
        for (int i = 0; i < numberOfColumns; i++) {
            dataPoints[i] = resultSet.extractColumn(i);
        }

        for (int rowNum = 0; rowNum < numberOfRows; rowNum++) {
            for (int colNum = 0; colNum < numberOfColumns; colNum++) {
                sb.append(dataPoints[colNum][rowNum]);
                sb.append(",");
            }

            sb.deleteCharAt(sb.lastIndexOf(","));
            sb.append("\n");
        }


        try {
            PrintWriter out = new PrintWriter(f);
            out.print(sb.toString());
            out.flush();
        } catch (FileNotFoundException e) {
            logger.error("Unable to find path, failed with err: " + e.getMessage(), e);
        }

    }

    public static void removeIntermediarySimFiles(File path) {
        File[] files = path.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".csv")) {
                // Do nothing
                continue;
            } else {
                f.delete();
            }
        }
    }

    public static void saveTimeSeriesMultitrialDataAsCSV(TimeSeriesMultitrialData data, File outDir) {
        File outFile = Paths.get(outDir.toString(), data.datasetName + ".csv").toFile();
        int numberOfRows = data.times.length;
        int numberOfVariables = data.varNames.length;
        // Headers for CSV
        ArrayList<String> headersList = new ArrayList<>();
        headersList.add("times");
        Collections.addAll(headersList, data.varNames);

        // Complete rows for CSV
        ArrayList<ArrayList<Double>> allRows = new ArrayList<>();

        for (int rowCounter = 0; rowCounter < numberOfRows; rowCounter++) {
            ArrayList<Double> row = new ArrayList<>();
            row.add(data.times[rowCounter]);

            for (int varCounter = 0; varCounter < numberOfVariables; varCounter++) {
                row.add(data.data[varCounter][rowCounter][0]);
            }

            allRows.add(row);

        }

        // Writing CSV in string buffer
        StringBuilder headersBuilder = new StringBuilder();

        for (String headerName : headersList) {
            headersBuilder.append(headerName);
            headersBuilder.append(",");
        }


        String headers = headersBuilder.replace(headersBuilder.length() - 1, headersBuilder.length(), "\n").toString();

        StringBuilder allRowsBuilder = new StringBuilder(headers);

        for (ArrayList<Double> rowValues : allRows) {
            StringBuilder rowBuilder = new StringBuilder();
            for (Double val : rowValues) {
                rowBuilder.append(val);
                rowBuilder.append(",");
            }
            allRowsBuilder.append(rowBuilder.replace(rowBuilder.length() - 1, rowBuilder.length(), "\n").toString());
        }

        String csvAsString = allRowsBuilder.toString();

        try {
            PrintWriter out = new PrintWriter(outFile);
            out.print(csvAsString);
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}
