package org.atlas.model.adapter.visualparadigm.xml;

public class VpAttribute extends VpTypedElement {

	private String type;

	public String getType() {
		return type;
	}
	public void setType(String typeReference) {
		this.type = typeReference;
	}

	protected String printProperties() {
		StringBuffer sb = new StringBuffer(super.printProperties());
		sb.append("\n\ttype: ");
		sb.append(getType());

		return sb.toString();
	}
}
