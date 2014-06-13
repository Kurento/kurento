#!/bin/sh

xvfb-run java -jar ${remoteSeleniumJar} -port ${remotePort} -role node -hub http://${hubIp}:${hubPort}/grid/register -browser browserName=${browser},maxInstances=${maxInstances} -Dwebdriver.chrome.driver=${remoteChromeDriver} -timeout 0 &
echo $! > ${remoteFolder}/${pidFile}
