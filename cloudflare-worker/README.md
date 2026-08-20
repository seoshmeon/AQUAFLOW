# AQUAFLOW Cloudflare Worker

Бесплатный сервер Telegram-кабинета и синхронизации AQUAFLOW на Cloudflare Workers + D1.

## Компоненты

- `/health` — проверка доступности;
- `/telegram/webhook` — защищённый Telegram webhook;
- `/v1/auth/anonymous` — создание анонимного профиля установки;
- `/v1/link/start` — одноразовый код связи с Telegram;
- `/v1/link/status` — проверка Telegram-привязки;
- `/v1/records/sync` — идемпотентная синхронизация до 100 подходов;
- D1 — приватные записи, месячные агрегаты и добровольный рейтинг.
- `/deleteaccount ПОДТВЕРЖДАЮ` — полное удаление облачного профиля и связанных данных.

`pages-proxy/_worker.js` опубликован как `aquaflow-api.pages.dev` и служит основным
мобильным адресом. Он передаёт только `/health` и `/v1/*` в основной Worker. Приложение
автоматически использует исходный `workers.dev` как резервный адрес.

Токены задаются только через `wrangler secret put` и не хранятся в Git.

## Локальная проверка

```powershell
npm install
npm run check
```

## Развёртывание

```powershell
npx wrangler login
npx wrangler d1 create aquaflow
npx wrangler d1 migrations apply aquaflow --remote
npx wrangler secret put TELEGRAM_BOT_TOKEN
npx wrangler secret put TELEGRAM_WEBHOOK_SECRET
npx wrangler deploy
npx wrangler pages deploy pages-proxy --project-name aquaflow-api --branch main
```
