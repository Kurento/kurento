<#assign node_name=module.code.api.js.nodeName>
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
scripts/prepublish
#!/usr/bin/env node

var exists  = require('fs').exists


exists('node_modules', function(found)
{
  if(!found)
    exists('lib', function(found)
    {
      if(!found)
      {
        var npm = require('npm')

        npm.load(function(error)
        {
          if(error) return console.error(error)

          npm.commands.install(function(error, data)
          {
            if(error) return console.error(error)
          })
        })
      }
    });
  else
    require('grunt').tasks()
});
</#if>
