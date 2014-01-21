glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

plugin_LTLIBRARIES = libpointerdetector2.la

# sources used to compile this plug-in
libpointerdetector2_la_SOURCES = pointerdetector2.c \
			$(BUILT_SOURCES) \
			kmspointerdetector2.c kmspointerdetector2.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libpointerdetector2_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) $(SOUP_CFLAGS) -Werror -I../..
libpointerdetector2_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS) $(SOUP_LIBS)
libpointerdetector2_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libpointerdetector2_la_LIBTOOLFLAGS = --tag=disable-static
