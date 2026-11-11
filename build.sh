#!/bin/bash
# Build Iffah Shield (public edition) without Gradle: aapt + ecj + dx + zipalign + apksigner.
set -e
cd "$(dirname "$0")"
# Needs an android.jar of API 30+ (for AccessibilityService.takeScreenshot), e.g. platforms/android-33/android.jar.
AJ=${ANDROID_JAR:-/root/hisn-ai/android-33.jar}
TFL=libs/tensorflow-lite.jar:libs/tensorflow-lite-api.jar
T=${DX_DIR:-/root/hisn-tools/x/usr/share/java}
KS=${KEYSTORE:-/root/iffah-shield-keys/iffah-release.keystore}
PW=$(cat "${KEYPASS_FILE:-/root/iffah-shield-keys/password.txt}")
rm -rf out gen obj && mkdir -p out gen obj
aapt package -f -m -M AndroidManifest.xml -S res -A assets -0 tflite -I $AJ -J gen -F out/res.apk \
  --min-sdk-version 24 --target-sdk-version 35 --version-code ${VC:-1} --version-name ${VN:-1.0.0}
java -jar /usr/share/java/eclipse-jdt-core-compiler-batch.jar -8 -nowarn -cp $AJ:$TFL -d obj $(find src gen -name '*.java')
java -jar $T/com.android.dx.jar --dex --min-sdk-version=24 --output=out/classes.dex obj libs/tensorflow-lite.jar libs/tensorflow-lite-api.jar
cp out/res.apk out/unsigned.apk
mkdir -p out/lib && cp -r libs/arm64-v8a libs/armeabi-v7a out/lib/
(cd out && aapt add unsigned.apk classes.dex lib/arm64-v8a/libtensorflowlite_jni.so lib/armeabi-v7a/libtensorflowlite_jni.so >/dev/null)
zipalign -f 4 out/unsigned.apk out/aligned.apk
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass "$PW" -keypass "$PW" -alias iffah \
  -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Iffah Shield, O=Iffah Shield, C=SY" >/dev/null 2>&1
java -cp $T/apksigner.jar:/usr/share/java/apksig.jar com.android.apksigner.ApkSignerTool sign \
  --ks "$KS" --ks-pass pass:"$PW" --out out/IffahShield-${VN:-1.0.0}.apk out/aligned.apk
ls -la out/IffahShield-*.apk
