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

#include <unistd.h>
#include <sys/syscall.h>
#include <linux/random.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>

#include <glib.h>

#include "kmsrandom.h"

#define RANDOM_NUMBER_SOURCE_DEVICE "/dev/random"

#ifdef SYS_getrandom
#define MAX_RANDOM_TRIES 3

static gchar *
sys_call_gen_random_key (guint size)
{
  guint8 *buff;
  gchar *key;
  guint ret, tries, l;

  buff = g_malloc0 (size);
  tries = l = 0;

  while (tries < MAX_RANDOM_TRIES && l < size) {
    ret = syscall (SYS_getrandom, buff + l, size - l, GRND_RANDOM);

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
