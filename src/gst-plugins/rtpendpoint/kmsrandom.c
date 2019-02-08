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

#include <unistd.h>  // 'syscall()' is not POSIX, requires GNU extensions in GCC
#include <sys/syscall.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <fcntl.h>

#include <linux/random.h>
#include <glib.h>

#include "kmsrandom.h"

#define RANDOM_NUMBER_SOURCE_DEVICE "/dev/urandom"

#ifdef SYS_getrandom
#define MAX_RANDOM_TRIES 3

static gchar *
sys_call_gen_random_key (guint size)
{
  guint8 *buff;
  gchar *key;
  long int ret;
  guint tries, l;

  buff = g_malloc0 (size);
  tries = l = 0;

  while (tries < MAX_RANDOM_TRIES && l < size) {
    ret = syscall (SYS_getrandom, buff + l, size - l, GRND_NONBLOCK);

    if (ret < 0) {
      goto error;
    }

    l += ret;
    tries++;
  }

  if (tries == MAX_RANDOM_TRIES) {
    goto error;
  }

  key = g_base64_encode (buff, size);
  g_free (buff);

  return key;

error:
  g_free (buff);

  return NULL;
}
#endif

static gchar *
file_gen_random_key (guint size)
{
  gint fd, entropy;
  guint8 *buff;
  gchar *key = NULL;
  ssize_t amount_read = 0;

  fd = open (RANDOM_NUMBER_SOURCE_DEVICE, O_RDONLY | O_NOFOLLOW);

  if (fd < 0) {
    return NULL;
  }

  /* Check if this is really a random device and whether it has enough entropy */
  if (ioctl (fd, RNDGETENTCNT, &entropy) != 0 ||
      (entropy < (sizeof (guint) * size))) {
    goto end;
  }

  buff = g_malloc0 (size);

  while (amount_read < size) {
    ssize_t r = read (fd, (gchar *) buff + amount_read, size - amount_read);

    if (r > 0) {
      amount_read += r;
    } else if (!r) {
      break;
    }
  }

  if (amount_read >= size) {
    key = g_base64_encode (buff, size);
  }

  g_free (buff);

end:
  close (fd);

  return key;
}

gchar *
generate_random_key (guint size)
{
  gchar *key = NULL;

#ifdef SYS_getrandom
  key = sys_call_gen_random_key (size);
#endif

  if (key == NULL) {
    /* Fallback method: Try to read from /dev/random. This might */
    /* deal with security problems. Read LibreSSL portability    */
    /* reports regarding this issue. */
    key = file_gen_random_key (size);
  }

  return key;
}
