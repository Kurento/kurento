glib_enum_define = KMS_FILTERS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

plugin_LTLIBRARIES = libfacedetector.la

# sources used to compile this plug-in
libfacedetector_la_SOURCES = facedetector.c \
			$(BUILT_SOURCES) \
			kmsfacedetector.c kmsfacedetector.h \
			classifier.h classifier.cpp

# compiler and linker flags used to compile this plugin, set in configure.ac
libfacedetector_la_CFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) -Werror
libfacedetector_la_CXXFLAGS = $(GST_CFLAGS) $(OPENCV_CFLAGS) -Werror
libfacedetector_la_LIBADD = $(GST_LIBS) $(OPENCV_LIBS)
libfacedetector_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libfacedetector_la_LIBTOOLFLAGS = --tag=disable-static
