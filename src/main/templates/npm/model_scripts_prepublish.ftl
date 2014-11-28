scripts/prepublish
#!/usr/bin/env node

var npm = require('npm')

var exists = require('fs').exists


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
    })
});
