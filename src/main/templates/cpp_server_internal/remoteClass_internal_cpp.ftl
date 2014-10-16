${remoteClass.name}ImplInternal.cpp
/* Autogenerated with kurento-module-creator */

#include <gst/gst.h>
<#list remoteClassDependencies(remoteClass) as dependency>
<#if module.remoteClasses?seq_contains(dependency)>
#include "${dependency.name}Impl.hpp"
<#else>
#include "${dependency.name}.hpp"
</#if>
</#list>
#include "${remoteClass.name}Impl.hpp"
#include "${remoteClass.name}ImplFactory.hpp"
#include "${remoteClass.name}Internal.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>

<#list module.code.implementation["cppNamespace"]?split("::") as namespace>
namespace ${namespace}
{
</#list>
<#if (!remoteClass.abstract) && remoteClass.constructor??>

MediaObjectImpl *${remoteClass.name}ImplFactory::createObjectPointer (const boost::property_tree::ptree &conf, const Json::Value &params) const
{
  kurento::JsonSerializer s (false);
  ${remoteClass.name}Constructor constructor;

  s.JsonValue = params;
  constructor.Serialize (s);

  return createObject (conf<#rt>
     <#lt><#list remoteClass.constructor.params as param><#rt>
        <#lt>, <#rt>
        <#lt>constructor.get${param.name?cap_first}()<#rt>
     <#lt></#list><#if remoteClass.constructor.params?size != 0> </#if>);
}
</#if>

void
${remoteClass.name}Impl::invoke (std::shared_ptr<MediaObjectImpl> obj, const std::string &methodName, const Json::Value &params, Json::Value &response)
{
<#list remoteClass.methods as method>
  if (methodName == "${method.name}") {
    kurento::JsonSerializer s (false);
    ${remoteClass.name}Method${method.name?cap_first} method;
    <#if method.return??>
    JsonSerializer responseSerializer (true);
    ${getCppObjectType(method.return.type, false)} ret;
    </#if>

    s.JsonValue = params;
    method.Serialize (s);

    <#if method.return??>
    ret = <#rt>
    <#else><#rt>
    </#if>method.invoke (std::dynamic_pointer_cast<${remoteClass.name}> (obj) );
    <#if method.return??>
    responseSerializer.SerializeNVP (ret);
    response = responseSerializer.JsonValue["ret"];
    </#if>
    return;
  }

</#list>
<#list remoteClass.properties as property>
  if (methodName == "get${property.name?cap_first}") {
    ${getCppObjectType (property.type, false)} ret;
    JsonSerializer responseSerializer (true);

    ret = std::dynamic_pointer_cast<${remoteClass.name}> (obj)->get${property.name?cap_first} ();
    responseSerializer.SerializeNVP (ret);
    response = responseSerializer.JsonValue["ret"];
    return;
  }

<#if !property.final && !property.readOnly>
  if (methodName == "set${property.name?cap_first}") {
    kurento::JsonSerializer s (false);
    ${getCppObjectType (property.type, false)} ${property.name} ${initializePropertiesValues (property.type)};

<#assign jsonData = getJsonCppTypeData(property.type)>
    if (!s.JsonValue.isMember ("${property.name}") || !s.JsonValue["${property.name}"].isConvertibleTo (Json::ValueType::${jsonData.getJsonValueType()}) ) {
      throw KurentoException (MARSHALL_ERROR,
                              "'${property.name}' parameter should be a ${jsonData.getTypeDescription()}");
    }

    s.SerializeNVP (${property.name});
    std::dynamic_pointer_cast<${remoteClass.name}> (obj)->set${property.name?cap_first} (${property.name});
    return;
  }

</#if>
</#list>
<#if (remoteClass.extends)??>
  ${remoteClass.extends.name}Impl::invoke (obj, methodName, params, response);
<#else>
  JsonRpc::CallException e (JsonRpc::ErrorCode::SERVER_ERROR_INIT,
                            "Method '" + methodName + "' with " + std::to_string (params.size() ) + " parameters not found");
  throw e;
</#if>
}

bool
${remoteClass.name}Impl::connect (const std::string &eventType, std::shared_ptr<EventHandler> handler)
{
<#list remoteClass.events as event>

  if ("${event.name}" == eventType) {
    sigc::connection conn = signal${event.name}.connect ([ &, handler] (${event.name} event) {
      JsonSerializer s (true);

      s.Serialize ("data", event);
      s.Serialize ("object", this);
      s.JsonValue["type"] = "${event.name}";
      handler->sendEvent (s.JsonValue);
    });
    handler->setConnection (conn);
    return true;
  }
</#list>

<#if (remoteClass.extends)??>
  return ${remoteClass.extends.name}Impl::connect (eventType, handler);
<#else>
  return false;
</#if>
}

void
${remoteClass.name}Impl::Serialize (JsonSerializer &serializer)
{
  if (serializer.IsWriter) {
    try {
      Json::Value v (getId() );

      serializer.JsonValue = v;
    } catch (std::bad_cast &e) {
    }
  } else {
    throw KurentoException (MARSHALL_ERROR,
                            "'${remoteClass.name}Impl' cannot be deserialized as an object");
  }
}
<#list module.code.implementation["cppNamespace"]?split("::")?reverse as namespace>
} /* ${namespace} */
</#list>

namespace kurento
{

void
Serialize (std::shared_ptr<${module.code.implementation["cppNamespace"]}::${remoteClass.name}Impl> &object, JsonSerializer &serializer)
{
  if (serializer.IsWriter) {
    if (object) {
      object->Serialize (serializer);
    }
  } else {
    try {
      std::shared_ptr<kurento::MediaObjectImpl> aux;
      aux = ${module.code.implementation["cppNamespace"]}::${remoteClass.name}ImplFactory::getObject (serializer.JsonValue.asString () );
      object = std::dynamic_pointer_cast<${module.code.implementation["cppNamespace"]}::${remoteClass.name}Impl> (aux);
      return;
    } catch (KurentoException &ex) {
      throw KurentoException (MARSHALL_ERROR,
                              "'${remoteClass.name}Impl' object not found: " + ex.getMessage() );
    }
  }
}

void
Serialize (std::shared_ptr<${module.code.implementation["cppNamespace"]}::${remoteClass.name}> &object, JsonSerializer &serializer)
{
  std::shared_ptr<${module.code.implementation["cppNamespace"]}::${remoteClass.name}Impl> aux = std::dynamic_pointer_cast<${module.code.implementation["cppNamespace"]}::${remoteClass.name}Impl> (object);

  Serialize (aux, serializer);
  object = std::dynamic_pointer_cast <${module.code.implementation["cppNamespace"]}::${remoteClass.name}> (aux);
}

} /* kurento */