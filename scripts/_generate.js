var exists = require('fs').exists

var npm = require('npm')


exists('lib', function(found)
{
//  if(!found)
    npm.load(function(error)
    {
      if(error) return console.trace(error)

      npm.commands.install(function(error, data)
      {
        if(error) return console.trace(error)
      })
    })
})
