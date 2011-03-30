package org.atlas.model.adapter.visualparadigm;

import java.util.HashMap;
import java.util.Map;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.atlas.engine.Stereotype;
import org.atlas.model.adapter.Adapter;
import org.atlas.model.adapter.AdapterException;
import org.atlas.model.metamodel.Association;
import org.atlas.model.metamodel.Boundary;
import org.atlas.model.metamodel.Element;
import org.atlas.model.metamodel.Literal;
import org.atlas.model.metamodel.Control;
import org.atlas.model.metamodel.Entity;
import org.atlas.model.metamodel.Enumeration;
import org.atlas.model.metamodel.Operation;
import org.atlas.model.metamodel.Parameter;
import org.atlas.model.metamodel.Model;
import org.atlas.model.metamodel.Property;
import org.atlas.model.metamodel.Tag;
import org.atlas.model.adapter.visualparadigm.xml.VpAssociation;
import org.atlas.model.adapter.visualparadigm.xml.VpAssociationEnd;
import org.atlas.model.adapter.visualparadigm.xml.VpAttribute;
import org.atlas.model.adapter.visualparadigm.xml.VpClass;
import org.atlas.model.adapter.visualparadigm.xml.VpGeneralization;
import org.atlas.model.adapter.visualparadigm.xml.VpElement;
import org.atlas.model.adapter.visualparadigm.xml.VpOperation;
import org.atlas.model.adapter.visualparadigm.xml.VpParameter;

public class ModelAdapter implements Adapter {

    private Map<String, String> stereotypes;
    private Map<String, String> datatypes;
    private Map<String, VpClass> classes;
    private List<VpAssociation> associations;
    private List<VpGeneralization> generalizations;
    private static final String NONE = "None";
    private static final String COMPOSITE = "Composited";
    private static final String MANY = "*";
    private static final String ONEMANY = "1..*";
    private static final String ZEROMANY = "0..*";
    private List<String> packageNames = new ArrayList();
    private List<String> packageXPaths = new ArrayList();
    private List<String> packageNamespaces = new ArrayList();
    private Model model;

    public ModelAdapter() {
        stereotypes = new HashMap<String, String>(15);
        datatypes = new HashMap<String, String>(15);
        generalizations = new ArrayList<VpGeneralization>(15);
        classes = new HashMap<String, VpClass>(15);
        associations = new ArrayList<VpAssociation>(15);
    }

    public Model adapt(File file, Model model) throws AdapterException {
        this.model = model;
        parse(file);
        normalize();

        return model;
    }

    private void processPackages(int index, AutoPilot ap, VTDNav vn) throws Exception {
        String packageName = packageNames.get(index);
        String packageXPath = packageXPaths.get(index);
        String packageNamespace = packageNamespaces.get(index);

        int i;
        ap.resetXPath();
        ap.selectXPath(packageXPath + "/Model[@name='" + packageName + "']/ChildModels/Model");

        while ((i = ap.evalXPath()) != -1) {
            String modelType = vn.toNormalizedString((vn.getAttrVal("modelType")));
            if ("Class".equals(modelType)) {
                parseClasses(vn, packageNamespace);
            } else if ("Package".equals(modelType)) {
                String name = vn.toNormalizedString((vn.getAttrVal("name")));
                packageNames.add(name);
                packageXPaths.add(packageXPath + "/Model[@name='" + packageName + "']/ChildModels");
                packageNamespaces.add(packageNamespace + "." + name);
            }
        }
        index++;
        if (index < packageNames.size()) {
            processPackages(index, ap, vn);
        }

    }

    public void parse(File file) {
        VTDGen vg = new VTDGen();
        AutoPilot ap = new AutoPilot();
        int i;

        try {
            // Process Stereotypes so we can reference by ID later
            ap.selectXPath("/Project/Models/Model");
            if (vg.parseFile(file.getPath(), true)) {
                VTDNav vn = vg.getNav();
                ap.bind(vn);
                while ((i = ap.evalXPath()) != -1) {
                    String modelType = vn.toNormalizedString((vn.getAttrVal("modelType")));
                    if ("Class".equals(modelType)) {
                        parseClasses(vn, "");
                    } else if ("Package".equals(modelType)) {
                        String name = vn.toNormalizedString((vn.getAttrVal("name")));
                        packageNames.add(name);
                        packageXPaths.add("/Project/Models");
                        packageNamespaces.add(name);
                    } else if ("DataType".equals(modelType)) {
                        datatypes.put(vn.toNormalizedString(vn.getAttrVal("id")), vn.toNormalizedString((vn.getAttrVal("name"))));
                    } else if ("Stereotype".equals(modelType)) {
                        String name = vn.toNormalizedString((vn.getAttrVal("name")));
                        String id = vn.toNormalizedString(vn.getAttrVal("id"));
                        stereotypes.put(id, name);
                    }
                }

                if(packageNames.size() > 0){
                    processPackages(0, ap, vn);
                }

                //Process Associations
                ap.resetXPath();
                ap.selectXPath("/Project/Models/Model[@name='relatioships']/ChildModels/Model[@name='Association']/ChildModels/Model[@modelType='Association']");

                while ((i = ap.evalXPath()) != -1) {
                    parseAssociations(vn);
                }

                //Process Generalizations
                ap.resetXPath();
                ap.selectXPath("/Project/Models/Model/ChildModels/Model[@name='Generalization']/ChildModels/Model[@modelType='Generalization']");

                while ((i = ap.evalXPath()) != -1) {
                    parseGeneralization(vn);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parseClasses(VTDNav vn, String packageNamespace) throws NavException {
        VpClass clazz = new VpClass();
        clazz.setNamespace(packageNamespace);

        String name = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
        String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);

        clazz.setName(name);
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to <ModelProperties>
        parseClassProperties(clazz, vn); // Parse properties
        vn.toElement(VTDNav.NEXT_SIBLING); // Move to <ChildModels>
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); // Move to <Model> for attribute
        //parseClassAttributes(clazz, vn); // Parse Attribute
        String modelType = vn.toNormalizedString((vn.getAttrVal("modelType") != -1) ? vn.getAttrVal("modelType") : 0);
        if ("Attribute".equals(modelType)) {
            parseClassAttributes(clazz, vn);
        } else if ("Operation".equals(modelType)) {
            parseOperations(clazz, vn);
        }

        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            modelType = vn.toNormalizedString((vn.getAttrVal("modelType") != -1) ? vn.getAttrVal("modelType") : 0);
            if ("Attribute".equals(modelType)) {
                parseClassAttributes(clazz, vn);
            } else if ("Operation".equals(modelType)) {
                parseOperations(clazz, vn);
            }
        }
        vn.pop();
        vn.pop();
        classes.put(id, clazz);
    }

    public void parseOperations(VpClass clazz, VTDNav vn) throws NavException {
        VpOperation operation = new VpOperation();
        String name = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
        operation.setName(name);
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to <ModelProperties>
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to first properties
        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            String fieldName = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
            if ("returnType".equals(fieldName)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                if(StringUtils.isBlank(id)){
                    id = vn.toNormalizedString((vn.getAttrVal("value") != -1) ? vn.getAttrVal("value") : 0);
                }

                operation.setReturnType(id);
                vn.pop();
            } else if ("typeModifier".equals(fieldName)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String typeModifierValue = vn.toNormalizedString((vn.getAttrVal("value") != -1) ? vn.getAttrVal("value") : 0);
                operation.setReturnMany(typeModifierValue);
                vn.pop();
            }

        }
        vn.pop();

        if(vn.toElement(VTDNav.NEXT_SIBLING)){
            vn.push();
            vn.toElement(VTDNav.FIRST_CHILD);
            parseParameter(operation, vn);

            while (vn.toElement(VTDNav.NEXT_SIBLING)) {
                parseParameter(operation, vn);
            }

            vn.pop();
        }

        vn.pop();
        clazz.addOperation(operation);
    }

    public void parseParameter(VpOperation operation, VTDNav vn) throws NavException {
        VpParameter parameter = new VpParameter();
        String name = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
        parameter.setName(name);
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to <ModelProperties>
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to first properties
        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            String fieldName = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
            if ("type".equals(fieldName)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                if(StringUtils.isBlank(id)){
                    id = vn.toNormalizedString((vn.getAttrVal("value") != -1) ? vn.getAttrVal("value") : 0);
                }
                parameter.setType(id);
                vn.pop();
            }
            else if("typeModifier".equals(fieldName)){
				String typeModifier = vn.toNormalizedString((vn.getAttrVal("value") != -1) ? vn.getAttrVal("value") : 0);
				if("*".equals(typeModifier)){
					parameter.setMany(true);
				}
			}
            else if ("direction".equals(fieldName)) {
                String directionValue = vn.toNormalizedString((vn.getAttrVal("value") != -1) ? vn.getAttrVal("value") : 0);
                parameter.setDirection(directionValue);
            }
        }
        vn.pop();
        vn.pop();
        operation.addParameter(parameter);
    }

    public void parseClassProperties(VpClass clazz, VTDNav vn) throws NavException {
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to first properties
        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            String fieldName = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
            if ("stereotypes".equals(fieldName)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                clazz.addStereotype(id);
                while (vn.toElement(VTDNav.NEXT_SIBLING)) {
                    id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                    clazz.addStereotype(id);
                }
                vn.pop();
            }
            if ("taggedValues".equals(fieldName)) {
                parseTaggedValues(clazz, vn);
            }
        }
        vn.pop();
    }

    public void parseClassAttributes(VpClass clazz, VTDNav vn) throws NavException {
        VpAttribute attribute = new VpAttribute();
        String name = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
        attribute.setName(name);
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to <ModelProperties>
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to first properties

        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            String fieldName = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
            if ("type".equals(fieldName)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                if(StringUtils.isBlank(id)){
                    id = vn.toNormalizedString((vn.getAttrVal("value") != -1) ? vn.getAttrVal("value") : 0);
                }
                attribute.setType(id);
                vn.pop();
            }
            if ("stereotypes".equals(fieldName)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                attribute.addStereotype(id);
                while (vn.toElement(VTDNav.NEXT_SIBLING)) {
                    id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                    attribute.addStereotype(id);
                }
                vn.pop();
            }
            if ("taggedValues".equals(fieldName)) {
                parseTaggedValues(attribute, vn);
            }
        }

        vn.pop();
        vn.pop();
        clazz.addAttribute(attribute);
    }

    public String findTaggedValue(VTDNav vn) throws NavException {
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); // move to model
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); // move to model
        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            String fieldName = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
            if ("value".equals(fieldName)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD); // move to model
                String tagValue = vn.toNormalizedString((vn.getAttrVal("value") != -1) ? vn.getAttrVal("value") : 0);
                vn.pop();
                vn.pop();
                vn.pop();
                return tagValue;
            }
        }
        vn.pop();
        vn.pop();
        return "";
    }

    public void parseTaggedValues(VpElement e, VTDNav vn) throws NavException {
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); // move to model
        String modelType = vn.toNormalizedString((vn.getAttrVal("modelType") != -1) ? vn.getAttrVal("modelType") : 0);
        if (!"TaggedValueContainer".equals(modelType)) {
            vn.pop();
            return;
        }
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); // move to ModelProperties
        vn.toElement(VTDNav.NEXT_SIBLING);  // Move to ChildModels
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); // move to First Model

        String name = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);

        String value = findTaggedValue(vn);
        if (name != null && value != null) {
            e.addTaggedValue(name, value);
        }

        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            name = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);

            value = findTaggedValue(vn);
            if (name != null && value != null) {
                e.addTaggedValue(name, value);
            }
        }
        vn.pop();
        vn.pop();
        vn.pop();
    }

    public void parseGeneralization(VTDNav vn) throws NavException {
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to <ModelProperties>
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to first properties

        VpGeneralization g = new VpGeneralization();
        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            String name = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
            g.setName(name);
            if ("from".equals(name)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                g.setToEnd(id);
                vn.pop();
            } else if ("to".equals(name)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                g.setFromEnd(id);
                vn.pop();
            }
            else if ("stereotypes".equals(name)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                g.addStereotype(id);
                while (vn.toElement(VTDNav.NEXT_SIBLING)) {
                    id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                    g.addStereotype(id);
                }
                vn.pop();
            }
            else if ("taggedValues".equals(name)) {
                parseTaggedValues(g, vn);
            }
        }
        generalizations.add(g);
        vn.pop();
        vn.pop();
    }

    public void parseAssociations(VTDNav vn) throws NavException {
        VpAssociation a = new VpAssociation();

        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to <ModelProperties>
        vn.push();
        vn.toElement(VTDNav.FIRST_CHILD); //Move to first properties
        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            String fieldName = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
            if ("stereotypes".equals(fieldName)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                a.addStereotype(id);
                while (vn.toElement(VTDNav.NEXT_SIBLING)) {
                    id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                    a.addStereotype(id);
                }
                vn.pop();
            }
            if ("taggedValues".equals(fieldName)) {
                parseTaggedValues(a, vn);
            }
        }
        vn.pop();


        while (vn.toElement(VTDNav.NEXT_SIBLING)) {
            String tag = vn.toString(vn.getCurrentIndex());

            VpAssociationEnd end = new VpAssociationEnd();
            if ("FromEnd".equals(tag) || "ToEnd".equals(tag)) {
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                String name = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
                end.setName(name);

                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                vn.push();
                vn.toElement(VTDNav.FIRST_CHILD);
                while (vn.toElement(VTDNav.NEXT_SIBLING)) {
                    String childTag = vn.toNormalizedString((vn.getAttrVal("name") != -1) ? vn.getAttrVal("name") : 0);
                    if ("EndModelElement".equals(childTag)) {
                        vn.push();
                        vn.toElement(VTDNav.FIRST_CHILD);
                        String id = vn.toNormalizedString((vn.getAttrVal("id") != -1) ? vn.getAttrVal("id") : 0);
                        end.setTarget(id);
                        vn.pop();
                    } else if ("multiplicity".equals(childTag)) {
                        end.setMulitpicity(vn.toNormalizedString((vn.getAttrVal("value") != -1) ? vn.getAttrVal("value") : 0));
                    } else if ("aggregationKind".equals(childTag)) {
                        end.setAggregation(vn.toNormalizedString((vn.getAttrVal("value") != -1) ? vn.getAttrVal("value") : 0));
                    }
                }
                vn.pop();
                vn.pop();
                vn.pop();
            }
            if ("FromEnd".equals(tag)) {
                a.setFromEnd(end);
            } else {
                a.setToEnd(end);
            }
        }
        associations.add(a);
        vn.pop();
    }

    private void normalize() throws AdapterException {

        for (VpClass e : classes.values()) {
            for (String stereotypeId : e.getStereotypes()) {
                String stereotypeName = stereotypes.get(stereotypeId);
                switch (Stereotype.valueOf(stereotypeName)) {
                    case entity:
                        addEntity(e);
                        break;
                    case enumeration:
                        addEnumeration(e);
                        break;
                    case control:
                        addControl(e);
                        break;
                    case boundary:
                        addBoundary(e);
                        break;
                    default:
                        break;
                }
            }
        }

        for (VpAssociation c : associations) {
            normalize(c);
        }

        for (VpGeneralization g : generalizations) {
            normalize(g);
        }
    }

    private void normalize(VpGeneralization c) {
        String entityName = classes.get(c.getFromEnd()).getName();
        Entity source = model.getEntity(entityName);
        source.setGeneralization(model.getEntity(classes.get(c.getToEnd()).getName()));
    }

    private void normalize(VpAssociation c) throws AdapterException {
        String sourceEntityName = classes.get(c.getFromEnd().getTarget()).getName();
        String targetEntityName = classes.get(c.getToEnd().getTarget()).getName();
        Entity source = model.getEntity(sourceEntityName);
        Entity target = model.getEntity(targetEntityName);

        if (source == null || target == null) {
            return;
        }

        String m = c.getFromEnd().getMulitpicity();
        boolean sourceIsMany = (m.equals(MANY) || m.equals(ZEROMANY) || m.equals(ONEMANY)) ? true : false;
        m = c.getToEnd().getMulitpicity();
        boolean targetIsMany = (m.equals(MANY) || m.equals(ZEROMANY) || m.equals(ONEMANY)) ? true : false;

        Association sourceAssn = new Association();
        sourceAssn.setEntity(model.getEntity(targetEntityName));
        addTags(c, sourceAssn);

        Association targetAssn = new Association();
        targetAssn.setEntity(model.getEntity(sourceEntityName));
        addTags(c, targetAssn);

        if (c.getFromEnd().getAggregation().equals(NONE) && c.getToEnd().getAggregation().equals(NONE)) {
            // Either M:1 or M:M
            if (sourceIsMany) {
                if (targetIsMany) {
                    sourceAssn.setMultiplicity(Association.Multiplicity.ManyToMany);
                    sourceAssn.setOwner(true);
                    targetAssn.setMultiplicity(Association.Multiplicity.ManyToMany);
                    source.addAssociation(sourceAssn);
                    target.addAssociation(targetAssn);
                } else {
                    sourceAssn.setMultiplicity(Association.Multiplicity.ManyToOne);
                    targetAssn.setMultiplicity(Association.Multiplicity.ManyToOne);
                    source.addAssociation(sourceAssn);
                }
            } else if (targetIsMany) {
                sourceAssn.setMultiplicity(Association.Multiplicity.ManyToOne);
                targetAssn.setMultiplicity(Association.Multiplicity.ManyToOne);
                target.addAssociation(targetAssn);
            }
        } else if (c.getFromEnd().getAggregation().equals(COMPOSITE) && c.getToEnd().getAggregation().equals(NONE)) {
            if (sourceIsMany) {
                throw new AdapterException("Only multiplicity of 1 can be on the composite side of association between [" + sourceEntityName + "] and [" + targetEntityName + "]");
            }

            sourceAssn.setOwner(true);
            targetAssn.setOwner(false);

            if (targetIsMany) {
                sourceAssn.setMultiplicity(Association.Multiplicity.OneToMany);
                targetAssn.setMultiplicity(Association.Multiplicity.OneToMany);
            } else {
                sourceAssn.setMultiplicity(Association.Multiplicity.OneToOne);
                targetAssn.setMultiplicity(Association.Multiplicity.OneToOne);
            }

            source.addAssociation(sourceAssn);
            target.addAssociation(targetAssn);
        } else if (c.getFromEnd().getAggregation().equals(NONE) && c.getToEnd().getAggregation().equals(COMPOSITE)) {
            if (targetIsMany) {
                throw new AdapterException("Only multiplicity of 1 can be on the composite side of association between [" + sourceEntityName + "] and [" + targetEntityName + "]");
            }

            sourceAssn.setOwner(false);
            targetAssn.setOwner(true);

            if (sourceIsMany) {
                sourceAssn.setMultiplicity(Association.Multiplicity.OneToMany);
                targetAssn.setMultiplicity(Association.Multiplicity.OneToMany);
            } else {
                sourceAssn.setMultiplicity(Association.Multiplicity.OneToOne);
                targetAssn.setMultiplicity(Association.Multiplicity.OneToOne);
            }

            source.addAssociation(sourceAssn);
            target.addAssociation(targetAssn);
        } else {
            throw new AdapterException("Invalid aggregation on assocation between [" + sourceEntityName + "] and [" + targetEntityName + "]");
        }
    }

    private void addEntity(VpClass e) {
        Entity entity = new Entity();
        entity.setName(e.getName());

        addTags(e, entity);

        entity.setNamespace(e.getNamespace());
        for (VpAttribute a : e.getAttributes()) {
            Property p = new Property();
            p.setName(a.getName());
            p.setType(resolveType(a.getType()));
            addTags(a, p);
            entity.addProperty(p);
        }

        for (VpOperation eo : e.getOperations()) {
            Operation o = new Operation();
            o.setName(eo.getName());
            addTags(eo, o);
            String returnType = resolveType(eo.getReturnType());
            o.setReturnType(returnType);
            o.setReturnMany((eo.getReturnMany().equals("[]") || eo.getReturnMany().equals("*") ? true : false));
            o.setDocumentation(StringUtils.remove(eo.getDocumentation(), '\n'));
            for (VpParameter ep : eo.getParams()) {
                if (ep != null) {
                    Parameter p = new Parameter();
                    p.setName(ep.getName());
                    p.setType(resolveType(ep.getType()));
                    addTags(ep, p);
                    o.addParameter(p);
                }
            }
            entity.addOperation(o);
        }
        model.addEntity(entity);
    }

    private void addEnumeration(VpClass e) {
        Enumeration en = new Enumeration();
        en.setName(e.getName());
        en.setNamespace(e.getNamespace());
        addTags(e, en);

        for (VpAttribute a : e.getAttributes()) {
            Literal literal = new Literal();
            literal.setName(a.getName());
            addTags(a, literal);
            en.addLiteral(literal);
        }

        model.addEnumeration(en);
    }

    private void addControl(VpClass e) {
        Control c = new Control();
        c.setName(e.getName());
        c.setNamespace(e.getNamespace());
        addTags(e, c);

        for (VpOperation eo : e.getOperations()) {
            Operation o = new Operation();
            o.setName(eo.getName());
            addTags(eo, o);

            String returnType = resolveType(eo.getReturnType());
            o.setReturnType(returnType);
            o.setReturnMany((eo.getReturnMany().equals("[]") || eo.getReturnMany().equals("*") ? true : false));
            o.setDocumentation(StringUtils.remove(eo.getDocumentation(), '\n'));
            if(eo.getParams() != null && eo.getParams().size() > 0){
                for (VpParameter ep : eo.getParams()) {
                    if (ep != null) {
                        Parameter p = new Parameter();
                        p.setName(ep.getName());
                        p.setMany(ep.getMany());
                        p.setType(resolveType(ep.getType()));
                        addTags(ep, p);
                        o.addParameter(p);
                    }
                }
            }
            c.addOperation(o);
        }

        model.addControl(c);
    }

    private String resolveType(String typeId) {
        if (StringUtils.isBlank(typeId)) {
            return null;
        }
        if (datatypes.containsKey(typeId)) {
            return datatypes.get(typeId);
        }
        if (classes.containsKey(typeId)) {
            return classes.get(typeId).getName();
        }
        return typeId;
    }

    private void addTags(VpElement e, Element element){

        for (String tagName : e.getTags().keySet()) {

            Tag tag = new Tag(tagName, e.getTags().get(tagName));
            element.addTag(tag);
        }
    }

    private void addBoundary(VpClass e) {
        Boundary entity = new Boundary();
        entity.setName(e.getName());

        addTags(e, entity);
        entity.setNamespace(e.getNamespace());

         for (VpAttribute a : e.getAttributes()) {
            Property p = new Property();
            p.setName(a.getName());
            p.setType(resolveType(a.getType()));
            addTags(a, p);
            entity.addProperty(p);
        }
        for (VpOperation eo : e.getOperations()) {
            Operation o = new Operation();
            o.setName(eo.getName());
            addTags(eo, o);
            String returnType = resolveType(eo.getReturnType());
            o.setReturnType(returnType);
            o.setReturnMany((eo.getReturnMany().equals("[]") ? true : false));
            o.setDocumentation(StringUtils.remove(eo.getDocumentation(), '\n'));
            for (VpParameter ep : eo.getParams()) {
                if (ep != null && !ep.getDirection().equals("return")) {
                    Parameter p = new Parameter();
                    p.setName(ep.getName());
                    p.setType(resolveType(ep.getType()));
                    addTags(ep, p);
                    o.addParameter(p);
                }
            }
            entity.addOperation(o);
        }
        model.addBoundary(entity);
    }
}
