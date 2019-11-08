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

#ifndef __KMS_ICE_NICE_AGENT_H__
#define __KMS_ICE_NICE_AGENT_H__

#include <nice/nice.h>
#include "kmsicebaseagent.h"
#include "kmswebrtcsession.h"

G_BEGIN_DECLS

#define KMS_TYPE_ICE_NICE_AGENT \
  (kms_ice_nice_agent_get_type())
#define KMS_ICE_NICE_AGENT(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_ICE_NICE_AGENT,KmsIceNiceAgent))
#define KMS_ICE_NICE_AGENT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_ICE_NICE_AGENT,KmsIceNiceAgentClass))
#define KMS_IS_ICE_NICE_AGENT(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_ICE_NICE_AGENT))
#define KMS_IS_ICE_NICE_AGENT_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_ICE_NICE_AGENT))
#define KMS_ICE_NICE_AGENT_CAST(obj) ((KmsIceNiceAgent*)(obj))

typedef struct _KmsIceNiceAgentPrivate KmsIceNiceAgentPrivate;
typedef struct _KmsIceNiceAgent KmsIceNiceAgent;
typedef struct _KmsIceNiceAgentClass KmsIceNiceAgentClass;

struct _KmsIceNiceAgent
{
  KmsIceBaseAgent parent;

  KmsIceNiceAgentPrivate *priv;
};

struct _KmsIceNiceAgentClass
{
  KmsIceBaseAgentClass parent_class;
};

GType kms_ice_nice_agent_get_type (void);

KmsIceNiceAgent *kms_ice_nice_agent_new (GMainContext * context);
NiceAgent* kms_ice_nice_agent_get_agent (KmsIceNiceAgent* agent);

G_END_DECLS
#endif /* __KMS_ICE_NICE_AGENT_H__ */
