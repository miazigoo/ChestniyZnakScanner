# ChestniyZnakScanner

Android MVP для сканирования Data Matrix Честного знака с локальной проверкой по базе.

Что уже реализовано:
- сканирование камерой через CameraX + ML Kit с фильтром только на `Data Matrix`;
- локальная Room-база с seed-данными из `assets`;
- доменный парсер Честного знака, перенесенный из backend-логики;
- проверка статусов `OK`, `NOT_FOUND`, `TAIL_MISMATCH`, `BAD_FORMAT`, `DUPLICATE_SCAN`;
- раздельные слои `feature / domain / data / core / di`.

## Структура

- `app/src/main/java/ru/devandprod/chestniyznak/feature`
  Экран, ViewModel и presentation state.
- `app/src/main/java/ru/devandprod/chestniyznak/domain`
  Модели, парсер, repository contract и use cases.
- `app/src/main/java/ru/devandprod/chestniyznak/data`
  Room, seed-loader и локальная реализация repository.
- `app/src/main/java/ru/devandprod/chestniyznak/core`
  Общие зависимости, тема и camera analyzer.
- `app/src/main/assets/seed/chestniy_znak_codes.json`
  Стартовая локальная база кодов для MVP.

## Как расширять дальше

- заменить `LocalChestniyZnakRepository` на hybrid/local+remote без переписывания UI;
- добавить экран истории сканов поверх уже существующей таблицы `scan_logs`;
- добавить синхронизацию с backend `/chestniy-znak/verify` и `/chestniy-znak/front/sync-1c/*`;
- вынести слои в отдельные Gradle-модули, если приложение вырастет в несколько feature.

## Запуск

1. Откройте проект в Android Studio.
2. Дождитесь sync Gradle.
3. Запустите `app` на устройстве или эмуляторе с камерой.

Для демо в проекте уже лежат несколько seed-кодов. При первом запуске они загружаются в локальную базу автоматически.
