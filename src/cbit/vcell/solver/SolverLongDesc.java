package cbit.vcell.solver;

/**
 * long descriptions of {@link SolverDescription}s
 *
 */
interface SolverLongDesc {
	 static final String Description_Start_Time = "<b>Starting Time</b>";
	 static final String Description_End_Time = "<b>Ending Time</b>";
	 static final String Description_TimeStep = "<b>Time Step</b>";
	 static final String Description_TimeStep_Default = "<b>Default:</b> the time step to numerically solve ODEs/PDEs.";
	 static final String Description_TimeStep_Min = "<b>Minimum:</b> the minimum time stepsize that the solver should attempt to use.";
	 static final String Description_TimeStep_Max = "<b>Maximum:</b> the maxmum time stepsize that the solver should attempt to use.";
	 static final String Description_ErrorTolerance = "<b>Error Tolerance</b>";
	 static final String Description_ErrorTolerance_Abs = "<b>Absolute:</b> the solver adjusts the stepsize in such a way " +
			"as to keep the absolute error in a step less than absolute tolerance.";
	 static final String Description_ErrorTolerance_Rel = "<b>Relative:</b> the solver adjusts the stepsize in such a way " +
			"as to keep the fractional error in a step less than relative tolerance.";
	 static final String Description_ErrorTolerance_LinearSolverRel = "<b>Linear Solver Tolerance:</b> the tolerance used to test for " +
			"convergence of the iteration. The iteration is considered to have converged when the size of residual is less than or " +
			"equal to the tolerance.";
	 static final String Description_OutputOptions = "<b>Output Options</b>";
	 static final String Description_OutputOptions_KeepEvery_ODE = "<b>Keep Every <i>N</i> / At Most <i>M</i>: </b> based on solver's " +
			"internal time step, saves solution at every <i>N</i><sup>th</sup> time step but at most <i>M</i> saved time points. " +
			"If exceeds <i>M</i> saved time points, attempts to remove nearly colinear saved time points.";
	 static final String Description_OutputOptions_KeepEvery_PDE = "<b>Keep Every <i>N</i> / At Most <i>M</i>: </b> based on solver's " +
			"internal time step, saves solution at every <i>N</i><sup>th</sup> time step and terminates if exceeds <i>M</i> saved time points.";
	 static final String Description_OutputOptions_KeepEvery_Gibson = "<b>Keep Every <i>N</i>:</b> based on solver's " +
			"internal time step, saves solution at every <i>N</i><sup>th</sup> time step.";
	 static final String Description_OutputOptions_OutputInterval = "<b>Output Interval:</b> uniformly sampled time points.";
	 static final String Description_OutputOptions_OutputTimes = "<b>Output Times:</b> explicit output time points.";

	 static final String Description_Stochastic_MSR_TOLERANCE = "<b>MSR Tolerance:</b> maximum allowed effect of executing multiple " +
			"slow reactions per numerical integration of the SDEs.";
	 static final String Description_Stochastic_LAMBDA = "<b>Lambda:</b> minimum rate of reaction required for approximation to a " +
			"continuous Markov process.";
	 static final String Description_Stochastic_EPSILON = "<b>Epsilon:</b> minimum number of molecules both reactant and product " +
			"species required for approximation as a continuous Markov process.";
	 static final String Description_Stochastic_NUMBER_OF_TRIALS = "<b>Number of Trials:</b> the number of multiple trials.";
	 static final String Description_Stochastic_CUSTOMIZED_SEED = "<b>Customized Seed:</b> a user specified number, which is used to " +
			"produce a series of uniformly distributed random numbers.";
	 static final String Description_Stochastic_RANDOM_SEED = "<b>Random Seed:</b> a random number generated by PC time, which is used to " +
			"produce a series of uniformly distributed random numbers.";
	 static final String Description_Stochastic_SDE_TOLERANCE = "<b>SDE Tolerance:</b> maximum allowed value of the drift and diffusion errors.";
	 static final String Description_Stochastic_DEFAULT_TIME_STEP = "<b>Default:</b> the maximum time step of the SDE numerical integrator.";
	 static final String Description_Stochastic_DEFAULT_TIME_STEP_Adaptive = "<b>Default:</b> the initial time step of the SDE numerical integrator. " +
			"It may be set for adaptive methods to decrease memory requirements.";
	 static final String Description_StochasticOptions = "<b>Stochastic Options</b>";
	 static final String Description_MaxBoxSize = "<b>Max Box Size:</b> Maximum allowable box size (0 means no limit).";
	 static final String Description_FillRatio = "<b>Fill Ratio:</b> Measure of how densely cells tagged for refinement will be covered by boxes in each refinement level.";
	 static final String Description_EBChombo_Mesh_Options_Advanced = "<b>EBChombo Mesh Options (advanced)</b>: Chombo breaks up the computational grid into sub-grids or boxes (especially for mesh refinement and parallel computation). Parameters can affect solver performance, but should not affect the results.</u>";
	 static final String Description_TimeBounds = "<b>Time Bounds</b>: Describe one or more intervals of integration and output options. For each interval, enter:";
	 static final String Description_TimeBounds_Details = "<ul>"
	 		+ "<li><b>Starting Time</b> of the interval</li>"
	 		+ "<li><b>Ending Time</b> of the interval</li>"
	 		+ "<li><b>Time Step</b> to numerically solve ODEs/PDEs</li>"
	 		+ "<li><b>Output Interval</b>: write results at uniformly sampled time points within the interval (must be a multiple of time step)</li>"
	 		+ "</ul>";

	 static final String Description_PARAMETERS_TO_BE_SET = "<p><u><b>Input Parameters:<b></u>";
	 static final String Description_REFERENCES = "<u><b>References:<b></u>";
	 static final String DISPLAY_LABEL = "";

    static final String FORWARD_EULER = 
		"<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
		"Forward Euler method is a fixed time step mehtod to solve ordinary differential equations. "+
		Description_PARAMETERS_TO_BE_SET +
		"<ul>"+
		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_TimeStep +
			"<ul>"+
			"<li>" + Description_TimeStep_Default + "</li>" +
			"</ul></li>"+
		"<li>" + Description_OutputOptions +
 			"<ul>" +
 			"<li>" + Description_OutputOptions_KeepEvery_ODE + "</li>" +
 			"</ul></li>"
		+ "</ul>"
	     +"</html>";

    static final String RUNGE_KUTTA2 = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
		"The Runge Kutta methods provide further systematic improvement in the spirit of the modified Euler method. " +
		"Second order fixed time step method is also called the midpoint method."+
		 Description_PARAMETERS_TO_BE_SET+
			"<ul>"+
			"<li>" + Description_Start_Time + "</li>"+
			"<li>" + Description_End_Time + "</li>"+
			"<li>" + Description_TimeStep +
				"<ul>"+
				"<li>" + Description_TimeStep_Default + "</li>" +
				"</ul></li>"+
			"<li>" + Description_OutputOptions +
	 			"<ul>" +
	 			"<li>" + Description_OutputOptions_KeepEvery_ODE + "</li>" +
	 			"</ul></li>"
			+ "</ul>"
	     +"</html>";

    static final String RUNGE_KUTTA4 = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
	     "The Runge Kutta methods provide further systematic improvement in the spirit of the modified Euler method. " +
	     "the fourth order Runge Kutta method does four function evaluations per step to give a method with fourth order accuracy. It is a fixed time step method."+
			Description_PARAMETERS_TO_BE_SET +
			"<ul>"+
			"<li>" + Description_Start_Time + "</li>"+
			"<li>" + Description_End_Time + "</li>"+
			"<li>" + Description_TimeStep +
				"<ul>"+
				"<li>" + Description_TimeStep_Default + "</li>" +
				"</ul></li>"+
			"<li>" + Description_OutputOptions +
	 			"<ul>" +
	 			"<li>" + Description_OutputOptions_KeepEvery_ODE + "</li>" +
	 			"</ul></li>"
			+ "</ul>"
	     +"</html>";

    static final String RUNGE_KUTTA_FEHLBERG = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
	     "The Runge-Kutta-Fehlberg integrator is primarily designed to solve non-stiff and " +
	     "mildly stiff differential equations when derivative evaluations are inexpensive. It should generally " +
	     "not be used when the user is demanding high accuracy. It is a variable time step method."+
		 Description_PARAMETERS_TO_BE_SET+
		 "<ul>"+
		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_TimeStep +
			"<ul>"+
			"<li>" + Description_TimeStep_Min + "</li>"+
			"<li>" + Description_TimeStep_Max + "</li>"+
			"</ul></li>"+
		"<li>" + Description_ErrorTolerance +
			"<ul>"+
			"<li>" + Description_ErrorTolerance_Abs + "</li>"+
			"<li>" + Description_ErrorTolerance_Rel + "</li>"+
			"</ul></li>"+
		"<li>" + Description_OutputOptions +
			"<ul>" +
			"<li>" + Description_OutputOptions_KeepEvery_ODE + "</li>" +
			"</ul></li>"
			+ "</ul>"
	     +"</html>";

    static final String ADAMS_MOULTON = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
	     "The methods such as Foward Euler, Runge-Kutta etc. are called single-step methods because they use only the information from one previous point to compute the successive point. Adams-Moulton methods are explicit linear multistep methods that depend on multiple previous solution points to generate a new approximate solution point. It is a fixed time step method.\n\n"+
		 Description_PARAMETERS_TO_BE_SET+
		 "<ul>"+
			"<li>" + Description_Start_Time + "</li>"+
			"<li>" + Description_End_Time + "</li>"+
			"<li>" + Description_TimeStep +
				"<ul>"+
				"<li>" + Description_TimeStep_Default + "</li>" +
				"</ul></li>"+
			"<li>" + Description_OutputOptions +
	 			"<ul>" +
	 			"<li>" + Description_OutputOptions_KeepEvery_ODE + "</li>" +
	 			"</ul></li>"
			+ "</ul>"
	     +"</html>";

    static final String IDA = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
	     "IDA addresses systems of differential-algebraic equations (DAEs), and uses Backward Differentiation Formula methods. ODEs are a subset of DAEs, therefore IDA may be used for solving ODEs. \n\n"+
	     Description_PARAMETERS_TO_BE_SET +
		 "<ul>"+
		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_TimeStep +
			"<ul>"+
			"<li>" + Description_TimeStep_Max + "</li>" +
			"</ul></li>"+
		"<li>" + Description_ErrorTolerance +
			"<ul>"+
			"<li>" + Description_ErrorTolerance_Abs + "</li>"+
			"<li>" + Description_ErrorTolerance_Rel + "</li>"+
			"</ul></li>"+
		"<li>" + Description_OutputOptions +
 			"<ul>" +
 			"<li>" + Description_OutputOptions_KeepEvery_ODE + "</li>" +
 			"<li>" + Description_OutputOptions_OutputInterval + "</li>" +
 			"<li>" + Description_OutputOptions_OutputTimes + "</li>" +
 			"</ul></li>"
	     +"</ul>" +
	     "</html>";

    static final String FINITE_VOLUME = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
	     "The finite volume method is a method for representing and evaluating partial differential equations as algebraic discretization equations which exactly preserves conservation laws. Similar to the finite difference method, values are calculated at discrete places on a meshed geometry.\n\n"+
	     Description_PARAMETERS_TO_BE_SET +
	     "<li>" + Description_Start_Time + "</li>"+
			"<li>" + Description_End_Time + "</li>"+
			"<li>" + Description_TimeStep +
				"<ul>"+
				"<li>" + Description_TimeStep_Default + "</li>" +
				"</ul></li>"+
		     "<li>" + Description_ErrorTolerance_LinearSolverRel + "</li>" +
		     "<li>" + Description_OutputOptions +
		     	"<ul>" +
	  			"<li>" + Description_OutputOptions_OutputInterval + "</li>" +
	  			"</ul></li>"
	  		 + "</ul>"
	     +"</html>";

    static final String STOCH_GIBSON = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
	     "Gibson-Bruck is an improved exact stochastic method based on Gllespie's SSA. It uses only a single random " +
	     "number per simulation event and takes time proportional to the logarithm of the number of reactions. Better " +
	     "performance is also acheived by utilizing a dependency graph and an indexed priority queue."+
	     Description_PARAMETERS_TO_BE_SET +
	     "<ul>" +
		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_StochasticOptions + 
			"<ul>" +
			"<li>" + Description_Stochastic_RANDOM_SEED+ "</li>"+
			"<li>" + Description_Stochastic_CUSTOMIZED_SEED+ "</li>"+
			"<li>" + Description_Stochastic_NUMBER_OF_TRIALS+ "</li>"+
			"</ul></li>" +
		"<li>" + Description_OutputOptions +
			"<ul>" +
			"<li>" + Description_OutputOptions_KeepEvery_Gibson + "</li>" +
			"<li>" + Description_OutputOptions_OutputInterval + "</li>" +
			"</ul></li>" +
         "</ul>" +
         Description_REFERENCES+
         "<ul>" +         
         "<li>M.A.Gibson and J.Bruck,'Efficient Exact Stochastic Simulation of Chemical Systems with Many Species and " +
         "Many Channels', J. Phys. Chem. 104, 1876(2000).</li>" +
         "</ul>"
	     +"</html>";	     

    static final String HYBRID_EULER = 
         "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
         "This is a hybrid stochastic method. It partitions the system into subsets of fast and slow reactions and " +
         "approximates the fast reactions as a continuous Markov process, using a chemical Langevin equation, and " +
         "accurately describes the slow dynamics using the Gibson algorithm. Fixed time step Euler-Maruyama is used " +
         "for approximate numerical solution of CLE."+
         Description_PARAMETERS_TO_BE_SET +
 		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_TimeStep +
			"<ul>"+
			"<li>" + Description_Stochastic_DEFAULT_TIME_STEP + "</li>" +
			"</ul></li>"+
		"<li>" + Description_StochasticOptions + 
			"<ul>" +			
			"<li>" + Description_Stochastic_RANDOM_SEED+"</li>" +
			"<li>" + Description_Stochastic_CUSTOMIZED_SEED+"</li>" +
			"<li>" + Description_Stochastic_NUMBER_OF_TRIALS+"</li>" +
			"<li>" + Description_Stochastic_EPSILON+"</li>" +
			"<li>" + Description_Stochastic_LAMBDA+"</li>" +
			"<li>" + Description_Stochastic_MSR_TOLERANCE+"</li>" +
			"</ul></li>" +
		"<li>" + Description_OutputOptions +
			"<ul>" +
			"<li>" + Description_OutputOptions_OutputInterval + "</li>" +
			"</ul></li>" +
		"</ul>" +
         Description_REFERENCES+
         "<ul>" +         
         "<li>H.Salis and Y.Kaznessis,'Accurate hybrid stochastic simulation of a system of coupled chemical or biochemical reactions', " +
         	"J. Chem. Phys. 122, 054103(2005).</li>"+
         "<li>H.Salis, V. Sotiropoulos and Y. Kaznessis,'Multiscale Hy3S: Hybrid stochastic simulation for supercomputers', " +
         "	BMC Bioinformatics 7:93(2006).</li>"
         +"</ul>"
	     +"</html>";

    static final String HYBRID_MILSTEIN = 
         "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
         "This is a hybrid stochastic method. It partitions the system into subsets of fast and slow reactions and " +
         "approximates the fast reactions as a continuous Markov process, using a chemical Langevin equation, and accurately describes " +
         "the slow dynamics using the Gibson algorithm. Fixed time step Milstein is used for approximate numerical solution of CLE.\n\n"+
         Description_PARAMETERS_TO_BE_SET +
 		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_TimeStep +
			"<ul>"+
			"<li>" + Description_Stochastic_DEFAULT_TIME_STEP + "</li>" +
			"</ul></li>"+
		"<li>" + Description_StochasticOptions + 
			"<ul>" +			
			"<li>" + Description_Stochastic_RANDOM_SEED + "</li>" +
			"<li>" + Description_Stochastic_CUSTOMIZED_SEED+ "</li>" +
			"<li>" + Description_Stochastic_NUMBER_OF_TRIALS+"</li>" +
			"<li>" + Description_Stochastic_EPSILON+"</li>" +
			"<li>" + Description_Stochastic_LAMBDA+"</li>" +
			"<li>" + Description_Stochastic_MSR_TOLERANCE+"</li>" +
			"</ul></li>" +
	     "<li>" + Description_OutputOptions +
			"<ul>" +
			"<li>" + Description_OutputOptions_OutputInterval + "</li>" +
			"</ul></li>" +
		"</ul>" +			
		Description_REFERENCES+
		"<ul>" +
         "<li>H.Salis and Y.Kaznessis,'Accurate hybrid stochastic simulation of a system of coupled " +
         	"chemical or biochemical reactions', J. Chem. Phys. 122, 054103(2005).</li>"+
         "<li>H.Salis, V. Sotiropoulos and Y. Kaznessis,'Multiscale Hy3S: Hybrid stochastic simulation for supercomputers', BMC Bioinformatics 7:93(2006).</li>"
	     +"</html>";         

    static final String HYBRID_MIL_ADAPTIVE = 
         "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
         "This is a hybrid stochastic method. It partitions the system into subsets of fast and slow reactions and approximates " +
         "the fast reactions as a continuous Markov process, using a chemical Langevin equation, and accurately describes " +
         "the slow dynamics using the Gibson algorithm. Adaptive time step Milstein is used for approximate numerical solution of CLE."+
         Description_PARAMETERS_TO_BE_SET +
 		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_TimeStep +
			"<ul>"+
			"<li>" + Description_Stochastic_DEFAULT_TIME_STEP_Adaptive + "</li>" +
			"</ul></li>"+
		"<li>" + Description_StochasticOptions + 
			"<ul>" +
			"<li>" + Description_Stochastic_RANDOM_SEED+"</li>" +
			"<li>" + Description_Stochastic_CUSTOMIZED_SEED+"</li>" +
			"<li>" + Description_Stochastic_NUMBER_OF_TRIALS+"</li>" +
			"<li>" + Description_Stochastic_EPSILON+"</li>" +
			"<li>" + Description_Stochastic_LAMBDA+"</li>" +
			"<li>" + Description_Stochastic_MSR_TOLERANCE+"</li>" +
			"<li>" + Description_Stochastic_SDE_TOLERANCE+"</li>" +
			"</ul></li>" +
	     "<li>" + Description_OutputOptions +
			"<ul>" +
			"<li>" + Description_OutputOptions_OutputInterval + "</li>" +
			"</ul></li>" +
		"</ul>" +						
		Description_REFERENCES+
		"<ul>" +
         "<li>H.Salis and Y.Kaznessis,'Accurate hybrid stochastic simulation of a system of coupled chemical or biochemical " +
         	"reactions', J. Chem. Phys. 122, 054103(2005).</li>"+
         "<li>H.Salis, V. Sotiropoulos and Y. Kaznessis,'Multiscale Hy3S: Hybrid stochastic simulation for supercomputers', " +
         	"BMC Bioinformatics 7:93(2006).</li>" +
         "</ul>"
	     +"</html>";         

    static final String CVODE = 
         "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
         "CVODE is used for solving initial value problems for ordinary differential equations. It solves both stiff and nonstiff " +
         "systems, using variable-coefficient Adams and BDF methods. In the stiff case, options for treating the Jacobian of the system " +
         "include dense and band matrix solvers, and a preconditioned Krylov (iterative) solver. In the highly modular organization of CVODE, " +
         "the core integrator module is independent of the linear system solvers, and all operations on N-vectors are isolated in a module of vector kernels."+
         Description_PARAMETERS_TO_BE_SET +
 		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_TimeStep +
			"<ul>"+
			"<li>" + Description_TimeStep_Max + "</li>" +
			"</ul></li>"+
		"<li>" + Description_ErrorTolerance +
			"<ul>"+
			"<li>" + Description_ErrorTolerance_Abs + "</li>"+
			"<li>" + Description_ErrorTolerance_Rel + "</li>"+
			"</ul></li>"+
	     "<li>" + Description_OutputOptions +
			"<ul>" +
			"<li>" + Description_OutputOptions_KeepEvery_ODE + "</li>" +
			"<li>" + Description_OutputOptions_OutputInterval + "</li>" +
			"<li>" + Description_OutputOptions_OutputTimes + "</li>" +
			"</ul></li>"
		 + "</ul>"
	     +"</html>";

    static final String FINITE_VOLUME_STANDALONE = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
	     "This is our interpreted standalone version of the finite volume method. It is a little slower but gives better error messages." +
	     "The finite volume method is a method for representing and evaluating partial differential equations as algebraic discretization " +
	     "equations which exactly preserves conservation laws. Similar to the finite difference method, values are calculated at discrete " +
	     "places on a meshed geometry."+
	     Description_PARAMETERS_TO_BE_SET +
		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_TimeStep +
			"<ul>"+
			"<li>" + Description_TimeStep_Default + "</li>" +
			"</ul></li>"+
	     "<li>" + Description_ErrorTolerance_LinearSolverRel + "</li>" +
	     "<li>" + Description_OutputOptions +
	     	"<ul>" +
  			"<li>" + Description_OutputOptions_OutputInterval + "</li>" +
  			"</ul></li>"
  		 + "</ul>"	
	     +"</html>";

    static final String COMBINED_SUNDIALS = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" 
	     + "This chooses between IDA and CVODE depending on the problem to be solved. <br>" 
	     + "<ul>" 
	     + "<li><b>CVODE</b> is used for ordinary differential equation (ODE) systems;</li>" 
	     + "<li><b>IDA</b> is used for differential-algebraic equation (DAE) systems.</li>" 
	     + "</ul>" 
	     + "VCell models with fast reactions (i.e. fast systems) are DAE systems. "
	     +"</html>";

    static final String SUNDIALS_PDE = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>"
	     + "This is our fully implicit, adaptive time step finite volume method. The finite volume method " 
	     + "represents partial differential equations as algebraic discretization equations which exactly preserves conservation laws. " 
	     + "Similar to the finite difference method, values are calculated at discrete places on a meshed geometry.\n\n"
	     + "This method employs Sundials stiff solver CVODE for time stepping (method of lines). " 
	     + "Please note that relative and absolute tolerances affect the accuracy of time descritization only, therefore spatial discritization " 
	     + "is the only significant source of solution error." +
	     Description_PARAMETERS_TO_BE_SET +
	     "<ul>" +
		"<li>" + Description_Start_Time + "</li>"+
		"<li>" + Description_End_Time + "</li>"+
		"<li>" + Description_ErrorTolerance +
			"<ul>"+
			"<li>" + Description_ErrorTolerance_Abs + "</li>"+
			"<li>" + Description_ErrorTolerance_Rel + "</li>"+
			"</ul></li>" +
		"<li>" + Description_OutputOptions +
	     		"<ul>" +
	     		"<li>" + Description_OutputOptions_KeepEvery_PDE + "</li>" +
	     		"<li>" + Description_OutputOptions_OutputInterval + "</li>" +
	     		"</ul></li>"
	     + "</ul>"
	     +"</html>";

    static final String SMOLDYN = 
	     "<html>"  +
	     "Smoldyn is a computer program for cell-scale biochemical simulations. " +
	     "It simulates each molecule of interest individually to capture natural stochasticity " +
	     "and for nanometer-scale spatial resolution. It treats other molecules implicity, " +
	     "so it can simulate tens of thousands of molecules over several minutes of real time. " +
	     "Simulated molecules diffuse, react, are confined by surfaces, and bind to membranes " +
	     "much as they would in a real biological system."
	     + "</html>";

    static final String CHOMBO = 
	     "<html>"
	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
	     "Chombo provides a set of tools for implementing finite difference methods for the solution of " +
	     "partial differential equations on block-structured adaptively refined rectangular grids. Both elliptic " +
	     "and time-dependent modules are included. Chombo supports calculations in complex geometries with both " +
	     "embedded boundaries and mapped grids, and Chombo also supports particle methods. Most parallel platforms " +
	     "are supported, and cross-platform self-describing file formats are included."
	     + Description_PARAMETERS_TO_BE_SET +
	     "<ul>" +
				"<li>" + Description_TimeBounds + "<br>"
				+ Description_TimeBounds_Details
				 + "</li>"
	  		 + "<li>" + Description_EBChombo_Mesh_Options_Advanced +
	  		 "<ul>" +
		     "<li>" + Description_MaxBoxSize + "</li>" +
		     "<li>" + Description_FillRatio + "</li>"
		     + "</ul>"
		     + "</li>"
		     + "</ul>"
	     + "</html>";
    
    static final String NFSIM =
   	     "<html>"
   	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
   	     "NFsim is a free, open-source, biochemical reaction simulator designed to handle systems" +
   	     " that have a large or even infinite number of possible molecular interactions or states." +
   	     " NFsim also has advanced and flexible options for simulating coarse-grained representations" +
   	     " of complex nonlinear reaction mechanisms. A publication describing NFsim can be found at" +
   	     " <a href='http://www.nature.com/nmeth/journal/v8/n2/full/nmeth.1546.html'>http://www.nature.com/nmeth/journal/v8/n2/full/nmeth.1546.html</a>"
   	     + Description_PARAMETERS_TO_BE_SET +
   			"<li>" + Description_Stochastic_RANDOM_SEED + "</li>"
   	     + "</html>";
    
    static final String MB =
   	     "<html>"
   	     + "<center><h3>DISPLAY_LABEL_TOKEN</h3></center>" +
   	    		 "Moving Boundary is a spatial solver which handles Moving Boundaries"
	     + "</html>";
    		
}