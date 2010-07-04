/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.atlas.model.adapter.visualparadigm.xml;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class VpTypedElement extends VpElement {

        private String namespace;
	private String xmlType;
	private String visibility;
	private List<String> stereotypes;

	public VpTypedElement() {
                super();
		stereotypes = new ArrayList<String>(5);
	}

        public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
        
	public String getXmlType() {
		return xmlType;
	}
	public void setXmlType(String type) {
		this.xmlType = type;
	}

	public String getVisibility() {
		return visibility;
	}
	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}

	public List<String> getStereotypes() {
		return stereotypes;
	}
	
	public void addStereotype(String stereotype) {
		stereotypes.add(stereotype);
	}

	protected String printProperties() {
		StringBuffer sb = new StringBuffer(super.printProperties());
		sb.append("\n\txmiType: ");
		sb.append(getXmlType());
		sb.append("\n\tvisibility: ");
		sb.append(getVisibility());

		return sb.toString();
	}

}
