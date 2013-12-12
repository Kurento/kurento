glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

if HAVE_OPENCV

plugin_LTLIBRARIES = libfaceoverlay.la

# sources used to compile this plug-in
libfaceoverlay_la_SOURCES = faceoverlay.c \
			$(BUILT_SOURCES) \
			kmsfaceoverlay.c kmsfaceoverlay.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libfaceoverlay_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) -Werror
libfaceoverlay_la_CXXFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) -Werror
libfaceoverlay_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS)
libfaceoverlay_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libfaceoverlay_la_LIBTOOLFLAGS = --tag=disable-static

endif
