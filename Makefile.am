plugin_LTLIBRARIES = libkmsdtls.la

# variables used for enum generation
glib_enum_headers = gstdtlssrtp.h
glib_enum_define = GST_DTLS
glib_gen_prefix = gst_dtls
glib_gen_basename = gstdtls

include $(top_srcdir)/gst-glib-gen.mak

built_sources = gstdtls-enumtypes.c
built_headers = gstdtls-enumtypes.h

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

# sources used to compile this plug-in
libkmsdtls_la_SOURCES = = gstdtls.c \
	gstdtlsenc.c \
	gstdtlsdec.c \
	gstdtlsbase.c \
	gstiostream.c \
	gstdtlsconnection.c \
	gstdtlssrtpdemux.c \
	gstdtlssrtpdec.c \
	gstdtlssrtpenc.c

nodist_libkmsdtls_la_SOURCES = \
        $(built_sources)

# headers we need but don't want installed
noinst_HEADERS = 	\
	gstdtlsenc.h 	\
	gstdtlsdec.h 	\
	gstiostream.h 	\
	gstdtlsbase.h 	\
	gstdtlsconnection.h \
	gstdtlssrtpdemux.h \
	gstdtlssrtpdec.h \
	gstdtlssrtpenc.h \
	gstdtlssrtp.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libkmsdtls_la_CFLAGS = $(GST_PLUGINS_BASE_CFLAGS) \
	$(GST_BASE_CFLAGS) $(GST_CFLAGS) $(GIO_CFLAGS)
libkmsdtls_la_LIBADD = $(GST_PLUGINS_BASE_LIBS) \
	$(GST_BASE_LIBS) $(GST_LIBS) $(GIO_LIBS)
libkmsdtls_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libkmsdtls_la_LIBTOOLFLAGS = --tag=disable-static
