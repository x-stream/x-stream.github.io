/*
 * Copyright (C) 2012 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * Created on 15. May 2012 by Joerg Schaible
 */
package com.thoughtworks.xstream.io.json;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import com.thoughtworks.acceptance.objects.Category;
import com.thoughtworks.acceptance.objects.OwnerOfExternalizable;
import com.thoughtworks.acceptance.objects.Product;
import com.thoughtworks.acceptance.objects.SomethingExternalizable;
import com.thoughtworks.acceptance.objects.StandardObject;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.testutil.TimeZoneChanger;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;


/**
 * @author Christoph Beck
 * @author J&ouml;rg Schaible
 */
public class StAXONDriverTest extends TestCase {
    private final static String SIMPLE = "{'product':{'name':'Banana','id':'123','price':'23.0'}}"
        .replace('\'', '"');
    private final static String HIERARCHY = "{'category':{'name':'fruit','id':'111','products':{'product':[{'name':'Banana','id':'123','price':'23.01','tags':{'string':['yellow','fresh','tasty']}},{'name':'Mango','id':'124','price':'34.01'}]}}}"
        .replace('\'', '"');

    private XStream xstream;

    protected void setUp() throws Exception {
        super.setUp();
        TimeZoneChanger.change("UTC");
        xstream = new XStream(new StAXONDriver(new JsonXMLConfigBuilder().multiplePI(true).build()));
        xstream.alias("category", Category.class);
        xstream.alias("product", Product.class);
    }

    protected void tearDown() throws Exception {
        TimeZoneChanger.reset();
        super.tearDown();
    }

    public void testReadSimple() {
        Product product = (Product)xstream.fromXML(SIMPLE);
        assertEquals(product.getName(), "Banana");
        assertEquals(product.getId(), "123");
        assertEquals("" + product.getPrice(), "" + 23.00);
    }

    public void testWriteSimple() {
        Product product = new Product("Banana", "123", 23.00);
        String result = xstream.toXML(product);
        assertEquals(SIMPLE, result);
    }

    public void testWriteHierarchy() {
        Category category = new Category("fruit", "111");
        ArrayList products = new ArrayList();
        Product banana = new Product("Banana", "123", 23.01);
        ArrayList bananaTags = new ArrayList();
        bananaTags.add("yellow");
        bananaTags.add("fresh");
        bananaTags.add("tasty");
        banana.setTags(bananaTags);
        products.add(banana);
        Product mango = new Product("Mango", "124", 34.01);
        products.add(mango);
        category.setProducts(products);
        String result = xstream.toXML(category);
        assertEquals(HIERARCHY, result);
    }

    public void testHierarchyRead() {
        Category parsedCategory = (Category)xstream.fromXML(HIERARCHY);
        Product parsedBanana = (Product)parsedCategory.getProducts().get(0);
        assertEquals("Banana", parsedBanana.getName());
        assertEquals(3, parsedBanana.getTags().size());
        assertEquals("yellow", parsedBanana.getTags().get(0));
        assertEquals("tasty", parsedBanana.getTags().get(2));
    }

    public void testObjectStream() throws IOException, ClassNotFoundException {
        Product product = new Product("Banana", "123", 23.00);
        StringWriter writer = new StringWriter();
        ObjectOutputStream oos = xstream.createObjectOutputStream(writer, "oos");
        oos.writeObject(product);
        oos.close();
        String json = writer.toString();
        assertEquals("{\"oos\":" + SIMPLE + "}", json);
        ObjectInputStream ois = xstream.createObjectInputStream(new StringReader(json));
        Product parsedProduct = (Product)ois.readObject();
        assertEquals(product.toString(), parsedProduct.toString());
    }

    public void testDoesHandleQuotesAndEscapes() {
        String[] strings = new String[]{
            "last\"", "\"first", "\"between\"", "around \"\" it", "back\\slash",
            "forward/slash"};
        // TODO: violates JSON spec, slash not escaped
        String expected = (""
            + "{#string-array#:{#string#:["
            + "#last\\\"#,"
            + "#\\\"first#,"
            + "#\\\"between\\\"#,"
            + "#around \\\"\\\" it#,"
            + "#back\\\\slash#,"
            + "#forward/slash#]}}").replace('#', '"');
        assertEquals(expected, xstream.toXML(strings));
    }

    public void testDoesEscapeValuesAccordingRfc4627() {
        String expected = "{'string':'\\u0000\\u0001\\u001F \uFFEE'}".replace('\'', '"');
        assertEquals(expected, xstream.toXML("\u0000\u0001\u001f\u0020\uffee"));
    }

    public void testSingletonListWithSimpleObject() {
        ArrayList list1 = new ArrayList();
        list1.add("one");
        String json = xstream.toXML(list1);
        assertEquals("{'list':{'string':['one']}}".replace('\'', '"'), json);
        ArrayList list2 = (ArrayList)xstream.fromXML(json);
        assertEquals(json, xstream.toXML(list2));
    }

    public void testListWithSimpleObjects() {
        ArrayList list1 = new ArrayList();
        list1.add("one");
        list1.add("two");
        list1.add("three");
        String json = xstream.toXML(list1);
        assertEquals("{'list':{'string':['one','two','three']}}".replace('\'', '"'), json);
        ArrayList list2 = (ArrayList)xstream.fromXML(json);
        assertEquals(json, xstream.toXML(list2));
    }

    public void testSingletonListWithComplexObject() {
        Product product = new Product("Banana", "123", 23.00);
        ArrayList list1 = new ArrayList();
        list1.add(product);
        String json = xstream.toXML(list1);
        assertEquals("{'list':{'product':[{'name':'Banana','id':'123','price':'23.0'}]}}"
            .replace('\'', '"'), json);
        ArrayList list2 = (ArrayList)xstream.fromXML(json);
        assertEquals(json, xstream.toXML(list2));
    }

    public void testListWithComplexNestedObjects() {
        ArrayList list1 = new ArrayList();
        list1.add(new Product("Banana", "123", 23.00));
        list1.add(new Product("Apple", "47", 11.00));
        list1.add(new Product("Orange", "100", 42.00));
        ArrayList tags = new ArrayList();
        ((Product)list1.get(1)).setTags(tags);
        tags.add(new Product("Braeburn", "47.1", 10.00));
        String json = xstream.toXML(list1);
        assertEquals(
            "{'list':{'product':[{'name':'Banana','id':'123','price':'23.0'},{'name':'Apple','id':'47','price':'11.0','tags':{'product':[{'name':'Braeburn','id':'47.1','price':'10.0'}]}},{'name':'Orange','id':'100','price':'42.0'}]}}"
                .replace('\'', '"'), json);
        ArrayList list2 = (ArrayList)xstream.fromXML(json);
        assertEquals(json, xstream.toXML(list2));
    }

    public void todoTestEmptyList() {
        ArrayList list1 = new ArrayList();
        String json = xstream.toXML(list1);
        assertEquals("{'list':[]}".replace('\'', '"'), json);
        ArrayList list2 = (ArrayList)xstream.fromXML(json);
        assertEquals(json, xstream.toXML(list2));
    }

    public static class Topic extends StandardObject {
        long id;
        String description;
        Date createdOn;
    }

    public void testDefaultValue() {
        Topic topic1 = new Topic();
        topic1.id = 4711;
        topic1.description = "JSON";
        topic1.createdOn = new Timestamp(1000);
        xstream.alias("topic", Topic.class);
        String json = xstream.toXML(topic1);
        assertEquals(
            "{'topic':{'id':'4711','description':'JSON','createdOn':{'@class':'sql-timestamp','$':'1970-01-01 00:00:01.0'}}}"
                .replace('\'', '"'), json);
        Topic topic2 = (Topic)xstream.fromXML(json);
        assertEquals(json, xstream.toXML(topic2));
    }

    public void testEmbeddedXml() {
        ArrayList list1 = new ArrayList();
        list1.add("<xml attribute=\"foo\"><![CDATA[&quot;\"\'<>]]></xml>");
        String json = xstream.toXML(list1);
        // TODO: violates JSON spec, slash not escaped
        assertEquals(
            "{\"list\":{\"string\":[\"<xml attribute=\\\"foo\\\"><![CDATA[&quot;\\\"'<>]]></xml>\"]}}",
            json);
        ArrayList list2 = (ArrayList)xstream.fromXML(json);
        assertEquals(json, xstream.toXML(list2));
    }

    public void testArrayList() {
            ArrayList list1 = new ArrayList();
            list1.clear();
            list1.add(new Integer(12));

            list1.add("string");
            list1.add(new Integer(13));
            String json = xstream.toXML(list1);

            ArrayList list2 = (ArrayList)xstream.fromXML(json);
            assertEquals(json, xstream.toXML(list2));
    }
    
    private static class SpecialCharacters extends StandardObject {
        String _foo__$_;
    }

    public void testSpecialNames() {
        SpecialCharacters sc = new SpecialCharacters();
        sc._foo__$_ = "bar";
        String json = xstream.toXML(sc);
        assertEquals(
            "{'com.thoughtworks.xstream.io.json.StAXONDriverTest$SpecialCharacters':{'_foo__$_':'bar'}}"
                .replace('\'', '"'), json);
        SpecialCharacters sc2 = (SpecialCharacters)xstream.fromXML(json);
        assertEquals(json, xstream.toXML(sc2));
    }

    public void testCanMarshalEmbeddedExternalizable() {
        xstream.alias("owner", OwnerOfExternalizable.class);
        
        OwnerOfExternalizable in = new OwnerOfExternalizable();
        in.target = new SomethingExternalizable("Joe", "Walnes");
        String json = xstream.toXML(in);
        // TODO: violates JSON spec, two labels named "string"
        assertEquals("{'owner':{'target':{'int':'3','string':'JoeWalnes','null':null,'string':'XStream'}}}".replace('\'', '"'), json);
        OwnerOfExternalizable owner = (OwnerOfExternalizable)xstream.fromXML(json);
        assertEquals(json, xstream.toXML(owner));
        assertEquals(in.target, owner.target);
    }

    /**
     * Test model class
     */
    private static class Customer {
        public String name;
        public List<String> phone;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
            result = prime * result + ((this.phone == null) ? 0 : this.phone.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Customer other = (Customer)obj;
            if (this.name == null) {
                if (other.name != null) return false;
            } else if (!this.name.equals(other.name)) return false;
            if (this.phone == null) {
                if (other.phone != null) return false;
            } else if (!this.phone.equals(other.phone)) return false;
            return true;
        }

    }

    public void testWrite() {
        XStream xstream = new XStream(new StAXONDriver());
        xstream.alias("customer", Customer.class);

        Customer customer = new Customer();
        customer.name = "Darth Vader";
        customer.phone = new ArrayList<String>();
        customer.phone.addAll(Arrays.asList("12345", "67890"));

        StringWriter writer = new StringWriter();
        xstream.toXML(customer, writer);
        String json = "{'customer':{'name':'Darth Vader','phone':{'string':['12345','67890']}}}"
            .replace('\'', '"');
        assertEquals(json, writer.toString());
    }

    public void testRead() {
        XStream xstream = new XStream(new StAXONDriver());
        xstream.alias("customer", Customer.class);

        String json = "{'customer':{'name':'Darth Vader','phone':{'string':['12345','67890']}}}"
            .replace('\'', '"');

        Customer customer = (Customer)xstream.fromXML(json);
        assertEquals("Darth Vader", customer.name);
        assertTrue(customer.phone != null && customer.phone.size() == 2);
        assertEquals("12345", customer.phone.get(0));
        assertEquals("67890", customer.phone.get(1));
    }

    public void testWrite2() {
        XStream xstream = new XStream(new StAXONDriver());
        xstream.alias("customer", Customer.class);

        StringWriter writer = new StringWriter();
        xstream.toXML(new Customer[]{new Customer(), new Customer()}, writer);
        String json = "{'customer-array':{'customer':[null,null]}}".replace('\'', '"');
        assertEquals(json, writer.toString());
    }

    public void testRead2() {
        XStream xstream = new XStream(new StAXONDriver());
        xstream.alias("customer", Customer.class);

        String json = "{'customer-array':{'customer':[null,null]}}".replace('\'', '"');

        Customer[] customers = (Customer[])xstream.fromXML(json);
        assertTrue(customers != null && customers.length == 2);
    }

    public void testSupportsVirtualRootAndImplicitCollection() {
        JsonXMLConfig config = new JsonXMLConfigBuilder().virtualRoot("customer").build();
        XStream xstream = new XStream(new StAXONDriver(config, "/phone"));
        xstream.alias("customer", Customer.class);
        xstream.addImplicitCollection(Customer.class, "phone", "phone", String.class);

        Customer customer = new Customer();
        customer.name = "Darth Vader";
        customer.phone = new ArrayList<String>();
        customer.phone.addAll(Arrays.asList("12345"));

        StringWriter writer = new StringWriter();
        xstream.toXML(customer, writer);
        String json = "{'name':'Darth Vader','phone':['12345']}".replace('\'', '"');
        assertEquals(json, writer.toString());
    }

    public void testReadVirtualRootAndImplicitCollection() {
        JsonXMLConfig config = new JsonXMLConfigBuilder().virtualRoot("customer").build();
        XStream xstream = new XStream(new StAXONDriver(config, "/phone"));
        xstream.alias("customer", Customer.class);
        xstream.addImplicitCollection(Customer.class, "phone", "phone", String.class);

        String json = "{'name':'Darth Vader','phone':['12345']}".replace('\'', '"');

        Customer customer = (Customer)xstream.fromXML(json);
        assertEquals("Darth Vader", customer.name);
        assertTrue(customer.phone != null && customer.phone.size() == 1);
        assertEquals("12345", customer.phone.get(0));
    }

    /**
     * Simple read/write demo
     */
    public static void main(String[] args) {
        String json = "{'customer':{'name':'Darth Vader','phone':{'string':['12345']}}}"
            .replace('\'', '"');

        JsonXMLConfig config = new JsonXMLConfigBuilder().prettyPrint(true).build();
        XStream xstream = new XStream(new StAXONDriver(config));
        xstream.alias("customer", Customer.class);

        xstream.toXML(xstream.fromXML(json), System.out);
    }
}
