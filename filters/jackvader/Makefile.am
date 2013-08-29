glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

if HAVE_OPENCV

plugin_LTLIBRARIES = libjackvader.la

# sources used to compile this plug-in
libjackvader_la_SOURCES = jackvader.c \
			$(BUILT_SOURCES) \
			kmsjackvader.c kmsjackvader.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libjackvader_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) -Werror
libjackvader_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS)
libjackvader_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libjackvader_la_LIBTOOLFLAGS = --tag=disable-static

libjackvader_ladir = $(datadir)/@PACKAGE@
libjackvader_la_DATA = jack.png vader.png

endif
