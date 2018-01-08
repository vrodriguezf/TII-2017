/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet;

import APFE.WFnet_OP.Deadlock;
import APFE.WFnet_OP.Firing;
import APFE.WFnet_OP.MarkingOld;
import business.Global;
import business.Place;
import business.Token;
import business.TokenSet;
import business.Transition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import presentation.GUI;

/**
 *
 * @author victor
 */
public class WorkflowSimulation {

    private static final Logger LOG = Logger.getLogger(WorkflowSimulation.class.getName());
    
    protected long time = 0;
    private GUI gui; //TODO remove this dependency
    
    private WorkflowNet wfNet;
    protected Collection<MarkingOld> markingTrace;
    protected List<Firing> firingTrace;
    protected ArrayList<TraceEntry> executionTrace;
    private Transition firedTransition;
    private ArrayList<Transition> enabledTransitions;
    
    public WorkflowSimulation(WorkflowNet wfNet) {
        
        this.wfNet = wfNet;
    }
    
    public void start() {
        //Reset marking trace
        this.setMarkingTrace(new ArrayList<>());
            this.getMarkingTrace().add(wfNet.getMarking());
            
        //Reset transition trace
        this.setFiringTrace(new ArrayList<>());

        //Run
        run();
    }
    
    
    public boolean isFinished() {
        enabledTransitions = this.enabledTransitionList();
        //boolean isDead = this.enabledTransitionList().isEmpty(); //CHANGE
        boolean isDead = enabledTransitions.isEmpty();
        if (isDead) { //CHANGE
            incrementTime();
            enabledTransitions = this.enabledTransitionList();
            if (!enabledTransitions.isEmpty()) {
                isDead = false;
            }
        } else {
            isDead = false;
        }
        return isDead;
    }
    
    public ArrayList enabledTransitionList() {
        Iterator it = Global.petriNet.getTransitions().iterator();
        ArrayList enabledTransitions = new ArrayList();
        while (it.hasNext()) {
            Transition transition = (Transition) it.next();
            if (transition.enabled(time)) {
                enabledTransitions.add(transition);
            }
        }
        return enabledTransitions;
    }
    
    protected void fireTransition() {
        if (!enabledTransitions.isEmpty()) {
            firedTransition = getRandomTransition();
            firedTransition.fire(this.gui, this.time);
        }
    }
    
    public void incrementTime() {
        // Visit all places tokens and check whether they have timestamp>0 and less than globalclock
        // assign the global clock to the mimnimum found
        long minTime = 999999999;
        ArrayList places = Global.petriNet.getPlaces();
        for (int i = 0; i < places.size(); i++) {
            Place place = (Place) places.get(i);
            TokenSet tokenList = place.getTokens();
            if (tokenList.size() > 0) {
                for (int j = 0; j < tokenList.size(); j++) {
                    Token token = (Token) tokenList.get(j);
                    if (token.getTimestamp() != 0 && token.getTimestamp() < minTime) {
                        minTime = token.getTimestamp();
                    }
                }
            }
        }
        if (minTime != 999999999) {
            this.time = minTime;
        }
        if (gui!=null) {
            this.gui.getTxtClock().setText(String.valueOf(this.time));            
        }
    }    
    
    /** Returns a random transition from the enabled transition list
     * @return  */
    public Transition getRandomTransition() {
        Random generator = new Random();
        int rand = generator.nextInt(enabledTransitions.size());
        return (Transition) enabledTransitions.get(rand);
    }
    
    
    public void run() {
        while (!isFinished()) {
            fireTransition();       
            //Reset currentCase
            postFireActions();
        }
        
        LOG.log(Level.INFO,"Deadlock.\n");
    }
    
    protected void postFireActions() {
        
        //Append current MarkingOld to marking trace
        this.getMarkingTrace().add(wfNet.getMarking());
        
        //Append fired transition to the transition trace
        Firing firing = new Firing(wfNet.currentCase,firedTransition);
        
        this.getFiringTrace().add(
                firing
        );
        //Establish the duration of the firing
        firing.setDuration(getFiringDuration(firing));
        
        //Reset current case
        wfNet.currentCase.setPassedTransitionGuard(false);
        wfNet.currentCase = null;
    }
    
    /**
     * Private methods
     */
    
    /**
     * Returns the token in mSrc who has traveled to mDst (Returns the instance
     * in the source marking)
     * @param mSrc
     * @param mDst
     * @return 
     */
    private Token getPassingToken(MarkingOld mSrc, MarkingOld mDst) {
        
        //Compute the difference between the source and destiny markings
        MarkingOld mDiff = mSrc.diff(mDst);
        
        //The diff marking should have only one token
        if (mDiff.getNumberOfTokens() > 1) {
            return null;
        } else if (mDiff.getNumberOfTokens() == 0) {
            //TODO: REVISAR!!!!!!
            LOG.log(Level.WARNING,"0 tokens in the difference between marking " + mSrc.toString() + " and " + mDst.toString());
           return null;
        }else {
            return new ArrayList<>(mDiff.values()).get(0).get(0);
        }
    }
    
    /**
     * Retrieves the token passing through a firing (before passing it)
     * @param f
     * @return 
     */
    public Token getPassingToken(Firing f) {
        List<Firing> firingList = new ArrayList<>(this.getFiringTrace());
        List<MarkingOld> markingList = new ArrayList<>(this.getMarkingTrace());
        
        int indexOfF = firingList.indexOf(f);
        if (indexOfF == -1) return null;
        else {
            return this.getPassingToken(
                    markingList.get(indexOfF),
                    markingList.get(indexOfF + 1));
        }
    }
    
    
    /**
     * The duration of a firing is computed as the difference between
     * the token timestamp in this firing and the timestamp of the same token
     * in the last firing of the net whose transition is a parent of this 
     * firing transition. TODO: Not working!!!
     * @param f
     * @return 
     */
    private Long getFiringDuration(Firing firing) {  
        Long currentTokenTS = firing.getToken().getTimestamp();
        Long previousTokenTS = null;
        
        //Retrives the firings of the previous transition to this one
        Firing previousFiring = this
                .getFiringTrace()
                .stream()
                .filter((Firing _f) -> {
                    return firing
                            .getTransition()
                            .getPreviousTransitions(this.wfNet)
                            .contains(_f.getTransition());
                })
                .filter((Firing _f) -> {
                    return firing.getToken().getObject().equals(_f.getToken().getObject());
                })
                .max((Firing f1, Firing f2) -> {
                    return (int) (f1.getToken().getTimestamp() - f2.getToken().getTimestamp());
                })
                .orElse(null);
        
        if (previousFiring != null) {
            previousTokenTS = previousFiring.getToken().getTimestamp();
        } else {
            //Retrieve the initial timestamp of this workflow case from the marking traces
            //TODO: Este metodo se puede hacer siempre así...
            previousTokenTS = (this.getPassingToken(firing) != null) 
                                    ? this.getPassingToken(firing).getTimestamp()
                                    : null;
        }
        
        if (currentTokenTS == null || previousTokenTS == null) {
            return null;
        } else {
            return currentTokenTS - previousTokenTS;
        }
    }
    
    /**
     * Find the firing in this simulation which caused that a given token 
     * moved from its source place.
     * @param t
     * @return 
     */
    public Firing findLiberator(Token t) {
        
        boolean lastFound = false;
        boolean found = false;
        
        List<MarkingOld> markingList = new ArrayList<>(this.getMarkingTrace());
        for (int i=0; i<getMarkingTrace().size();i++) {
            MarkingOld m = markingList.get(i);
                       
            if (m.getPlaceOf(t)!=null) found = true;
            else found = false;
            
            if (found == false && lastFound == true) {
                //Entre estos 2 markings esta el firing que queremos (i-1)
                return getFiringTrace().get(i-1);
            }
            lastFound = found;
        }
        
        //Si no se ha salido en el bucle, no hay liberador
        return null;
    }
    
    /**
     * Encuentra el liberador de un deadlock a traves de una comparativa de token
     * y transicion por contenido. Podría fallar en un caso extremo en el que durante este
     * workflow un token diferente al deadlock.token llegara a la locking transition con 
     * exactamente el mismo timestamp y color que el locking token (extremisimo)
     * @param dl
     * @return 
     */
    public Firing findLiberatorFiring(Deadlock dl) {
        return getFiringTrace()
                .stream()
                .filter((Firing f) -> {
                    return f.getTransition() == dl.getLockingTransition() &&
                            getPassingToken(f).equals(dl.getLiberatorToken());
                })
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Getters & Setters
     */
    public Collection<MarkingOld> getMarkingTrace() {
        return markingTrace;
    }

    public void setMarkingTrace(Collection<MarkingOld> markingTrace) {
        this.markingTrace = markingTrace;
    }

    public List<Firing> getFiringTrace() {
        return firingTrace;
    }

    public void setFiringTrace(List<Firing> firingTrace) {
        this.firingTrace = firingTrace;
    }
}
