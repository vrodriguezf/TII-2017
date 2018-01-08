/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package presentation.figures;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import presentation.Grid;

/**
 *
 * @author victor
 */
public class InstantTransitionFigure extends TransitionFigure {
    
    public static int I_WIDTH = Grid.cellSize;
    public static int I_HEIGHT = 2;
    
    public InstantTransitionFigure(String transitionId, Point2D position) {
        super(transitionId, position);
    }
    
    @Override
    public RectangularShape getBounds() {
        return new Rectangle2D.Double(position.getX() - I_WIDTH / 2, position.getY() - I_HEIGHT / 2, I_WIDTH, I_HEIGHT);
    }
}
