/*
 *
 *     Copyright [ 2020 - 2022 ] [Matthew Buckton]
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
import io.mapsmessaging.schemas.config.impl.CsvSchemaConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestCsvFormatter extends BaseTest{

  @Override
  List<byte[]> packList(List<Person> list) throws IOException {
    List<byte[]> packed = new ArrayList<>();
    for(io.mapsmessaging.schemas.formatters.Person p:list){
      packed.add((
          "\""+p.getStringId()+"\","+
              p.getLongId()+","+
              p.getIntId()+","+
              p.getFloatId()+","+
              p.getDoubleId()).getBytes());
    }
    return packed;
  }

  @Override
  SchemaConfig getSchema() throws IOException {
    return new CsvSchemaConfig("stringId, longId, intId, floatId, doubleId");
  }
}
