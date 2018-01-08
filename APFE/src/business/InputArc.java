/* Copyright Guillem Catala. www.guillemcatala.com/petrinetsim. Licensed http://creativecommons.org/licenses/by-nc-sa/3.0/ */
package business;

import java.util.Iterator;

/**
 *
 * @author Guillem
 */
public class InputArc extends Arc implements Inscription {

    /** Text string that represents the expression this arc will evaluate by default. */
    //private String evaluateText = "return getTokenSet().size()>0;";
    private String evaluateText = "getTokenSet().size()>0";
    /** Text string that represents the expression this arc will execute by default. */
    //private String executeText = "getTokenSet()"; CHANGE
    //TODO put this in a sublcass of InputArc
    private String executeText= "currentCase = getTokenSet().get(0)";

    /** Constructor to create an input arc. */
    public InputArc(Place place, Transition transition) {
        this.id = "i" + this.id + "_" + place.getId() + "_" + transition.getId();
        //this.id = "i" + this.id;
        this.place = place;
        this.transition = transition;
    }

    /** Constructor to create a new input arc with specific attributes. */
    public InputArc(String id, Place place, Transition transition, String evaluation, String action) {
        //this.id = "i" + this.id + "_" + place.getId() + "_" + transition.getId();
        this.id = id;
        this.place = place;
        this.transition = transition;
        this.evaluateText = evaluation;
        this.executeText = action;
    }

    /** Default method that is evaluated to check if the arc is enabled. */
    public boolean evaluate() {
        return getTokenSet().size() > 0;
    }

    /** Default method that is executed when a transition fires. */
    public TokenSet execute() {
        return new TokenSet(executeText);
    }

    /** Returns the TokenSet of the place this arc is connected*/
    public TokenSet getTokenSet() {
        return this.getPlace().getTokens();
    }

    /** Given a set of timed tokens it returns a token whose
     * timestamp value is less or equal than the current simulation
     * global clock */
    public Token removeTimedToken(TokenSet tokenSet) {
        Iterator it = tokenSet.iterator();
        Token token = null;
        while (it.hasNext()) {
            Token i = (Token) it.next();
            if (i.getTimestamp() <= transition.getGlobalClock()) {
                token = i;
            }
        }
        
        return token;
    }

    /**
     * @return the evaluateText
     */
    public String getEvaluateText() {
        return evaluateText;
    }

    /**
     * @param evaluateText the evaluateText to set
     */
    public void setEvaluateText(String evaluateText) {
        this.evaluateText = evaluateText;
    }

    /**
     * @return the executeText
     */
    public String getExecuteText() {
        return executeText;
    }

    /**
     * @param executeText the executeText to set
     */
    public void setExecuteText(String executeText) {
        this.executeText = executeText;
    }
}
