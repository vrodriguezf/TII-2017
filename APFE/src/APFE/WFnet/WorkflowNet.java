/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet;

import APFE.WFnet_OP.MarkingOld;
import business.InputArc;
import business.OutputArc;
import business.PetriNet;
import business.Place;
import business.Token;
import business.TokenSet;
import business.Transition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author victor
 */
public class WorkflowNet extends PetriNet {

    private static final Logger LOG = Logger.getLogger(WorkflowNet.class.getName());

    //IMPORTANT!!
    public Token currentCase;

    //Para resolver las politicas de las AND join, sobre que token de entrada coger
    // Lo establece la función ANDJoinGuard
    public Token ANDJoinWinner;

    private ArrayList<MarkingOld> markingTrace = new ArrayList<>();

    public WorkflowNet() {
    }

    //TODO COmprobar que solo existe un Input/Output place?
    public Place getInputPlace() {
        Stream<Place> inputPlaces = super.getPlaces().stream().filter(new Predicate<Place>() {
            public boolean test(final Place p) {
                return (getOutputArcs().stream().filter(new Predicate<OutputArc>() {
                    public boolean test(OutputArc oa) {
                        return (oa.getPlace().equals(p));
                    }
                }).count() == 0);
            }
        });

        return inputPlaces.findFirst().get();
    }

    public Place getOutputPlace() {
        Stream<Place> outputPlaces = super.getPlaces().stream().filter(new Predicate<Place>() {
            public boolean test(final Place p) {
                return (getInputArcs().stream().filter(new Predicate<InputArc>() {
                    public boolean test(InputArc oa) {
                        return (oa.getPlace().equals(p));
                    }
                }).count() == 0);
            }
        });

        return outputPlaces.findFirst().get();
    }

    /**
     * Transition guard associated to an AND join transition, typical from a
     * workflow net. Check if all the input places of the transition share a
     * token
     * TODO Este metodo se ejecuta mas de una vez par alas transiciones AND...
     *
     * @param t
     * @return
     */
    public boolean ANDJoinGuard(Transition t) {

        Collection<Place> inputPlaces = t.getInputPlaces();

        //Check if all the input places of the transition share a token
        Collection<Object> distinctTokenObjects = (Collection<Object>) inputPlaces.stream()
                .map(Place::getTokens)
                .flatMap((TokenSet t1) -> {
                    return t1.getTokenList()
                            .stream()
                            .map(o -> ((Token) o).getObject());
                })
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        boolean result = distinctTokenObjects
                .stream()
                .anyMatch((Object t1) -> {
                    return inputPlaces.stream()
                            .allMatch((Place p) -> {
                                return p.getTokens()
                                        .getTokenList()
                                        .stream()
                                        .map(o -> ((Token) o).getObject())
                                        .filter(o -> o.equals(t1))
                                        .count() > 0;
                            });
                });

        //If there is a shared token object, this function saves the 
        //winner for the workflow process (the one with the maximum timestamp)
        if (result) {
            //Get the token whose object is present in all the places
            Object sharedTokenObject = 
                    distinctTokenObjects
                    .stream()
                    .filter(tokenObject -> {
                        return inputPlaces
                                .stream()
                                .allMatch((Place p) -> {
                                    return p
                                            .getTokens()
                                            .getTokenList()
                                            .stream()
                                            .map(o -> ((Token) o).getObject())
                                            .filter(o -> o.equals(tokenObject))
                                            .count() > 0;
                                });
                    })
                    .findFirst()
                    .get();
      

            // Get the maximum timestamp of this shared token among all the input
            // places
            Long maxTS = inputPlaces
                    .stream()
                    .mapToLong(p -> {
                        Token pSharedToken = (Token) p.getTokens()
                                .getTokenList()
                                .stream()
                                .filter(token -> {
                                    return ((Token) token)
                                            .getObject()
                                            .equals(sharedTokenObject);
                                })
                                .findFirst()
                                .get();

                        return pSharedToken.getTimestamp();
                    })
                    .max()
                    .getAsLong();
            
            ANDJoinWinner = new Token(sharedTokenObject,maxTS);
        }

        return result;
    }

    public boolean evaluateInputTokens(Transition t, TokenEvaluator evaluator) {
        boolean result = false;
        for (Object _token : t.getTokenSet().getTokenList()) {
            Token token = (Token) _token;
            Boolean eval = evaluator.evaluate(token);

            token.setPassedTransitionGuard(eval);
            if (eval) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Function for input arcs execution
     *
     * @param ia
     * @return
     * @deprecated 
     */
    public Token getPassedToken(InputArc ia) {
        Token firstToken = (Token) ia.getTokenSet().getTokenList()
                .stream()
                .filter((Object o) -> {
                    return ((Token) o).isPassedTransitionGuard();
                })
                .findFirst()
                .get();

        return firstToken;
    }

    /**
     * Get the next possible places from a given place
     *
     * @param p
     * @return
     */
    public Collection<Place> nextPlaces(Place p) {
        return p.getNextTransitions(this)
                .stream()
                .flatMap((Transition t) -> t.getOutputPlaces().stream())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Check if a place acts as an OR-split in the net
     *
     * @param p
     * @return
     */
    public boolean isORSplit(Place p) {
        
        boolean result = true;
        
        //check that the place has more than two arcs
        result = result && (p.getOutputArcs(this).size() > 1);
        
        //Check if the output transitions are only accessible by this place
        // TODO: Relajar esta condicion!!! (Ver notas de cuaderno)
        //result = result &&
        
        result = result && 
                p.getNextTransitions(this)
                .stream()
                .allMatch((Transition t) -> {
                    return (t.getInputPlaces().size() == 1);
                });
        
        return result;
    }

    /**
     * Getters & Setters
     */
    public ArrayList<MarkingOld> getMarkingTrace() {
        return markingTrace;
    }

    public void setMarkingTrace(ArrayList<MarkingOld> markingTrace) {
        this.markingTrace = markingTrace;
    }
}
