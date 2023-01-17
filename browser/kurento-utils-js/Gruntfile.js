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

module.exports = function (grunt) {
  var DIST_DIR = "dist";

  var pkg = grunt.file.readJSON("package.json");

  var bower = {
    TOKEN: process.env.TOKEN,
    repository: "git://github.com/Kurento/<%= pkg.name %>-bower.git"
  };

  // Project configuration.
  grunt.initConfig({
    pkg: pkg,
    bower: bower,

    // Plugins configuration
    clean: {
      generated_code: DIST_DIR,
      coverage: "lib-cov",

      generated_doc: "<%= jsdoc.all.dest %>"
    },

    githooks: {
      all: {
        "pre-commit": "jsbeautifier:git-pre-commit"
      }
    },

    // Generate documentation
    jsdoc: {
      all: {
        src: ["package.json", "README.md", "lib/**/*.js", "test/*.js"],
        dest: "doc/jsdoc",
        options: {
          configure: ".jsdoc.conf.js"
        }
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
      default: {
        src: ["lib/**/*.js", "*.js", "test/*.js", "scripts/*.js"]
      },
      "git-pre-commit": {
        src: ["lib/**/*.js", "*.js", "test/*.js", "scripts/*.js"],
        options: {
          mode: "VERIFY_ONLY"
        }
      }
    },

    // Generate instrumented version for coverage analisis
    jscoverage: {
      all: {
        expand: true,
        cwd: "lib/",
        src: ["**/*.js"],
        dest: "lib-cov/"
      }
    },

    jshint: {
      all: ["lib/**/*.js", "test/*.js"],
      options: {
        curly: true,
        indent: 2,
        unused: true,
        undef: true,
        camelcase: false,
        newcap: true,
        node: true,
        browser: true
      }
    },

    // Generate browser versions and mapping debug file
    browserify: {
      options: {
        transform: ["browserify-optional"]
      },

      require: {
        src: "lib/browser.js",
        dest: DIST_DIR + "/<%= pkg.name %>_require.js"
      },

      standalone: {
        src: "lib/browser.js",
        dest: DIST_DIR + "/<%= pkg.name %>.js",

        options: {
          browserifyOptions: {
            standalone: "<%= pkg.name %>"
          }
        }
      },

      coverage: {
        src: "lib-cov/browser.js",
        dest: DIST_DIR + "/<%= pkg.name %>.cov.js",

        options: {
          browserifyOptions: {
            standalone: "<%= pkg.name %>"
          }
        }
      },

      "require minified": {
        src: "lib/browser.js",
        dest: DIST_DIR + "/<%= pkg.name %>_require.min.js",

        options: {
          browserifyOptions: {
            debug: true
          },
          plugin: [
            [
              "minifyify",
              {
                compressPath: DIST_DIR,
                map: "<%= pkg.name %>.map"
              }
            ]
          ]
        }
      },

      "standalone minified": {
        src: "lib/browser.js",
        dest: DIST_DIR + "/<%= pkg.name %>.min.js",

        options: {
          browserifyOptions: {
            debug: true,
            standalone: "<%= pkg.name %>"
          },
          plugin: [
            [
              "minifyify",
              {
                compressPath: DIST_DIR,
                map: "<%= pkg.name %>.map",
                output: DIST_DIR + "/<%= pkg.name %>.map"
              }
            ]
          ]
        }
      }
    },

    // Generate bower.json file from package.json data
    sync: {
      bower: {
        options: {
          sync: [
            "name",
            "description",
            "license",
            "keywords",
            "homepage",
            "repository"
          ],
          overrides: {
            authors: (pkg.author ? [pkg.author] : []).concat(
              pkg.contributors || []
            )
          }
        }
      }
    },

    // Publish / update package info in Bower
    shell: {
      bower: {
        command: [
          'curl -X DELETE "https://bower.herokuapp.com/packages/<%= pkg.name %>?auth_token=<%= bower.TOKEN %>"',
          "node_modules/.bin/bower register <%= pkg.name %> <%= bower.repository %>",
          "node_modules/.bin/bower cache clean"
        ].join("&&")
      }
    }
  });

  // Load plugins
  grunt.loadNpmTasks("grunt-browserify");
  grunt.loadNpmTasks("grunt-contrib-clean");
  grunt.loadNpmTasks("grunt-githooks");
  grunt.loadNpmTasks("grunt-jsbeautifier");
  grunt.loadNpmTasks("grunt-jscoverage");
  grunt.loadNpmTasks("grunt-jsdoc");
  grunt.loadNpmTasks("grunt-contrib-jshint");
  grunt.loadNpmTasks("grunt-npm2bower-sync");
  grunt.loadNpmTasks("grunt-shell");

  // Alias tasks
  grunt.registerTask("default", [
    "clean",
    "jsdoc",
    "browserify",
    "jsbeautifier:git-pre-commit"
  ]);
  grunt.registerTask("bower", ["sync:bower", "shell:bower"]);
  grunt.registerTask("coverage", [
    "clean:coverage",
    "jscoverage",
    "browserify:coverage"
  ]);
};
