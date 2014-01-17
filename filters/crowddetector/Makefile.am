
glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

plugin_LTLIBRARIES = libcrowddetector.la

# sources used to compile this plug-in
libcrowddetector_la_SOURCES = crowddetector.c \
			$(BUILT_SOURCES) \
			kmscrowddetector.c kmscrowddetector.h
# compiler and linker flags used to compile this plugin, set in configure.ac
libcrowddetector_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) -Werror
libcrowddetector_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS) -ltesseract
libcrowddetector_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libcrowddetector_la_LIBTOOLFLAGS = --tag=disable-static

