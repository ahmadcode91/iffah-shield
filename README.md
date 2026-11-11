<div dir="rtl">

# 🛡️ درع العفّة: حاجب المحتوى غير اللائق، مجاني لأندرويد

**درع العفّة** تطبيق مجاني ومفتوح المصدر يحجب المحتوى غير اللائق على أندرويد، بلا إعلانات ولا اشتراكات ولا إنترنت. يساعدك على **غض البصر** وحماية نفسك وأولادك، وهو مصمَّم بالعربية أولاً. صدقة جارية.

> تنبيه: هذه الصفحة مكتوبة عمداً بدون كلمات صريحة، حتى لا يحجبها التطبيق نفسه لمستخدميه.

## ⬇️ التحميل
- [أحدث نسخة من صفحة الإصدارات (Releases)](../../releases/latest)
- [موقع التطبيق](https://ahmadcode91.github.io/iffah-shield/)
- الحجم أقل من 100 كيلوبايت، ويعمل على أندرويد 7 فما فوق.

## ✨ المميزات
- **حجب المواقع والكلمات غير اللائقة** في كل التطبيقات والمتصفحات، بأكثر من 1000 كلمة ونطاق مدمج، بالعربية والإنجليزية والفرانكو و10 لغات أخرى. القائمة مخفية داخل التطبيق.
- **فهم ذكي للعربية:** الهمزات والتاء المربوطة والتشكيل والكتابة المموّهة بالأرقام، مع تجنّب الحجب الخاطئ للكلمات البريئة.
- **حجب الريلز والشورتس:** ريلز إنستغرام وفيسبوك وشورتس يوتيوب، مع بقاء الرسائل والمنشورات. ويمكن حجب ستوريات إنستغرام أيضاً.
- **كشف الصور بالذكاء الاصطناعي 👁 (جديد):** يفحص الشاشة نفسها ويحجب الصور والمقاطع غير اللائقة حتى لو لم يكن فيها أي كلمة، كالمولَّدة بالذكاء الاصطناعي. يعمل داخل الهاتف فقط، دون إنترنت.
- **أذكار الصباح والمساء** بعدّاد كالمسبحة، و**خطة تعافٍ** علمية وإيمانية، و**رحلة الحرية 40 يوماً**، ومحاسبة يومية.
- **قفل الليل**، و**البحث الآمن الإجباري**، و**وضع الأطفال**، وواجهة عربية وإنجليزية.
- **منع التصفح المتخفي** في Chrome وSamsung Internet وغيرهما.
- **حظر التطبيقات** بالكامل، أو وضع **حد يومي للوقت** لأي تطبيق.
- **حظر أي تطبيق جديد تلقائياً**.
- **الوضع الصارم:** لا يمكن إيقاف الحماية ولا حذف التطبيق ولا تغيير DNS الخاص أو الـVPN. الإيقاف يكون فقط بطلب ينتظر 24 ساعة.
- **حماية من الحذف** عبر صلاحية «مسؤول الجهاز».
- **زر «أشعر بالضعف الآن» 🚨:** تمرين تنفّس، وعدّاد صمود، وأسبابك، والاتصال بصديق، وقفل كل التطبيقات لفترة.
- **شريك المحاسبة:** رمز سري يكتبه صديق موثوق.
- **تنظيف الإشعارات** غير اللائقة.
- **عدّاد أيام النقاء** وإحصاءات وشاشة حجب تحفيزية.
- **دليل مدمج لفلتر DNS العائلي المجاني** على مستوى الهاتف كله، مع فرض البحث الآمن.

## 🔒 الخصوصية
- لا يملك التطبيق **إذن الإنترنت** إطلاقاً.
- لا يجمع ولا يحفظ أي نص أو صورة من شاشتك.
- [سياسة الخصوصية](PRIVACY.md)

## ⚙️ الإعداد: 5 خطوات تظهر داخل التطبيق
1. تشغيل الحماية من «إمكانية الوصول».
2. الوصول للإشعارات.
3. الحماية من الحذف.
4. فلتر DNS العائلي.
5. الوضع الصارم.

## 🛠️ البناء من المصدر
يُبنى بدون Gradle، عبر `build.sh` (يستخدم aapt وecj وdx وzipalign وapksigner).

## 🤲 صدقة جارية
إن نفعك التطبيق فادعُ لمن صنعه وانشره لغيرك؛ فالدال على الخير كفاعله.

</div>

---

# 🛡️ Iffah Shield: free content blocker for Android (Arabic-first)

Iffah Shield is a free, open-source blocker for inappropriate content on Android. No ads, no subscription, **no internet permission**.

**Features:**
- Keyword and site blocking in every app (500+ built-in terms, 12+ languages)
- Reels and Shorts blocking, incognito blocking
- App blocking and daily time limits
- Strict mode with a 24h cooldown, plus uninstall protection
- Panic button, accountability partner, notification filtering, streak counter
- Built-in guide for the free CleanBrowsing family DNS

*This page intentionally avoids explicit words so the app doesn't block its own page for its users.*

License: GPL-3.0

## Third-party components

- [TensorFlow Lite](https://www.tensorflow.org/lite) runtime (`libs/`), Apache-2.0.
- Image classifier model `assets/model.tflite`: an [open-source MobileNet v2 classifier](https://github.com/GantMan/nsfw_model) (224x224). See that repository for its license.
- Building needs an `android.jar` of API 30 or newer: `ANDROID_JAR=/path/to/android.jar VC=5 VN=1.2.0 ./build.sh`.
