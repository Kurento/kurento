/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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


module.exports = function(grunt)
{
  var DIST_DIR = 'dist';

  var pkg = grunt.file.readJSON('package.json');

  var bower =
  {
    TOKEN:      process.env.TOKEN,
    repository: 'git://github.com/Kurento/<%= pkg.name %>-bower.git'
  };

  // Project configuration.
  grunt.initConfig(
  {
    pkg:   pkg,
    bower: bower,

    // Plugins configuration
    clean:
    {
      generated_code: [DIST_DIR, 'src'],

      generated_doc: '<%= jsdoc.all.dest %>'
    },

    // Generate documentation
    jsdoc:
    {
      all:
      {
        src: ['README.md', 'lib/**/*.js', 'test/*.js'], 
        dest: 'doc/jsdoc'
      }
    },

    // Generate browser versions and mapping debug file
    browserify:
    {
      require:
      {
        src:  '<%= pkg.main %>',
        dest: DIST_DIR+'/<%= pkg.name %>_require.js'
      },
      standalone:
      {
        src:  '<%= pkg.main %>',
        dest: DIST_DIR+'/<%= pkg.name %>.js',

        options:
        {
          exclude: ['ws', 'bufferutil', 'utf-8-validate' ],
          browserifyOptions: {
            debug: true,
            standalone: 'RpcBuilder'
          }
        }
      },

      'require minified':
      {
        src:  '<%= pkg.main %>',
        dest: DIST_DIR+'/<%= pkg.name %>_require.min.js',

        options:
        {
          exclude: ['ws', 'bufferutil', 'utf-8-validate' ],
          debug: true,
          browserifyOptions: {
            debug: true,
            standalone: 'RpcBuilder'
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
          exclude: ['ws', 'bufferutil', 'utf-8-validate' ],
          debug: true,
          browserifyOptions: {
            debug: true,
            standalone: 'RpcBuilder'
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
            authors: (pkg.author ? [pkg.author] : []).concat(pkg.contributors || [])
          }
        }
      }
    },

    // Publish / update package info in Bower
    shell:
    {
      bower: {
        command: [
          'curl -X DELETE "https://bower.herokuapp.com/packages/<%= pkg.name %>?auth_token=<%= bower.TOKEN %>"',
          'node_modules/.bin/bower register <%= pkg.name %> <%= bower.repository %>',
          'node_modules/.bin/bower cache clean'
        ].join('&&')
      }
    }
  });

  // Load plugins
  grunt.loadNpmTasks('grunt-browserify');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-jsdoc');
  grunt.loadNpmTasks('grunt-npm2bower-sync');
  grunt.loadNpmTasks('grunt-shell');

  // Alias tasks
  grunt.registerTask('default', ['clean', 'jsdoc', 'browserify']);
  grunt.registerTask('bower',   ['sync:bower', 'shell:bower']);
};
