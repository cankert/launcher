.PHONY: install run release uninstall clean check set-default

ANDROID_HOME ?= $(HOME)/Library/Android/sdk
ADB := $(ANDROID_HOME)/platform-tools/adb
PKG := de.dm.launcher.debug
RELEASE_PKG := de.dm.launcher

# Use Android Studio's bundled JBR (Java 21) – avoids issues with system Java 25.
export JAVA_HOME := /Applications/Android Studio.app/Contents/jbr/Contents/Home

install:
	./gradlew installDebug
	-$(ADB) shell cmd role add-role-holder android.app.role.HOME $(PKG)

set-default:
	$(ADB) shell cmd role add-role-holder android.app.role.HOME $(PKG)
	$(ADB) shell cmd role get-role-holders android.app.role.HOME

run: install
	$(ADB) shell monkey -p $(PKG) -c android.intent.category.LAUNCHER 1

release:
	./gradlew assembleRelease
	@echo "APK: $$(find app/build/outputs/apk/release -name '*.apk')"

uninstall:
	-$(ADB) uninstall $(PKG)
	-$(ADB) uninstall $(RELEASE_PKG)

clean:
	./gradlew clean

check:
	./gradlew lint
