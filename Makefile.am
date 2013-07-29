plugin_LTLIBRARIES = libkmsfilters.la

glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

# sources used to compile this plug-in
libkmsfilters_la_SOURCES = kmsfilters.c \
			$(BUILT_SOURCES)

# compiler and linker flags used to compile this plugin, set in configure.ac
libkmsfilters_la_CFLAGS = $(GST_CFLAGS) $(GIO_CFLAGS) $(NICE_CFLAGS) -Werror
libkmsfilters_la_LIBADD = $(GST_LIBS) $(GIO_LIBS) $(NICE_LIBS)
libkmsfilters_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libkmsfilters_la_LIBTOOLFLAGS = --tag=disable-static

