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

/* The multi-channel controller protocol (MCCP) */

/* maximum transmission unit for control channels */
#define MCCP_MTU	48

/* Standard Op Codes */
#define MCCP_ERROR_RSP			0x00
#define MCCP_CREATE_CHANNEL_REQ		0x01
#define MCCP_CREATE_CHANNEL_RSP		0x02

/* MCAP Response codes */
#define MCCP_SUCCESS			0x00
#define MCCP_INVALID_OP_CODE		0x01
#define MCCP_INVALID_PARAM_VALUE	0x02
#define MCAP_UNSPECIFIED_ERROR		0x03
#define MCAP_REQUEST_NOT_SUPPORTED	0x04

/* Channel type */
#define MCCP_STREAM_TYPE_AUDIO		0x00
#define MCCP_STREAM_TYPE_VIDEO		0x01

/*
 * MCCP Request Packet Format
 */

typedef struct {
  guint8 op;
  guint8 ct;
  guint16 chanid;
} __attribute__ ((packed)) mccp_create_channel_req;

/*
 * MCCP Response Packet Format
 */

typedef struct {
  guint8 op;
  guint8 rc;
  guint16 chanid;
  guint8 data[0];
} __attribute__ ((packed)) mccp_rsp;

typedef enum {
  MCL_CONNECTED,
  MCL_PENDING,
  MCL_ACTIVE,
  MCL_IDLE
} MCLState;

typedef enum {
  MCL_ACCEPTOR,
  MCL_INITIATOR
} MCLRole;