<#assign api_js=module.code.api.js>
<#assign node_name=api_js.nodeName>
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
  <#assign kurentoClient_path="node_modules/kurento-client">
<#else>
  <#assign kurentoClient_path="../..">
</#if>
Gruntfile.js
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


module.exports = function(grunt)
{
  var DIST_DIR = 'dist';

  var pkg = grunt.file.readJSON('package.json');

  // Project configuration.
  grunt.initConfig({
    pkg: pkg,

    // Plugins configuration
    clean:
    {
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
      'doc': '<%= jsdoc.all.dest %>',

      'browser': DIST_DIR,
</#if>
      'code': 'lib'
    },

    // Check if Kurento Module Creator exists
    'path-check':
    {
      'generate plugin': {
        src: 'kurento-module-creator',
        options: {
          tasks: ['shell:kmd']
        }
      }
    },

<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
  <#if api_js.npmGit??>
    bower:
    {
      TOKEN:      process.env.TOKEN,
      repository: '${api_js.npmGit}'
    },

  </#if>
    // Generate documentation
    jsdoc:
    {
      all:
      {
        src: [
          'README.md',
          'lib/**/*.js',
          'test/*.js'
        ],
        dest: 'doc/jsdoc'
      }
    },

    // Generate browser versions and mapping debug file
    browserify:
    {
      options: {
        external: ['kurento-client']
      },

      'require':
      {
        src:  '<%= pkg.main %>',
        dest: DIST_DIR+'/<%= pkg.name %>_require.js'
      },

      'standalone':
      {
        src:  '<%= pkg.main %>',
        dest: DIST_DIR+'/<%= pkg.name %>.js',

        options: {
          browserifyOptions: {
            standalone: '<%= pkg.name %>',
          }
        }
      },

      'require minified':
      {
        src:  '<%= pkg.main %>',
        dest: DIST_DIR+'/<%= pkg.name %>_require.min.js',

        options:
        {
          browserifyOptions: {
            debug: true
          },
          plugin: [
            ['minifyify',
             {
               compressPath: DIST_DIR,
               map: '<%= pkg.name %>.map'
             }]
          ]
        }
      },

      'standalone minified':
      {
        src:  '<%= pkg.main %>',
        dest: DIST_DIR+'/<%= pkg.name %>.min.js',

        options:
        {
          browserifyOptions: {
            debug: true,
            standalone: '<%= pkg.name %>'
          },
          plugin: [
            ['minifyify',
             {
               compressPath: DIST_DIR,
               map: '<%= pkg.name %>.map',
               output: DIST_DIR+'/<%= pkg.name %>.map'
             }]
          ]
        }
      }
    },

    // Generate bower.json file from package.json data
    sync:
    {
      bower:
      {
        options:
        {
          sync: [
            'name', 'description', 'license', 'keywords', 'homepage',
            'repository'
          ],
          overrides: {
            authors: (pkg.author ? [pkg.author] : []).concat(pkg.contributors || []),
            ignore: ['doc/', 'lib/', 'Gruntfile.js', 'package.json'],
            main: DIST_DIR+'/<%= pkg.name %>.js'
          }
        }
      }
    },

</#if>
    shell:
    {
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters"
  && api_js.npmGit??>
      // Publish / update package info in Bower
      bower: {
        command: [
          'curl -X DELETE "https://bower.herokuapp.com/packages/<%= pkg.name %>?auth_token=<%= bower.TOKEN %>"',
          'node_modules/.bin/bower register <%= pkg.name %> <%= bower.repository %>',
          'node_modules/.bin/bower cache clean'
        ].join('&&')
      },

</#if>
      // Generate the Kurento Javascript client
      kmd: {
        command: [
          'mkdir -p ./lib',
          'kurento-module-creator --delete'
          +' --templates ${kurentoClient_path}/templates'
<#list module.imports as import>
          +' --deprom node_modules/${import.module.code.api.js.nodeName}/src'
</#list>
          +' --rom ./src --codegen ./lib'
        ].join('&&')
      }
    }
  });

  // Load plugins
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-path-check');
  grunt.loadNpmTasks('grunt-shell');

<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
  grunt.loadNpmTasks('grunt-browserify');
  grunt.loadNpmTasks('grunt-jsdoc');
  <#if api_js.npmGit??>
  grunt.loadNpmTasks('grunt-npm2bower-sync');
  </#if>

</#if>
  // Alias tasks
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
  grunt.registerTask('generate', ['path-check:generate plugin', 'browserify']);
  grunt.registerTask('default',  ['clean', 'jsdoc', 'generate'<#if api_js.npmGit??>, 'sync:bower'</#if>]);
  <#if api_js.npmGit??>
  grunt.registerTask('bower',    ['shell:bower']);
  </#if>
<#else>
  grunt.registerTask('default', ['clean', 'path-check:generate plugin']);
</#if>
};
