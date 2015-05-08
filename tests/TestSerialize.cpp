/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MODULE Serializer

#include <boost/test/unit_test.hpp>
#include <jsonrpc/JsonSerializer.hpp>
#include <map>

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

  array.push_back ("first");
  array.push_back ("2");
  array.push_back ("three");

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
