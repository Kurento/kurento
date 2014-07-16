${remoteClass.name}ImplFactory.hpp
#ifndef __${camelToUnderscore(remoteClass.name)}_IMPL_FACTORY_HPP__
#define __${camelToUnderscore(remoteClass.name)}_IMPL_FACTORY_HPP__

#include "${remoteClass.name}Impl.hpp"
<#if remoteClass.extends??>
#include "${remoteClass.extends.name}ImplFactory.hpp"
</#if>
#include <MediaObjectImpl.hpp>

namespace kurento
{

class ${remoteClass.name}ImplFactory : public virtual <#if remoteClass.extends??>${remoteClass.extends.name}Impl<#else>kurento::</#if>Factory
{
public:
  ${remoteClass.name}ImplFactory () {};

  virtual std::string getName () const {
    return "${remoteClass.name}";
  };

<#if (remoteClass.constructor)??>
private:

</#if>
<#if (!remoteClass.abstract) && (remoteClass.constructor)??>
  virtual MediaObjectImpl *createObjectPointer (const Json::Value &params) const;

</#if>
  <#if remoteClass.constructor??><#rt>
  MediaObjectImpl *createObject (<#rt>
   <#lt><#list remoteClass.constructor.params as param><#rt>
      <#lt>${getCppObjectType(param.type, true)}${param.name}<#rt>
      <#lt><#if param_has_next>, </#if><#rt>
   <#lt></#list>) const;
  </#if>
};

} /* kurento */

#endif /*  __${camelToUnderscore(remoteClass.name)}_IMPL_FACTORY_HPP__ */