/*
 *
 *     Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package io.mapsmessaging.schemas.formatters;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.XmlSchemaConfig;
import io.mapsmessaging.schemas.formatters.impl.XmlFormatter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.XML;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestXMLFormatter extends BaseTest {

  byte[] pack(io.mapsmessaging.schemas.formatters.Person p) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("stringId", p.getStringId());
    jsonObject.put("longId", p.getLongId());
    jsonObject.put("intId", p.getIntId());
    jsonObject.put("floatId", p.getFloatId());
    jsonObject.put("doubleId", p.getDoubleId());
    String xml = XML.toString(jsonObject);
    xml = "<?xml version=\"1.0\"?>\n" +
        "<!DOCTYPE person  >\n"
        + "<person>\n"
        + xml + "\n" +
        "</person>\n";

    return xml.getBytes();
  }


  @Override
  List<byte[]> packList(List<io.mapsmessaging.schemas.formatters.Person> list) throws IOException {
    List<byte[]> packed = new ArrayList<>();
    for (io.mapsmessaging.schemas.formatters.Person p : list) {
      packed.add(pack(p));
    }
    return packed;
  }

  @Override
  SchemaConfig getSchema() {
    XmlSchemaConfig xmlSchemaConfig = new XmlSchemaConfig();
    xmlSchemaConfig.setRootEntry("person");
    return xmlSchemaConfig;
  }

  @Test
  void testBadDocument() throws IOException {
    XmlSchemaConfig config = new XmlSchemaConfig();
    config.setRootEntry("catalog");
    XmlFormatter xmlFormatter = (XmlFormatter) MessageFormatterFactory.getInstance().getFormatter(config);
    Assertions.assertNotNull(xmlFormatter.parse("This is not a XML document".getBytes()));
    Assertions.assertNull(xmlFormatter.parse("This is not a XML document".getBytes()).get("Whatever"));
  }

  @Test
  void testComplexDocument() throws IOException {
    XmlSchemaConfig config = new XmlSchemaConfig();
    config.setRootEntry("catalog");
    XmlFormatter xmlFormatter = (XmlFormatter) MessageFormatterFactory.getInstance().getFormatter(config);
    Assertions.assertEquals("Cardigan Sweater", xmlFormatter.parse(XMLString.getBytes()).get("product.description"));
    Assertions.assertEquals(39.95, xmlFormatter.parse(XMLString.getBytes()).get("product.catalog_item[0].price"));
  }

  private static final String XMLString = "<?xml version=\"1.0\"?>\n"
      + "<?xml-stylesheet href=\"catalog.xsl\" type=\"text/xsl\"?>\n"
      + "<!DOCTYPE catalog  >\n"
      + "<catalog>\n"
      + "   <product description=\"Cardigan Sweater\" product_image=\"cardigan.jpg\">\n"
      + "      <catalog_item gender=\"Men's\">\n"
      + "         <item_number>QWZ5671</item_number>\n"
      + "         <price>39.95</price>\n"
      + "         <size description=\"Medium\">\n"
      + "            <color_swatch image=\"red_cardigan.jpg\">Red</color_swatch>\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "         </size>\n"
      + "         <size description=\"Large\">\n"
      + "            <color_swatch image=\"red_cardigan.jpg\">Red</color_swatch>\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "         </size>\n"
      + "      </catalog_item>\n"
      + "      <catalog_item gender=\"Women's\">\n"
      + "         <item_number>RRX9856</item_number>\n"
      + "         <price>42.50</price>\n"
      + "         <size description=\"Small\">\n"
      + "            <color_swatch image=\"red_cardigan.jpg\">Red</color_swatch>\n"
      + "            <color_swatch image=\"navy_cardigan.jpg\">Navy</color_swatch>\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "         </size>\n"
      + "         <size description=\"Medium\">\n"
      + "            <color_swatch image=\"red_cardigan.jpg\">Red</color_swatch>\n"
      + "            <color_swatch image=\"navy_cardigan.jpg\">Navy</color_swatch>\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "            <color_swatch image=\"black_cardigan.jpg\">Black</color_swatch>\n"
      + "         </size>\n"
      + "         <size description=\"Large\">\n"
      + "            <color_swatch image=\"navy_cardigan.jpg\">Navy</color_swatch>\n"
      + "            <color_swatch image=\"black_cardigan.jpg\">Black</color_swatch>\n"
      + "         </size>\n"
      + "         <size description=\"Extra Large\">\n"
      + "            <color_swatch image=\"burgundy_cardigan.jpg\">Burgundy</color_swatch>\n"
      + "            <color_swatch image=\"black_cardigan.jpg\">Black</color_swatch>\n"
      + "         </size>\n"
      + "      </catalog_item>\n"
      + "   </product>\n"
      + "</catalog>";
}
