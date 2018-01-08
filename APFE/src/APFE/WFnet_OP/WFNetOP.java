/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet_OP;

import APFE.OPResponse;
import APFE.input.models.OPInfo;
import APFE.constants.Constants;
import APFE.WFnet.WorkflowNet;
import APFE.data_log.DataLog;
import APFE.data_log.LogEntry;
import APFE.utils.Helpers;
import APFE.utils.SetOperations;
import business.Place;
import business.Token;
import business.Transition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author victor
 */
public class WFNetOP extends WorkflowNet {

    private static final Logger LOG = Logger.getLogger(WFNetOP.class.getName());

    public WFNetOP() {
    }

    public WFNetOP(DataLog dataLog) {
        this.dataLog = dataLog;
    }
    
    /**
     * 
     * @return Estimated Procedure Duration (seconds)
     */
    public long getEstimatedDuration() {
        return 15000;
    }
    
    /**
     * Create initial marking of empty tokens (no object associated)
     * @param initialTimestamps
     * @return 
     */
    public Marking createInitialMarking(Long[] initialTimestamps) {
        Marking m0;
        m0 = new Marking();
        
        for (Long ts : initialTimestamps) {
            m0.add(getInputPlace(), new Token(null,ts));
        }
        
        return m0;
    }
    
    /**
     * 
     * @param responses
     * @return 
     * @deprecated
     */
    public MarkingOld createInitialMarking(Collection<OPResponse> responses) {
        MarkingOld m0;
        m0 = new MarkingOld();
        
        for (OPResponse response: responses) {
            m0.add(getInputPlace(), new Token(response,response.OPTriggering.getTriggerTs()));
        }
        
        /*
        responses.forEach((response) -> {
            m0.add(getInputPlace(), new Token(response,response.OPTriggering.getTriggerTs()));
        });
        */
            
        return m0;
    }
    
    public MarkingOld createFinalMarking(Collection<OPResponse> responses) {
        MarkingOld m = new MarkingOld();
        responses.stream().forEach((OPResponse response) -> {
            m.add(getOutputPlace(),
                    new Token(
                            response,
                            response.OPTriggering.getTriggerTs() + getEstimatedDuration())
            );
        });
            
        return m;
    }
    
    /**
     * Gets the net place of a given place (NOTE: If the place has multiple 
     * next places, only the first will be retrieved.
     * @param p
     * @return 
     */
    public Place getNextPlace(Place p) {
        return (Place) super.nextPlaces(p).toArray()[0];
    }
    
    public StepType getStepType(Transition t) {
        
        StepType result;
        
        //AND routing blocks are part of concurrent container steps
        if (t.getLabel().startsWith("AND"))
            return StepType.CONCURRENT_CONTAINER;
        
        Optional<OPInfo> opInfo = this.getOPInfo();
        if (opInfo.isPresent()) {
            result = Optional.ofNullable(opInfo.get().findStep(t.getLabel()))
                    .flatMap(s -> Optional.ofNullable(s.getType()))
                    .map(s -> StepType.valueOf(s))
                    .orElse(StepType.UNKNOWN);
        } else {
            // The transition label assigns the step type
            if (t.getLabel().startsWith("ACTION"))
                result =  StepType.ACTION;
            else if (t.getLabel().startsWith("CHECK") || t.getLabel().startsWith("!CHECK"))
                result =  StepType.CHECK;
            else if (t.getLabel().startsWith("VERIFY") || t.getLabel().startsWith("!VERIFY")
                    || t.getLabel().startsWith("SUPERVISION"))
                result =  StepType.SUPERVISION;
            else 
                result = StepType.UNKNOWN;        
        }
        
        return result;
    }
    
    /**
     * Retrieve the maximum step duration of a transition from the OP info.
     * the OP info is stored in a MongoDB database
     * The link between a WFNET_OP object and the OP in the database comes from the
     * label of each transition with the label of each step in the OP
     * @param t
     * @return 
     */
    public long getStepDurationMean(Transition t) throws Exception {
        long result, defaultStepDuration;
        
        if (getStepType(t).equals(StepType.CHECK))
            defaultStepDuration = Constants.DEFAULT_CHECK_DURATION;
        else if (getStepType(t).equals(StepType.ACTION))
            defaultStepDuration = Constants.DEFAULT_ACTION_DURATION;
        else if (getStepType(t).equals(StepType.SUPERVISION))
            defaultStepDuration = Constants.DEFAULT_SUPERVISION_DURATION;
        else if (getStepType(t).equals(StepType.CONCURRENT_CONTAINER))
            defaultStepDuration = Constants.DEFAULT_CONCURRENT_CONTAINER_DURATION;
        else
            throw new Exception("Transition " + t.getLabel() + " has no associated Step Type");
        
        //Get information about the procedure
        OPInfo eopInfo = this.getOPInfo().get();
        if (eopInfo == null) {
            LOG.log(Level.WARNING,"EOP information for {0} not found.",
                    new Object[]{
                        this.getClass().getSimpleName()
                    }
            );
            result = defaultStepDuration;
        } else {
            OPInfo.Step s = eopInfo.findStep(t.getLabel());
            if (s == null) {
                LOG.log(
                    Level.WARNING,"Step {0} not found in EOP info.",
                    new Object[]{t.getLabel()}
                );                    
                result = defaultStepDuration;
            } else {             
                if (s.getMaximumStepDuration()== null) {
                    LOG.log(
                        Level.WARNING,"Step {0} has no Maximum Step Duration (returning default).",
                        new Object[]{t.getLabel()}
                    );
                    result = defaultStepDuration;
                } else {
                    result = s.getMaximumStepDuration().longValue();
                }
            }
        }
        return result;
    }
    
    /**
     * 
     * @param t
     * @return 
     */
    public long getStepDurationDeviation(Transition t) {
        if (!getStepType(t).equals(StepType.ACTION))
            return 0;
        else 
            return 10000;
    }
    
    /**
     * 
     * @param t
     * @param M
     * @param kappa
     * @param x_out
     * @return 
     */
    public Marking fireTransition(Transition t,
                                    Marking M,
                                    Set<Token> kappa,
                                    Long x_out) {
        
        Marking Mprime = new Marking();
        Set<Place> input_t, t_output; //Input and output places of t
        input_t = new HashSet<Place>(t.getInputPlaces());
        t_output = new HashSet<Place>(t.getOutputPlaces());
        
        this.getPlaces().forEach((Place p) -> {
            Set<Token> kappaPrime;
            if (SetOperations.difference(input_t, t_output).contains(p)) {
                // M(p)\kappa if p in .t\t.
                kappaPrime = SetOperations.difference(
                                M.getOrDefault(p, Collections.<Token>emptySet()),
                                kappa
                        );
            }
            else if(SetOperations.difference(t_output, input_t).contains(p)) {
                // M(p) U kappa if p in t.\.t
                kappaPrime = SetOperations.union(
                        M.getOrDefault(p, Collections.<Token>emptySet()),
                        new HashSet<Token>(Arrays.asList(new Token(null,x_out)))
                    );
            } else if (SetOperations.intersection(input_t, t_output).contains(p)) {
                kappaPrime = SetOperations.union(
                        SetOperations.difference(
                                M.getOrDefault(p, Collections.<Token>emptySet()), 
                                kappa
                        ), 
                        new HashSet<Token>(Arrays.asList(new Token(null,x_out)))
                    );
            } else {
                kappaPrime = M.getOrDefault(
                        p, 
                        Collections.<Token>emptySet()
                );
            }
            
            if (!kappaPrime.isEmpty()) {
                Mprime.put(p, kappaPrime);
            }
        });
        
        return Mprime;
    }
    
    /**
     * TODO: Esta funcion deberia hcer queries de acciones teniendo en cuenta una
     * cola de timestamps con los ultimos firings del modelo
     * @deprecated 
     * @param alertResp
     * @param startTime
     * @param endTime
     * @param params
     * @param aEvaluator
     * @return 
     */
    public static Long getActionTriggerTimestamp(
            OPResponse alertResp,
            Long startTime, 
            Long endTime,
            Map<String,Object> params,
            ActionEvaluator aEvaluator) {
        
        Long result = null;
        Long resultBeforeStart = null;
        
        //TODO: ¡Establecer las condiciones extremas de start time y end time desde aqui?
        if (endTime == null) {
            endTime = alertResp.getOPTriggering().getEndTime();
        }
        
        result = aEvaluator.getFirstActionTimestamp(alertResp, 
                startTime,
                endTime,
                params);
        
        if (result == null) return result;
        else {
            //Busco la accion desde el comienzo de la alerta hasta el start time
            //TODO Mejorar esto con la cola de firing timestamps
            resultBeforeStart = aEvaluator.getFirstActionTimestamp(
                    alertResp,
                    alertResp.getOPTriggering().getTriggerTs(),
                    startTime,
                    params
            );
            
            if (resultBeforeStart == null) return result;
            else return null; //si se encuentra antes, no cuenta
        }
    }
    
    /**
     * Time of Completion (ToC)
     * @param delta
     * @param t
     * @param x_in
     * @param GC
     * @return
     * @throws Exception 
     */
    public Long timeOfCompletion(Transition t, Long x_in) throws Exception {

        Long result;
        
        //DEBUG
        if (t.getLabel().equals("SUPERVISION:S_3:NOT FULFILLED")) {
            LOG.log(Level.WARNING,"OLA K ASE");
        }
        
        //Maximum step duration for the step associated to transition t
        long mu_t = this.getStepDurationMean(t);
        
        Collection<String> involvedVariables = t.getGuardCondition().getInvolvedVariables();
        
        //First we have to verify whether the Guard Condition is fulfilled for
        // the whole interval [x_in, x_in + mu(t)].
        boolean isValid = t.getGuardCondition().evaluate(
                Optional.ofNullable(involvedVariables)
                        .orElse(
                                this.getDataLog().getEntries().stream()
                                .map(LogEntry::getVarname)
                                .distinct()
                                .collect(Collectors.toSet())
                        )
                        .stream()
                        .map(v -> this.getDataLog().logState(x_in, x_in + mu_t, v))
                        .flatMap(x -> x.stream())
                        .collect(Collectors.toSet())
        );
        if (!isValid) return -1L;
        
        //In case the condition is valid, compute the minimum time for which
        //it becomes valid
        // Since we have to compute the log state from x_in to x_in+mu_t 
        //increasingly, we do it for every timestamp when the log state has some 
        // new records
        List<Long> record_times = new ArrayList<>(Arrays.asList(x_in));
        if (involvedVariables != null) {
            record_times.addAll(
                this.getDataLog().getEntries().stream()
                .filter(e -> e.getTs() > x_in)
                .filter(e -> e.getTs() <= (x_in + mu_t))
                .filter(e -> involvedVariables.contains(e.getVarname()))
                .map(LogEntry::getTs)
                .distinct()
                .sorted()
                .collect(Collectors.toList())
            );
        } else {
            record_times.addAll(
                this.getDataLog().getEntries().stream()
                .filter(e -> e.getTs() > x_in)
                .filter(e -> e.getTs() <= (x_in + mu_t))
                .map(LogEntry::getTs)
                .distinct()
                .sorted()
                .collect(Collectors.toList())
            );
        }
                
        boolean GC_fulfilled = false;

        int i = 1;
        while (i <= record_times.size() && !GC_fulfilled) {
            Collection<Long> subRecordTimes = record_times.subList(0,i);
            Collection<LogEntry> logState;
            
            if (subRecordTimes.contains(559838860410L))
                LOG.log(Level.INFO, "ola ka ase");
            
            if (involvedVariables != null) {
                
                Collection<List<Object>> xvPerms = 
                        Helpers.<Object>permutations(
                            Arrays.asList(
                                    subRecordTimes.stream().map(o -> (Object) o).collect(Collectors.toSet()), 
                                    involvedVariables.stream().map(o -> (Object) o).collect(Collectors.toSet())
                            )
                        );
                
                logState = xvPerms.stream()
                        .map((List<Object> xv) -> {
                            return  this.getDataLog().logState(
                                    (Long) xv.get(0), 
                                    (String) xv.get(1)
                            );
                        })
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());
            } else {
                logState = subRecordTimes.stream()
                        .map(x -> this.getDataLog().logState(x))
                        .flatMap(x -> x.stream())
                        .collect(Collectors.toSet());
            }
            
            GC_fulfilled = t.getGuardCondition().evaluate(logState);
            if (!GC_fulfilled)
                i++;
        }
        
         if (GC_fulfilled)
            result = record_times.get(i-1);
        else
            result = -1L;
        
        LOG.log(Level.INFO, "ToC({0},{1}) = {2}", 
                new Object[]{t.getLabel(), x_in, result});
        return result;
    }
    

    /**
     * Getters & Setters
     */
    public DataLog getDataLog() {
        return dataLog;
    }
    
    public void setDataLog(DataLog dataLog) {
        this.dataLog = dataLog;
    }

    public Optional<OPInfo> getOPInfo() {
        return Optional.ofNullable(opInfo);
    }

    public void setOpInfo(OPInfo opInfo) {
        this.opInfo = opInfo;
    }

    private DataLog dataLog;
    private OPInfo opInfo;
}
