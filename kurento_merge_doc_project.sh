#!/bin/bash

sed -e "s@mvn@mvn --settings $MAVEN_SETTINGS@g" < Makefile > Makefile.jenkins
make -f Makefile.jenkins clean langdoc || make -f Makefile.jenkins javadoc
make -f Makefile.jenkins html epub latexpdf dist
