/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet_OP;

import APFE.WFnet.WorkflowNet;
import business.Place;
import business.Token;
import business.TokenSet;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author victor
 */
public class MarkingOld extends HashMap<Place,TokenSet> {
    
    //TODO Possible methods -> Parse MarkingOld expression via string
    public void add(Place p, Token t) {
        TokenSet placeTokens = get(p);
        
        if (placeTokens == null) {
            put(p, new TokenSet(t));
        } else {
            placeTokens.add(t);
        }
    }
    
    /**
     * 
     * @param p
     * @param ts 
     */
    public void add(Place p, TokenSet ts) {
        
        TokenSet placeTokens = get(p);
        
        if (placeTokens == null) {
            put(p, ts);
        } else {
            placeTokens.addAll(ts);
        }
    }
    
    public int getNumberOfTokens() {
        return 
        values().stream()
                .mapToInt(TokenSet::size)
                .reduce((int left, int right) -> left + right)
                .orElse(0);
    }
    
    public Place getPlaceOf(Token token) {
        
        for (Entry<Place,TokenSet> entry : entrySet()) {
            
            for (Object _token : entry.getValue().getTokenList()) {
                if (_token == token)
                    return entry.getKey();
            }
            
            //if (entry.getValue().contains(token)) return entry.getKey();
        }
        
        return null;
    }
    
    /**
     * This is not the intersection, it is the difference (this\m)
     * @param m
     * @return 
     */
    public MarkingOld diff(MarkingOld m) {
        MarkingOld result = new MarkingOld();
        
        entrySet().stream().forEach((entry) -> {
            if (m.get(entry.getKey()) == null) 
                result.add(entry.getKey(), entry.getValue());
            else {
                //Diferencias entre TokenSets
                /*
                TokenSet ts = new TokenSet(entry.getValue());
                ts.removeAll(m.get(entry.getKey()));
                if (!ts.isEmpty()) result.add(entry.getKey(), ts);
                */
                TokenSet ts = new TokenSet();
                entry
                        .getValue()
                        .getTokenList()
                        .stream()
                        .filter((Object _token) -> {
                            Token __token = (Token) _token;
                            return !m.get(entry.getKey()).contains(_token);
                        })
                        .forEach((Object _token) -> {
                            Token __token = (Token) _token;
                            ts.add(__token);
                        });
                if (!ts.isEmpty()) result.add(entry.getKey(), ts);
            }
        });
        
        return result; 
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
        return this.toString().equals(m.toString());
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
        
        for (Entry<Place,TokenSet> entry : entrySet()) {
            if (entry.getValue().size() > 0) {
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
