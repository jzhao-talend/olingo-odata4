/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.client.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.olingo.client.api.domain.ClientCollectionValue;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.core.uri.URIUtils;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Delta;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.format.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONTest extends AbstractTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  protected ContentType getODataPubFormat() {
    return ContentType.JSON;
  }

  protected ContentType getODataFormat() {
    return ContentType.JSON;
  }

  private void cleanup(final ObjectNode node) {
    if (node.has(Constants.JSON_CONTEXT)) {
      node.remove(Constants.JSON_CONTEXT);
    }
    if (node.has(Constants.JSON_ETAG)) {
      node.remove(Constants.JSON_ETAG);
    }
    if (node.has(Constants.JSON_TYPE)) {
      node.remove(Constants.JSON_TYPE);
    }
    if (node.has(Constants.JSON_EDIT_LINK)) {
      node.remove(Constants.JSON_EDIT_LINK);
    }
    if (node.has(Constants.JSON_READ_LINK)) {
      node.remove(Constants.JSON_READ_LINK);
    }
    if (node.has(Constants.JSON_MEDIA_EDIT_LINK)) {
      node.remove(Constants.JSON_MEDIA_EDIT_LINK);
    }
    if (node.has(Constants.JSON_MEDIA_READ_LINK)) {
      node.remove(Constants.JSON_MEDIA_READ_LINK);
    }
    if (node.has(Constants.JSON_MEDIA_CONTENT_TYPE)) {
      node.remove(Constants.JSON_MEDIA_CONTENT_TYPE);
    }
    if (node.has(Constants.JSON_COUNT)) {
      node.remove(Constants.JSON_COUNT);
    }
    final List<String> toRemove = new ArrayList<String>();
    for (final Iterator<Map.Entry<String, JsonNode>> itor = node.fields(); itor.hasNext();) {
      final Map.Entry<String, JsonNode> field = itor.next();

      final String key = field.getKey();
      if (key.charAt(0) == '#'
          || key.endsWith(Constants.JSON_TYPE)
          || key.endsWith(Constants.JSON_MEDIA_EDIT_LINK)
          || key.endsWith(Constants.JSON_MEDIA_CONTENT_TYPE)
          || key.endsWith(Constants.JSON_ASSOCIATION_LINK)
          || key.endsWith(Constants.JSON_MEDIA_ETAG)) {

        toRemove.add(key);
      } else if (field.getValue().isObject()) {
        cleanup((ObjectNode) field.getValue());
      } else if (field.getValue().isArray()) {
        for (final Iterator<JsonNode> arrayItems = field.getValue().elements(); arrayItems.hasNext();) {
          final JsonNode arrayItem = arrayItems.next();
          if (arrayItem.isObject()) {
            cleanup((ObjectNode) arrayItem);
          }
        }
      }
    }
    node.remove(toRemove);
  }

  protected void assertSimilar(final String filename, final String actual) throws Exception {
    final JsonNode expected = OBJECT_MAPPER.readTree(IOUtils.toString(getClass().getResourceAsStream(filename)).
        replace(Constants.JSON_NAVIGATION_LINK, Constants.JSON_BIND_LINK_SUFFIX));
    cleanup((ObjectNode) expected);
    final ObjectNode actualNode = (ObjectNode) OBJECT_MAPPER.readTree(new ByteArrayInputStream(actual.getBytes()));
    cleanup(actualNode);
    assertEquals(expected, actualNode);
  }

  protected void entitySet(final String filename, final ContentType contentType) throws Exception {
    final StringWriter writer = new StringWriter();
    client.getSerializer(contentType).write(writer, client.getDeserializer(contentType).toEntitySet(
        getClass().getResourceAsStream(filename + "." + getSuffix(contentType))).getPayload());

    assertSimilar(filename + "." + getSuffix(contentType), writer.toString());
  }

  @Test
  public void entitySets() throws Exception {
    entitySet("Customers", getODataPubFormat());
    entitySet("collectionOfEntityReferences", getODataPubFormat());
  }

  protected void entity(final String filename, final ContentType contentType) throws Exception {
    final StringWriter writer = new StringWriter();
    client.getSerializer(contentType).write(writer, client.getDeserializer(contentType).toEntity(
        getClass().getResourceAsStream(filename + "." + getSuffix(contentType))).getPayload());
    assertSimilar(filename + "." + getSuffix(contentType), writer.toString());
  }

  @Test
  public void additionalEntities() throws Exception {
    entity("entity.minimal", getODataPubFormat());
    entity("entity.primitive", getODataPubFormat());
    entity("entity.complex", getODataPubFormat());
    entity("entity.collection.primitive", getODataPubFormat());
    entity("entity.collection.complex", getODataPubFormat());
  }

  @Test
  public void entities() throws Exception {
    entity("Products_5", getODataPubFormat());
    entity("VipCustomer", getODataPubFormat());
    entity("Advertisements_f89dee73-af9f-4cd4-b330-db93c25ff3c7", getODataPubFormat());
    entity("entityReference", getODataPubFormat());
    entity("entity.withcomplexnavigation", getODataPubFormat());
    entity("annotated", getODataPubFormat());
  }

  protected void property(final String filename, final ContentType contentType) throws Exception {
    final StringWriter writer = new StringWriter();
    client.getSerializer(contentType).write(writer, client.getDeserializer(contentType).
        toProperty(getClass().getResourceAsStream(filename + "." + getSuffix(contentType))).getPayload());

    assertSimilar(filename + "." + getSuffix(contentType), writer.toString());
  }

  @Test
  public void properties() throws Exception {
    property("Products_5_SkinColor", getODataFormat());
    property("Products_5_CoverColors", getODataFormat());
    property("Employees_3_HomeAddress", getODataFormat());
    property("Employees_3_HomeAddress", getODataFormat());
  }

  @Test
  public void crossjoin() throws Exception {
    assertNotNull(client.getDeserializer(ContentType.JSON_FULL_METADATA).toEntitySet(
        getClass().getResourceAsStream("crossjoin.json")));
  }

  protected void delta(final String filename, final ContentType contentType) throws Exception {
    final Delta delta = client.getDeserializer(contentType).toDelta(
        getClass().getResourceAsStream(filename + "." + getSuffix(contentType))).getPayload();
    assertNotNull(delta);
    assertNotNull(delta.getDeltaLink());
    assertEquals(5, delta.getCount(), 0);

    assertEquals(1, delta.getDeletedEntities().size());
    assertTrue(delta.getDeletedEntities().get(0).getId().toASCIIString().endsWith("Customers('ANTON')"));

    assertEquals(1, delta.getAddedLinks().size());
    assertTrue(delta.getAddedLinks().get(0).getSource().toASCIIString().endsWith("Customers('BOTTM')"));
    assertEquals("Orders", delta.getAddedLinks().get(0).getRelationship());

    assertEquals(1, delta.getDeletedLinks().size());
    assertTrue(delta.getDeletedLinks().get(0).getSource().toASCIIString().endsWith("Customers('ALFKI')"));
    assertEquals("Orders", delta.getDeletedLinks().get(0).getRelationship());

    assertEquals(2, delta.getEntities().size());
    Property property = delta.getEntities().get(0).getProperty("ContactName");
    assertNotNull(property);
    assertTrue(property.isPrimitive());
    property = delta.getEntities().get(1).getProperty("ShippingAddress");
    assertNotNull(property);
    assertTrue(property.isComplex());
  }

  @Test
  public void deltas() throws Exception {
    delta("delta", getODataPubFormat());
  }

  @Test
  public void issueOLINGO390() throws Exception {
    final ClientEntity message = client.getObjectFactory().
        newEntity(new FullQualifiedName("Microsoft.Exchange.Services.OData.Model.Message"));

    final ClientComplexValue toRecipient = client.getObjectFactory().
        newComplexValue("Microsoft.Exchange.Services.OData.Model.Recipient");
    toRecipient.add(client.getObjectFactory().newPrimitiveProperty("Name",
        client.getObjectFactory().newPrimitiveValueBuilder().buildString("challen_olingo_client")));
    toRecipient.add(client.getObjectFactory().newPrimitiveProperty("Address",
        client.getObjectFactory().newPrimitiveValueBuilder().buildString("challenh@microsoft.com")));
    final ClientCollectionValue<ClientValue> toRecipients = client.getObjectFactory().
        newCollectionValue("Microsoft.Exchange.Services.OData.Model.Recipient");
    toRecipients.add(toRecipient);
    message.getProperties().add(client.getObjectFactory().newCollectionProperty("ToRecipients", toRecipients));

    final ClientComplexValue body =
        client.getObjectFactory().newComplexValue("Microsoft.Exchange.Services.OData.Model.ItemBody");
    body.add(client.getObjectFactory().newPrimitiveProperty("Content",
        client.getObjectFactory().newPrimitiveValueBuilder().
            buildString("this is a simple email body content")));
    body.add(client.getObjectFactory().newEnumProperty("ContentType",
        client.getObjectFactory().newEnumValue("Microsoft.Exchange.Services.OData.Model.BodyType", "text")));
    message.getProperties().add(client.getObjectFactory().newComplexProperty("Body", body));

    final String actual = IOUtils.toString(client.getWriter().writeEntity(message, ContentType.JSON));
    final JsonNode expected =
        OBJECT_MAPPER.readTree(IOUtils.toString(getClass().getResourceAsStream("olingo390.json")).
            replace(Constants.JSON_NAVIGATION_LINK, Constants.JSON_BIND_LINK_SUFFIX));
    final ObjectNode actualNode = (ObjectNode) OBJECT_MAPPER.readTree(new ByteArrayInputStream(actual.getBytes()));
    assertEquals(expected, actualNode);
  }

  @Test
  public void testOLINGO1114() throws Exception {
    ClientEntity entityIncNullValue = client.getObjectFactory()
            .newEntity(new FullQualifiedName("Microsoft.Dynamics.CRM", "account"));
    List<ClientProperty> properties = entityIncNullValue.getProperties();

    // Property "name"
    ClientPrimitiveValue.Builder valueBuilder = client.getObjectFactory().newPrimitiveValueBuilder();
    valueBuilder.setType(EdmPrimitiveTypeKind.String);
    valueBuilder.setValue("testString");
    ClientProperty name = client.getObjectFactory().newPrimitiveProperty("name", valueBuilder.build());
    properties.add(name);

    // Property "testDecimal"
    valueBuilder = client.getObjectFactory().newPrimitiveValueBuilder();
    valueBuilder.setType(EdmPrimitiveTypeKind.Decimal);
    valueBuilder.setValue(null);
    ClientProperty revenue = client.getObjectFactory().newPrimitiveProperty("testDecimal", valueBuilder.build());
    properties.add(revenue);

    // Property "testByte"
    valueBuilder = client.getObjectFactory().newPrimitiveValueBuilder();
    valueBuilder.setType(EdmPrimitiveTypeKind.Byte);
    valueBuilder.setValue(null);
    ClientProperty testByte = client.getObjectFactory().newPrimitiveProperty("testByte", valueBuilder.build());
    properties.add(testByte);

    // Property "testDouble"
    valueBuilder = client.getObjectFactory().newPrimitiveValueBuilder();
    valueBuilder.setType(EdmPrimitiveTypeKind.Double);
    valueBuilder.setValue(null);
    ClientProperty testDouble = client.getObjectFactory().newPrimitiveProperty("testDouble", valueBuilder.build());
    properties.add(testDouble);

    // Property "testInt64"
    valueBuilder = client.getObjectFactory().newPrimitiveValueBuilder();
    valueBuilder.setType(EdmPrimitiveTypeKind.Int64);
    valueBuilder.setValue(null);
    ClientProperty testInt64 = client.getObjectFactory().newPrimitiveProperty("testInt64", valueBuilder.build());
    properties.add(testInt64);

    // Property "testInt32"
    valueBuilder = client.getObjectFactory().newPrimitiveValueBuilder();
    valueBuilder.setType(EdmPrimitiveTypeKind.Int32);
    valueBuilder.setValue(null);
    ClientProperty testInt32 = client.getObjectFactory().newPrimitiveProperty("testInt32", valueBuilder.build());
    properties.add(testInt32);

    // Property "testInt16"
    valueBuilder = client.getObjectFactory().newPrimitiveValueBuilder();
    valueBuilder.setType(EdmPrimitiveTypeKind.Int16);
    valueBuilder.setValue(null);
    ClientProperty testInt16 = client.getObjectFactory().newPrimitiveProperty("testInt16", valueBuilder.build());
    properties.add(testInt16);

    InputStream inputStream = client.getWriter().writeEntity(entityIncNullValue, ContentType.JSON);
    HttpEntity httpEntity = URIUtils.buildInputStreamEntity(client, inputStream);

    final String actual = EntityUtils.toString(httpEntity);
    final JsonNode expected =
            OBJECT_MAPPER.readTree(IOUtils.toString(getClass().getResourceAsStream("olingo1114.json")).
                    replace(Constants.JSON_NAVIGATION_LINK, Constants.JSON_BIND_LINK_SUFFIX));
    final ObjectNode actualNode = (ObjectNode) OBJECT_MAPPER.readTree(new ByteArrayInputStream(actual.getBytes()));
    assertEquals(expected, actualNode);
  }

}
