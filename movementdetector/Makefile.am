glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

if HAVE_OPENCV

plugin_LTLIBRARIES = libmovementdetector.la

# sources used to compile this plug-in
libmovementdetector_la_SOURCES = movementdetector.c \
			$(BUILT_SOURCES) \
			kmsmovementdetector.c kmsmovementdetector.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libmovementdetector_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) -Werror
libmovementdetector_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS)
libmovementdetector_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libmovementdetector_la_LIBTOOLFLAGS = --tag=disable-static

libmovementdetector_ladir = $(datadir)/@PACKAGE@

endif
