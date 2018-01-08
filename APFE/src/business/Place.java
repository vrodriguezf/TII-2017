/* Copyright Guillem Catala. www.guillemcatala.com/petrinetsim. Licensed http://creativecommons.org/licenses/by-nc-sa/3.0/ */
package business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Guillem
 */
public class Place extends NetObject {

    /** List of tokens this place contains. */
    private TokenSet tokens = new TokenSet();
    /** Maximum number of tokens this place can hold (0 = no limit).*/
    private int capacity = 0;

    /** Place constructor. */
    public Place() {
        this.id = "p" + this.id;
    }

    /** Constructor to create a new place with specific attributes. */
    public Place(String id) {
        this.id = id;
    }

    /**
     * @return the tokens
     */
    public TokenSet getTokens() {
        return tokens;
    }
    
    public Set<Token> getTokens_() {
        return tokens.toSet();
    }

    /**
     * @param tokens the tokens to set
     */
    public void setTokens(TokenSet tokens) {
        this.tokens = tokens;
    }

    /** Adds a TokenSet to the current place TokenSet*/
    /**
     * 
     * @param tokenSet 
     * @deprecated
     */
    public void addTokens(TokenSet tokenSet) {
        tokens.addAll(tokenSet);
    }
    
    public void addTokens(Set<Token> kappa) {
        if (kappa!= null) {
            kappa.forEach(k -> tokens.add(k));
        }
    }

    /** Removes a TokenSet in the current place TokenSet*/
    public void removeTokens(TokenSet tokenSet) {
        tokens.removeAll(tokenSet);
    }
    
    /**
     * Retrieves the possible which can put tokens into this place
     * @param pn
     * @return 
     */
    public Collection<Transition> getPreviousTransitions(PetriNet pn) {
        return 
                pn
                .getOutputArcs()
                .stream()
                .filter((OutputArc oa) -> oa.getPlace().equals(this))
                .map(OutputArc::getTransition)
                .collect(Collectors.toCollection(ArrayList::new));
    }
    
    /**
     * Retrieves the possible transitions to go from this place
     * @param pn
     * @return 
     */
    public Collection<Transition> getNextTransitions(PetriNet pn) {
        return 
        pn.getInputArcs().stream()
                .filter(ia -> ia.getPlace() == this)
                .map(InputArc::getTransition)
                .collect(Collectors.toCollection(ArrayList::new));
    }
    
    public ArrayList<InputArc> getOutputArcs(PetriNet pn) {
        return 
                pn.getInputArcs().stream()
                .filter(ia -> ia.getPlace() == this)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * @return the capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * @param capacity the capacity to set
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}