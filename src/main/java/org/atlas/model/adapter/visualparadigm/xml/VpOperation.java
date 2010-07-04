package org.atlas.model.adapter.visualparadigm.xml;

import java.util.ArrayList;
import java.util.List;

public class VpOperation  extends VpElement  {
 private String name;
    private String returnType;
    private String returnMany;
    private String documentation;
    private List<VpParameter> params = new ArrayList<VpParameter>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<VpParameter> getParams() {
        return params;
    }

    public void setParams(List<VpParameter> params) {
        this.params = params;
    }

    public void addParameter(VpParameter p) {
        params.add(p);
    }

    public String getReturnMany() {
        return returnMany;
    }

    public void setReturnMany(String returnMany) {
        this.returnMany = returnMany;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

}
