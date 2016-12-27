#!/bin/sh

unittest=$1
if [ $# -ne 1 ]; then
    echo "Usage: debug.sh <unittest>"; exit 1
fi

echo "running unit test $unittest"

##mvn  -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE" -Dtest=$unittest test
mvn  -Dmaven.surefire.debug -Dtest=$unittest test
