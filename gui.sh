#!/bin/bash

#Location of jar file for SigVis
JAR_FILE="target/sigvis-1.0.1-SNAPSHOT-jar-with-dependencies.jar"
LAUNCH_CLASS="com.owlplatform.sigvis.SignalVisualizer"

FLAG_GL="-Dsun.java2d.opengl=true"
GL_INDIRECT="Mesa GLX Indirect"

LAUNCH_FLAGS="-Xmx256m "


usage() {
  echo "Usage: `basename $0` [-h] WM_HOST WM_PORT \
REGION"
}

parseopts() {
  while getopts ":hs" optname 
    do
      case "$optname" in
        "h")
          usage
          exit 0
          ;;
        "?")
          echo "Unknown option $OPTARG"
          ;;
        ":")
          echo "Missing value for option $OPTARG"
          ;;
        *)
          echo "Unknown error has occurred"
          ;;
    esac
  done
  return $OPTIND
}

parseopts "$@"
argstart=$?
shift $(($argstart-1))

# OpenGL renderer string: Mesa DRI Intel(R) Ironlake Mobile
OPENGL_DRI=$(glxinfo 2>/dev/null | grep "OpenGL renderer string:" | awk \
  'BEGIN { FS = ":[\b\f\n\r\t\v ]+" }; { print $2 }')
echo $OPENGL_DRI

if [[ "x$OPENGL_DRI" != "x" && "$OPENGL_DRI" != "$GL_INDIRECT" ]]
then
  LAUNCH_FLAGS="$LAUNCH_FLAGS $FLAG_GL"
  echo "Using OpenGL driver \"$OPENGL_DRI\"."
else
  echo "OpenGL rendering not supported."
fi


#if [ $# -ne 2 ]
#then
#  usage
#  exit 1
#fi

#WM_HOST=$1
#REGION=$2

java -cp $JAR_FILE $LAUNCH_FLAGS $LAUNCH_CLASS $@
