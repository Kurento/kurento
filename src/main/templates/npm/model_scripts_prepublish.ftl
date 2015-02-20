<#assign node_name=module.code.api.js.nodeName>
scripts/prepublish
#!/usr/bin/env node

var npm = require('npm')

var exists = require('fs').exists

<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">

function updateFile(file, obj, callback)
{
  var jsonfile  = require('jsonfile')
  var recursive = require('merge').recursive

  jsonfile.readFile(file, function(error, orig)
  {
    if(error) return callback(error)

    jsonfile.writeFile(file, recursive(orig, obj), callback)
  })
}
</#if>

function onerror(error, code)
{
  console.trace(error)
  process.exit(code)
}


exists('node_modules/grunt', function(found)
{
  if(!found)
    exists('lib', function(found)
    {
//      if(!found)
      {
        npm.load(function(error)
        {
          if(error) return onerror(error, -1)

          npm.commands.install(function(error, data)
          {
            if(error) return onerror(error, -2)
          })
        })
      }
    })
  else
    require('grunt').tasks([], {}, function()
    {
      npm.load(function(error)
      {
        if(error) return onerror(error, -3)

        npm.config.set('production', true)
        npm.commands.prune(function(error, data)
        {
          if(error) return onerror(error, -4)

          process.exit()
        })
      })
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">

      var obj =
      {
        peerDependencies:
        {
          "kurento-client": "${generateKurentoClientJsVersion(module.kurentoVersion)}"
        }
      }

      updateFile('package.json', obj, function(error)
      {
        if(error) throw error;
      })
</#if>
    })
});
