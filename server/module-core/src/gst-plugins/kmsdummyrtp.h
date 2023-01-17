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
#ifndef _KMS_DUMMY_RTP_H_
#define _KMS_DUMMY_RTP_H_

#include "kmselement.h"
#include "commons/kmsbasertpendpoint.h"

G_BEGIN_DECLS
#define KMS_TYPE_DUMMY_RTP     \
  (kms_dummy_rtp_get_type())
#define KMS_DUMMY_RTP(obj) (   \
  G_TYPE_CHECK_INSTANCE_CAST(  \
    (obj),                     \
    KMS_TYPE_DUMMY_RTP,        \
    KmsDummyRtp                \
  )                            \
)

#define KMS_DUMMY_RTP_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (          \
    (klass),                         \
    KMS_TYPE_DUMMY_RTP,              \
    KmsDummyRtpClass                 \
  )                                  \
)
#define KMS_IS_DUMMY_RTP(obj) (  \
  G_TYPE_CHECK_INSTANCE_TYPE (   \
    (obj),                       \
    KMS_TYPE_DUMMY_RTP           \
  )                              \
)
#define KMS_IS_DUMMY_RTP_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_TYPE(               \
    (klass),                             \
    KMS_TYPE_DUMMY_RTP                   \
  )                                      \
)

typedef struct _KmsDummyRtp KmsDummyRtp;
typedef struct _KmsDummyRtpClass KmsDummyRtpClass;

struct _KmsDummyRtp
{
  KmsBaseRtpEndpoint parent;
};

struct _KmsDummyRtpClass
{
  KmsBaseRtpEndpointClass parent_class;
};

GType kms_dummy_rtp_get_type (void);

gboolean kms_dummy_rtp_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_DUMMY_rTP_H_ */
