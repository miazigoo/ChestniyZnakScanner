# ChestniyZnakScanner

Android-приложение для сканирования Data Matrix кодов Честного знака и проверки их наличия на backend и в локальном fallback-хранилище.

MVP-сценарий простой:
- пользователь наводит камеру на код;
- приложение считывает `Data Matrix`;
- код нормализуется и разбирается по правилам Честного знака;
- выполняется проверка по локальной Room-базе;
- на экране показывается результат:
  `OK` в зеленой плашке, если код найден;
  `NO` в красной плашке, если код не найден или формат некорректный.

## Что реализовано

- сканирование камерой через `CameraX`;
- декодирование `Data Matrix` через `ML Kit Barcode Scanning`;
- отдельный доменный парсер Честного знака;
- локальная база `Room` для кодов и истории сканов;
- вход через backend `accounts/login` с Django session cookie;
- вход через backend `accounts/login/token`;
- автологин тестовым токеном `testtokentablet`;
- серверная проверка через `POST /api/v2/chestniy-znak/verify`;
- получение статистики через `GET /api/v2/chestniy-znak/stats`;
- стартовая загрузка seed-данных из `assets`;
- проверка статусов:
  `OK`,
  `OK_GS_RESTORED`,
  `DUPLICATE_SCAN`,
  `BAD_FORMAT`,
  `NOT_FOUND`,
  `TAIL_MISMATCH`,
  `INTERNAL_ERROR`;
- многослойная архитектура без смешивания UI и бизнес-логики.

## Архитектура

Проект разделен по слоям:

- `feature`
  Экран, `ViewModel`, UI-state и presentation-логика.
- `domain`
  Бизнес-модели, парсер, контракты репозиториев и use case.
- `data`
  `Room`, DAO, entity, seed-loader и реализация репозитория.
- `core`
  Переиспользуемые части: тема, scanner analyzer, common dependencies.
- `di`
  Hilt-модули и wiring зависимостей.

Текущая структура пакетов:

```text
app/src/main/java/ru/devandprod/chestniyznak/
├── app
├── core
├── data
├── di
├── domain
└── feature
```

Это сделано с расчетом на дальнейшее развитие:
- гибридный репозиторий уже поддерживает `remote + local fallback`;
- можно вынести фичи в отдельные Gradle-модули;
- можно добавить синхронизацию с backend без переписывания UI.

## Технологии

- `Kotlin`
- `Jetpack Compose`
- `Hilt`
- `Room`
- `CameraX`
- `ML Kit`
- `Retrofit`
- `OkHttp`
- `Gradle Kotlin DSL`

## Как работает проверка

Проверка вынесена в доменный слой и не зависит от UI.

Основной поток:
1. Сканер получает строку из `Data Matrix`.
2. Парсер нормализует ввод:
   поддерживает `GS`,
   экранированные разделители,
   bracketed AI-представление.
3. Из кода извлекаются:
   `GTIN`,
   `serial`,
   `AI tail`.
4. Если есть активная серверная сессия, приложение отправляет код на backend.
5. Если сервер недоступен по сети, используется локальный fallback.
6. Если полного совпадения нет, дополнительно проверяется совпадение `GTIN + serial`.
7. Возвращается доменный результат, который UI отображает как `OK` или `NO`.

## Локальные данные

Стартовые seed-данные лежат здесь:

- `app/src/main/assets/seed/chestniy_znak_codes.json`

При первом запуске они автоматически загружаются в локальную базу.

Схема базы:
- `marking_codes` — коды Честного знака;
- `scan_logs` — история проверок и результатов сканирования.

## Backend integration

Сервер подключен по базе:

- `https://srv-dnp.argos.loc/api/v2/`

Используемые endpoint'ы:

- `POST /accounts/login/token`
- `GET /auth-check`
- `POST /accounts/logout`
- `POST /chestniy-znak/verify`
- `GET /chestniy-znak/stats`

Для тестового режима токен сейчас захардкожен в `BuildConfig.AUTH_TOKEN`.
Следующим шагом его можно заменить на значение, считанное из QR-кода.

В проекте учтена логика из backend-модуля `chestniy_znak`:
- нормализация scanner input;
- разбор `01 + GTIN + 21 + serial + AI tail`;
- статусы проверки;
- сценарий `TAIL_MISMATCH`, когда совпадает `GTIN + serial`, но отличается полный хвост.

Сессия хранится через cookie `dnp_session_id`.

Важно:
- backend использует self-signed certificate;
- в текущем окружении `/api/v2/health` отвечает, но стандартная TLS-проверка вне корпоративного trust store падает;
- в приложение добавлен `network_security_config`, который разрешает доверять `user` certificates для `srv-dnp.argos.loc`;
- для POST-запросов приложение автоматически прокидывает `X-CSRFToken` и `Referer` на основе cookies, полученных после token login;
- на тестовом Android-устройстве должен быть установлен пользовательский или системный сертификат, которому соответствует сервер.

В приложении отключение TLS-проверки не добавлялось.

## Сборка и запуск

Требования:
- `JDK 17`;
- Android SDK;
- Android Studio актуальной версии.

Запуск:

1. Открыть проект в Android Studio.
2. Дождаться Gradle Sync.
3. Запустить модуль `app` на устройстве или эмуляторе.
4. Разрешить доступ к камере.
5. Войти под учетной записью backend.

Для локальной проверки через консоль:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Тесты

Сейчас покрыта доменная логика парсера:
- разбор валидного кода с `GS`;
- восстановление разделителя при его отсутствии;
- поддержка bracketed AI ввода;
- ошибка при некорректном формате;
- сценарий короткого serial без хвоста.

Тесты лежат здесь:

- `app/src/test/java/ru/devandprod/chestniyznak/domain/parser/ChestniyZnakParserTest.kt`

## Что расширять дальше

- добавить импорт базы кодов с backend;
- добавить онлайн-верификацию через API;
- добавить экран истории сканов;
- добавить фильтрацию и поиск по результатам;
- добавить offline/online режимы;
- вынести `domain`, `data`, `feature` в отдельные модули;
- добавить instrumented UI tests.

## Git

В `.gitignore` уже исключены:
- локальные IDE-файлы;
- build-артефакты;
- APK/AAB;
- keystore-файлы;
- локальные Gradle и Kotlin cache директории.
