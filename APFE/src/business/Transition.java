/* Copyright Guillem Catala. www.guillemcatala.com/petrinetsim. Licensed http://creativecommons.org/licenses/by-nc-sa/3.0/ */
package business;

import APFE.guard_conditions.GuardCondition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import presentation.GUI;

/**
 *
 * @author Guillem
 */
public class Transition extends NetObject implements Inscription {

    private static final Logger LOG = Logger.getLogger(Transition.class.getName());

    public Transition() {
        this.id = "t" + this.id;
    }

    public Transition(String id) {
        this.id = id;
    }

    public Transition(String id, String guardText) {
        this.id = id;
        this.guardText = guardText;
    }
    
    //CHANGE
    public Transition(String id, String guardText, String executeText) {
        this.id = id;
        this.guardText = guardText;
        this.executeText = executeText;
    }

    /** Fires a transition. */
    public void fire(GUI gui, long globalClock) {
        String msg;
        this.globalClock = globalClock;

        // Highlight places ON
        if (gui!= null && gui.getCanvas() != null)
            gui.getCanvas().highlightPlaces(Global.petriNet.getInputArcs(), id, true, false);
            
        // Highlight inputArcs ON
        if (gui!= null && gui.getCanvas() != null)
            gui.getCanvas().highlightArcs(Global.petriNet.getInputArcs(), id, true, true);
        
        //Create a copy of the transition tokens CHANGE
        //TokenSet oldTokens = getTokenSet();

        //remove all tokens from places
        Iterator it = Global.petriNet.getInputArcs().iterator();
        while (it.hasNext()) {
            InputArc arc = (InputArc) it.next();
            if (arc.getTransition().getId().equals(getId())) {
                arc.getPlace().removeTokens(arc.execute());
                
                if (gui!=null) {
                    gui.getJTextArea1().append("- " + arc.getExecuteText() + "\n");
                    gui.getJTextArea1().setCaretPosition(gui.getJTextArea1().getText().length());                    
                } else {
                    LOG.log(Level.FINE, "- {0}\n", arc.getExecuteText());
                }
                
            }
        }

        // Highlight places OFF
        if (gui!=null && gui.getCanvas() != null)        
            gui.getCanvas().highlightPlaces(Global.petriNet.getInputArcs(), id, false, false);

        // Highlight inputArcs OFF
        if (gui!=null && gui.getCanvas() != null)        
            gui.getCanvas().highlightArcs(Global.petriNet.getInputArcs(), id, false, false);

        // Highlight transition ON
        if (gui!=null && gui.getCanvas() != null)        
            gui.getCanvas().highlightTransition(id, true, true);
        if (!this.getLabel().equals(this.getId())) {
            msg = this.getLabel() + " (" + this.getId() + ") fired!\n";
            if (gui!=null)
                gui.getJTextArea1().append(msg);
            else
                LOG.log(Level.FINE,msg);
        } else {
            msg = this.getId() + " fired.\n";
            if (gui!=null)
                gui.getJTextArea1().append(msg);
            else 
                LOG.log(Level.FINE,msg);
        }
        if (gui!=null)
            gui.getJTextArea1().setCaretPosition(gui.getJTextArea1().getText().length());


        // Highlight transition OFF
        if (gui!=null && gui.getCanvas() != null)        
            gui.getCanvas().highlightTransition(id, false, false);

        // Highlight outputArcs ON
        if (gui!=null && gui.getCanvas() != null)        
            gui.getCanvas().highlightArcs(Global.petriNet.getOutputArcs(), id, true, false);


        // Highlight places ON
        if (gui!=null && gui.getCanvas() != null)        
            gui.getCanvas().highlightPlaces(Global.petriNet.getOutputArcs(), id, true, true);
        
        //EXECUTE transition!!!! (add time to the tokens)
        //TODO revisar esto
        TokenSet tokenSet = execute();
        if (gui!=null) {
            gui.getJTextArea1().append("Execution Time: " + tokenSet.get(0).getTimestamp() + "\n");
            gui.getJTextArea1().setCaretPosition(gui.getJTextArea1().getText().length());
            gui.getJTextArea1().append("Execution TokenSet: " + tokenSet.toString() + "\n");
            gui.getJTextArea1().setCaretPosition(gui.getJTextArea1().getText().length());            
        } else {
            LOG.log(Level.FINE, "Execution Time: {0}\n", tokenSet.get(0).getTimestamp());
            LOG.log(Level.FINE, "Execution TokenSet: {0}\n", tokenSet.toString());
        }


        // Create all tokens to output places
        it = Global.petriNet.getOutputArcs().iterator();
        while (it.hasNext()) {
            OutputArc arc = (OutputArc) it.next();
            if (arc.getTransition().getId().equals(getId())) {
                //TODO Hay que revisar como hacer de forma elegante el estilo Token Workflow
                //TokenSet tokenSet = arc.execute(); //CHANGE
                tokenSet = arc.execute();
                //TokenSet tokenSet = new TokenSet(oldTokens.get(0)); //CHANGE!
                //tokenSet.incrementTime(globalClock);// set time of all new tokens of the tokenSet
                arc.getPlace().addTokens(tokenSet);

                if (gui!=null) {
                    gui.getJTextArea1().append("+ " + arc.getExecuteText() + "\n");
                    gui.getJTextArea1().setCaretPosition(gui.getJTextArea1().getText().length());                    
                } else {
                    LOG.log(Level.FINE, "+ {0}\n", arc.getExecuteText());
                }
            }
        }

        // Highlight outputArcs OFF
        if (gui!=null && gui.getCanvas() != null)        
            gui.getCanvas().highlightArcs(Global.petriNet.getOutputArcs(), id, false, false);

        // Highlight places OFF
        if (gui!=null && gui.getCanvas() != null)        
            gui.getCanvas().highlightPlaces(Global.petriNet.getOutputArcs(), id, false, false);

        if (gui!=null) {
            gui.getJTextArea1().append("----------------------------\n");            
        }
        
        if (gui!=null && gui.getCanvas() != null)        
            gui.getCanvas().repaint();
    }
    
    public boolean inputArcsEnabled(long time) {
        boolean enabled = true;
        
        // input arc guards
        if (enabled && !Global.petriNet.getInputArcs().isEmpty()) {
            Iterator it = Global.petriNet.getInputArcs().iterator();
            while (enabled && it.hasNext()) {
                InputArc arc = (InputArc) it.next();
                if (arc.getTransition().getId().equals(getId())) {
                    TokenSet tokensList = arc.getPlace().getTokens();
                    // TODO Quitamos la comprobación de tiempo??
                    //enabled = tokensList.containsTime(time);
                    // check arc's evaluation expression
                    enabled = enabled & arc.evaluate();
                }
            }
        }
        
        return enabled;
    }
    
    public boolean outputArcsEnabled(long time) {
        boolean enabled = true;
        
        if (enabled && !Global.petriNet.getOutputArcs().isEmpty()) {
            Iterator it = Global.petriNet.getOutputArcs().iterator();
            while (enabled && it.hasNext()) {
                OutputArc arc = (OutputArc) it.next();
                if (arc.getTransition().getId().equals(getId())) {
                    TokenSet tokensList = arc.getPlace().getTokens();
                    // check if places have capacity limit
                    if (arc.getPlace().getCapacity() != 0) {
                        enabled = enabled & arc.getPlace().getCapacity() > tokensList.size();
                    }
                }
            }
        }
        
        return enabled;
    }

    
    /**
     * //TODO En un Workflow, las transiciones que no están asociadas a places con tokens 
     * no deberían comprobar su query de evaluate, porque es ineficientee!!!
     * @param time
     * @return 
     * @deprecated
     */
    public boolean enabled(long time) {
        // transition guard evaluation
        //boolean enabled = evaluate(); CHANGE
        boolean enabled = true;

        // input arc guards
        enabled = enabled && inputArcsEnabled(time);

        // check output arc place capacity restriction
        enabled = enabled && outputArcsEnabled(time);
        
        //Check the transition guard (Only if the rest of the checks have been successfull)
        if (enabled) {
            enabled = enabled && evaluate();
        }

        return enabled;
    }
    
    /**
     * 
     * @param time
     * @param verifyGuards
     * @return 
     */
    public boolean enabled(long time, boolean verifyGuards) {
        boolean enabled = true;

        // input arc guards
        enabled = enabled && inputArcsEnabled(time);

        // check output arc place capacity restriction
        enabled = enabled && outputArcsEnabled(time);
        
        //Check the transition guard 
        // (Only if the rest of the checks have been successfull)
        if (verifyGuards && enabled) {
            enabled = enabled && evaluate();
        }

        return enabled;
    }

    /**
     * This function is overwritten when creating Petri Nets using the GUI
     * It is also known as Guard Condition
     * @return 
     */
    public boolean evaluate() {
        return true;
    }

    //TODO Definir aquí los comportamientos especiales de algunas transiciones (AND-join)
    public TokenSet execute() {
        throw new UnsupportedOperationException("Not supported yet.");
        //return new TokenSet(executeText);
    }

    public TokenSet getTokenSet() {
        // Union of all the tokens of input arcs
        TokenSet result = new TokenSet();
        
        for (InputArc arc : Global.petriNet.getInputArcs()) {
            if (arc.getTransition().getId().equals(getId())) {
                result.addAll(arc.getTokenSet());
            }        
        }
        
        return result;        
    }
    
    public Collection<Place> getInputPlaces() {
        return
        Global.petriNet.getInputArcs().stream()
         .filter((InputArc t) -> t.getTransition().getId().equals(getId()))
         .map(InputArc::getPlace)
         .collect(Collectors.toCollection(ArrayList::new)); 
    }
    
    public Collection<Place> getOutputPlaces() {
        
        return
        Global.petriNet.getOutputArcs().stream()
                .filter((OutputArc t) -> t.getTransition().getId().equals(getId()))
                .map(OutputArc::getPlace)
                .collect(Collectors.toCollection(ArrayList::new));
    }
    
    /**
     * Retrieve all the trnasitions which COULD be fired in this Petri Net once
     * this one has been fired, based on the union of the transitions associated to every
     * output place of this transition
     * @param pn
     * @return List (with no repetitions) of next possible trnasitions
     */
    public List<Transition> getNextTransitions(PetriNet pn) {
        return this
                .getOutputPlaces()
                .stream()
                .flatMap((Place p) -> {
                    return p.getNextTransitions(pn).stream();
                })
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<Transition> getPreviousTransitions(PetriNet pn) {
        return this
                .getInputPlaces()
                .stream()
                .flatMap((Place p) -> {
                    return p.getPreviousTransitions(pn).stream();
                })
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * @return the inscriptionText
     */
    public String getGuardText() {
        return guardText;
    }

    /**
     * @param inscriptionText the inscriptionText to set
     */
    public void setGuardText(String guardText) {
        this.guardText = guardText;
    }

    /**
     * @return the globalClock
     */
    public long getGlobalClock() {
        return globalClock;
    }

    public String getExecuteText() {
        return executeText;
    }

    public void setExecuteText(String executeText) {
        this.executeText = executeText;
    }

    public String getGuardConditionText() {
        return guardConditionText;
    }

    public void setGuardConditionText(String guardConditionText) {
        this.guardConditionText = guardConditionText;
    }

    public GuardCondition getGuardCondition() {
        return guardCondition;
    }

    public void setGuardCondition(GuardCondition guardCondition) {
        this.guardCondition = guardCondition;
    }

    @Override
    public String toString() {
        return "Transition{" + 
                "id=" + id + 
                ", label=" + label +
                '}';
    }
    
    /**
     * Attributes
     */
    /** Global clock when the transition fires.*/
    private long globalClock;
    
    //CHANGE TODO hacer esto en una clase especial
    private String guardText = "return true;";
    private String executeText = "currentCase = new Token(currentCase.getObject(),currentCase.getTimestamp())";
    private String guardConditionText = "new TrivialGuardCondition()";
    
    //Guard Condition (APFE)
    private GuardCondition guardCondition;
}


