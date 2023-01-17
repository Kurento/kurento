/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "FilterType.hpp"
#include <GStreamerFilterImplFactory.hpp>
#include "GStreamerFilterImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <commons/kms-core-enumtypes.h>

#include <algorithm>
#include <sstream>
#include <stdexcept>
#include <string>

#define GST_CAT_DEFAULT kurento_gstreamer_filter_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoGStreamerFilterImpl"

namespace kurento
{

static void string2enum (const GValue *src_value, GValue *dst_value);

GStreamerFilterImpl::GStreamerFilterImpl (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, const std::string &command,
    std::shared_ptr<FilterType> filterType) : FilterImpl (conf,
          std::dynamic_pointer_cast<MediaObjectImpl> ( mediaPipeline) )
{
  GstElement *filter, *filter_check;
  GError *error = nullptr;

  this->cmd = command;

  GST_DEBUG ("Command %s", command.c_str() );

  switch (filterType->getValue() ) {
  case FilterType::VIDEO:
    g_object_set (element, "type", 2, NULL);
    break;

  case FilterType::AUDIO:
    g_object_set (element, "type", 1, NULL);
    break;

  case FilterType::AUTODETECT:
    g_object_set (element, "type", 0, NULL);
    break;

  default:
    break;
  }

  filter = gst_parse_launch (command.c_str(), &error);

  if (filter == nullptr || error != nullptr) {
    std::string error_str = "GStreamer element cannot be created";

    if (filter) {
      g_object_unref (filter);
    }

    if (error != nullptr) {
      if (error->message != nullptr) {
        error_str += ": " + std::string (error->message);
      }

      g_error_free (error);
    }

    throw KurentoException (MARSHALL_ERROR, error_str);
  } else if (GST_IS_BIN (filter) ) {
    g_object_unref (filter);

    throw KurentoException (MARSHALL_ERROR,
                            "Given command is not valid, only one element can be created");
  }

  g_object_set (element, "filter", filter, NULL);

  g_object_get (element, "filter", &filter_check, NULL);

  if (filter_check != filter) {
    g_object_unref (filter);
    g_object_unref (filter_check);

    throw KurentoException (MARSHALL_ERROR,
                            "Given command is not valid, pad templates don't match");
  }

  g_object_unref (filter);
  g_object_unref (filter_check);

  // No ref held; will be released by the pipeline
  gstElement = filter;

  // Used by method setElementProperty() when the property is an enum
  g_value_register_transform_func (G_TYPE_STRING, G_TYPE_ENUM, string2enum);
}

MediaObjectImpl *
GStreamerFilterImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, const std::string &command,
    std::shared_ptr<FilterType> filterType) const
{
  return new GStreamerFilterImpl (conf, mediaPipeline, command, filterType);
}

std::string GStreamerFilterImpl::getCommand ()
{
  return this->cmd;
}

static void
string2enum (const GValue *src_value, GValue *dst_value)
{
  // Find and set the requested enum value among all possible ones
  //
  // See:
  // - https://developer.gnome.org/gobject/stable/gobject-Enumeration-and-Flag-Types.html
  // - https://developer.gnome.org/gobject/stable/gobject-Generic-values.html
  // - https://developer.gnome.org/gobject/stable/gobject-Standard-Parameter-and-Value-Types.html

  const gchar *src_string = g_value_get_string (src_value);

  const GType enum_type = G_VALUE_TYPE (dst_value);
  const GEnumClass *enum_class = G_ENUM_CLASS (g_type_class_ref (enum_type));
  const GEnumValue *enum_values = enum_class->values;
  gboolean found = FALSE;

  for (guint i = 0; i < enum_class->n_values; ++i) {
    if (g_strcmp0 (src_string, enum_values[i].value_nick) == 0) {
      found = TRUE;
      g_value_set_enum (dst_value, enum_values[i].value);
      break;
    }
  }

  if (!found) {
    gchar *message = g_strdup_printf ("Invalid value for enum %s",
        G_VALUE_TYPE_NAME(dst_value));
    throw std::invalid_argument(message);
    g_free (message);
  }
}

void GStreamerFilterImpl::setElementProperty(const std::string &propertyName,
    const std::string &propertyValue)
{
  // Get the property _type_ from the GStreamer element
  const char* property_name = propertyName.c_str();
  GParamSpec *pspec = g_object_class_find_property (
      G_OBJECT_GET_CLASS (gstElement), property_name);
  if (pspec == NULL) {
    std::ostringstream oss;
    oss << "No property named '" << property_name << "' in object '"
        << GST_ELEMENT_NAME (gstElement) << "'";
    std::string message = oss.str();

    GST_WARNING ("%s", message.c_str());
    throw KurentoException (MARSHALL_ERROR, message);
  }

  // Convert the input string to the correct value type
  GValue value = G_VALUE_INIT;
  g_value_init (&value, G_PARAM_SPEC_VALUE_TYPE(pspec));

  if (G_IS_PARAM_SPEC_INT (pspec)) {
    gint converted = 0;
    try {
      converted = std::stoi (propertyValue);
    }
    catch (std::exception &ex) {
      std::ostringstream oss;
      oss << "Cannot convert '" << propertyValue << "' to int: " << ex.what();
      std::string message = oss.str();

      GST_WARNING ("%s", message.c_str());
      throw KurentoException (MARSHALL_ERROR, message);
    }
    g_value_set_int (&value, converted);
  }
  else if (G_IS_PARAM_SPEC_FLOAT (pspec)) {
    gfloat converted = 0.0f;
    try {
      converted = std::stof (propertyValue);
    }
    catch (std::exception &ex) {
      std::ostringstream oss;
      oss << "Cannot convert '" << propertyValue << "' to float: " << ex.what();
      std::string message = oss.str();

      GST_WARNING ("%s", message.c_str());
      throw KurentoException (MARSHALL_ERROR, message);
    }
    g_value_set_float (&value, converted);
  }
  else if (G_IS_PARAM_SPEC_DOUBLE (pspec)) {
    gdouble converted = 0.0;
    try {
      converted = std::stod (propertyValue);
    }
    catch (std::exception &ex) {
      std::ostringstream oss;
      oss << "Cannot convert '" << propertyValue << "' to double: " << ex.what();
      std::string message = oss.str();

      GST_WARNING ("%s", message.c_str());
      throw KurentoException (MARSHALL_ERROR, message);
    }
    g_value_set_double (&value, converted);
  }
  else if (G_IS_PARAM_SPEC_ENUM (pspec)) {
    // Source type: string
    GValue src_value = G_VALUE_INIT;
    g_value_init (&src_value, G_TYPE_STRING);
    g_value_set_static_string (&src_value, propertyValue.c_str());

    // Destination type: enum
    try {
      g_value_transform (&src_value, &value);
    }
    catch (std::exception &ex) {
      std::ostringstream oss;
      oss << "Cannot convert '" << propertyValue << "' to enum: " << ex.what();
      std::string message = oss.str();

      GST_WARNING ("%s", message.c_str());
      throw KurentoException (MARSHALL_ERROR, message);
    }
  }
  else if (G_IS_PARAM_SPEC_STRING (pspec)) {
    g_value_set_static_string (&value, propertyValue.c_str());
  }
  // else if (...) { Add here whatever types are needed }
  else {
    std::ostringstream oss;
    oss << "Property type not implemented: " << G_PARAM_SPEC_TYPE_NAME (pspec);
    std::string message = oss.str();

    GST_WARNING ("%s", message.c_str());
    throw KurentoException (NOT_IMPLEMENTED, message);
  }

  g_object_set_property (G_OBJECT (gstElement), property_name, &value);
  g_value_unset (&value);
}

GStreamerFilterImpl::StaticConstructor GStreamerFilterImpl::staticConstructor;

GStreamerFilterImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
