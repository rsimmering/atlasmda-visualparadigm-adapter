/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.atlas.model.adapter.visualparadigm.xml;

/**
 *
 * @author Administrator
 */
public class VpGeneralization  extends VpTypedElement {

	private String fromEnd;
	private String toEnd;

	public String getFromEnd() {
		return fromEnd;
	}
	public void setFromEnd(String fromEnd) {
		this.fromEnd = fromEnd;
	}

        public String getToEnd() {
		return toEnd;
	}
	public void setToEnd(String toEnd) {
		this.toEnd = toEnd;
	}

	protected String printProperties() {
		StringBuffer sb = new StringBuffer(super.printProperties());
		sb.append("\n\tFromEnd: ");
		sb.append(getFromEnd());
		sb.append("\n\tToEnd: ");
		sb.append(getToEnd());

		return sb.toString();
	}

}
