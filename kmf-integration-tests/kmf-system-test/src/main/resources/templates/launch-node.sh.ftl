#!/bin/sh

xvfb-run -s "-screen 0 1224x768x16" java -jar ${remoteSeleniumJar} -port ${remotePort} -role node -hub http://${hubIp}:${hubPort}/grid/register -browser browserName=${browser},maxInstances=${maxInstances} -Dwebdriver.chrome.driver=${remoteChromeDriver} -timeout 0 &
echo $! > ${tmpFolder}/${pidFile}
