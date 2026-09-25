# Canlı Navigasyon - GitHub'dan APK üretimi

Bu proje Android Studio gerektirmeden GitHub Actions ile debug APK üretmek için hazırlanmıştır.

## 1. GitHub'a yükleme

ZIP'i bilgisayarınızda açın. İçindeki TÜM dosya ve klasörleri GitHub repository köküne yükleyin.

Önemli:
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/`
- `.github/workflows/build-apk.yml`

dosyaları da yüklenmelidir.

## 2. APK üretme

GitHub'da:
Actions -> Canli Navigasyon APK -> Run workflow

Çalışma tamamlanınca:
Actions -> ilgili çalışma -> Artifacts -> CanliNavigasyon-debug

ZIP'i indirin ve içindeki `app-debug.apk` dosyasını Android telefona kurun.

## 3. Telefonda

İlk açılışta konum ve bildirim izinlerini verin.

Arka plan navigasyonunu başlatmadan önce Android'in konum iznini uygulama için mümkün olan en yüksek seviyeye getirin. Bazı cihazlarda pil optimizasyonundan uygulamayı hariç tutmak gerekebilir.

## Not

Bu sürüm GPS, arka plan Foreground Service ve sesli tehlike uyarısının temel Android altyapısını içerir. Orijinal web uygulamasındaki OSRM map-matching, gelişmiş trafik/rota mantığı ve tüm mevcut tehlike algoritmaları ayrıca native servise taşınabilir.
