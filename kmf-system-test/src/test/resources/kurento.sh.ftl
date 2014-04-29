#!/bin/sh

export GST_PLUGIN_PATH=$GST_PLUGIN_PATH:${gstPlugins}
export GST_DEBUG=${debugOptions}
${serverCommand} -f ${workspace}kurento.conf
