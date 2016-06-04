/*
 * (C) Copyright 2014-2015 Kurento (http://kurento.org/)
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

module.exports = function (grunt) {
  var DIST_DIR = 'dist';
  var DIST_TEST_DIR = 'dist_test';

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
      generated_code: DIST_DIR + '*',
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
      'test': {
        options: {
          alias: [
            '<%= pkg.main %>:<%= pkg.name %>',
            'async',
            'es6-promise',
            'inherits',
            'kurento-client-core',
            'kurento-client-elements',
            'kurento-client-filters',
            'promisecallback',
            'dockerode',
            'minimist',
            'child_process'
          ]
        },
        src: PKG_BROWSER,
        dest: DIST_TEST_DIR + '/<%= pkg.name %>.test.js'
      },

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
            authors: (pkg.author ? [pkg.author] : []).concat(pkg.contributors ||
              []),
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
    'shell:pre-coverage', 'browserify:coverage', 'browserify:test',
    'shell:post-coverage'
  ]);
};
