/*
 * Copyright 2011 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.odysseus.staxon.xstream;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;

public class StAXONDriverTest {
	/**
	 * Test model class
	 */
	static class Customer {
		public String name;
		public List<String> phone;
	}

	@Test
	public void testWrite() {
		XStream xstream = new XStream(new StAXONDriver());
		xstream.alias("customer", Customer.class);

		Customer customer = new Customer();
		customer.name = "Darth Vader";
		customer.phone = new ArrayList<String>();
		customer.phone.addAll(Arrays.asList("12345", "67890"));

		StringWriter writer = new StringWriter();
		xstream.toXML(customer, writer);
		String json = "{'customer':{'name':'Darth Vader','phone':{'string':['12345','67890']}}}".replace('\'', '"');
		Assert.assertEquals(json, writer.toString());
	}

	@Test
	public void testRead() {
		XStream xstream = new XStream(new StAXONDriver());
		xstream.alias("customer", Customer.class);

		String json = "{'customer':{'name':'Darth Vader','phone':{'string':['12345','67890']}}}".replace('\'', '"');

		Customer customer = (Customer) xstream.fromXML(json);
		Assert.assertEquals("Darth Vader", customer.name);
		Assert.assertTrue(customer.phone != null && customer.phone.size() == 2);
		Assert.assertEquals("12345", customer.phone.get(0));
		Assert.assertEquals("67890", customer.phone.get(1));
	}

	@Test
	public void testWrite2() {
		XStream xstream = new XStream(new StAXONDriver());
		xstream.alias("customer", Customer.class);

		StringWriter writer = new StringWriter();
		xstream.toXML(new Customer[] { new Customer(), new Customer() }, writer);
		String json = "{'customer-array':{'customer':[null,null]}}".replace('\'', '"');
		Assert.assertEquals(json, writer.toString());
	}

	@Test
	public void testRead2() {
		XStream xstream = new XStream(new StAXONDriver());
		xstream.alias("customer", Customer.class);

		String json = "{'customer-array':{'customer':[null,null]}}".replace('\'', '"');

		Customer[] customers = (Customer[]) xstream.fromXML(json);
		Assert.assertTrue(customers != null && customers.length == 2);
	}

	@Test
	public void testWriteVirtualRootAndImplicitCollection() {
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
		Assert.assertEquals(json, writer.toString());
	}

	@Test
	public void testReadVirtualRootAndImplicitCollection() {
		JsonXMLConfig config = new JsonXMLConfigBuilder().virtualRoot("customer").build();
		XStream xstream = new XStream(new StAXONDriver(config, "/phone"));
		xstream.alias("customer", Customer.class);
		xstream.addImplicitCollection(Customer.class, "phone", "phone", String.class);

		String json = "{'name':'Darth Vader','phone':['12345']}".replace('\'', '"');

		Customer customer = (Customer) xstream.fromXML(json);
		Assert.assertEquals("Darth Vader", customer.name);
		Assert.assertTrue(customer.phone != null && customer.phone.size() == 1);
		Assert.assertEquals("12345", customer.phone.get(0));
	}
	
	/**
	 * Simple read/write demo
	 */
	public static void main(String[] args) {
		String json = "{'customer':{'name':'Darth Vader','phone':{'string':['12345']}}}".replace('\'', '"');

		JsonXMLConfig config = new JsonXMLConfigBuilder().prettyPrint(true).build();
		XStream xstream = new XStream(new StAXONDriver(config));
		xstream.alias("customer", Customer.class);

		xstream.toXML(xstream.fromXML(json), System.out);
	}
}
