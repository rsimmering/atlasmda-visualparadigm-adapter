package org.atlas.model.adapter.visualparadigm.xml;

public class VpAssociation extends VpTypedElement {

	private VpAssociationEnd fromEnd;
	private VpAssociationEnd toEnd;

	public VpAssociationEnd getFromEnd() {
		return fromEnd;
	}
	public void setFromEnd(VpAssociationEnd fromEnd) {
		this.fromEnd = fromEnd;
	}

        public VpAssociationEnd getToEnd() {
		return toEnd;
	}
	public void setToEnd(VpAssociationEnd toEnd) {
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
