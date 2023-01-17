/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MODULE Serializer

#include <boost/test/unit_test.hpp>
#include <jsonrpc/JsonSerializer.hpp>
#include <map>
#include <iostream>

BOOST_AUTO_TEST_CASE (serialize_map)
{
  std::map<std::string, int> intMap;
  std::map<std::string, int> newMap;
  kurento::JsonSerializer writer (true);
  kurento::JsonSerializer writer2 (true);
  kurento::JsonSerializer reader (false);

  intMap ["key1"] = 1;
  intMap ["key2"] = 2;

  writer.Serialize ("map", intMap);

  reader.JsonValue = writer.JsonValue;
  reader.Serialize ("map", newMap);

  writer2.Serialize ("map", newMap);

  BOOST_ASSERT (writer.JsonValue.toStyledString() ==
                writer2.JsonValue.toStyledString() );
}

BOOST_AUTO_TEST_CASE (serialize_map_of_map)
{
  std::map<std::string, int> intMap;
  std::map<std::string, int> intMap2;
  std::map<std::string, std::map<std::string, int>> mapMap;
  std::map<std::string, std::map<std::string, int>> newMap;
  kurento::JsonSerializer writer (true);
  kurento::JsonSerializer writer2 (true);
  kurento::JsonSerializer reader (false);

  intMap ["key1"] = 1;
  intMap ["key2"] = 2;

  intMap2 ["key3"] = 3;
  intMap2 ["key4"] = 4;

  mapMap ["map1"] = intMap;
  mapMap ["map2"] = intMap2;

  writer.Serialize ("map", mapMap);

  reader.JsonValue = writer.JsonValue;
  reader.Serialize ("map", newMap);

  writer2.Serialize ("map", newMap);

  BOOST_ASSERT (writer.JsonValue.toStyledString() ==
                writer2.JsonValue.toStyledString() );
}

BOOST_AUTO_TEST_CASE (serialize_empty_array)
{
  std::vector <int> array;
  std::vector <int> newArray;
  kurento::JsonSerializer writer (true);
  kurento::JsonSerializer writer2 (true);
  kurento::JsonSerializer reader (false);

  writer.Serialize ("array", array);

  reader.JsonValue = writer.JsonValue;
  reader.Serialize ("array", newArray);
  BOOST_ASSERT (writer.JsonValue.toStyledString().find ("[]") !=
                std::string::npos );

  writer2.Serialize ("array", newArray);

  BOOST_ASSERT (writer.JsonValue.toStyledString() ==
                writer2.JsonValue.toStyledString() );
}

BOOST_AUTO_TEST_CASE (serialize_list)
{
  std::list <std::string> array;
  std::list <std::string> newArray;
  kurento::JsonSerializer writer (true);
  kurento::JsonSerializer writer2 (true);
  kurento::JsonSerializer reader (false);

  array.emplace_back("first");
  array.emplace_back("2");
  array.emplace_back("three");

  writer.Serialize ("array", array);

  reader.JsonValue = writer.JsonValue;
  reader.Serialize ("array", newArray);

  writer2.Serialize ("array", newArray);

  BOOST_ASSERT (writer.JsonValue.toStyledString() ==
                writer2.JsonValue.toStyledString() );
}

BOOST_AUTO_TEST_CASE (serialize_vector)
{
  std::vector <int> array;
  std::vector <int> newArray;
  kurento::JsonSerializer writer (true);
  kurento::JsonSerializer writer2 (true);
  kurento::JsonSerializer reader (false);

  array.push_back (1);
  array.push_back (2);
  array.push_back (3);

  writer.Serialize ("array", array);

  reader.JsonValue = writer.JsonValue;
  reader.Serialize ("array", newArray);

  writer2.Serialize ("array", newArray);

  BOOST_ASSERT (writer.JsonValue.toStyledString() ==
                writer2.JsonValue.toStyledString() );
}

BOOST_AUTO_TEST_CASE (serialize_int64)
{
  int64_t data = LLONG_MAX;
  int64_t newData = 0;
  kurento::JsonSerializer writer (true);
  kurento::JsonSerializer writer2 (true);
  kurento::JsonSerializer reader (false);

  writer.Serialize ("intValue", data);

  std::cout << writer.JsonValue.toStyledString () << std::endl;

  reader.JsonValue = writer.JsonValue;
  reader.Serialize ("intValue", newData);

  writer2.Serialize ("intValue", newData);

  std::cout << "old value " << data << " new value " << newData << std::endl;
  BOOST_ASSERT (writer.JsonValue.toStyledString() ==
                writer2.JsonValue.toStyledString() );
}
