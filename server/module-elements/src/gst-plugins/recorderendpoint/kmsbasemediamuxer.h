/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#ifndef _KMS_BASE_MEDIA_MUXER_H_
#define _KMS_BASE_MEDIA_MUXER_H_

#include <gst/gst.h>
#include <commons/kmsmediatype.h>
#include <commons/kmsrecordingprofile.h>

G_BEGIN_DECLS
#define KMS_TYPE_BASE_MEDIA_MUXER               \
  (kms_base_media_muxer_get_type())
#define KMS_BASE_MEDIA_MUXER_CAST(obj)          \
  ((KmsBaseMediaMuxer *)(obj))
#define KMS_BASE_MEDIA_MUXER(obj)               \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),            \
  KMS_TYPE_BASE_MEDIA_MUXER,KmsBaseMediaMuxer))
#define KMS_BASE_MEDIA_MUXER_CLASS(klass)       \
  (G_TYPE_CHECK_CLASS_CAST((klass),             \
  KMS_TYPE_BASE_MEDIA_MUXER,                    \
  KmsBaseMediaMuxerClass))
#define KMS_IS_BASE_MEDIA_MUXER(obj)            \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),            \
  KMS_TYPE_BASE_MEDIA_MUXER))
#define KMS_IS_BASE_MEDIA_MUXER_CLASS(klass)    \
  (G_TYPE_CHECK_CLASS_TYPE((klass),             \
  KMS_TYPE_BASE_MEDIA_MUXER))
#define KMS_BASE_MEDIA_MUXER_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                 \
    (obj),                                    \
    KMS_TYPE_BASE_MEDIA_MUXER,                \
    KmsBaseMediaMuxerClass                    \
  )                                           \
)
#define KMS_BASE_MEDIA_MUXER_VIDEO_APPSRC "video-appsrc"
#define KMS_BASE_MEDIA_MUXER_AUDIO_APPSRC "audio-appsrc"
#define KMS_BASE_MEDIA_MUXER_PROFILE "profile"
#define KMS_BASE_MEDIA_MUXER_SINK "sink"
#define KMS_BASE_MEDIA_MUXER_URI "uri"

#define KMS_BASE_MEDIA_MUXER_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_BASE_MEDIA_MUXER ((elem))->mutex))
#define KMS_BASE_MEDIA_MUXER_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_BASE_MEDIA_MUXER ((elem))->mutex))
#define KMS_BASE_MEDIA_MUXER_GET_PIPELINE(elem) \
  (KMS_BASE_MEDIA_MUXER ((elem))->pipeline)
#define KMS_BASE_MEDIA_MUXER_GET_URI(elem) \
  (KMS_BASE_MEDIA_MUXER ((elem))->uri)
#define KMS_BASE_MEDIA_MUXER_GET_PROFILE(elem) \
  (KMS_BASE_MEDIA_MUXER ((elem))->profile)
typedef struct _KmsBaseMediaMuxer KmsBaseMediaMuxer;
typedef struct _KmsBaseMediaMuxerClass KmsBaseMediaMuxerClass;
typedef struct _KmsBaseMediaMuxerPrivate KmsBaseMediaMuxerPrivate;

struct _KmsBaseMediaMuxer
{
  GObject parent;

  /*< protected > */
  GstElement *pipeline;
  GRecMutex mutex;
  gchar *uri;
  KmsRecordingProfile profile;
};

struct _KmsBaseMediaMuxerClass
{
  GObjectClass parent_class;

  /* <protected> */
  GstElement * (*create_sink) (KmsBaseMediaMuxer *obj, const gchar *uri);
  void (*emit_on_sink_added) (KmsBaseMediaMuxer *obj, GstElement *sink);

  /* <public> */
  GstStateChangeReturn (*set_state) (KmsBaseMediaMuxer *obj, GstState state);
  GstState (*get_state) (KmsBaseMediaMuxer *obj);
  GstClock * (*get_clock) (KmsBaseMediaMuxer *obj);
  GstBus * (*get_bus) (KmsBaseMediaMuxer *obj);
  void (*dot_file) (KmsBaseMediaMuxer *obj);

  /* <virtual> */
  GstElement * (*add_src) (KmsBaseMediaMuxer *obj, KmsMediaType type, const gchar *id);
  gboolean (*remove_src) (KmsBaseMediaMuxer *obj, const gchar *id);

  /* <signals> */
  void (*on_sink_added) (KmsBaseMediaMuxer *obj, GstElement *sink);
};

GType kms_base_media_muxer_get_type ();

GstStateChangeReturn kms_base_media_muxer_set_state (KmsBaseMediaMuxer *obj,
  GstState state);
GstState kms_base_media_muxer_get_state (KmsBaseMediaMuxer *obj);
GstClock * kms_base_media_muxer_get_clock (KmsBaseMediaMuxer *obj);
GstBus * kms_base_media_muxer_get_bus (KmsBaseMediaMuxer *obj);
void kms_base_media_muxer_dot_file (KmsBaseMediaMuxer *obj);
GstElement * kms_base_media_muxer_add_src (KmsBaseMediaMuxer *obj, KmsMediaType type, const gchar *id);
gboolean kms_base_media_muxer_remove_src (KmsBaseMediaMuxer *obj, const gchar *id);

G_END_DECLS

#endif
