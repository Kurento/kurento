glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

plugin_LTLIBRARIES = libpointerdetector.la

# sources used to compile this plug-in
libpointerdetector_la_SOURCES = pointerdetector.c \
			$(BUILT_SOURCES) \
			kmspointerdetector.c kmspointerdetector.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libpointerdetector_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) $(SOUP_CFLAGS) -Werror -I../..
libpointerdetector_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS) $(SOUP_LIBS)
libpointerdetector_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libpointerdetector_la_LIBTOOLFLAGS = --tag=disable-static
