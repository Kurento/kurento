glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

plugin_LTLIBRARIES = libh264filter.la

# sources used to compile this plug-in
libh264filter_la_SOURCES = h264filter.c \
			$(BUILT_SOURCES) \
			kmsh264filter.c kmsh264filter.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libh264filter_la_CFLAGS = $(GST_CFLAGS) -Werror
libh264filter_la_LIBADD = $(GST_LIBS)
libh264filter_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libh264filter_la_LIBTOOLFLAGS = --tag=disable-static


