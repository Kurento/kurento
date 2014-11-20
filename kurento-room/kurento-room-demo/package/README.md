${project.name}
======================

The ${project.name} shows a demo application of Kurento Room with WebRTC.

Installation instructions
-------------------------

By running install.sh, the jar file containing the demo will be copied into
_/var/lib/kurento_, and the startup script will be put in _/etc/init.d/${project.artifactId}_. Once the demo is installed, it can be managed as a regular service with

```
sudo service ${project.artifactId} {start|stop|restart}
```

If you want to change the port, you can do so by editing the startup script, and setting the desired value in _APP\_PORT_.