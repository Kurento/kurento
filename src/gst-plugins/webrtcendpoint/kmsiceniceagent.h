/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

KmsIceNiceAgent *kms_ice_nice_agent_new (GMainContext * context,
                                         KmsWebrtcSession *session);
NiceAgent* kms_ice_nice_agent_get_agent (KmsIceNiceAgent* agent);

G_END_DECLS
#endif /* __KMS_ICE_NICE_AGENT_H__ */
