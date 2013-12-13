glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

if HAVE_OPENCV

plugin_LTLIBRARIES = libimageoverlay.la

# sources used to compile this plug-in
libimageoverlay_la_SOURCES = imageoverlay.c \
			$(BUILT_SOURCES) \
			kmsimageoverlay.c kmsimageoverlay.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libimageoverlay_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) $(SOUP_CFLAGS) -Werror
libimageoverlay_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS) $(SOUP_LIBS)
libimageoverlay_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libimageoverlay_la_LIBTOOLFLAGS = --tag=disable-static

endif
