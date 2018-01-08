/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package debriefing;

import APFE.OPTriggering;
import APFE.WFnet_OP.WFNetOP;
import APFE.WFnet_OP.StepType;
import APFE.WFnet_OP.Deadlock;
import APFE.WFnet_OP.Firing;
import APFE.evaluation.EvaluationResult;
import business.Transition;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @deprecated 
 * @author victor
 */
public class Metrics {

    private static final Logger LOG = Logger.getLogger(Metrics.class.getName());
    
    public static final double NON_COMPUTABLE_METRIC = -1;
    
    public static double Reach(EvaluationResult ev, WFNetOP eop) {
        
        //Get the position of the last performed action in the list of 
        //expected actions
        List<Transition> actionTrace = ev.getBaseFiringTrace()
                .stream()
                .map(Firing::getTransition)
                .filter((Transition t) -> eop.getStepType(t) == StepType.ACTION)
                .collect(Collectors.toList());
        
        if (actionTrace.isEmpty()) return 1;
        else {
            List<Firing> performedActions = ev.getExpectedPerformedActions();            
            int maxPerformedActionIndex = performedActions
                    .stream()
                    .map(Firing::getTransition)
                    .mapToInt((Transition t) -> actionTrace.indexOf(t))
                    .max()
                    .orElse(0);
            
            return (maxPerformedActionIndex + 1)/actionTrace.size();
        }
    }
    
    /**
     * Se calcula a partir de duraciones, no de posicion en intervalo temporal
     * @param ev
     * @param eop
     * @param a
     * @return 
     */
    public static double Fastness(EvaluationResult ev, WFNetOP eop, OPTriggering a) throws Exception {        
        
        /*
        List<Firing> performedActions = (List<Firing>) 
                ev.getRightActions()
                .stream()
                .sorted((Firing f1,Firing f2) -> {
                    return (int) (f1.getToken().getTimestamp() - f2.getToken().getTimestamp());
                })
                .collect(Collectors.toList());
        */
        
        List<Firing> performedActions = ev.getExpectedPerformedActions();
        
        //if (orderedRightActions.isEmpty()) return NON_COMPUTABLE_METRIC;
        if (performedActions.isEmpty()) return 0;
        
        long realDuration;
        long stepDurationMean;
        long stepDurationDeviation;
        long distanceToMean;
        List<Double> subResults = new ArrayList<>();
        for (int i=0; i <performedActions.size(); i++) {
            /*
            if (i ==0) {
                realDuration = orderedRightActions.get(i).getToken().getTimestamp() - a.getTriggerTs();
            } else {
                realDuration = orderedRightActions.get(i).getToken().getTimestamp() - 
                        orderedRightActions.get(i-1).getToken().getTimestamp();
            }
            */
            realDuration = performedActions.get(i).getDuration();
            stepDurationMean = eop.getStepDurationMean(performedActions.get(i).getTransition());
            stepDurationDeviation = eop.getStepDurationDeviation(performedActions.get(i).getTransition());
            distanceToMean = Math.min(
                    Math.abs(realDuration - stepDurationMean),
                    stepDurationDeviation
            );
            
            if (realDuration < stepDurationMean) 
                subResults.add(0.5 + 0.5*(distanceToMean/stepDurationDeviation));
            else
                subResults.add(0.5 - 0.5*(distanceToMean/stepDurationDeviation));
        }
        
        return subResults
                .stream()
                .mapToDouble(x->x)
                .average()
                .orElse(NON_COMPUTABLE_METRIC);
    }

    /**
     * 
     * @param ev
     * @param eop
     * @param wf
     * @return 
     */
    public static double InformationOrdering(EvaluationResult ev, WFNetOP eop) {
        
        /* Retrieve the action base trace */
        List<Firing> baseActionTrace = ev
                .getBaseFiringTrace()
                .stream()
                .filter((Firing f) -> eop.getStepType(f.getTransition()) == StepType.ACTION )
                .collect(Collectors.toList());
        
        List<Transition> sequentialMismatches = ev.getSequentialMismatches()
                .stream()
                .map(Deadlock::getLockingTransition)
                .collect(Collectors.toList());
        
        List<Double> subResults = new ArrayList<>();
        int mismatchCount = 0;
        for (int i=0; i< baseActionTrace.size(); i++) {
            mismatchCount = 0;
            //Veo todo lo que ha pasado antes en el log, pero que sin embargo tiene 
            //un timestamp mayor que el de este sequential mismatch
            if (sequentialMismatches.contains(baseActionTrace.get(i).getTransition())) {
                for (int j=0; j <i; j++) {
                    if (baseActionTrace.get(j).getToken().getTimestamp() > 
                            baseActionTrace.get(i).getToken().getTimestamp()) {
                        mismatchCount++;
                    }
                }
                subResults.add(1 - ((double) mismatchCount)/baseActionTrace.size());
            }
        }
        
        //Resultado final: Productorio de mismatches (cuantos mas peor)
        return subResults
                .stream()
                .reduce(new Double(1),(a,b) -> a*b);
    }    
    
    /**
     * 
     * @param ev
     * @param eop
     * @return 
     */
    public static double Precision(EvaluationResult ev, WFNetOP eop) {
        int RA = ev.getRightActions().size();
        int SM = ev.getSequentialMismatches().size();
        int OR = ev.getOverreactions().size();
        
        if (RA == 0 && SM == 0 && OR == 0) return 1;
        
        return (double) (RA + SM)/(RA + SM + OR);
    }
}
