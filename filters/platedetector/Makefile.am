
glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

plugin_LTLIBRARIES = libplatedetector.la

# sources used to compile this plug-in
libplatedetector_la_SOURCES = platedetector.c \
			$(BUILT_SOURCES) \
			kmsplatedetector.c kmsplatedetector.h
# compiler and linker flags used to compile this plugin, set in configure.ac
libplatedetector_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) -Werror
libplatedetector_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS) -ltesseract
libplatedetector_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libplatedetector_la_LIBTOOLFLAGS = --tag=disable-static

libplatedetector_ladir = $(datadir)/@PACKAGE@/tessdata
libplatedetector_la_DATA = tessdata/plateLanguage.traineddata
