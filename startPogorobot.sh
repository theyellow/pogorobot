#!/bin/bash

if [ ! -z $1 ]
then
: # $1 was given
VERSION=$1
else
: # $1 was not given
VERSION='1.0.7-SNAPSHOT'
fi

screen -S telegram-bot -d -m java -Dext.properties.dir=file:$PWD/config -XX:CompressedClassSpaceSize=190m -XX:MetaspaceSize=256M -Xmx600m -jar pogorobot-$VERSION-exec.jar
