# Wireless Mic — میکروفون بی‌سیم اندروید

این یک پروژه کامل Android Studio (Kotlin + Native C++) است که میکروفون گوشی را می‌گیرد،
پردازش (حذف نویز، AGC، Gain دستی، Bass Boost، Loudness) می‌کند و از طریق مسیر
Bluetooth SCO با کمترین تأخیر ممکن به یک اسپیکر/هدست بلوتوث ارسال می‌کند.

## ساختار پروژه

```
WirelessMic/
├── app/
│   ├── build.gradle.kts          # وابستگی‌ها + تنظیمات NDK/CMake
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # مجوزها + سرویس Foreground
│   │   ├── java/com/example/wirelessmic/
│   │   │   ├── MainActivity.kt           # UI + مدیریت مجوزها + bind به سرویس
│   │   │   ├── AudioStreamViewModel.kt   # نگهداری تنظیمات افکت‌ها (MVVM)
│   │   │   ├── AudioStreamService.kt     # Foreground Service
│   │   │   ├── AudioEngine.kt            # حلقه Capture→Playback + افکت‌ها
│   │   │   ├── BluetoothRouteHelper.kt   # مسیریابی SCO
│   │   │   ├── NativeGain.kt             # پل JNI به کد Native
│   │   │   └── WirelessMicApp.kt         # ساخت Notification Channel
│   │   ├── cpp/
│   │   │   ├── CMakeLists.txt
│   │   │   └── native-gain.cpp   # اعمال سریع Gain روی بافر PCM16
│   │   └── res/                  # لایه‌ها، رنگ‌ها، آیکون
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/wrapper/gradle-wrapper.properties
```

## نحوه ساخت APK

1. **Android Studio** نسخه Koala (2024.1) یا جدیدتر نصب کنید (شامل SDK 34 و NDK 26.1.10909125).
2. پوشه `WirelessMic` (بعد از extract کردن zip) را با گزینه **Open** در Android Studio باز کنید.
3. اگر Android Studio فایل `gradlew`/`gradle-wrapper.jar` را نیافت، پیام بازسازی Wrapper را
   تأیید کنید (یا از منوی `File > Sync Project with Gradle Files` استفاده کنید) — این کار
   خودکار Gradle 8.7 را دانلود می‌کند.
4. صبر کنید تا Gradle Sync و دانلود NDK/CMake کامل شود (نیاز به اینترنت دارد).
5. از منوی `Build > Build Bundle(s) / APK(s) > Build APK(s)` استفاده کنید.
6. فایل خروجی در مسیر `app/build/outputs/apk/debug/app-debug.apk` قرار می‌گیرد.

برای نسخه امضا‌شده (Release APK) باید یک Keystore بسازید:
`Build > Generate Signed Bundle / APK`.

## نکات فنی مهم

- **minSdk = 31 (Android 12)**: به همین دلیل مجوزهای جدید بلوتوث
  (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`) به‌جای مجوزهای قدیمی استفاده می‌شوند.
- **مسیریابی صدا (به‌روزشده)**:
  - **ورودی همیشه از میکروفون خود گوشی است.** `AudioRecord.preferredDevice` صراحتاً
    روی `TYPE_BUILTIN_MIC` قفل شده، پس هیچ دستگاه بلوتوثی نمی‌تواند به‌جای میکروفون
    گوشی مورد استفاده قرار بگیرد.
  - **خروجی به‌صورت خودکار به هر دستگاه بلوتوثی متصل** (هدست، اسپیکر، هرچی) می‌رود.
    این کار با `AudioDeviceCallback` انجام می‌شود: به‌محض این‌که یک دستگاه صوتی بلوتوث
    در تنظیمات اندروید Pair/Connect بشه، اپ خودش تشخیص می‌ده و صدا رو به سمتش می‌فرسته
    — نیازی به دکمه «اتصال» دستی نیست؛ دکمه فعلی فقط یک بررسی فوری/دستی اضافه است.
  - پروفایل **A2DP** (همون پروفایل پخش موزیک با کیفیت کامل) در اولویت است؛ اگر دستگاه
    فقط SCO پشتیبانی کند (هدست‌های قدیمی تلفن)، به‌صورت Fallback از SCO استفاده می‌شود.
  - **چرا نه SCO به‌صورت پیش‌فرض؟** فعال‌کردن SCO هم‌زمان میکروفون *و* اسپیکر رو به
    دستگاه بلوتوث می‌فرسته (مثل تماس تلفنی) — این دقیقاً برعکس چیزیه که خواسته شده بود
    (میکروفون گوشی + خروجی بلوتوث). SCO هم‌چنین کیفیت پایین‌تری (نوار باریک، ~8kHz) دارد.
  - پیر (Pair) اولیه بلوتوث باید از تنظیمات خود اندروید انجام شود؛ این یک محدودیت پلتفرمه
    و اپ‌های عادی (بدون امضای سیستمی) نمی‌توانند این هندشیک را از داخل اپ انجام دهند.
- **تقویت صدا (Gain) بالاتر از حد معمول**: اسلایدر Gain حالا تا **۸ برابر (۸x)** اجازه
  می‌دهد (`AudioEngine.MAX_MANUAL_GAIN`). برای این‌که بلندترشدن صدا باعث خش‌خش/بریدگی
  زشت نشه، یک **Soft Limiter** در کد Native (`native-gain.cpp`, تابع
  `applyGainWithLimiter`) اضافه شده: تا ۸۵٪ سقف صدا بدون تغییر رد می‌شه، بعد از اون با
  منحنی نرم (tanh) به سمت سقف اشباع می‌شه به‌جای بریدگی سخت.
- **AGC و حذف نویز به‌صورت پیش‌فرض خاموش هستند** (`EffectSettings.agc = false`,
  `noiseSuppression = false`) چون این‌ها دقیقاً همون رفتاری‌ان که باعث می‌شد صدا وقتی
  حرف می‌کشید خودش کم بشه. کاربر می‌تونه از سوییچ‌های UI دوباره روشنشون کنه.
- **منبع ضبط**: تلاش اول با `MediaRecorder.AudioSource.UNPROCESSED` انجام می‌شود (صدای
  کاملاً خام میکروفون، بدون پردازش خودکار سیستم‌عامل)؛ اگر دستگاه پشتیبانی نکند،
  به‌صورت خودکار روی `MIC` معمولی Fallback می‌شود.
- **تأخیر**: بافر AudioRecord/AudioTrack برابر ۲.۵ برابر `getMinBufferSize()` است.
  توجه: چون خروجی حالا از A2DP است (نه SCO)، تأخیر ذاتاً کمی بیشتر از قبل خواهد بود —
  این هزینه‌ی معمول برای گرفتن کیفیت کامل صداست. اگر صدا بریده‌بریده شنیده شد، این ضریب
  را در `AudioEngine.kt` (`recBufSize`/`trackBufSize`) بالا ببرید.
- **افکت‌ها**: `NoiseSuppressor`، `AutomaticGainControl` و `AcousticEchoCanceler` روی
  Session مربوط به `AudioRecord` نصب می‌شوند؛ `BassBoost`، `Equalizer` و
  `LoudnessEnhancer` روی Session مربوط به `AudioTrack`. پشتیبانی از این افکت‌ها به
  سازنده‌ی چیپ صوتی گوشی بستگی دارد و ممکن است روی برخی گوشی‌ها در دسترس نباشد (کد این
  حالت را با try/catch مدیریت می‌کند).

## محدودیت‌های شناخته‌شده / قدم‌های بعدی پیشنهادی

- اگر چند دستگاه بلوتوث هم‌زمان متصل باشند، اولین دستگاه A2DP که سیستم برمی‌گرداند
  انتخاب می‌شود؛ برای انتخاب دستی از بین چند دستگاه یک `AlertDialog` روی نتایج
  `audioManager.getDevices(GET_DEVICES_OUTPUTS)` اضافه کنید.
- سقف Gain (۸x) و آستانه Limiter (۸۵٪) در کد قابل تنظیم‌اند — اگر با گوشی/اسپیکر خاصی
  هنوز کم است یا برعکس خیلی تهاجمی به نظر می‌رسد، `MAX_MANUAL_GAIN` در `AudioEngine.kt`
  و `kThreshold` در `native-gain.cpp` را تغییر دهید.
