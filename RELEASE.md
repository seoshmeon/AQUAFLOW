# Выпуск AQUAFLOW

Для обновлений Android требуется постоянно использовать один и тот же закрытый ключ. Сам ключ,
пароли и `keystore.properties` нельзя добавлять в Git.

1. Создайте release-keystore один раз и храните его минимум в двух защищённых местах.
2. Скопируйте `keystore.properties.example` в `keystore.properties` и заполните четыре значения.
3. Выполните проверки и соберите пакет:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:bundleRelease
```

Результат для Google Play находится в `app/build/outputs/bundle/release/`. Для прямой установки
на телефон можно собрать `:app:assembleRelease`; такой APK будет подписан только при наличии
локального `keystore.properties`.

Debug APK предназначен для тестирования и не должен публиковаться как официальный релиз.
