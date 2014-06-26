/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
#ifndef _KMS_CONF_CONTROLLER_H_
#define _KMS_CONF_CONTROLLER_H_

G_BEGIN_DECLS
#define KMS_TYPE_CONF_CONTROLLER (kms_conf_controller_get_type())
#define KMS_CONF_CONTROLLER(obj) (         \
  G_TYPE_CHECK_INSTANCE_CAST (             \
    (obj),                                 \
    KMS_TYPE_CONF_CONTROLLER,              \
    KmsConfController                      \
  )                                        \
)
#define KMS_CONF_CONTROLLER_CLASS(klass) ( \
  G_TYPE_CHECK_CLASS_CAST (                \
    (klass),                               \
    KMS_TYPE_CONF_CONTROLLER,              \
    KmsConfControllerClass                 \
  )                                        \
)
#define KMS_IS_CONF_CONTROLLER(obj) (      \
  G_TYPE_CHECK_INSTANCE_TYPE (             \
    (obj),                                 \
    KMS_TYPE_CONF_CONTROLLER               \
  )                                        \
)
#define KMS_IS_CONF_CONTROLLER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), KMS_TYPE_CONF_CONTROLLER))
#define KMS_CONF_CONTROLLER_GET_CLASS(obj) ( \
  G_TYPE_INSTANCE_GET_CLASS (                \
    (obj),                                   \
    KMS_TYPE_CONF_CONTROLLER,                \
    KmsConfControllerClass                   \
  )                                          \
)

#define KMS_CONF_CONTROLLER_KMS_ELEMENT "kmselement"
#define KMS_CONF_CONTROLLER_PIPELINE "pipeline"
#define KMS_CONF_CONTROLLER_PROFILE "profile"

typedef struct _KmsConfController KmsConfController;
typedef struct _KmsConfControllerClass KmsConfControllerClass;
typedef struct _KmsConfControllerPrivate KmsConfControllerPrivate;

struct _KmsConfController
{
  GObject parent;

  /*< private > */
  KmsConfControllerPrivate *priv;
};

struct _KmsConfControllerClass
{
  GObjectClass parent_class;

 /* public virtual methods */
  void (*link_valve) (KmsConfController * self,
    GstElement * element, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname);

  /* signal callbacks */
  void (*matched_elements) (KmsConfController * self, GstElement * appsink,
      GstElement * appsrc);
  void (*sink_required) (KmsConfController * self);
  void (*sink_unrequired) (KmsConfController * self, GstElement * sink);
};

GType kms_conf_controller_get_type (void);

KmsConfController * kms_conf_controller_new (const char *optname1, ...);
void kms_conf_controller_link_valve (KmsConfController * self,
    GstElement * valve, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname);

G_END_DECLS
#endif /* _KMS_CONF_CONTROLLER_H_ */