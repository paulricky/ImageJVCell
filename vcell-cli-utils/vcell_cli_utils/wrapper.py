__package__ = "vcell_cli_utils"
import os
from deprecated import deprecated
if __name__ == "__main__":
    from vcell_cli_utils import cli, status
else:
    from . import cli, status

# Since file is a wrapper access point for a java program, functions will use *java* name-style conventions (camel case)

def genSedml2d3d(omexFilePath : str, baseOutPath : str) -> None:
    cli.gen_sedml_2d_3d(omexFilePath, baseOutPath)

def genPlotsPseudoSedml(sedmlPath : str, resultOutDir : str) -> None:
    cli.gen_plots_for_sed2d_only(sedmlPath, resultOutDir)

@deprecated("This method is no longer used") 
def execSedDoc(omexFilePath : str, baseOutPath : str) -> None:
    cli.exec_sed_doc(omexFilePath, baseOutPath)

def transposeVcmlCsv(csvFilePath: str) -> None:
    cli.transpose_vcml_csv(csvFilePath)

def genPlotPdfs(sedmlPath : str, resultOutDir : str) -> None:
    cli.gen_plot_pdfs(sedmlPath, resultOutDir)

def genStatusYaml(omexFile: str, outDir: str) -> None:
    status.status_yml(omexFile, outDir)

def updateTaskStatus(sedml: str, task: str, statusVar: str, outDir: str, duration: str, algorithm: str) -> None:
    status.update_task_status(sedml, task, statusVar, outDir, int(duration), algorithm)

def updateSedmlDocStatus(sedml: str, statusVar: str, outDir: str) -> None:
    status.update_sedml_doc_status(sedml, statusVar, outDir)

def updateOmexStatus(statusVar: str, outDir: str, duration: str) -> None:
    status.update_omex_status(statusVar, outDir, int(duration))

def updateDataSetStatus(sedml: str, report: str, dataset: str, statusVar: str, outDir: str) -> None:
    status.update_dataset_status(sedml, report, dataset, statusVar, outDir)

def updatePlotStatus(sedml: str, plot_id: str, statusVar: str, out_dir: str) -> None:
    status.update_plot_status(sedml, plot_id, statusVar, out_dir)

def setOutputMessage(sedmlAbsolutePath:str, entityId:str, outDir:str, entityType:str , message:str) -> None:
    status.set_output_message(sedmlAbsolutePath, entityId, outDir, entityType, message)

def setExceptionMessage(sedmlAbsolutePath:str, entityId:str, outDir:str, entityType:str, type:str, message:str) -> None:
    status.set_exception_message(sedmlAbsolutePath, entityId, outDir, entityType, type, message)