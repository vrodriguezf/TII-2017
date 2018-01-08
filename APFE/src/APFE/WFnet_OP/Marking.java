/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet_OP;

import APFE.WFnet.WorkflowNet;
import business.Place;
import business.Token;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author victor
 */
public class Marking extends HashMap<Place,Set<Token>> { 
    
    @Override
    public Set<Token> get(Object key) {
        return super.getOrDefault(key, Collections.<Token>emptySet());
    }
    
    //TODO Possible methods -> Parse Marking expression via string
    public void add(Place p, Token t) {
        Set<Token> placeTokens = get(p);
        
        if (placeTokens.isEmpty()) {
            put(p, new HashSet<Token>(Arrays.asList(t)));
        } else {
            placeTokens.add(t);
        }
    }
    
    /**
     * 
     * @param p
     * @param kappa 
     */
    public void add(Place p, Set<Token> kappa) {
        Set<Token> placeTokens = get(p);
        
        if (placeTokens.isEmpty()) {
            put(p, kappa);
        } else {
            placeTokens.addAll(kappa);
        }
    }
    
    /**
     * 
     * @return 
     */
    public int getNumberOfTokens() {
        return 
        values().stream()
                .mapToInt(Set::size)
                .reduce((int left, int right) -> left + right)
                .orElse(0);
    }
    
    /**
     * TODO: test this
     * @param token
     * @return 
     */
    public Place getPlaceOf(Token token) {
        
        for (Entry<Place,Set<Token>> entry : entrySet()) {
            
            for (Token _token : entry.getValue()) {
                if (_token == token)
                    return entry.getKey();
            }            
        }
        
        return null;
    }
    
    /**
     * This is not the intersection, it is the difference (this\m)
     * @param m
     * @return 
     */
    public MarkingOld diff(MarkingOld m) {
        throw new UnsupportedOperationException();
    }
    
     /**
     * Returns whether this marking has the same token distribution to the markings
     * passed as parameter. It does not analyze the content of the tokens, only the number and
     * position
     * @param m
     * @return 
     */
    public boolean equalsDistribution(MarkingOld m) {
        //TODO: Improve this implementation, which depends on the toString method
        throw new UnsupportedOperationException();
    }
    
     /**
     * Return whether this marking is final or not in a given workflow net
     * @param wfNet
     * @return 
     */
    public boolean isFinal(WorkflowNet wfNet) {
        Set<Place> markingPlaces = keySet();
        
        return markingPlaces.size() == 1 && 
                markingPlaces.iterator().next() == wfNet.getOutputPlace();
    }
    
    @Override
    public String toString() {
        String result =  "Marking{";
        
        for (Entry<Place,Set<Token>> entry : entrySet()) {
            if (entry.getValue() != null && entry.getValue().size() > 0) {
                if (!entry.getKey().getLabel().equals(""))
                    result += entry.getValue().size() + entry.getKey().getLabel();
                else
                    result += entry.getValue().size() + entry.getKey().getId();
                
                result += " + ";                
            }
        }
        
        if (size() >= 1) {
            result = result.substring(0, result.length()- 3);
        }
        
        result += "}";
        
        return result;
    }
}
