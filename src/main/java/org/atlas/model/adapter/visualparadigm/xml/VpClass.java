package org.atlas.model.adapter.visualparadigm.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VpClass extends VpTypedElement {
	private Map<String,VpAttribute> attributes;
        private Map<String,VpOperation> operations;

	public VpClass() {
		super();
                attributes = new HashMap<String,VpAttribute>(20);
                operations = new HashMap<String,VpOperation>(20);
	}

	public void addAttribute(VpAttribute oa) {
		attributes.put(oa.getName(), oa);
	}

	public VpAttribute getAttribute(String id) {
		return attributes.get(id);
	}

	public Collection<VpAttribute> getAttributes() {
		return attributes.values();
	}

        public void addOperation(VpOperation oa) {
		operations.put(oa.getName(), oa);
	}

	public VpOperation getOperation(String id) {
		return operations.get(id);
	}

	public Collection<VpOperation> getOperations() {
		return operations.values();
	}

	protected String printChildren() {
		StringBuffer sb = new StringBuffer();

		for(VpAttribute oa : getAttributes()) {
			sb.append(oa.toString());
		}

                for(VpOperation oa : getOperations()) {
			sb.append(oa.toString());
		}
		return sb.toString();
	}


}
