package org.atlas.adapter.visualparadigm;

import org.atlas.engine.Context;
import org.atlas.engine.ModelInput;
import org.atlas.engine.ModelTransformer;
import org.atlas.engine.TransformException;
import org.atlas.model.adapter.AdapterException;
import org.atlas.model.adapter.visualparadigm.ModelAdapter;
import org.atlas.model.metamodel.Control;
import org.atlas.model.metamodel.Entity;
import org.atlas.model.metamodel.Enumeration;
import org.atlas.model.metamodel.Literal;
import org.atlas.model.metamodel.Model;
import org.atlas.model.metamodel.Operation;
import org.atlas.model.metamodel.Property;

import java.io.File;
import junit.framework.Assert;
import org.junit.Test;

public class ModelAdapterTest {

    public ModelAdapterTest() {
    }

    /**
     * Test of adapt method, of class ModelAdapter.
     */
    @Test
    public void testAdapt() throws TransformException {
        ModelInput m = Context.getModelInputs().get(0);
        File file = new File(m.getFile());
        ModelAdapter adapter = new ModelAdapter();
        Model model = new Model();
        try {
            model = adapter.adapt(file, model);
        } catch (AdapterException e) {
            Assert.fail(e.getMessage());
        }

        Entity school = model.getEntity("School");
        Assert.assertEquals("testpackage", school.getNamespace());

        Entity person = model.getEntity("Person");
        Assert.assertNotNull(person);
        Assert.assertEquals(2, person.getProperties().size());

        Entity student = model.getEntity("Student");
        Assert.assertNotNull(student.getGeneralization());
        Assert.assertEquals("Person", student.getGeneralization().getName());

        Assert.assertTrue(student.getManyToMany().size() == 2);

        boolean propertyFound = false;
        for (Property property : student.getProperties()) {
            if ("StudentID".equals(property.getName())) {
                String length = property.getTagValue("length");
                Assert.assertEquals("30", length);
                String unique = property.getTagValue("unique");
                Assert.assertEquals("yes", unique);
                propertyFound = true;
            }
        }
        Assert.assertTrue(propertyFound);

        Control control = model.getControl("AuthenticationService");
        boolean operationFound = false;
        for (Operation operation : control.getOperations()) {
            if ("authenticate".equals(operation.getName())) {
                Assert.assertEquals("Person", operation.getReturnType());
                Assert.assertEquals(1, operation.getParameters().size());
                operationFound = true;
            }
        }
        Assert.assertTrue(operationFound);

        Enumeration enumeration = model.getEnumeration("GenderType");
        Assert.assertNotNull(enumeration);
        boolean literalFound = false;
        for (Literal literal : enumeration.getLiterals()) {
            if ("MALE".equals(literal.getName())) {
                literalFound = true;
            }
        }
        Assert.assertTrue(literalFound);
    }

    @Test
    public void testTransform() throws TransformException {
        ModelTransformer t = new ModelTransformer();
        t.transform();
    }
}
