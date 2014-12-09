scripts/prepublish
#!/usr/bin/env node

var jsonfile = require('jsonfile')
var npm      = require('npm')

var exists = require('fs').exists

var recursive = require('merge').recursive


function onerror(error, code)
{
  console.trace(error)
  process.exit(code)
}

function updateFile(file, obj, callback)
{
  jsonfile.readFile(file, function(error, orig)
  {
    if(error) return callback(error)

    jsonfile.writeFile(file, recursive(orig, obj), callback)
  })
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

      var obj =
      {
        peerDependencies:
        {
          "kurento-client": "${module.kurentoVersion}"
        }
      }

      updateFile('package.json', obj, function(error)
      {
        if(error) throw error;
      })
    })
});
