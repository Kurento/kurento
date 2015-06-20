/*
 * (C) Copyright 2014-2015 Kurento (http://kurento.org/)
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

module.exports = function (grunt) {
  var DIST_DIR = 'dist';

  var pkg = grunt.file.readJSON('package.json');

  const PKG_BROWSER = 'lib/browser.js';

  var bower = {
    TOKEN: process.env.TOKEN,
    repository: 'git://github.com/Kurento/<%= pkg.name %>-bower.git'
  };

  // Project configuration.
  grunt.initConfig({
    pkg: pkg,
    bower: bower,

    // Plugins configuration
    clean: {
      generated_code: DIST_DIR,
      coverage: 'lib-cov',

      generated_doc: '<%= jsdoc.all.dest %>'
    },

    // Generate documentation
    jsdoc: {
      all: {
        src: [
          'README.md',
          'lib/**/*.js',
          'node_modules/kurento-client-core/lib/**/*.js',
          'node_modules/kurento-client-elements/lib/**/*.js',
          'node_modules/kurento-client-filters/lib/**/*.js',
          'test/*.js'
        ],
        dest: 'doc/jsdoc'
      }
    },

    // Generate instrumented version for coverage analisis
    jscoverage: {
      all: {
        expand: true,
        cwd: 'lib/',
        src: ['**/*.js'],
        dest: 'lib-cov/'
      }
    },

    // Generate browser versions and mapping debug file
    browserify: {
      options: {
        alias: [
          '<%= pkg.main %>:<%= pkg.name %>',
          'async',
          'es6-promise',
          'inherits',
          'kurento-client-core',
          'kurento-client-elements',
          'kurento-client-filters',
          'promisecallback'
        ]
      },

      coverage: {
        src: PKG_BROWSER,
        dest: DIST_DIR + '/<%= pkg.name %>.cov.js'
      },

      'standard': {
        src: PKG_BROWSER,
        dest: DIST_DIR + '/<%= pkg.name %>.js'
      },

      'minified': {
        src: PKG_BROWSER,
        dest: DIST_DIR + '/<%= pkg.name %>.min.js',

        options: {
          browserifyOptions: {
            debug: true
          },
          plugin: [
            ['minifyify', {
              compressPath: DIST_DIR,
              map: '<%= pkg.name %>.map',
              output: DIST_DIR + '/<%= pkg.name %>.map'
            }]
          ]
        }
      }
    },

    // Generate bower.json file from package.json data
    sync: {
      bower: {
        options: {
          sync: [
            'name', 'description', 'license', 'keywords', 'homepage',
            'repository'
          ],
          overrides: {
            authors: (pkg.author ? [pkg.author] : []).concat(pkg.contributors || []),
            main: 'js/<%= pkg.name %>.js'
          }
        }
      }
    },

    // Publish / update package info in Bower
    shell: {
      bower: {
        command: [
          'curl -X DELETE "https://bower.herokuapp.com/packages/<%= pkg.name %>?auth_token=<%= bower.TOKEN %>"',
          'node_modules/.bin/bower register <%= pkg.name %> <%= bower.repository %>',
          'node_modules/.bin/bower cache clean'
        ].join('&&')
      },

      'pre-coverage': {
        command: [
          'rm -rf lib_orig',
          'mv lib lib_orig',
          'mv lib-cov lib'
        ].join('&&')
      },
      'post-coverage': {
        command: [
          'rm -rf lib',
          'mv lib_orig lib'
        ].join('&&')
      }
    },

    // githooks configuration
    githooks: {
      all: {
        'pre-commit': 'jsbeautifier:git-pre-commit'
      }
    },

    jsbeautifier: {
      options: {
        js: {
          braceStyle: "collapse",
          breakChainedMethods: false,
          e4x: false,
          evalCode: false,
          indentChar: " ",
          indentLevel: 0,
          indentSize: 2,
          indentWithTabs: false,
          jslintHappy: true,
          keepArrayIndentation: false,
          keepFunctionIndentation: false,
          maxPreserveNewlines: 2,
          preserveNewlines: true,
          spaceBeforeConditional: true,
          spaceInParen: false,
          unescapeStrings: false,
          wrapLineLength: 80
        }
      },

      "default": {
        src: ["lib/**/*.js", "*.js", "test/*.js", "test_reconnect/*.js",
          "scripts/*.js"
        ]
      },
      "git-pre-commit": {
        src: ["lib/**/*.js", "*.js", "test/*.js", "test_reconnect/*.js",
          "scripts/*.js"
        ],
        options: {
          mode: "VERIFY_ONLY"
        }
      }
    },

    jshint: {
      all: ['lib/**/*.js', "test/*.js"],
      options: {
        "curly": true,
        "indent": 2,
        "unused": true,
        "undef": true,
        "camelcase": false,
        "newcap": true,
        "node": true,
        "browser": true
      }
    }
  });

  // Load plugins
  grunt.loadNpmTasks('grunt-browserify');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-jscoverage');
  grunt.loadNpmTasks('grunt-jsdoc');
  grunt.loadNpmTasks('grunt-npm2bower-sync');
  grunt.loadNpmTasks('grunt-shell');
  grunt.loadNpmTasks('grunt-githooks');
  grunt.loadNpmTasks('grunt-jsbeautifier');
  grunt.loadNpmTasks('grunt-contrib-jshint');

  // Alias tasks
  grunt.registerTask('default', ['clean', 'jsdoc', 'browserify',
    'jsbeautifier:git-pre-commit'
  ]);
  grunt.registerTask('bower', ['sync:bower', 'shell:bower']);
  grunt.registerTask('coverage', [
    'jscoverage',
    'shell:pre-coverage', 'browserify:coverage', 'shell:post-coverage'
  ]);
};
