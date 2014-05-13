SUBDIRS =				\
	filters				\
	vp8parse


plugin_LTLIBRARIES = libgstkurento.la

# variables used for enum generation
glib_enum_headers = kmsuriendpointstate.h \
                kmshttpendpointmethod.h \
                kmsrecordingprofile.h \
                kmsmediatype.h

glib_enum_define = KMS
glib_gen_prefix = __kms
glib_gen_basename = kms

include $(top_srcdir)/gst-glib-gen.mak

built_sources = kms-marshal.c \
                kms-enumtypes.c

built_headers = kms-marshal.h \
                kms-enumtypes.h

BUILT_SOURCES = $(built_sources) $(built_headers)

CLEANFILES = $(BUILT_SOURCES)

EXTRA_DIST = kms-marshal.list

# sources used to compile this plug-in
libgstkurento_la_SOURCES = kmsplugin.c \
			$(BUILT_SOURCES) \
                        kmsloop.c kmsloop.h \
                        kmsconfcontroller.c kmsconfcontroller.h \
			kmsrecordingprofile.h kmsrecordingprofile.c \
			kmsutils.c kmsutils.h \
			sdp_utils.c sdp_utils.h \
			kmsagnosticbin.c kmsagnosticbin.h \
			kmselement.c kmselement.h \
			kmsbasesdpendpoint.c kmsbasesdpendpoint.h \
			kmsbasertpendpoint.c kmsbasertpendpoint.h \
			kmsrtpendpoint.c kmsrtpendpoint.h \
			kmsautomuxerbin.c kmsautomuxerbin.h \
			kmsrecorderendpoint.c kmsrecorderendpoint.h \
			kmsfilterelement.c kmsfilterelement.h \
			kmsplayerendpoint.c kmsplayerendpoint.h \
			kmsuriendpoint.c kmsuriendpoint.h \
			kmswebrtcendpoint.c kmswebrtcendpoint.h \
                        kmshttpendpoint.c httpendpoint.h \
                        kmsmixerport.c kmsmixerport.h \
                        kmsbasehub.c kmsbasehub.h \
                        kmsdispatcheronetomany.c kmsdispatcheronetomany.h \
                        gstsctp.c gstsctp.h  \
                        gstsctpbasesink.c gstsctpbasesink.h \
                        gstsctpclientsink.c gstsctpclientsink.h \
                        gstsctpserversrc.c gstsctpserversrc.h \
			kmscompositemixer.c kmscompositemixer.h \
			kmsdispatcher.c kmsdispatcher.h \
			kmsaudiomixer.c kmsaudiomixer.h \
			kmsselectablemixer.c kmsselectablemixer.h

# compiler and linker flags used to compile this plugin, set in configure.ac
libgstkurento_la_CFLAGS = $(GST_CFLAGS) $(GIO_CFLAGS) $(NICE_CFLAGS) $(SOUP_CFLAGS) -Werror
libgstkurento_la_LIBADD = $(GST_LIBS) $(GIO_LIBS) $(NICE_LIBS) $(SOUP_LIBS) $(SCTP_LIBS)
libgstkurento_la_LDFLAGS = $(GST_PLUGIN_LDFLAGS)
libgstkurento_la_LIBTOOLFLAGS = --tag=disable-static

# headers we need but don't want installed
noinst_HEADERS =
