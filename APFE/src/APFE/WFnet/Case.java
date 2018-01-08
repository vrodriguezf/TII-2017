/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet;

import business.Token;

/**
 *
 * @author victor
 */
public class Case extends Token {
    private boolean evaluatedColor;
    
    public Case(Object object, long timestamp, boolean evaluatedColor) {
        super(object,timestamp);
        this.evaluatedColor = evaluatedColor;
    }
    
    public Case (Object object, long timestamp) {
        this(object,timestamp,false);
    }
}
