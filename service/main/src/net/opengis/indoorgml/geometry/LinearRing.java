package net.opengis.indoorgml.geometry;

import java.io.Serializable;
import java.util.ArrayList;

import edu.pnu.visitor.IndoorGMLElementVisitor;

public class LinearRing extends LineString implements Serializable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 966994570010972393L;
	
	public boolean isClosed(){
		return true;
	}
	
	public boolean isRing() {
		return true;
	}
	
	@Override
	public void accept(IndoorGMLElementVisitor visitor) {
		// TODO Auto-generated method stub
		//visitor.visit(this);
	}

	@Override
	public LinearRing clone() {
		LinearRing clone = new LinearRing();
		clone.setPoints((ArrayList<Point>) this.getPoints().clone());
		return clone;
	}
	
}