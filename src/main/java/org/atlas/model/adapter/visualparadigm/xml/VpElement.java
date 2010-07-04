package org.atlas.model.adapter.visualparadigm.xml;

import java.util.HashMap;
import java.util.Map;

public class VpElement {
        private String xmlId;
	private String name;

        private Map<String, String> tags;

        public VpElement(){
            tags = new HashMap<String, String>();
        }

        public void addTaggedValue(String name, String value){
            tags.put(name, value);
        }

        public String getTaggedValue(String name){
            return tags.get(name);
        }

        public Map<String, String> getTags(){
            return tags;
        }

        public void setTags(Map<String, String> tags){
            this.tags = tags;
        }

	public String getXmlId() {
		return xmlId;
	}

	public void setXmlId(String xmlId) {
		this.xmlId = xmlId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n[");
		sb.append(printProperties());
		sb.append(printChildren());
		sb.append(" ]");

		return sb.toString();
	}

	protected String printProperties() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n\txmlId: ");
		sb.append(getXmlId());
		sb.append("\n\tname: ");
		sb.append(getName());

		return sb.toString();
	}

	protected String printChildren() {
		return "";
	}
}
