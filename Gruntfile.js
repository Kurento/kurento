module.exports = function(grunt)
{
  var DIST_DIR = 'dist';

  // Project configuration.
  grunt.initConfig(
  {
    pkg: grunt.file.readJSON('package.json'),

    // Plugins configuration
    clean: [DIST_DIR, 'src', 'doc/jsdoc'],

    jsdoc:
    {
        dist:
        {
            src: ['README.md', 'lib/*.js', 'test/*.js'], 
            options:
            {
                destination: 'doc/jsdoc'
            }
        }
    },

    browserify:
    {
      standalone:
      {
        src:  '<%= pkg.main %>',
        dest: DIST_DIR+'/<%= pkg.name %>.js',

        options:
        {
          standalone: '<%= pkg.name %>'
        }
      },

      require:
      {
        src:  '<%= pkg.main %>',
        dest: DIST_DIR+'/<%= pkg.name %>_require.js'
      }
    },

    uglify:
    {
      options:
      {
        banner: '/*! <%= pkg.name %> <%= grunt.template.today("yyyy-mm-dd") %> */\n'
      },

      standalone:
      {
        src:  DIST_DIR+'/<%= pkg.name %>.js',
        dest: DIST_DIR+'/<%= pkg.name %>.min.js'
      },

      require:
      {
        src:  DIST_DIR+'/<%= pkg.name %>_require.js',
        dest: DIST_DIR+'/<%= pkg.name %>_require.min.js'
      }
    },

    copy:
    {
      maven:
      {
        expand: true,
        cwd: DIST_DIR,
        src: '*',
        dest: 'src/main/resources/js/',
      }
    }
  });

  // Load plugins
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-uglify');

  grunt.loadNpmTasks('grunt-browserify');
  grunt.loadNpmTasks('grunt-jsdoc');

  // Default task(s).
  grunt.registerTask('default', ['clean', 'jsdoc', 'browserify', 'uglify']);
  grunt.registerTask('maven',   ['default', 'copy']);
};