glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

plugin_LTLIBRARIES = libchroma.la

# sources used to compile this plug-in
libchroma_la_SOURCES = chroma.c \
			$(BUILT_SOURCES) \
			kmschroma.c kmschroma.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libchroma_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) $(SOUP_CFLAGS) -Werror
libchroma_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS) $(SOUP_LIBS)
libchroma_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libchroma_la_LIBTOOLFLAGS = --tag=disable-static
