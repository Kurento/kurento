plugin_LTLIBRARIES = libgstkurento.la

glib_enum_define = GST_KURENTO
glib_gen_prefix = __gst_kurento
glib_gen_basename = gstkurento

include $(top_srcdir)/gst-glib-gen.mak

built_sources = gstkurento-marshal.c
built_headers = gstkurento-marshal.h

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

EXTRA_DIST = gstkurento-marshal.list

# sources used to compile this plug-in
libgstkurento_la_SOURCES = gstkurento.c \
			$(BUILT_SOURCES) \
			sdp_utils.c sdp_utils.h \
			gstagnosticbin.c gstagnosticbin.h \
			kmselement.c kmselement.h \
			kmsbasesdpendpoint.c kmsbasesdpendpoint.h \
			kmsbasertpendpoint.c kmsbasertpendpoint.h \
			gstudpstream.c gstudpstream.h \
			gstautomuxerbin.c automuxerbin.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libgstkurento_la_CFLAGS = $(GST_CFLAGS) $(GIO_CFLAGS) $(NICE_CFLAGS) -Werror
libgstkurento_la_LIBADD = $(GST_LIBS) $(GIO_LIBS) $(NICE_LIBS)
libgstkurento_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libgstkurento_la_LIBTOOLFLAGS = --tag=disable-static

# headers we need but don't want installed
noinst_HEADERS = gstagnosticbin.h
