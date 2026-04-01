#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
IDE_INPUT=${1:-${EMACSJUMP_IDE_HOME:-/Applications/GoLand.app}}

if [ -d "$IDE_INPUT/Contents/lib" ]; then
    IDE_HOME="$IDE_INPUT/Contents"
elif [ -d "$IDE_INPUT/lib" ]; then
    IDE_HOME="$IDE_INPUT"
else
    echo "Cannot find IDE libraries under: $IDE_INPUT" >&2
    exit 1
fi

PLUGIN_XML="$ROOT_DIR/resources/META-INF/plugin.xml"
PLUGIN_VERSION=$(sed -n 's#.*<version>\(.*\)</version>#\1#p' "$PLUGIN_XML" | head -n 1)
PLUGIN_NAME=emacsJump
BUILD_ROOT="$ROOT_DIR/build/legacy-213"
CLASS_DIR="$BUILD_ROOT/classes"
TEST_CLASS_DIR="$BUILD_ROOT/test-classes"
STAGE_DIR="$BUILD_ROOT/stage"
DIST_DIR="$ROOT_DIR/build/distributions"
MANUAL_DIR="$ROOT_DIR/build/manual-plugin"
LINE_SEPARATOR='
'

if [ -x "$IDE_HOME/jbr/Contents/Home/bin/javac" ]; then
    JAVAC="$IDE_HOME/jbr/Contents/Home/bin/javac"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
else
    JAVAC=javac
fi

if [ -x "$IDE_HOME/jbr/Contents/Home/bin/java" ]; then
    JAVA_BIN="$IDE_HOME/jbr/Contents/Home/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
else
    JAVA_BIN=java
fi

rm -rf "$BUILD_ROOT" "$MANUAL_DIR/$PLUGIN_NAME.jar" "$DIST_DIR/$PLUGIN_NAME-$PLUGIN_VERSION.zip" "$DIST_DIR/$PLUGIN_NAME"
mkdir -p "$CLASS_DIR" "$TEST_CLASS_DIR" "$STAGE_DIR/META-INF" "$STAGE_DIR/org/hunmr/options" "$DIST_DIR/$PLUGIN_NAME/lib" "$MANUAL_DIR"

IDE_CLASSPATH=$(find "$IDE_HOME/lib" "$IDE_HOME/plugins" -path '*/lib/*.jar' -print 2>/dev/null | paste -sd: -)
if [ -z "$IDE_CLASSPATH" ]; then
    echo "Cannot build classpath from IDE home: $IDE_HOME" >&2
    exit 1
fi

MAIN_SOURCES=$(find "$ROOT_DIR/src" -name '*.java' ! -path '*/test/*' | sort)
TEST_SOURCES=$(find "$ROOT_DIR/src/test/java" -name '*.java' | sort)
ARGUMENT_SOURCES=$(find "$ROOT_DIR/src/org/hunmr/argument" -name '*.java' ! -name 'ArgumentSelectorSupport.java' | sort)

"$JAVAC" --release 11 -cp "$IDE_CLASSPATH" -d "$CLASS_DIR" $MAIN_SOURCES
"$JAVAC" --release 11 -d "$TEST_CLASS_DIR" $ARGUMENT_SOURCES $TEST_SOURCES
"$JAVA_BIN" -cp "$TEST_CLASS_DIR" org.hunmr.argument.ArgumentParserTestRunner

cp -R "$CLASS_DIR/." "$STAGE_DIR/"
cp "$ROOT_DIR/resources/META-INF/plugin.xml" "$STAGE_DIR/META-INF/plugin.xml"
cp "$ROOT_DIR/resources/META-INF/plugin_windows.xml" "$STAGE_DIR/META-INF/plugin_windows.xml"
cp "$ROOT_DIR/src/org/hunmr/options/IdeaConfigurable.form" "$STAGE_DIR/org/hunmr/options/IdeaConfigurable.form"

(cd "$STAGE_DIR" && jar cf "$MANUAL_DIR/$PLUGIN_NAME.jar" .)
cp "$MANUAL_DIR/$PLUGIN_NAME.jar" "$DIST_DIR/$PLUGIN_NAME/lib/$PLUGIN_NAME.jar"
(cd "$DIST_DIR" && zip -qr "$PLUGIN_NAME-$PLUGIN_VERSION.zip" "$PLUGIN_NAME")

printf '%s\n' "Built $MANUAL_DIR/$PLUGIN_NAME.jar"
printf '%s\n' "Built $DIST_DIR/$PLUGIN_NAME-$PLUGIN_VERSION.zip"
