/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet_OP;

import business.Token;
import business.Transition;

/**
 *
 * @author victor
 */
public class Deadlock {
   private Token lockedToken; 
   private Transition lockingTransition;
   private Token liberatorToken;
   private Firing liberatorFiring;

    public Deadlock(Token lockedToken, Transition lockingTransition) {
        this.lockedToken = lockedToken;
        this.lockingTransition = lockingTransition;
    }

    public Deadlock(Token lockedToken, Transition lockingTransition, Firing liberator) {
        this.lockedToken = lockedToken;
        this.lockingTransition = lockingTransition;
        this.liberatorFiring = liberator;
    }

    public Token getLockedToken() {
        return lockedToken;
    }

    public Transition getLockingTransition() {
        return lockingTransition;
    }

    public Firing getLiberatorFiring() {
        return liberatorFiring;
    }

    public void setLiberatorFiring(Firing liberator) {
        this.liberatorFiring = liberator;
    }

    public Token getLiberatorToken() {
        return liberatorToken;
    }

    public void setLiberatorToken(Token liberatorToken) {
        this.liberatorToken = liberatorToken;
    }
    
    public boolean equalDistribution(Deadlock d) {
        
        if (d.getLockingTransition().equals(lockingTransition) && 
                d.getLockedToken().getObject() == lockedToken.getObject())
            return true;
        
        return false;
    }

    @Override
    public String toString() {
        return "Deadlock{" + 
                "lockedToken=" + lockedToken + 
                ", lockingTransition=" + lockingTransition.getLabel() + 
                '}';
    }
    
    
}
