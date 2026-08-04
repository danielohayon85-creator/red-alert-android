# אזעקון

אפליקציית אנדרואיד להתרעות פיקוד העורף עם צליל אזעקה מותאם אישית.

**⚠️ אינה אפליקציה רשמית של פיקוד העורף.** אפליקציה עצמאית שאינה קשורה לפיקוד העורף או לצה"ל, ואינה תחליף למערכות ההתרעה הרשמיות.

## סטטוס נוכחי

בפיתוח לפי מפרט הפרויקט, בשלבים. **שלבים 1–4 הושלמו**, שלבים 5–7 עדיין לא.

| שלב | תיאור | סטטוס |
|---|---|---|
| 1 | שלד: Gradle, Hilt, ניווט, ערכת נושא RTL, `areas.json` | ✅ הושלם |
| 2 | שכבת רשת: polling, parsing, BOM, דה-דופליקציה, מיפוי אזורים, מצב Mock | ✅ הושלם |
| 3 | Foreground Service + שרידות + בריאות מערכת | ✅ הושלם |
| 4 | מנוע אודיו: MediaPlayer, TTS, רטט, בחירת צליל | ✅ הושלם |
| 5 | מסך התרעה + מכונת מצבים (PREWARNING→IMMEDIATE→ALL_CLEAR) | ⏳ טרם |
| 6 | הגדרות ואונבורדינג | ⏳ טרם |
| 7 | ליטוש, נגישות, תיעוד Play | ⏳ טרם |

## Stack

Kotlin, Jetpack Compose (Material 3), Hilt, Coroutines/Flow, DataStore, Room, OkHttp + kotlinx.serialization, media3, WorkManager. `minSdk 26`, `targetSdk`/`compileSdk 36`.

## בנייה והרצה

```bash
./gradlew assembleDebug
./gradlew installDebug
```

**⚠️ הערה חשובה על הסביבה שבה נכתב הקוד:** קובץ ה-wrapper (`gradlew`) נוצר ותוכן הקוד נכתב בסביבת פיתוח ללא גישת רשת ל-`dl.google.com` (חסום ברמת מדיניות הרשת של הסביבה) — כלומר **לא ניתן היה להריץ כאן `gradlew build` בפועל** ולוודא קומפילציה, כי גם Android Gradle Plugin וגם ה-Android SDK עצמו מתארחים שם. הקוד נכתב בקפידה לפי מוסכמות Kotlin/Compose/Hilt עדכניות, אבל **יש להריץ בנייה מלאה במכשיר/סביבה עם גישת אינטרנט רגילה (או Android Studio) לפני שמסתמכים עליו**, ולתקן קונפליקטים אם יתגלו (למשל גרסאות תלויות ב-`gradle/libs.versions.toml`).

## החלטות ארכיטקטוניות וסטיות מהמפרט המקורי

- **שם ו-package**: הפרומפט המקורי לא קבע שם. נבחר השם **"אזעקון"** (לפי בקשת המשתמש), ו-`applicationId` = `com.shomerapp.alerts` — כדי לא לרמוז על שיוך רשמי לפיקוד העורף (§7.1.D של המפרט).
- **פרסומות (סטייה מכוונת מהמפרט)**: המפרט המקורי אסר במפורש הוספת AdMob/SDK צד ג' (§7.1.E), כדי לשמור על טופס Data Safety פשוט ועל 100% פרטיות מקומית. **לבקשת המשתמש הוספו פרסומות באנר (Google AdMob)** במסך הראשי, בהיסטוריה ובהגדרות. הוחלט **שלא** להציג פרסומות במסך ההתרעה (`AlertActivity`) או בשלבי אונבורדינג קריטיים (הרשאות, בדיקת צליל) — פרסומת שם עלולה לעכב אדם מלהיכנס לממ"ד. יש לעדכן בהתאם את טופס ה-Data Safety ב-Play Console (ר' `PLAY_DECLARATIONS.md`, ייכתב בשלב מאוחר): הוספת SDK חיצוני שאוסף Advertising ID.
  - **`app/src/main/kotlin/.../ui/ads/BannerAdView.kt`** משתמש כרגע במזהי פרסומת בדיקה **פומביים של גוגל** (`ca-app-pub-3940256099942544/...`) — כלומר לא ייווצרו הכנסות אמיתיות. יש להחליף למזהי AdMob אמיתיים (App ID ב-`AndroidManifest.xml` + Ad Unit ID ב-`BannerAdView.kt`) מתוך חשבון AdMob של המפתח לפני release.
- **Repo נפרד**: הריפו `emergency-system` המקורי הוא פרויקט Flask/Python לא קשור (מערכת ניהול חירום לרשות מקומית) — הפרויקט הזה נוצר כריפו GitHub נפרד (`red-alert-android`) לבקשת המשתמש.

## שלב 4 — הערות

- **`AlarmAudioEngine`** הוא ה"ליבה" לפי §5 של המפרט: הכל מושמע ידנית (MediaPlayer + TextToSpeech + Vibrator), **לעולם לא** דרך `NotificationChannel.setSound()` — ה-API הזה "צורב" את הצליל לערוץ לצמיתות מ-API 26 ומעלה. הערוצים של שלב 3 (סטטוס/אובדן חיבור) נשארים שקטים לגמרי ונפרדים לחלוטין מהצליל עצמו.
- **IMMEDIATE**: מעלה את `STREAM_ALARM` ל-80%+ (`AlarmVolumeController`, עם שחזור אחרי קריסה — נבדק ב-`ShomerApplication.onCreate()` בכל עליית תהליך), `AudioFocusRequest` בלעדי, רטט קבוע, לולאה אינסופית עד `stop()` מפורש (§2.7 — לא נעצר לבד). TTS מוכרז **מעל** הצליל: הצליל כבר מתנגן, וה-TTS מנמיך אותו ל-30% (`onStart`) ומחזיר למלא (`onDone`).
- **PREWARNING**: צליל ורטט נפרדים לגמרי מ-IMMEDIATE, עוצמה 60% בלבד, fade-in של 4 שניות, **לא לולאה** — נעצר אוטומטית אחרי 15 שניות סה"כ (כולל ה-fade-in). מעבר PREWARNING→IMMEDIATE (§4.1) עובר דרך אותה `stopPlaybackKeepingVolume()` פנימית שלא משחזרת עוצמה — כך ה-baseline המקורי (מלפני האזעקה) נשמר גם כששני הצלילים רצים ברצף, ולא נדרס בעוצמת הביניים של ה-PREWARNING.
- **בחירת צליל ואימות**: `SoundResolver` הוא שער יחיד — צליל שהמשתמש בחר לא ייכנס לתוקף בלי `confirm*SoundTested()` (§2.5, "בדיקת חובה"), וגם אם אושר, `SoundUriValidator` בודק שה-URI עדיין נגיש (§5, קובץ שנמחק → נופל בחזרה לצליל ברירת המחדל של המערכת). ה-Picker עצמו (`ACTION_RINGTONE_PICKER` / SAF `OpenDocument` + `takePersistableUriPermission`) ומסך הבדיקה נבנים בשלב 6 — השכבה הזו רק שומרת/מאמתת/פותרת מה לנגן.
- **מה עוד לא קיים**: שום דבר עדיין לא **קורא** ל-`AlarmAudioEngine.playImmediate/playPrewarning` — החיבור בפועל בין `PollOutcome.AlertUpdate` (שלב 2/3) לבין הפעלת הצליל וההודעה הקולית המדויקת ("`<סוג ההתרעה>. <יישוב>. היכנסו למרחב המוגן.`") מגיע בשלב 5 יחד עם מסך ההתרעה ומכונת המצבים.
- **פישוט ידוע**: callbacks של MediaPlayer/TTS (ducking, prepared-listener) יכולים להגיע מ-thread שונה מזה שיצר את הנגן. זה עובד בפועל לקריאות הפשוטות כאן (הגדרת עוצמה, start), אבל אינו thread-confined באופן מלא — סומן כמועמד לחיזוק בשלב 7 אם בדיקה על מכשיר אמיתי תיתקל ב-race.
- אין כרגע קובץ צליל "נעים" מובנה ל-ALL_CLEAR (המפרט אוסר לארוז הקלטת סירנה אמיתית/CC0 לא מתועד) — `playAllClear()` פשוט משתיק; ניתן להוסיף אסט CC0 מתועד בהמשך.

## שלב 3 — הערות

- **`AlertForegroundService`** (`LifecycleService`) עוטף את `OrefPollingRepository` משלב 2: מאזין ל-`ConnectivityObserver.isConnected` עם `collectLatest` — כשהחיבור נופל, ה-collect הפנימי על `pollLoop()` מבוטל אוטומטית (הפולינג נעצר לגמרי); כשהחיבור חוזר, נפתח collect חדש שגם מאפס את ה-backoff לקצב הבסיס. זה בדיוק "עצור פולינג כשאין רשת, חדש מיד כשחוזרת" מ-§6.
- **התראה מתמדת** (`service_status`, IMPORTANCE_LOW) מתעדכנת בכל פעימת פולינג עם "עדכון אחרון: לפני X" חי. **התראת אובדן חיבור** (`connection_lost`, IMPORTANCE_HIGH) יורה **פעם אחת** בדיוק ברגע שחוצה 10 כשלים רצופים — לא בכל כשל בנפרד — לפי `ServiceHealthTracker.onPollFailure()` שמחזיר `true` רק על מעבר הסף.
- **בריאות המערכת מחוברת בפועל למסך הראשי**: `ServiceHealthTracker` הוא singleton משותף בין ה-Service ל-UI (אין binding, שני הצדדים פשוט מזריקים את אותו singleton דרך Hilt). `MainScreen` הוחלף מ-placeholder סטטי לכרטיס חי (🟢/🟡/🔴 + "עדכון אחרון" מתעדכן כל שנייה).
- **WakeLock**: `PARTIAL_WAKE_LOCK` עם timeout חסום (15 דקות) שמתחדש בכל פעימת פולינג — כל עוד הפולינג "נושם" הנעילה לא פוקעת, אבל אם הלולאה נתקעת/מתה היא פוקעת מעצמה במקום לדלוף לנצח (§6).
- **שרידות**: `BootReceiver` (מטפל ב-`BOOT_COMPLETED`/`MY_PACKAGE_REPLACED`) ו-`ServiceWatchdogWorker` (WorkManager periodic, 15 דק') **שניהם לא מפעילים את השירות אם `onboardingCompleted` עדיין `false`** ב-`AppPreferences` (DataStore חדש, מינימלי בכוונה — שלב 6 יוסיף עוד מפתחות). ה-watchdog לא בודק "האם השירות חי" עם flag שביר — הוא פשוט קורא שוב ל-`startForegroundService` בלי תנאי; זה idempotent ובטוח יותר.
- **`ShomerApplication` מיישם `Configuration.Provider`** כדי ש-WorkManager ישתמש ב-`HiltWorkerFactory` (נדרש כדי ש-`ServiceWatchdogWorker` יוכל להזריק `AppPreferences`), ומתזמן את ה-watchdog ב-`onCreate()` (idempotent דרך `ExistingPeriodicWorkPolicy.KEEP`).
- **מה עוד לא קיים**: שום UI לא מפעיל את השירות בפועל עדיין (אין onboarding שמסמן `onboardingCompleted=true`, ואין בקשת POST_NOTIFICATIONS בזמן ריצה) — זה מכוון, ומגיע בשלב 6. גם הטיפול בהתרעה שהתקבלה בפועל (`PollOutcome.AlertUpdate`) עדיין רק מעדכן בריאות ולא מפעיל צליל/מסך — מסומן ב-TODO בקוד, שלבים 4–5.
- באג אמיתי מ-שלב 2 שתוקן כאן: ל-`AlertDeduplicator` לא היה `@Inject constructor` — Hilt לא היה מצליח לבנות את `OrefPollingRepository` שתלוי בו. תוקן והוסף `@Singleton`.

## שלב 2 — הערות

- `OrefPollingRepository.pollOnce()` הוא ה-pipeline המלא: fetch → הסרת BOM/parse → דה-דופליקציה (`AlertDeduplicator`) → סיווג (`AlertClassifier`) → חילוץ `duration`. `pollLoop()` עוטף את זה בלולאה עם backoff אקספוננציאלי (2s→4s→8s→...→30s), שמתאפסת לקצב הבסיס בהצלחה הראשונה אחרי כשל — בדיוק לפי §6. חיבור ל-`ConnectivityManager` (עצירה/חידוש לפי מצב רשת), ההתראה המתמדת, וה-watchdog של WorkManager הם באחריות ה-Foreground Service בשלב 3, לא כאן.
- **דה-דופליקציה היא כרגע In-memory בלבד** (`AlertDeduplicator`) — לא שורדת קריסה/הריגת תהליך. שחזור מ-Room אחרי קריסה באמצע אירוע (§10, "ריסטארט של הטלפון באמצע התרעה פעילה") ייפתר בשלב 5 יחד עם מכונת המצבים של האירוע.
- **`AlertClassifier` ו-`AreaRepository` הם pure Kotlin** בכוונה (לא תלויים ב-Context/AssetManager) — טעינת הקבצים מ-`assets/` קורית ב-`di/NetworkModule.kt` בלבד, כדי שאפשר יהיה לבדוק את שתי המחלקות ביחידות בדיקה רגילות של JVM בלי Robolectric.
- **מצב Mock** (§10): `AlertFetcherSwitch` הוא ה-`AlertFetcher` היחיד שמחובר ב-Hilt; הוא מפנה ל-`OrefAlertFetcher` האמיתי או ל-`MockAlertFetcher` לפי flag שניתן להחליף בזמן ריצה. פאנל הדיבוג (שלב 6) פשוט יזריק Hilt ויקרא ל-`setMockMode()`/`mock.enqueue(...)`.
- **בדיקות יחידה**: `app/src/test/kotlin/...` — כולל בדיקה לכל שורה בטבלת הסיווג של §4 (`AlertClassifierTest`), בדיקות BOM/פענוח, `data` כמערך מול מחרוזת מופרדת בפסיקים, דה-דופליקציה, ומקרה "אותה התרעה שמתפשטת לעוד ערים". **לא הורצו בפועל** בסביבה הזו מאותה סיבה שמנועה גם `assembleDebug` (ר' למעלה) — מודול ה-Android דורש AGP+SDK גם כדי להריץ JVM unit tests רגילים. יש להריץ `./gradlew test` אצלך כדי לוודא שהן עוברות.

## מבנה הפרויקט

ראו `app/src/main/kotlin/com/shomerapp/alerts/` — חלוקה לפי `data/` (יתמלא בשלב 2), `domain/`, `service/`, `audio/` (שלב 4), `receiver/`, `ui/` (theme, navigation, main, history, settings, alert, onboarding, ads, debug).

מחלקות `AlertActivity`, `AlertForegroundService`, `BootReceiver` קיימות כרגע כ-stubs ריקים — רק כדי שה-manifest יתקמפל; המימוש המלא שלהן מגיע בשלבים 3 ו-5.
