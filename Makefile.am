
plugin_LTLIBRARIES = libgstkurento.la

# sources used to compile this plug-in
libgstkurento_la_SOURCES = gstkurento.c \
			sdp_utils.c sdp_utils.h \
			gstagnosticbin.c gstagnosticbin.h \
			gstjoinable.c gstjoinable.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libgstkurento_la_CFLAGS = $(GST_CFLAGS) -Werror
libgstkurento_la_LIBADD = $(GST_LIBS)
libgstkurento_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libgstkurento_la_LIBTOOLFLAGS = --tag=disable-static

# headers we need but don't want installed
noinst_HEADERS = gstagnosticbin.h
