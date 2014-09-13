#!/bin/sh

export GST_PLUGIN_PATH=$GST_PLUGIN_PATH:${gstPlugins}
export GST_DEBUG=${debugOptions}
${serverCommand} --gst-debug-no-color -f ${workspace}kurento.conf.json &
echo $! > ${workspace}kms-pid
