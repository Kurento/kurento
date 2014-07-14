Module.cpp
/* Generated using ktool-rom-processor */

#include "FactoryRegistrar.hpp"

<#list model.remoteClasses as remoteClass>
#include "${remoteClass.name}Impl.hpp"
</#list>

extern "C" {

  const kurento::FactoryRegistrar *getFactoryRegistrar ();

}

const kurento::FactoryRegistrar *
getFactoryRegistrar ()
{
  static bool loaded = false;
  static std::list<std::shared_ptr<kurento::Factory>> factories;

  if (!loaded) {
<#list model.remoteClasses as remoteClass>
  <#if !(remoteClass.abstract)>
    factories.push_back (std::shared_ptr <kurento::Factory> (new kurento::${remoteClass.name}Impl::Factory() ) );
  </#if>
</#list>
    loaded = true;
  }

  static kurento::FactoryRegistrar factory (factories);
  return &factory;
}
