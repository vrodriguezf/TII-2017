/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE;

import APFE.WFnet_OP.WFNetOP;
import APFE.WFnet_OP.MarkingOld;
import APFE.WFnet.WorkflowNet;
import APFE.WFnet.WorkflowSimulation;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author victor
 */
public class ProcedureExecution extends WorkflowSimulation {

    private static final Logger LOG = Logger.getLogger(ProcedureExecution.class.getName());
    
    private WFNetOP procedure;
    private MarkingOld initialMarking;
    
    public ProcedureExecution(WFNetOP procedure, MarkingOld initialMarking) {
        //A procedure execution is a workflow simulation (Thread) not stepped (automatic)
        super((WorkflowNet) procedure);
        
        this.procedure = procedure;
        this.initialMarking = initialMarking;
    }
    
    @Override
    public void start() {
        
        //Configurate the PetriNet properly (initial marking)
        if (initialMarking!= null) {
            this.procedure.setMarking(this.initialMarking);            
        }
        
        //Run the simulation
        LOG.log(Level.INFO,"Running WFNET-OP [" + 
                procedure.getClass().getSimpleName() + 
                "with marking: [" + 
                initialMarking.toString() + 
                "]");
        super.start();
    }
    
    @Override
    public void run() {
        while (!isFinished()) {
            fireTransition();
            //Post fire actions
            super.postFireActions();  
        }
        
        LOG.log(Level.INFO,"Reached Deadlock Marking: {0}",
                procedure.getMarking());
    }
}
