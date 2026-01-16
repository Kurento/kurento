#!/bin/sh

xvfb-run -s "-screen 0 1224x768x16" java -cp ${classpath} org.openqa.selenium.grid.Main node --port ${remotePort} --hub http://${hubIp}:${hubPort} --max-sessions ${maxInstances} -Dwebdriver.chrome.driver=${remoteChromeDriver} &
echo $! > ${tmpFolder}/${pidFile}
