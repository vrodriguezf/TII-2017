/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.evaluation;

import APFE.WFnet_OP.Firing;
import business.Transition;

/**
 *
 * @author victor
 */
public class Overreaction {
    private Firing action;
    private Transition missingCheck;

    public Overreaction(Firing action, Transition missingCheck) {
        this.action = action;
        this.missingCheck = missingCheck;
    }

    public Firing getAction() {
        return action;
    }

    public Transition getMissingCheck() {
        return missingCheck;
    }

    @Override
    public String toString() {
        return "Overreaction{" + 
                "action=" + action.getTransition()+ 
                ", missingCheck=" + missingCheck.getLabel() + 
                '}';
    }
}
