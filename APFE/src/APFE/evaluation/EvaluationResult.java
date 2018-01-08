/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.evaluation;

import APFE.Operation;
import APFE.Operator;
import APFE.ProcedureExecution;
import APFE.WFnet_OP.Deadlock;
import APFE.WFnet_OP.Firing;
import business.Transition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author victor
 */
public class EvaluationResult {
    private String id;
    private Operation operation;
    private Operator operator;
    private Collection<Deadlock> sequentialMismatches;
    private Collection<Deadlock> missingActions;
    private Collection<Firing> rightActions;
    private Collection<MissingCheck> missingChecks;
    private Collection<Overreaction> overreactions;
    private ArrayList<ArrayList<ProcedureExecution>> forwardExecutionTraces;
    
    /**
     * Retrieve one EvResult from DB
     * @param id
     * @return 
     */
    public static EvaluationResult retrieveFromId(String id) {
        return null;
    }
    

    public EvaluationResult(Collection<Deadlock> sequentialMismatches, Collection<Deadlock> missingActions) {
        this.sequentialMismatches = sequentialMismatches;
        this.missingActions = missingActions;
    }

    public EvaluationResult() {
        sequentialMismatches = new ArrayList<>();
        missingActions = new ArrayList<>();
        rightActions = new ArrayList<>();
        missingChecks = new ArrayList<>();
        overreactions = new ArrayList<>();
        forwardExecutionTraces = new ArrayList<>();
    }
    
    /**
     * RA + SM + OR
     * @return 
     */
    public List<Firing> getPerformedActions() {
        List<Firing> result = new ArrayList<>();
        result.addAll(getRightActions());
        result.addAll(
            getSequentialMismatches()
                .stream()
                .map(Deadlock::getLiberatorFiring)
                .collect(Collectors.toList())
        );
        result.addAll(
                getOverreactions()
                .stream()
                .map(Overreaction::getAction)
                .collect(Collectors.toList())
        );
        
        return result;
    }
    
    /**
     * RA + SM + MA
     * @return 
     */
    public List<Transition> getExpectedActions() {
        List<Transition> result = new ArrayList<>();
        
        result.addAll(
                getRightActions()
                        .stream()
                        .map(Firing::getTransition)
                        .collect(Collectors.toList())
        );
        result.addAll(
            getSequentialMismatches()
                .stream()
                .map(Deadlock::getLockingTransition)
                .collect(Collectors.toList())
        );       
        result.addAll(
            getMissingActions()
                .stream()
                .map(Deadlock::getLockingTransition)
                .collect(Collectors.toList())
        );
        return result;
    }
    
    /**
     * RA + SM
     * @return 
     */
    public List<Firing> getExpectedPerformedActions() {
        List<Firing> result = new ArrayList<>();
        result.addAll(getRightActions());
        result.addAll(
            getSequentialMismatches()
                .stream()
                .map(Deadlock::getLiberatorFiring)
                .collect(Collectors.toList())
        );
        
        return result;
    }    
    
    /**
     * 
     * @return 
     */
    public List<Firing> getBaseFiringTrace() {
        List<Firing> result = new ArrayList<>();
        
        getForwardExecutionTraces().get(0).forEach((ProcedureExecution pe) -> {
            result.addAll(pe.getFiringTrace());
        });
        
        return result;
    }
    
    /**
     * GETTERs & SETTERs
     */
    public Collection<Deadlock> getSequentialMismatches() {
        return sequentialMismatches;
    }

    public Collection<Deadlock> getMissingActions() {
        return missingActions;
    }

    public Collection<MissingCheck> getMissingChecks() {
        return missingChecks;
    }

    public Collection<Overreaction> getOverreactions() {
        return overreactions;
    }

    public Collection<Firing> getRightActions() {
        return rightActions;
    }

    public void setRightActions(Collection<Firing> performedActions) {
        this.rightActions = performedActions;
    }

    public void setSequentialMismatches(Collection<Deadlock> sequentialMismatches) {
        this.sequentialMismatches = sequentialMismatches;
    }

    public void setMissingActions(Collection<Deadlock> missingActions) {
        this.missingActions = missingActions;
    }

    public void setMissingChecks(Collection<MissingCheck> missingChecks) {
        this.missingChecks = missingChecks;
    }

    public void setOverreactions(Collection<Overreaction> overreactions) {
        this.overreactions = overreactions;
    }

    public ArrayList<ArrayList<ProcedureExecution>> getForwardExecutionTraces() {
        return forwardExecutionTraces;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
