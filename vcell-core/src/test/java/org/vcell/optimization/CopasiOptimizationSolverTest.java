package org.vcell.optimization;

import cbit.vcell.biomodel.BioModel;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.mapping.SimulationContext;
import cbit.vcell.math.MathException;
import cbit.vcell.modelopt.ParameterEstimationTask;
import cbit.vcell.opt.CopasiOptimizationMethod;
import cbit.vcell.opt.CopasiOptimizationParameter;
import cbit.vcell.opt.OptimizationResultSet;
import cbit.vcell.opt.OptimizationSolverSpec;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.resource.PropertyLoader;
import cbit.vcell.resource.PythonSupport;
import cbit.vcell.resource.VCellConfiguration;
import cbit.vcell.xml.XMLSource;
import cbit.vcell.xml.XmlHelper;
import cbit.vcell.xml.XmlParseException;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vcell.util.ClientTaskStatusSupport;
import org.vcell.util.ProgressDialogListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CopasiOptimizationSolverTest {

    private static File previousPythonExe;

    @BeforeClass
    public static void before(){
        previousPythonExe = VCellConfiguration.getFileProperty(PropertyLoader.pythonExe);
        if (previousPythonExe == null){
            VCellConfiguration.setFileProperty(PropertyLoader.pythonExe,new File(""));
        }
    }

    @AfterClass
    public static void after(){
        VCellConfiguration.setFileProperty(PropertyLoader.pythonExe, previousPythonExe);
    }

    @Test
    public void testLocalOptimization() throws ExpressionException, IOException, XmlParseException, MathException, MappingException {
        String filename = "Biomodel_simple_parest.vcml";
        BioModel bioModel = getBioModelFromResource(filename);
        ParameterEstimationTask parameterEstimationTask = (ParameterEstimationTask) bioModel.getSimulationContext(0).getAnalysisTasks(0);
//        CopasiOptimizationMethod copasiOptimizationMethod = new CopasiOptimizationMethod(CopasiOptimizationMethod.CopasiOptimizationMethodType.ParticleSwarm);
//        CopasiOptimizationParameter iterationLimit = copasiOptimizationMethod.getParameter(CopasiOptimizationParameter.CopasiOptimizationParameterType.IterationLimit);
//        CopasiOptimizationParameter swarmSize = copasiOptimizationMethod.getParameter(CopasiOptimizationParameter.CopasiOptimizationParameterType.Swarm_Size);
//        CopasiOptimizationParameter stdDeviation = copasiOptimizationMethod.getParameter(CopasiOptimizationParameter.CopasiOptimizationParameterType.Std_Deviation);
//        CopasiOptimizationParameter randomNumberGenerator = copasiOptimizationMethod.getParameter(CopasiOptimizationParameter.CopasiOptimizationParameterType.Random_Number_Generator);
//        CopasiOptimizationParameter seed = copasiOptimizationMethod.getParameter(CopasiOptimizationParameter.CopasiOptimizationParameterType.Seed);
//        OptimizationSolverSpec optimizationSolverSpec = new OptimizationSolverSpec(copasiOptimizationMethod);
//        parameterEstimationTask.setOptimizationSolverSpec(optimizationSolverSpec);
        parameterEstimationTask.refreshMappings();

        CopasiOptimizationSolver copasiOptimizationSolver = new CopasiOptimizationSolver();
        ParameterEstimationTaskSimulatorIDA taskSimulatorIDA = new ParameterEstimationTaskSimulatorIDA();
        CopasiOptSolverCallbacks optSolverCallbacks = new CopasiOptSolverCallbacks();
        SimulationContext.MathMappingCallback mathMappingCallback = new TestMathMappingCallback();
        OptimizationResultSet optimizationResultSet = copasiOptimizationSolver.solveLocalPython(
                taskSimulatorIDA, parameterEstimationTask, optSolverCallbacks, mathMappingCallback);
    }

    @Test
    public void testRemoteOptimization() throws ExpressionException, IOException, XmlParseException {
        String filename = "Biomodel_simple_parest.vcml";
        BioModel bioModel = getBioModelFromResource(filename);
        ParameterEstimationTask parameterEstimationTask = (ParameterEstimationTask) bioModel.getSimulationContext(0).getAnalysisTasks(0);

        CopasiOptimizationSolver copasiOptimizationSolver = new CopasiOptimizationSolver();
        ParameterEstimationTaskSimulatorIDA taskSimulatorIDA = new ParameterEstimationTaskSimulatorIDA();
        CopasiOptSolverCallbacks optSolverCallbacks = new CopasiOptSolverCallbacks();
        SimulationContext.MathMappingCallback mathMappingCallback = new TestMathMappingCallback();
        ClientTaskStatusSupport clientTaskStatusSupport = new TestClientTaskStatusSupport();
        OptimizationResultSet optimizationResultSet = copasiOptimizationSolver.solveRemoteApi(
                taskSimulatorIDA, parameterEstimationTask, optSolverCallbacks, mathMappingCallback, clientTaskStatusSupport);
    }

    private static class TestMathMappingCallback implements SimulationContext.MathMappingCallback {
        @Override
        public void setMessage(String message) {
            System.out.println(message);
        }

        @Override
        public void setProgressFraction(float fractionDone) {
            System.out.println("fraction done "+fractionDone);
        }

        @Override
        public boolean isInterrupted() {
            return false;
        }
    }

    private static class TestClientTaskStatusSupport implements ClientTaskStatusSupport {
        @Override
        public void setMessage(String message) {
            System.out.println(message);
        }

        @Override
        public void setProgress(int progress) {
            System.out.println("Progress : "+progress);
        }

        @Override
        public int getProgress() {
            return 0;
        }

        @Override
        public boolean isInterrupted() {
            return false;
        }

        @Override
        public void addProgressDialogListener(ProgressDialogListener progressDialogListener) {
        }
    }

    private static BioModel getBioModelFromResource(String fileName) throws IOException, XmlParseException {
        InputStream inputStream = CopasiOptimizationSolverTest.class.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new FileNotFoundException("file not found! " + fileName);
        } else {
            String vcml = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return XmlHelper.XMLToBioModel(new XMLSource(vcml));
        }
    }


}
