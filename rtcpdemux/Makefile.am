glib_enum_define = KMS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

plugin_LTLIBRARIES = librtcpdemux.la

# sources used to compile this plug-in
librtcpdemux_la_SOURCES = rtcpdemux.c \
			$(BUILT_SOURCES) \
			kmsrtcpdemux.c kmrtcpdemux.h

# compiler and linker flags used to compile this plugin, set in configure.ac
librtcpdemux_la_CFLAGS = $(GST_CFLAGS) -Werror
librtcpdemux_la_LIBADD = $(GST_LIBS)
librtcpdemux_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
librtcpdemux_la_LIBTOOLFLAGS = --tag=disable-static
