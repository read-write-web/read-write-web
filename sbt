#!/bin/sh

dir=$(dirname $0)
cd "$dir"

if [ ! -f sbt-launch.jar ]; then
    wget http://typesafe.artifactoryonline.com/typesafe/ivy-releases/org.scala-tools.sbt/sbt-launch/0.10.1/sbt-launch.jar
fi

java -Xmx512M -jar -Dfile.encoding=UTF8 -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256m "$dir/sbt-launch.jar" "$@"

