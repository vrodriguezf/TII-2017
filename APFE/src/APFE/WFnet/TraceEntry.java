/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet;

import APFE.WFnet_OP.MarkingOld;
import business.TokenSet;
import business.Transition;

/**
 *
 * @author victor
 */
public class TraceEntry {
    private Transition firedTransition;
    private MarkingOld previousMarking;
    private MarkingOld newMarking;
    private TokenSet consumedTokens;
    private TokenSet producedTokens;

    public TraceEntry(Transition firedTransition, MarkingOld previousMarking, MarkingOld newMarking, TokenSet consumedTokens, TokenSet producedTokens) {
        this.firedTransition = firedTransition;
        this.previousMarking = previousMarking;
        this.newMarking = newMarking;
        this.consumedTokens = consumedTokens;
        this.producedTokens = producedTokens;
    }

    public Transition getFiredTransition() {
        return firedTransition;
    }

    public void setFiredTransition(Transition firedTransition) {
        this.firedTransition = firedTransition;
    }

    public MarkingOld getPreviousMarking() {
        return previousMarking;
    }

    public void setPreviousMarking(MarkingOld previousMarking) {
        this.previousMarking = previousMarking;
    }

    public MarkingOld getNewMarking() {
        return newMarking;
    }

    public void setNewMarking(MarkingOld newMarking) {
        this.newMarking = newMarking;
    }

    public TokenSet getConsumedTokens() {
        return consumedTokens;
    }

    public void setConsumedTokens(TokenSet consumedTokens) {
        this.consumedTokens = consumedTokens;
    }

    public TokenSet getProducedTokens() {
        return producedTokens;
    }

    public void setProducedTokens(TokenSet producedTokens) {
        this.producedTokens = producedTokens;
    }
    
    
}
