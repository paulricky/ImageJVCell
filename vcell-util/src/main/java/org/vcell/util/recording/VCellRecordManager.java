package org.vcell.util.recording;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class VCellRecordManager implements RecordManager {
    protected static VCellRecordManager instance;
    protected Map<String,Record> logMap; 

    public static RecordManager getInstance(){
        return VCellRecordManager.instance == null ? instance = new VCellRecordManager() : VCellRecordManager.instance;
    }

    protected VCellRecordManager(){
        this.logMap = new HashMap<>();
    }

    @Override
    public FileRecord requestNewFileLog(String filePath){
        // Make parent if it doesn't exist
        File parent = (new File(filePath)).getParentFile();
        if (!parent.exists())parent.mkdirs();
        
        if (this.logMap.containsKey(filePath)) 
            return (FileRecord)this.logMap.get(filePath);
        return (FileRecord)this.addLog(filePath, new FileRecord(filePath));
    }

    // Note: uses AutoClose
    public void close() throws IOException {
        for (Record l : this.logMap.values()) 
            l.close();
        logMap.clear();
    }

    protected Record addLog(String name, Record log){
        this.logMap.put(name, log);
        return log;
    }
}
