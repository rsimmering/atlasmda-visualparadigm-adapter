/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.atlas.model.adapter.visualparadigm.xml;

/**
 *
 * @author Administrator
 */
public class VpAssociationEnd  extends VpTypedElement {
    private String target;
    private String aggregation;
    private String mulitpicity;

    public String getTarget() {
            return target;
    }
    public void setTarget(String target) {
            this.target = target;
    }

    public String getAggregation() {
            return aggregation;
    }
    public void setAggregation(String association) {
            this.aggregation = association;
    }
    
    public String getMulitpicity() {
            return mulitpicity;
    }
    public void setMulitpicity(String mulitpicity) {
            this.mulitpicity = mulitpicity;
    }

}
