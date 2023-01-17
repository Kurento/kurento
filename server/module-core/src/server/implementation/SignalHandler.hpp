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

#ifndef __SIGNAL_HANDLER_HPP__
#define __SIGNAL_HANDLER_HPP__

#include <map>
#include <functional>
#include <memory>

namespace kurento
{

template <class F, class T>
class MessageAdaptorAux
{
public:
  MessageAdaptorAux (F func,
                     std::weak_ptr <T> object) : func (func),
    object (object) {}

  F func;
  std::weak_ptr <T> object;
};


template <typename R, typename ...Args>
static R
signal_handler_adaptor (Args... args, gpointer data)
{
  MessageAdaptorAux <std::function <R (Args...) >, MediaObjectImpl> *adaptor =
    (MessageAdaptorAux <std::function <R (Args...) >, MediaObjectImpl> *) data;

  std::shared_ptr <MediaObjectImpl> aux (adaptor->object.lock() );

  if (!aux) {
    return (R) 0;
  }

  return adaptor->func (args...);
}

template <typename R, typename... Args>
static void
signal_handler_destroy (gpointer d, GClosure *closure)
{
  MessageAdaptorAux <std::function <R (Args...) >, MediaObjectImpl> *data =
    (MessageAdaptorAux <std::function <R (Args...) >, MediaObjectImpl> *) d;

  delete data;
}

template <typename R, typename... Args>
static gulong
register_signal_handler (GObject *element,
                         const gchar *signal_name,
                         std::function <R (Args...) > func,
                         std::shared_ptr <MediaObjectImpl> object)
{
  gulong id;

  MessageAdaptorAux<std::function <R (Args...) >, MediaObjectImpl> *data =
    new MessageAdaptorAux <std::function <R (Args...) >, MediaObjectImpl> (func,
        object);

  id = g_signal_connect_data (element, signal_name,
                              G_CALLBACK ( (&signal_handler_adaptor<R, Args...>) ),
                              (gpointer) data, (&signal_handler_destroy<R, Args...>), (GConnectFlags) 0);
  return id;
}

static void
unregister_signal_handler (gpointer element, gulong id)
{
  g_signal_handler_disconnect (element, id);
}

} /* kurento */

#endif /*  __SIGNAL_HANDLER_HPP__ */
