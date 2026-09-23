# Ant Media Android P2P

Bu proje Ant Media WebRTC P2P bağlantısına standart SDK `join` çağrısıyla katılır. Yerel kamera görüntüsü uygulamada render edilmez; yalnızca uzak eşin görüntüsü gösterilir.

## Çalıştırma

1. Projeyi Android Studio ile açın ve Gradle senkronizasyonunu tamamlayın.
2. Android cihazda uygulamayı çalıştırın ve kamera/mikrofon izinlerini verin.
3. Ant Media Server WebSocket adresini girin. TLS için örnek: `wss://SUNUCU:5443/WebRTCAppEE/websocket`.
4. Embedded SDK yayıncısıyla aynı stream kimliğini yazıp **Peer bağlantısına katıl** düğmesine basın.

SDK standart P2P davranışıyla çalışır; uygulamada yalnızca local renderer kullanılmaz.

> WebRTC oynatma özelliği Ant Media Server Enterprise Edition gerektirir.

Rehberdeki `2.8.0-SNAPSHOT` artık Sonatype deposunda bulunmadığı için Maven Central'daki güncel kararlı `2.17.1` sürümü kullanılmıştır.
