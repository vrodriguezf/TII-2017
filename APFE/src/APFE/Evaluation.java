/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE;

import APFE.WFnet_OP.WFNetOP;
import APFE.WFnet_OP.StepType;
import APFE.WFnet_OP.Deadlock;
import APFE.WFnet_OP.Firing;
import APFE.WFnet_OP.Marking;
import APFE.WFnet_OP.MarkingOld;
import APFE.evaluation.EvaluationResult;
import APFE.evaluation.Overreaction;
import APFE.utils.Helpers;
import APFE.data_log.DataLog;
import APFE.utils.Triple;
import business.Global;
import business.Place;
import business.Token;
import business.TokenSet;
import business.Transition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.util.Pair;

/**
 *
 * @author victor
 */
public class Evaluation {

    private static final Logger LOG = Logger.getLogger(Evaluation.class.getName());
    
    public static Pair<MarkingOld,Long> basicAPFE(
            MarkingOld initialMarking, 
            OPTriggering alert, 
            WFNetOP eop) throws InterruptedException 
    {
        //Variables declaration;
        EvaluationResult evaluationResult = new EvaluationResult();
        MarkingOld Mi;
        MarkingOld M,Mprime,MdoublePrime;
        ProcedureExecution procExec;
        
        //Initial MarkingOld
        Mi = initialMarking;
        
        //Finish marking
        //Mf = eop.createFinalMarking(responses);
        
        //Initialize iterative marking
        M = Mi;
        
        procExec = new ProcedureExecution(eop, M);
        procExec.start();
        
        // Update iterative marking
        M = eop.getMarking();
        
        //Time spent
        Long timeSpent = M.entrySet()
                .stream()
                .map(Entry::getValue)
                .mapToLong((TokenSet ts) -> {
                    long max_time = 
                            ts.getTokenList()
                            .stream()
                            .mapToLong((Object o) -> {
                                Token t = (Token) o;
                                return t.getTimestamp();
                            })
                            .max()
                            .getAsLong();
                    return max_time;
                })
                .max()
                .orElse(0L);
        
        return new Pair<>(M,timeSpent);
    }
    
    public static Triple<Boolean,Marking,Long> APFE(
                            WFNetOP wfnetOP,
                            DataLog Delta,
                            Long x_0) throws Exception {
        //Variables declaration;
        Marking M_in;
        
        Global.petriNet = wfnetOP;
        
        //Assign the datalog to the wfnetOP
        wfnetOP.setDataLog(Delta);
        
        //Set Initial Marking
        M_in = wfnetOP.createInitialMarking(new Long[]{x_0});
        wfnetOP.setMarking(M_in);
        
        //Invalid conditions (IC)
        Collection<Pair<Transition,Token>> IC = new HashSet<>();
        
        //Main loop (WFNETOP execution)
        boolean finished = false;
        
        while (!finished) {
            Marking M = wfnetOP.getMarking_();
            Collection<Transition> enabledTransitions = wfnetOP.enabledTransitions(0L, false);
            Collection<Transition> validTransitions = enabledTransitions.stream()
                    .filter((Transition t) -> {
                        return  t.getInputPlaces().stream()
                                .allMatch((Place p) -> {
                                    return  M.get(p).stream()
                                            .anyMatch((Token k) -> {
                                                return !IC.contains(new Pair(t,k));
                                            });
                                });
                    })
                    .collect(Collectors.toList());
            
            if (validTransitions.isEmpty()) finished = true;
            else {
                Transition t = Helpers.getRandom(validTransitions).get();
                
                //Set of valid tokens (kappa)
                Set<Token> kappa = new HashSet<>();
                //Times of completion (gamma)
                Collection<Pair<Token,Long>> gamma = new ArrayList<>();
                
                for (Place p : t.getInputPlaces()) {
                    Token k = M.get(p).stream()
                            .filter((Token kprime) -> {
                                return !IC.contains(new Pair(t,kprime));
                            })
                            .max((Token k1, Token k2) -> {
                                return (int) (k1.getTimestamp() - k2.getTimestamp());
                            })
                            .orElseThrow(Exception::new);
                    
                    kappa.add(k);
                    gamma.add(
                        new Pair<Token,Long>(
                            k,
                            wfnetOP.timeOfCompletion(t, k.getTimestamp())
                        )
                    );
                }
                
                //Fire only if every input valid token has a time of completion 
                //different than -1, i.e, chech if there is any invalid condition.
                //if so, add them to the set of invalid conditions
                Collection<Pair<Transition,Token>> newInvalidConditions = 
                        gamma.stream()
                        .filter((Pair<Token,Long> x) -> x.getValue() == -1L)
                        .map(Pair::getKey)
                        .map((Token k) -> new Pair<>(t,k))
                        .collect(Collectors.toList());
                
                if (newInvalidConditions.isEmpty()) {
                    //Firing
                    Long x_out = gamma.stream()
                            .mapToLong(Pair<Token,Long>::getValue)
                            .max()
                            .orElseThrow(Exception::new);
                    
                    Marking Mprime = wfnetOP.fireTransition(t, M, kappa, x_out);
                     wfnetOP.setMarking(Mprime);                            
                } else {
                    IC.addAll(newInvalidConditions);
                }
            }
        } //endwhile
        
        //Get final Marking
        Marking Mf = wfnetOP.getMarking_();
        Long PFETime = Mf.values().stream()
                .flatMap(
                        x -> x.stream())
                .mapToLong(Token::getTimestamp)
                .max()
                .orElseThrow(Exception::new);
        
        if (!Mf.get(wfnetOP.getOutputPlace()).isEmpty()) {
            return new Triple<>(true, Mf, PFETime);
        } else {
            return new Triple<>(false, Mf, PFETime);
        }
    }
    
    /**
     * 
     * @param initialMarking
     * @param eop
     * @param alert
     * @return
     * @throws InterruptedException 
     */
    public static EvaluationResult forwardEvaluation(
            MarkingOld initialMarking, 
            OPTriggering alert, 
            WFNetOP eop) throws InterruptedException {
        
        //Variables declaration;
        EvaluationResult evaluationResult = new EvaluationResult();
        MarkingOld Mi;
        MarkingOld M,Mprime,MdoublePrime;
        ProcedureExecution procExec;
        Collection<Deadlock> deadlocks = new ArrayList<Deadlock>();
        ArrayList<ProcedureExecution> executions = new ArrayList<>();
        
        //Initial MarkingOld
        Mi = initialMarking;
        
        //Finish marking
        //Mf = eop.createFinalMarking(responses);
        
        //Initialize iterative marking
        M = Mi;
        
        //MAIN LOOP UPDATE: Condicion esta mal
        do {
            //Execute procedure with current marking M and add it to the executions queue
            procExec = new ProcedureExecution(eop, M);
            procExec.start();
            //procExec.join();
            executions.add(procExec);
            
            // Update iterative marking
            M = eop.getMarking();
            
            //Add the list of performed actions to the evaluation result
            evaluationResult.getRightActions().addAll(
                    procExec.getFiringTrace()
                            .stream()
                            .filter(f -> eop.getStepType(f.getTransition()) == StepType.ACTION)
                            .collect(Collectors.toCollection(ArrayList::new))
            );
            
            //Retrieve deadlocks in the current marking
            deadlocks = getDeadlocks(eop);
            
            if (!deadlocks.isEmpty()) {
                
                // Soft repair - If passed, the blocker transitions will be marked
                // as sequential mismatches
                //TODO: Este marking hay que revisar como se crea. No hay por qué coger
                //los tokens del place final sin más, hay que coger todos los tokens que no esten 
                // metidos en un deadlock
                Mprime = new MarkingOld();
                    Mprime.add(eop.getOutputPlace(), new TokenSet(eop.getOutputPlace().getTokens()));
                    for (Deadlock deadlock : deadlocks) {
                        deadlock.setLiberatorToken(
                                new Token(
                                        deadlock.getLockedToken().getObject(),
                                        alert.getTriggerTs() //Reset TIME!
                                )
                        );
                        Mprime.add(
                                M.getPlaceOf(deadlock.getLockedToken()),
                                deadlock.getLiberatorToken()
                        );
                    }
                    
                // Re-execute the procedure evaluation with marking Mprime (Esta no se guarda en el array)
                procExec = new ProcedureExecution(eop, Mprime);
                procExec.start();        
                //procExec.join();
                
                //Classify deadlocks comparing them with the deadlocks after repairing
                Collection<Deadlock> deadlocksAfterSoftRepair = getDeadlocks(eop);
                for (Deadlock originalDl : deadlocks) {
                    if (deadlocksAfterSoftRepair.stream()
                            .filter(d -> d.equalDistribution(originalDl))
                            .count() == 0 ) {
                        //El deadlock ha desparecido con el soft repair - Sequential Mismatch (SM)
                        //Buscamos el liberador de este mismatch
                        originalDl.setLiberatorFiring(procExec.findLiberatorFiring(originalDl));
                        evaluationResult.getSequentialMismatches().add(originalDl);
                    } else {
                        //El deadlock no ha desaparecido -- Missing action (MA)
                        evaluationResult.getMissingActions().add(originalDl);
                    }
                }
                
                //Hard repair (avanzar los tokens que estan en deadlock)
                MdoublePrime = new MarkingOld();
                    for (Entry<Place,TokenSet> mEntry: M.entrySet()) {
                        if (mEntry.getKey().equals(eop.getOutputPlace())) {
                            //The output place is not changed
                            MdoublePrime.add(mEntry.getKey(), new TokenSet(mEntry.getValue()));
                        } else {
                            // Pass the tokens of each place to the next place 
                            // (manually firing the associated transition)
                            for (Place p : eop.nextPlaces(mEntry.getKey())) {
                                MdoublePrime.add(p, new TokenSet(mEntry.getValue()));
                            }
                        }
                    }
                    
                //Reset iterative marking
                M = MdoublePrime;
            }
        } while (!deadlocks.isEmpty());
        
        //Optional: Save the execution traces in the EvaluationResult object
        evaluationResult.getForwardExecutionTraces().add(executions);
        
        return evaluationResult;
    }
    
    public static EvaluationResult FBEvaluation(
            MarkingOld initialMarking, 
            OPTriggering alert, 
            WFNetOP eop) throws InterruptedException {
        
        MarkingOld Mi;
        ArrayList<MarkingOld> reversedMarkingTrace;
        EvaluationResult evResult;
        ArrayList<ProcedureExecution> backwardExecutionTraces;
        
        //Initial MarkingOld
        Mi = initialMarking;
        
        /**
         * ANALYZE PERFORMED ACTIONS, MISSING ACTIONS AND SEQUENTIAL MISMATCHES
         */
        
        // Perform a Forward Evaluation, saving the execution traces
        evResult = forwardEvaluation(Mi, alert, eop);
        
        //Backward loop in the forward execution traces
        backwardExecutionTraces = new ArrayList<>(evResult.getForwardExecutionTraces().get(0));
        Collections.reverse(backwardExecutionTraces);
        
        for (ProcedureExecution procExec : backwardExecutionTraces) {
            //Reverse marking trace
            reversedMarkingTrace = new ArrayList<>(procExec.getMarkingTrace());
            Collections.reverse(reversedMarkingTrace);

            //Backward marking trace loop
            for (MarkingOld M : reversedMarkingTrace) {
                //Get all the possible markings which can be arrived from this marking
                // (if there are no or splits places in the marking, the list will be empty)
                Collection<MarkingOld> alternativeMarkings = getNextPossibleMarkings(M, eop);
                for (MarkingOld altM : alternativeMarkings) {
                    //Check if the marking is not present in the correct marking trace
                    if (!reversedMarkingTrace
                            .stream()
                            .anyMatch((MarkingOld auxM) -> auxM.equalsDistribution(altM))) {

                        //Otherwise, re-execute a forward evaluation from this marking and see what happens
                        EvaluationResult auxEv = forwardEvaluation(
                                altM, 
                                alert, 
                                eop);

                        //Overreactions are defined as actions obtained in a 
                        // secondary evaluation process, which are not present in any of the
                        // actions or deadlocks of the main evaluation
                        // TODO revisar esto
                        Collection<Overreaction> overreactions =
                                retrieveOverreactions(evResult, auxEv, null); 

                        if (!overreactions.isEmpty()) {
                            evResult.getOverreactions().addAll(overreactions);
                        }
                        
                        //Save the traces of this sub-evaluation
                        evResult.getForwardExecutionTraces().addAll(auxEv.getForwardExecutionTraces());
                    }
                }
                
                //Extract the OR-splits of the marking
                /*
                Entry<Place,TokenSet> orSplitMarks = 
                        M.entrySet()
                        .stream()
                        .filter(mark -> eop.isORSplit(mark.getKey()))
                        .collect(Collectors.toSet());
                
                //Check if the marking contains OR-split places
                for (Entry<Place,TokenSet> mark : M.entrySet()) {
                    if (eop.isORSplit(mark.getKey())) {
                        //If one place is an or split, put the tokenset in all the 
                        //net paths different than the folloed in the marking trace,
                        // and re-execute the procedure

                        //1. Extract the other net branch (not executed transition)
                        Collection<Transition> notExecutedTransitions = mark.getKey()
                                .getNextTransitions(eop)
                                .stream()
                                .filter((Transition t) -> !procExec.getFiringTrace().contains(t))
                                .collect(Collectors.toCollection(ArrayList::new));

                        for (Transition t : notExecutedTransitions) {
                            //TODO Recursive call to DBEvaluation
                            
                            //NOW: re-execute forward evaluation from the output
                            // of this transition
                            MarkingOld auxM = new MarkingOld();
                            t.getOutputPlaces().stream().forEach((p) -> {
                                auxM.add(p, mark.getValue());
                            });
                            
                            EvaluationResult auxEv = forwardEvaluation(
                                    auxM, 
                                    alert, 
                                    eop);
                            
                            //Overreactions are defined as actions obtained in a 
                            // secondary evaluation process, which are not present in any of the
                            // actions or deadlocks of the main evaluation
                            // TODO revisar esto
                            Collection<Overreaction> overreactions =
                                    retrieveOverreactions(evResult, auxEv, t);
                            
                            if (!overreactions.isEmpty()) {
                                evResult.getOverreactions().addAll(overreactions);
                            }
                        }
                    }
                }
                */
            }            
        }
        
        //Remove duplicate overreaction, caused by similarity 
        //among markings in the marking trace
        //TODO revisar esto
        evResult.setOverreactions(
                evResult.getOverreactions()
                    .stream()
                    .filter(Helpers.distinctByKey((Overreaction o) -> o.getAction().getTransition()))
                    .collect(Collectors.toCollection(ArrayList::new))
        );
        
        return evResult;
    }    
    
    /**
     * PRIVATE METHODS
     */
    
    /**
     * Given a marking, extract the possible OR split places and generate markings
     * for every routing combination of those markings
     * @return 
     */
    private static Collection<MarkingOld> getNextPossibleMarkings(MarkingOld m,WFNetOP eop) { 
        
        Collection<MarkingOld> result = new ArrayList<>();
        
        /**
         * OR-SPLIT es solo aquel place que enruta 2 transiciones, 
         * que no dependen de nada mas que de ella
         */
        Collection<Place> orPlaces = m
                .entrySet()
                .stream()
                .map(Entry::getKey)
                .filter(p -> eop.isORSplit(p))
                .collect(Collectors.toCollection(ArrayList::new));
        
        List<Collection<Transition>> branches = orPlaces
                .stream()
                .map(p -> p.getNextTransitions(eop))
                .collect(Collectors.toList());
        
        Collection<List<Transition>> combinations = Helpers.permutations(branches);
        
        for (List<Transition> combination : combinations) {
            MarkingOld newM = new MarkingOld();
            for (Transition t : combination) {
                //Move the tokens of the input place to the output places
                Place inputORPlace = orPlaces
                        .stream()
                        .filter(p -> p.getNextTransitions(eop).contains(t))
                        .findFirst()
                        .get();
                
                t.getOutputPlaces().stream().forEach((p) -> {
                    newM.add(p, new TokenSet(m.get(inputORPlace)));
                });
            }
            //Complete the marking with the normal places (DO not move tokens here)
            m.entrySet()
                    .stream()
                    .filter(e -> !orPlaces.contains(e.getKey()))
                    .forEach(e -> newM.add(e.getKey(), new TokenSet(e.getValue())));
            
            //Add the new marking to the result
            result.add(newM);
        }
        
        return result;
    }
        
    
    /**
     * Get deadlocks (Only for ACTION transitions)
     * @param eop
     * @return 
     */
    private static Collection<Deadlock> getDeadlocks(WFNetOP eop) {
        Collection<Deadlock> result = new ArrayList<>();
        
        //UPDATE: Una transicion se considera bloqueante siempre y cuando 
        // tiene todos los tokens de entrada completos (no basta con tener alguno,
        // porque si no las puertas logicas se consideran bloqueantes (y por tanto
        // missing actions, lo cual es un poco raro))
        
        Stream<Transition> blockerTransitions;
        blockerTransitions = eop.getTransitions()
                            .stream()
                            //.filter((Transition t) -> t.inputArcsEnabled(0)) //TODO Revisar
                            .filter((Transition t) -> {
                                return t.getInputPlaces()
                                        .stream()
                                        .allMatch(p -> !p.getTokens().isEmpty());
                                //return !t.getTokenSet().isEmpty();
                            });
                            //.collect(Collectors.toCollection(ArrayList::new));
        
        blockerTransitions.forEach((Transition t) -> {
            LOG.log(Level.INFO,"Blocker Transition: {0}",t);            
            t.getTokenSet().getTokenList().forEach((Object _token) -> {
                result.add(new Deadlock(
                        (Token) _token, 
                        t
                ));
            });
        });
        
        return result;
    }
    
    //Overreactions are defined as actions obtained in a 
    // secondary evaluation process, which are not present in any of the
    // actions or deadlocks of the main evaluation
    private static Collection<Overreaction> retrieveOverreactions(EvaluationResult mainEv,
                                                            EvaluationResult auxEv,
                                                            Transition missingCheck) {
        
        ArrayList<Transition> mainTransitions = new ArrayList<>();
            mainTransitions.addAll(mainEv.getRightActions()
                    .stream()
                    .map(Firing::getTransition)
                    .collect(Collectors.toCollection(ArrayList::new))
            );
            mainTransitions.addAll(mainEv.getSequentialMismatches()
                    .stream()
                    .map(Deadlock::getLockingTransition)
                    .collect(Collectors.toCollection(ArrayList::new))
            ); 
        
        ArrayList<Transition> auxTransitions = new ArrayList<>();
            auxTransitions.addAll(auxEv.getRightActions()
                    .stream()
                    .map(Firing::getTransition)
                    .collect(Collectors.toCollection(ArrayList::new))
            );
            auxTransitions.addAll(auxEv.getSequentialMismatches()
                    .stream()
                    .map(Deadlock::getLockingTransition)
                    .collect(Collectors.toCollection(ArrayList::new))
            );
        
        ArrayList<Transition> badTransitions = 
                auxTransitions
                .stream()
                .filter(t -> !mainTransitions.contains(t))
                .collect(Collectors.toCollection(ArrayList::new));
        
        if (!badTransitions.isEmpty()) {
            LOG.log(Level.INFO,"Holaaaaa");
        }
        
        ArrayList<Overreaction> result = new ArrayList<>();
        result.addAll(
                auxEv.getRightActions().stream()
                .filter((a) -> badTransitions.contains(a.getTransition()))
                .map((a) -> new Overreaction(a, missingCheck))
                .collect(Collectors.toCollection(ArrayList::new))
        );
        result.addAll(
                auxEv.getSequentialMismatches().stream()
                .filter((d) -> badTransitions.contains(d.getLockingTransition()))
                .map((d) -> new Overreaction(new Firing(d.getLockedToken(),d.getLockingTransition()), missingCheck))
                .collect(Collectors.toCollection(ArrayList::new))
        );
        
        //Overreactions coming from sceondary performed actions (TODO)
        /*
        ArrayList<Overreaction> fromPerformedActions = auxEv.getPerformedActions()
                .stream()
                .filter((Firing action) -> {
                    return !mainEv
                            .getPerformedActions()
                            .stream()
                            .map(Firing::getTransition)
                            .collect(Collectors.toCollection(ArrayList::new))
                            .contains(action.getTransition());
                })
                .map((action) -> new Overreaction(action, missingCheck))
                .collect(Collectors.toCollection(ArrayList::new));
        
        ArrayList<Overreaction> fromSequentialMismatches = auxEv.getSequentialMismatches()
                .stream()
                .filter((Deadlock deadlock) -> {
                    return !mainEv
                            .getPerformedActions()
                            .stream()
                            .map(Firing::getTransition)
                            .collect(Collectors.toCollection(ArrayList::new))
                            .contains(deadlock.getLockingTransition());
                })
                .map((deadlock) -> new Overreaction(
                        new Firing(deadlock.getLockedToken(), deadlock.getLockingTransition()),
                        missingCheck)
                )
                .collect(Collectors.toCollection(ArrayList::new));
        
        result.addAll(fromPerformedActions);
        result.addAll(fromSequentialMismatches);
        
        return result;
        */
        
        return result;
    }
}
