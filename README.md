# אזעקון

אפליקציית אנדרואיד להתרעות פיקוד העורף עם צליל אזעקה מותאם אישית.

**⚠️ אינה אפליקציה רשמית של פיקוד העורף.** אפליקציה עצמאית שאינה קשורה לפיקוד העורף או לצה"ל, ואינה תחליף למערכות ההתרעה הרשמיות.

## סטטוס נוכחי

בפיתוח לפי מפרט הפרויקט, בשלבים. **שלבים 1–2 הושלמו**, שלבים 3–7 עדיין לא.

| שלב | תיאור | סטטוס |
|---|---|---|
| 1 | שלד: Gradle, Hilt, ניווט, ערכת נושא RTL, `areas.json` | ✅ הושלם |
| 2 | שכבת רשת: polling, parsing, BOM, דה-דופליקציה, מיפוי אזורים, מצב Mock | ✅ הושלם |
| 3 | Foreground Service + שרידות + בריאות מערכת | ⏳ טרם |
| 4 | מנוע אודיו: MediaPlayer, TTS, רטט, בחירת צליל | ⏳ טרם |
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

## שלב 2 — הערות

- `OrefPollingRepository.pollOnce()` הוא ה-pipeline המלא: fetch → הסרת BOM/parse → דה-דופליקציה (`AlertDeduplicator`) → סיווג (`AlertClassifier`) → חילוץ `duration`. `pollLoop()` עוטף את זה בלולאה עם backoff אקספוננציאלי (2s→4s→8s→...→30s), שמתאפסת לקצב הבסיס בהצלחה הראשונה אחרי כשל — בדיוק לפי §6. חיבור ל-`ConnectivityManager` (עצירה/חידוש לפי מצב רשת), ההתראה המתמדת, וה-watchdog של WorkManager הם באחריות ה-Foreground Service בשלב 3, לא כאן.
- **דה-דופליקציה היא כרגע In-memory בלבד** (`AlertDeduplicator`) — לא שורדת קריסה/הריגת תהליך. שחזור מ-Room אחרי קריסה באמצע אירוע (§10, "ריסטארט של הטלפון באמצע התרעה פעילה") ייפתר בשלב 5 יחד עם מכונת המצבים של האירוע.
- **`AlertClassifier` ו-`AreaRepository` הם pure Kotlin** בכוונה (לא תלויים ב-Context/AssetManager) — טעינת הקבצים מ-`assets/` קורית ב-`di/NetworkModule.kt` בלבד, כדי שאפשר יהיה לבדוק את שתי המחלקות ביחידות בדיקה רגילות של JVM בלי Robolectric.
- **מצב Mock** (§10): `AlertFetcherSwitch` הוא ה-`AlertFetcher` היחיד שמחובר ב-Hilt; הוא מפנה ל-`OrefAlertFetcher` האמיתי או ל-`MockAlertFetcher` לפי flag שניתן להחליף בזמן ריצה. פאנל הדיבוג (שלב 6) פשוט יזריק Hilt ויקרא ל-`setMockMode()`/`mock.enqueue(...)`.
- **בדיקות יחידה**: `app/src/test/kotlin/...` — כולל בדיקה לכל שורה בטבלת הסיווג של §4 (`AlertClassifierTest`), בדיקות BOM/פענוח, `data` כמערך מול מחרוזת מופרדת בפסיקים, דה-דופליקציה, ומקרה "אותה התרעה שמתפשטת לעוד ערים". **לא הורצו בפועל** בסביבה הזו מאותה סיבה שמנועה גם `assembleDebug` (ר' למעלה) — מודול ה-Android דורש AGP+SDK גם כדי להריץ JVM unit tests רגילים. יש להריץ `./gradlew test` אצלך כדי לוודא שהן עוברות.

## מבנה הפרויקט

ראו `app/src/main/kotlin/com/shomerapp/alerts/` — חלוקה לפי `data/` (יתמלא בשלב 2), `domain/`, `service/`, `audio/` (שלב 4), `receiver/`, `ui/` (theme, navigation, main, history, settings, alert, onboarding, ads, debug).

מחלקות `AlertActivity`, `AlertForegroundService`, `BootReceiver` קיימות כרגע כ-stubs ריקים — רק כדי שה-manifest יתקמפל; המימוש המלא שלהן מגיע בשלבים 3 ו-5.
