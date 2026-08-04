# אזעקון

אפליקציית אנדרואיד להתרעות פיקוד העורף עם צליל אזעקה מותאם אישית.

**⚠️ אינה אפליקציה רשמית של פיקוד העורף.** אפליקציה עצמאית שאינה קשורה לפיקוד העורף או לצה"ל, ואינה תחליף למערכות ההתרעה הרשמיות.

## סטטוס נוכחי

בפיתוח לפי מפרט הפרויקט, בשלבים. **כל 7 השלבים הושלמו, והאפליקציה נבנית בהצלחה מקצה לקצה** ב-CI — בדיקות יחידה, APK debug, וגם APK+AAB **release מלא כולל R8/minification ו-Lint** (לא-חתומים כברירת מחדל) — עדיין **לא נבדקה על מכשיר אמיתי**, ר' "הערה חשובה" למטה.

| שלב | תיאור | סטטוס |
|---|---|---|
| 1 | שלד: Gradle, Hilt, ניווט, ערכת נושא RTL, `areas.json` | ✅ הושלם |
| 2 | שכבת רשת: polling, parsing, BOM, דה-דופליקציה, מיפוי אזורים, מצב Mock | ✅ הושלם |
| 3 | Foreground Service + שרידות + בריאות מערכת | ✅ הושלם |
| 4 | מנוע אודיו: MediaPlayer, TTS, רטט, בחירת צליל | ✅ הושלם |
| 5 | מסך התרעה + מכונת מצבים (PREWARNING→IMMEDIATE→ALL_CLEAR) | ✅ הושלם |
| 6 | הגדרות ואונבורדינג | ✅ הושלם |
| 7 | ליטוש, נגישות, תיעוד Play | ✅ הושלם |

## Stack

Kotlin, Jetpack Compose (Material 3), Hilt, Coroutines/Flow, DataStore, Room, OkHttp + kotlinx.serialization, media3, WorkManager. `minSdk 26`, `targetSdk`/`compileSdk 36`.

## בנייה והרצה

```bash
./gradlew assembleDebug
./gradlew installDebug
```

**מסלול מהיר לקבלת APK בלי Android Studio מקומי:** יש workflow ב-`.github/workflows/build.yml` שרץ אוטומטית בכל push ל-`main` (ו-workflow_dispatch ידני) — מריץ את כל בדיקות היחידה, בונה APK debug, ובונה גם APK+AAB release מלא (חתום אוטומטית אם הוגדרו CI secrets, אחרת **לא-חתום**). הכל זמין להורדה מטאב **Actions** בריפו: `azakon-debug-apk` ו-`azakon-release`.

### ✅ מצב אימות — CI ירוק מקצה לקצה (debug + release)

**סביבת הפיתוח שבה נכתב הקוד** (כאן) חסומה מ-`dl.google.com` ברמת מדיניות רשת — Android Gradle Plugin וה-SDK עצמו מתארחים שם, ולכן מעולם לא הצלחתי להריץ `./gradlew assembleDebug` על המודול המלא בסביבה הזו. זו עדיין המגבלה הקבועה של סביבת הכתיבה.

**אבל ב-CI (GitHub Actions, גישת רשת מלאה) הבנייה המלאה רצה בפועל ועברה בירוק על כל השלבים**, אחרי כמה סבבי תיקון-אמיתי-מ-CI:

- **3 שגיאות קומפילציה אמיתיות** בריצה הראשונה (`import androidx.compose.runtime.getValue` חסר בשני קבצים, `@OptIn(ExperimentalLayoutApi::class)` חסר ב-`DebugPanelScreen.kt`) — תוקנו.
- **שגיאת `lintVitalRelease` אמיתית**: `ShomerApplication` מיישם `Configuration.Provider` לצורך Hilt+WorkManager, אבל ה-Manifest לא הסיר במפורש את `WorkManagerInitializer` המובנה — Lint חסם את ה-release build עד שנוסף `<provider tools:node="merge">` עם `tools:node="remove"` על המטא-דאטה. תוקן.
- **שגיאת syntax ב-workflow YAML עצמו**: `if: ${{ secrets.KEYSTORE_BASE64 != '' }}` ברמת step הוא לא syntax חוקי (GitHub Actions לא מכיר `secrets` בהקשר `if:` של step) — זה גרם ל-2 ריצות CI להיכשל **מיידית בלי להריץ אף job** (0 jobs). תוקן: הסוד מועבר קודם ל-`env:` ברמת ה-job, וה-`if:` בודק `env.HAS_KEYSTORE_SECRET`.

לאחר כל התיקונים, ריצת CI מלאה (commit `746e710`) **עברה בירוק על כל 12 השלבים**: בדיקות יחידה, `assembleDebug`, **וגם `assembleRelease`+`bundleRelease` המלאים כולל R8/minification ו-`lintVitalRelease`**. המשמעות בפועל:

- **כל הקומפילציה, כולל Compose, אומתה** — לא רק שכבת הלוגיקה הטהורה.
- **כל גרף ה-Hilt אומת**: Dagger מוודא בזמן קומפילציה שכל תלות ניתנת לספק; הצלחת ה-build מוכיחה רטרואקטיבית שאין עוד באגים כמו החסר-constructor שתפסתי ידנית בשלב 3.
- **סכימת Room אומתה** (KSP מייצר ומוודא את ה-DAOs/Entities בזמן build).
- **מיזוג ה-Manifest אומת** — כולל ה-provider override שמונע את שגיאת ה-Lint.
- **R8/minification ב-release אומת** (`assembleRelease`/`bundleRelease` ב-CI) — זה בדיוק סוג הבעיה (keep rules חסרים) שגורמת ל-crash רק ב-release, אחרי שה-debug עבד מצוין.
- **`lintVitalRelease` (בדיקת ה-Lint שחוסמת release) עברה בהצלחה** — לא רק R8, גם ה-Lint pass הנפרד שרץ אחריו.
- קודם לכן אימתתי גם 61 בדיקות יחידה אמיתיות (לא רק "קריאה זהירה") על מודול Kotlin/JVM טהור זמני, לכל לוגיקת הליבה (`AlertSessionReducer`, `AlertClassifier`, parsing, dedup, אזורים) — זה עדיין תקף ורץ עכשיו גם דרך ה-Gradle module האמיתי ב-CI (`./gradlew test` כחלק מה-workflow).

**מה עדיין *לא* אומת** (ואי אפשר לאמת בלי מכשיר אמיתי / Android Studio עם emulator): התנהגות runtime בפועל — מסך התרעה שמופיע על מסך נעול, צליל/רטט/TTS בפועל, מעברי מצב חלקים, TalkBack/נגישות אמיתית, שני מצבי FSI, ועוד סעיפי הבדיקה שמפורטים ב-§11 של המפרט המקורי.

## מוכנות להעלאה ל-Play Store — מה נעשה כאן ומה עדיין דורש אותך

| נושא | סטטוס |
|---|---|
| הקוד מתקמפל ובונה (debug + release, כולל R8 ו-Lint) | ✅ מאומת ב-CI מקצה לקצה |
| בדיקות יחידה ללוגיקת הליבה | ✅ 61/61 עוברות |
| טופס PLAY_DECLARATIONS.md (specialUse, FSI, תיאור חנות, Data Safety) | ✅ טיוטת נוסח קיימת — **לא הוגשה** |
| **מנגנון חתימה** (build.gradle.kts + workflow) | ✅ מוכן ומחובר — קורא credentials מקובץ מקומי או מ-CI secrets |
| **ה-keystore בפועל + הסיסמאות** | ❌ **לא נוצר בכוונה** — זה שלך ליצור ולשמור; ר' למטה |
| מזהי AdMob אמיתיים (במקום מזהי הבדיקה של גוגל) | ✅ הוחלפו — App ID + Ad unit ID מהחשבון האמיתי |
| אייקון אפליקציה | ✅ הוחלף מ-placeholder לאייקון וקטורי מעוצב (ר' סעיף למטה) — לא עבר בדיקת עיצוב מקצועית |
| בדיקה על מכשיר/emulator אמיתי | ❌ לא בוצעה בשום שלב |
| חשבון Google Play Console + הגשה בפועל | ❌ לא בוצע |

### יצירת keystore וחתימת release

**חשוב לפני שמתחילים:** שמור את קובץ ה-keystore **מחוץ לריפו**, במקום גיבוי בטוח (מנג'ר סיסמאות, כספת ענן פרטית). איבוד שלו אחרי שהאפליקציה כבר עלתה לחנות = **אין אפשרות לעדכן אותה לעולם**, רק לפרסם אפליקציה חדשה עם applicationId אחר.

**שלב 1 — יצירת ה-keystore (פעם אחת):**
```bash
keytool -genkeypair -v -keystore azakon-release.jks -alias azakon \
  -keyalg RSA -keysize 2048 -validity 10000
```
יבקש ליצור סיסמה לקובץ (`storePassword`) וסיסמה למפתח (`keyPassword`, אפשר זהה) ופרטי זהות (שם, ארגון וכו' — לא קריטי, לא מוצג למשתמשים).

**שלב 2 — בנייה חתומה מקומית:**
```bash
cp keystore.properties.example keystore.properties
# ערוך את keystore.properties: נתיב מלא ל-.jks + שתי הסיסמאות מהשלב הקודם
./gradlew assembleRelease bundleRelease
```
`keystore.properties` כבר ב-`.gitignore` — לא ייכנס לריפו בטעות. `app/build.gradle.kts` בודק אם הקובץ קיים ומחתים אוטומטית אם כן; בלעדיו (המצב הנוכחי ב-CI) ה-build נשאר לא-חתום בדיוק כמו קודם.

**שלב 3 (אופציונלי) — חתימה אוטומטית ב-GitHub Actions:**
1. קודד את הקובץ: `base64 -i azakon-release.jks | pbcopy` (מק) או `base64 -w0 azakon-release.jks` (לינוקס).
2. ברפו: **Settings → Secrets and variables → Actions → New repository secret**, הוסף ארבעה: `KEYSTORE_BASE64` (התוכן המקודד), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. זהו — ה-workflow כבר בודק אם `KEYSTORE_BASE64` קיים ומחתים אוטומטית; בלי הסודות האלה הוא ממשיך לבנות unsigned בדיוק כמו היום, בלי לשבור כלום.

### אייקון האפליקציה

הוחלף ה-placeholder (פעמון פשוט) באייקון וקטורי מעוצב: תג התרעה עגול בצבע הענבר של האפליקציה (`#F5A623`, זהה ל-`AmberPrimary` בערכת הנושא) עם סימן קריאה כהה במרכז ושני קשתות "גלי קול" בצדדים — על רקע כהה תואם (`#0B0F14`, זהה ל-`BackgroundDark`). וקטור בלבד (`ic_launcher_foreground.xml`/`ic_launcher_background.xml`), לא PNG, כך שהוא חד בכל צפיפות מסך. **זה עדיין לא עיצוב מקצועי** (לא עבר בדיקת גרפיקאי/A-B), אבל הוא כבר לא placeholder גנרי — סמל אזעקה מזוהה, שונה במכוון מהסמל הרשמי של פיקוד העורף (§7.1.D).

תוך כדי כך תוקן גם **באג אמיתי**: שלוש נקודות ה-`setSmallIcon()` בהתראות (`ServiceNotifications.kt`, `AlertNotifications.kt`) השתמשו קודם ב-`ic_launcher_foreground` הרב-צבעוני — אבל אנדרואיד (API 21+) מרנדר אייקון small-icon של התראה כמסכת אלפא חד-צבעית בלבד (מתעלם מ-RGB בפועל), כך שאייקון עם כמה צורות אטומות היה מתמזג לכתם לבן אחיד בלי פרטים. נוסף `ic_notification.xml` — סילואטת פעמון פשוטה, מיועדת בדיוק לשימוש הזה — ושלוש נקודות ה-`setSmallIcon()` עודכנו להשתמש בו.

## החלטות ארכיטקטוניות וסטיות מהמפרט המקורי

- **שם ו-package**: הפרומפט המקורי לא קבע שם. נבחר השם **"אזעקון"** (לפי בקשת המשתמש), ו-`applicationId` = `com.shomerapp.alerts` — כדי לא לרמוז על שיוך רשמי לפיקוד העורף (§7.1.D של המפרט).
- **פרסומות (סטייה מכוונת מהמפרט)**: המפרט המקורי אסר במפורש הוספת AdMob/SDK צד ג' (§7.1.E), כדי לשמור על טופס Data Safety פשוט ועל 100% פרטיות מקומית. **לבקשת המשתמש הוספו פרסומות באנר (Google AdMob)** במסך הראשי, בהיסטוריה ובהגדרות. הוחלט **שלא** להציג פרסומות במסך ההתרעה (`AlertActivity`) או בשלבי אונבורדינג קריטיים (הרשאות, בדיקת צליל) — פרסומת שם עלולה לעכב אדם מלהיכנס לממ"ד. יש לעדכן בהתאם את טופס ה-Data Safety ב-Play Console (ר' `PLAY_DECLARATIONS.md`, ייכתב בשלב מאוחר): הוספת SDK חיצוני שאוסף Advertising ID.
  - **מזהי AdMob אמיתיים כבר משולבים**: App ID ב-`AndroidManifest.xml`, Ad Unit ID ב-`app/src/main/kotlin/.../ui/ads/BannerAdView.kt` — מהחשבון AdMob של המפתח, לא מזהי הבדיקה של גוגל יותר.
- **מיקום אוטומטי (סטייה מכוונת מהמפרט)**: המפרט המקורי לא כלל שום איסוף מיקום. **לבקשת המשתמש נוספה תכונה אופציונלית** (כבויה כברירת מחדל, בהגדרות → "מיקום אוטומטי"): כשמופעלת, `AlertForegroundService` בודק כל כ-20 דקות (`LocationTracker` + `LocationSettlementMatcher`) באיזה יישוב המשתמש נמצא (FusedLocationProviderClient + Geocoder מקומי — שם היישוב בלבד נשמר, לא הקואורדינטות, ולא נשלח לשום שרת), ומוסיף אותו ל-`SettlementRelevanceFilter` **בנוסף** לרשימה הידנית, לא במקומה (`AppPreferences.autoDetectedSettlement` נדרס בכל בדיקה — "עוקב" אחרי המשתמש בלי צבירת רשומות ישנות). דורש `ACCESS_BACKGROUND_LOCATION`, הרשאה עם בדיקת מדיניות מחמירה במיוחד ב-Play Console — ר' `PLAY_DECLARATIONS.md` §7 לנוסח ההצהרה. **באג אמיתי שנתפס על מכשיר בפועל ותוקן**: ניסיון "לתקן מראש" הצהיר את השירות גם כ-`location` foreground-service-type בנוסף ל-`specialUse` — אבל אנדרואיד דורש שהרשאת המיקום כבר תהיה מאושרת ברגע ש-`startForeground()` נקרא כשסוג `location` מוצהר, וכמעט אף משתמש לא מחזיק אותה (התכונה opt-in, לא חלק מהאונבורדינג) — האפליקציה קרסה מיד בסיום האונבורדינג לכל משתמש רגיל. הוחזר ל-`specialUse` בלבד; קריאות המיקום לא דורשות שהשירות יהיה מוצהר כ-`location` כדי לפעול, ר' `PLAY_DECLARATIONS.md` §7 לפירוט המלא.
- **Repo נפרד**: הריפו `emergency-system` המקורי הוא פרויקט Flask/Python לא קשור (מערכת ניהול חירום לרשות מקומית) — הפרויקט הזה נוצר כריפו GitHub נפרד (`red-alert-android`) לבקשת המשתמש.

## שלב 6 — הערות

- **סוגר פער אמיתי משלב 5**: עד עכשיו `AlertForegroundService` העביר **כל** התרעה נכנסת ל-`AlertSessionManager`, בלי לסנן לפי מה שהמשתמש בחר (כי לא היה מסך בחירה). `SettlementRelevanceFilter` החדש עושה את הסינון בפועל: משווה (מנורמל, דרך `AreaRepository.normalize`) בין יישובי ההתרעה לרשימת הבחירה מ-`AppPreferences.selectedSettlements`, ומצמצם את `PollOutcome.AlertUpdate` רק ליישובים הרלוונטיים. אם המשתמש עדיין לא בחר כלום (למשל הזרקת Debug Panel לפני סיום onboarding) — ברירת המחדל הבטוחה היא **לא לסנן כלום**, לא להשתיק הכול.
- **"סימולציית התרעה מלאה" / הזרקת Debug Panel רצות דרך ה-pipeline האמיתי**, לא UI מדומה בנפרד: `AlertSimulator` מפעיל את מצב ה-Mock מ-שלב 2 (`AlertFetcherSwitch`), מזריק JSON מזויף שנראה בדיוק כמו תגובת oref אמיתית, וה-poll הבא מעבד אותו דרך כל השכבות האמיתיות (parsing → classify → dedup → filter → session state → audio → notification). **פרט קריטי לבטיחות**: מצב ה-Mock מתבטל אוטומטית אחרי 10 שניות ללא תלות בשום UI פתוח — אם זה היה נשאר דלוק בטעות, האפליקציה הייתה **מפספסת התרעות אמיתיות** בזמן שהיא "חושבת" שהיא במצב תרגיל.
- **דגל `isDrill` עובר דרך כל השרשרת** (Event → Reducer → State → Room → מסך) ולא רק בשכבת ה-UI — כי המפרט (§8) דורש שתרגיל **לעולם** לא ייראה כמו התרעה אמיתית, וזה כולל שחזור אחרי קריסה. באנר לבן-על-שחור "⚠️ תרגיל — לא התרעה אמיתית" מוצג על כל מסכי ה-alert הרלוונטיים כש-`isDrill=true`.
- **אונבורדינג בן 9 השלבים** (`OnboardingScreen.kt`) — שלב 7 (הדרכת OEM) מדלג את עצמו אוטומטית אם `Build.MANUFACTURER` לא ברשימת היצרנים הבעייתיים הידועים (`OemGuidance`, נבדק ביחידה). מסכי ההרשאות (התראות, DND, FSI, סוללה) בודקים מצב אמיתי מול המערכת (`PermissionChecks`) ומתעדכנים אוטומטית כשחוזרים מההגדרות (`rememberPermissionState`, מבוסס lifecycle ON_RESUME — אין API אחר לזה).
- **תוקן תוך כדי סקירה עצמית**: השלבים המחייבים (3=התראות, 6=סוללה) התחילו עם `enabled = granted` על כפתור ה"המשך" — כלומר משתמש שדוחה הרשאה לצמיתות (Android "אל תשאל שוב") היה **נתקע לצמיתות** באונבורדינג בלי שום דרך להתקדם. תוקן: "המשך" נפתח גם אם המשתמש רק *ניסה* לבקש את ההרשאה, לא רק אם היא אושרה בפועל — "חובה לשאול", לא "חובה לכפות". לעומת זאת שלב הצליל (8) **כן** נשאר חסימה קשה על "המשך" (`immediateConfirmed && prewarningConfirmed`), כי בדיקת צליל היא לגמרי בשליטת האפליקציה ואין סיכון להיתקעות אמיתית.
- **מסך ההגדרות** בנוי עם ניווט-פנימי פשוט (state מקומי, לא route חדש ב-NavGraph) — תפריט עם קיצורים לאזורים/צלילים/אבחון, ופאנל הדיבוג הנסתר נחשף אחרי 5 לחיצות על שם האפליקציה (§10).
- **מה סומן ב-scope-trim מכוון** (ר' פירוט בקוד `DebugPanelScreen.kt`): הדמיית ניתוק רשת/תגובה פגומה, לוג בקשות בזמן אמת, ומצב Replay מקובץ JSON — **לא מומשו**. הזרקת התרעה בודדת (הליבה של §10) כן מומשה במלואה ורצה דרך ה-pipeline האמיתי. אלה שלושת הפריטים הבולטים ביותר שנשארו לא-מיושמים מהמפרט המקורי כולו.
- **מסך אבחון הרשאות** (`DiagnosticsScreen.kt`, §7.1.H) מדווח בכנות מה דלוק/כבוי ומה נשבר בלעדיו — אף פעם לא חוסם.

## שלב 5 — הערות

- **מפתח קורלציה קריטי: שם היישוב, לא ה-`id` של פיקוד העורף.** PREWARNING, IMMEDIATE ו-ALL_CLEAR לאותו אירוע אמיתי הם שלוש הודעות **נפרדות** עם שלושה `id` שונים לגמרי — הדבר היחיד שמקשר ביניהן הוא חפיפה ברשימת היישובים. `AlertSessionReducer` (§4.1) בנוי סביב זה מהיסוד.
- **הפרדה בין לוגיקה טהורה לתופעות לוואי**: `AlertSessionReducer` הוא `object` טהור (state, event) → state חדש, בלי Android/Room/coroutines — נבדק ביחידה מול **כל** מקרי הקצה שמפורטים ב-§10 ("מחזור חיי האירוע"). `AlertSessionManager` הוא ה-singleton עם ה-side effects בפועל: תזמון שני הטיימרים שה-reducer לא יכול לתזמן בעצמו (ספירת duration של IMMEDIATE, פקיעת 5 דקות של PREWARNING), הפעלת `AlarmAudioEngine`, הצגת ה-Full-Screen notification, ושמירה/שחזור מ-Room.
- **`AlertActivity` לא קוראת שום Intent extra.** זה מה שהופך את "המעבר החלק" ב-§4.1 לטריוויאלי: ה-Activity רק מציגה מה שה-`AlertSessionManager` (singleton משותף) כבר חושב שהמצב הוא; `onNewIntent` לא צריך לעשות כלום מעבר לכך שה-Activity חוזרת לחזית (`singleTop`). ⚠️ **חשוב: זה שונה ממה שהוצג לך לאישור בשלב 1** — שם המנגנון תואר כ"מעביר Intent חדש", אבל בפועל אין צורך בזה כלל כי המצב חי ב-Hilt singleton ולא ב-Intent; זו החלטה ארכיטקטונית טובה יותר מהתיאור המקורי, לא סטייה בעייתית ממנו.
- **מסך ההתרעה מכיל את כל חמשת המצבים** (`AlertScreen.kt`): Prewarning (ענבר, טיימר עולה), PrewarningExpired (מתפוגג אוטומטית אחרי 6 שניות), Immediate (אדום, טבעת ספירה לאחור מתמלאת, כפתור "אני במרחב המוגן" שרק עוצר צליל ולא סוגר טיימר/רשימה), WaitingForAllClear (**לא ירוק** — בדיוק לפי §4.1), Cleared (ירוק, נעלם אוטומטית אחרי 15 שניות או בלחיצה).
- **AdMob לא מופיע במסך ההתרעה** — כפי שסוכם.
- **Room**: טבלה אחת ל"מצב האירוע הנוכחי" (שורה יחידה, נדרסת/נמחקת בכל מעבר — שחזור בקריסה) וטבלה שנייה להיסטוריה (נכתבת בסיום כל session, מוצגת עכשיו במסך ההיסטוריה האמיתי — הוחלף מ-placeholder).
- **Full-Screen Intent (§7.1.A)**: `AlertNotifications` בונה ערוץ **שקט לגמרי** (`setSound(null,null)`, `enableVibration(false)`) — כל תפקידו להפעיל FSI/heads-up; הצליל האמיתי רץ עצמאית לגמרי דרך `AlarmAudioEngine`. המשמעות: אם המשתמש לא אישר את הרשאת ה-FSI (או שהאפליקציה לא אושרה אוטומטית ב-API 34+), המערכת פשוט מורידה את זה ל-heads-up רגיל — **הצליל וההכרזה הקוליים ימשיכו לעבוד במלואם בכל מקרה**, בלי שום שינוי קוד נדרש כאן. מסך הבקשה בפועל (`canUseFullScreenIntent`) הוא שלב 6.
- **מה עוד לא קיים**: שום סינון של "האם ההתרעה הזו רלוונטית ליישובים שבחרתי" — כרגע `AlertForegroundService` מעביר **כל** התרעה שמגיעה ישר ל-`AlertSessionManager` (מסומן ב-TODO), כי מסך בחירת האזורים עדיין לא קיים. זה מגיע בשלב 6.
- **פישוט ידוע**: שחזור מ-Room אחרי קריסה (`restoreFromDatabase`) משחזר את המצב הוויזואלי (טיימר, רשימת יישובים) אבל **לא** מפעיל מחדש צליל/רטט/הכרזה — אלה כבר נעצרו כשהתהליך מת, וההנחה היא שעדיף שקט על פני הכרזה חוזרת מבלבלת. שווי שיקול מחדש בליטוש (שלב 7) אם בדיקה אמיתית מראה שזה בעייתי בפועל.

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

ראו `app/src/main/kotlin/com/shomerapp/alerts/` — חלוקה לפי `data/`, `domain/`, `service/`, `audio/`, `receiver/`, `work/`, `ui/` (theme, navigation, main, history, settings, alert, onboarding, debug, ads).

## שלב 7 — הערות

- **אנימציה**: `AlertScreen` עטוף ב-`Crossfade` בין שלבי ההתרעה (Prewarning/Immediate/WaitingForAllClear/Cleared), עם מפתח לפי **סוג המצב** ולא שוויון מלא — כדי שרשימת יישובים גדלה או טיק שנייתי בטיימר לא יגרמו לפייד מיותר, רק מעבר אמיתי בין שלבים.
- **נגישות**: נבדקו ידנית כל שימושי `Icon(...)` בקוד — היחיד (סרגל הניווט התחתון) כבר משתמש ב-`contentDescription = null` בכוונה, כי לכל אייקון יש `label` טקסטואלי צמוד ש-TalkBack קורא ממילא (הימנעות מהכרזה כפולה). לא נמצאו מקומות שמסתמכים על צבע בלבד להעברת מידע (סטטוס/ספירה לאחור תמיד מלווים בטקסט). **לא נבדק בפועל** עם TalkBack/ניגודיות/הגדלת גופן על מכשיר — נדרשת בדיקה ידנית לפני release, כפי שהמפרט דורש (§11 בדיקות תאימות).
- **`PLAY_DECLARATIONS.md` נוסף** — טיוטת נוסח הצדקת `specialUse`, תסריט סרטון הדגמה, הצהרת FSI, תיאור חנות עם הדיסקליימר בפתיחה, עדכון Data Safety בעקבות הוספת AdMob, והצדקת תדירות הרשת. **טיוטה בלבד, לא הוגשה בפועל**.
- **מה עדיין לא קיים בכלל** (מעבר לפערי scope-trim שכבר תועדו בשלבים 4/6): בדיקות אינסטרומנטציה (Espresso/Compose UI tests) על מכשיר אמיתי, בדיקת Android 14/15/16 בפועל, בדיקת שני מצבי FSI (מאושר/דחוי) בפועל, ואייקון אפליקציה סופי (הנוכחי הוא placeholder פשוט — פעמון על רקע כהה, לא עוצב מקצועית).
