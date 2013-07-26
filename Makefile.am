plugin_LTLIBRARIES = libgstkurento.la

glib_enum_define = KMS_PLUGINS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

built_sources = kms-marshal.c
built_headers = kms-marshal.h

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

EXTRA_DIST = kms-marshal.list

# sources used to compile this plug-in
libgstkurento_la_SOURCES = gstkurento.c \
			$(BUILT_SOURCES) \
			sdp_utils.c sdp_utils.h \
			kmsagnosticbin.c kmsagnosticbin.h \
			kmselement.c kmselement.h \
			kmsbasesdpendpoint.c kmsbasesdpendpoint.h \
			kmsbasertpendpoint.c kmsbasertpendpoint.h \
			kmsrtpendpoint.c kmsrtpendpoint.h \
			gstautomuxerbin.c automuxerbin.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libgstkurento_la_CFLAGS = $(GST_CFLAGS) $(GIO_CFLAGS) $(NICE_CFLAGS) -Werror
libgstkurento_la_LIBADD = $(GST_LIBS) $(GIO_LIBS) $(NICE_LIBS)
libgstkurento_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libgstkurento_la_LIBTOOLFLAGS = --tag=disable-static

# headers we need but don't want installed
noinst_HEADERS = kmsagnosticbin.h
