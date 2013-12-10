glib_enum_define = KMS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

plugin_LTLIBRARIES = libvp8parse.la

# sources used to compile this plug-in
libvp8parse_la_SOURCES = vp8parse.c \
			$(BUILT_SOURCES) \
			kmsvp8parse.c kmsvp8parse.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libvp8parse_la_CFLAGS = $(GST_CFLAGS) -Werror
libvp8parse_la_LIBADD = $(GST_LIBS)
libvp8parse_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libvp8parse_la_LIBTOOLFLAGS = --tag=disable-static
