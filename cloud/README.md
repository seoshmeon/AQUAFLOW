# AQUAFLOW Cloud + Telegram

Серверный фундамент гибридной версии AQUAFLOW. Android остаётся основным безопасным
тренером и работает без сети; Telegram предоставляет личную статистику и добровольный
рейтинг.

## Уже реализовано

- Firebase-authenticated API для идемпотентной синхронизации подходов;
- одноразовый 8-символьный код привязки со сроком жизни 10 минут;
- Telegram webhook с проверкой секретного заголовка;
- команды `/progress`, `/record`, `/month`, `/leaderboard`, `/privacy`, `/unlink`;
- приватная история и серверная запись агрегатов;
- рейтинг только после явной команды `/leaderboard on`;
- защита от повторной загрузки одной записи и базовая валидация значений.

## Подготовка

1. Создать Firebase-проект, включить Authentication и Cloud Firestore.
2. Скопировать `.firebaserc.example` в `.firebaserc` и указать project ID.
3. Создать бота через `@BotFather`. Токен никому не отправлять и не добавлять в Git.
4. Установить Firebase CLI и зависимости:

```powershell
cd cloud\functions
npm install
npm run check
cd ..
firebase login
firebase use --add
firebase functions:secrets:set TELEGRAM_BOT_TOKEN
firebase functions:secrets:set TELEGRAM_WEBHOOK_SECRET
firebase deploy --only firestore:rules,firestore:indexes,functions
```

`TELEGRAM_WEBHOOK_SECRET` — случайная строка из букв, цифр, `_` и `-`. После деплоя
зарегистрировать выданный HTTPS URL функции `telegramWebhook` через метод Telegram
`setWebhook`, передав тот же `secret_token`.

## API для Android

Все запросы используют `Authorization: Bearer <Firebase ID token>`.

- `POST /link/start` → `{ "code": "ABCD2345", "expiresInSeconds": 600 }`;
- `POST /records/sync` → до 200 элементов в `{ "records": [...] }`.

`clientRecordId` должен быть стабильным UUID. Повторная отправка не дублирует статистику.

## Следующий пакет

- Firebase Anonymous Auth в Android с возможностью позднее привязать постоянный аккаунт;
- экран «Telegram и синхронизация» с явным согласием;
- очередь синхронизации Room + WorkManager и статус последней успешной отправки;
- Telegram Mini App с графиками и безопасной серверной проверкой `initData`;
- удаление облачной копии и аккаунта непосредственно из Android.
