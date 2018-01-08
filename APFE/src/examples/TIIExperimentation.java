/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package examples;

import APFE.Evaluation;
import APFE.WFnet_OP.Marking;
import APFE.WFnet_OP.WFNetOP;
import APFE.data_log.DataLog;
import APFE.data_log.LogEntry;
import APFE.input.models.OPInfo;
import APFE.utils.Triple;
import OP_models.TII_ENGINE_BAY_OVERHEATING;
import business.Global;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author victor
 */
public class TIIExperimentation {

    private static final Logger LOG = Logger.getLogger(TIIExperimentation.class.getName());
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, Exception {
        
        /**
         * Main Arguments: Datalog_filepath [OP_info_filepath]
         */
        
        //Variables
        Long data_log_record_frequency = 4000L; //milliseconds
        
        // Error control for command line arguments
        if (args.length < 1) {
            LOG.log(Level.SEVERE, 
                    "Wrong number of command line arguments (Correct is at least {0})",
                    new Object[]{1});
            return;
        }
        
        //Read the data log
        LOG.log(Level.INFO, "Loading Data log: {0}", args[0]);
        DataLog Delta = DataLog.loadFromXLSX(args[0], "sheet1", "x", "v", "R(x,v)");
        LOG.log(Level.INFO, "Data log successfully loaded");
        
        //Load the corresponding WFNetOP (from package OP_models)
        WFNetOP wfnetOP = new TII_ENGINE_BAY_OVERHEATING();
        
        //Read the OP Info (from a json file)
        if (args.length > 1) {
            OPInfo opInfo = OPInfo.retrieveFromJSON(args[1]);
            wfnetOP.setOpInfo(opInfo);
        }
        
        //Time to start the procedure following evaluation 
        //(Earliest timestamp in the log)
        Long x_0 = Delta.getEntries().stream()
                .mapToLong(LogEntry::getTs)
                .min()
                .orElseThrow(Exception::new);
        
        //Ensure that every variable has been recorded when executing APFE
        x_0 += data_log_record_frequency; 
        
        //Call the APFE algorithm
        Triple<Boolean,Marking,Long> result = Evaluation.APFE(wfnetOP, Delta, x_0);
        
        LOG.log(Level.INFO,
                "APFE algorithm returns : <{0},{1},{2}>",
                new Object[] {
                    result.first,
                    result.second,
                    result.third
                });
        
        LOG.log(Level.INFO, "The experiment has run successfully");
        return;
    }
    
}
