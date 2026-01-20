
# Rodovia Recorder — MVP

Aplicativo Android para **gravar vídeo** de viagem rodoviária enquanto exibe e registra **BR / km + metros** e **velocidade** em tempo real. Gera **SRT** (legendas) e **CSV** de telemetria.

> **Status:** MVP para testes em campo. Usa uma polilinha de exemplo da **BR-116 (região de Roseira/SP)** apenas para validação. Para produção, substitua a geometria por dados oficiais e calibrados por marco quilométrico.

## Funcionalidades
- Pré-visualização da câmera (CameraX) e gravação em **MP4 + áudio**.
- HUD com **BR-116**, `km+metros`, velocidade e hora.
- **SRT** (1 linha/seg) com `BR  km X+YYY | V km/h`.
- **CSV** com timestamp, lat, lon, km, metros, velocidade.

## Build (Android Studio)
1. Abra o projeto no **Android Studio Hedgehog/Koala** (AGP 8.5+).
2. Aguarde o Gradle sincronizar as dependências.
3. Conecte um dispositivo Android (SDK 24+). Conceda permissões de **câmera, áudio e localização**.
4. Rode **Run › app** para instalar.
5. Para **gerar APK debug**: `Build › Build Bundle(s) / APK(s) › Build APK(s)`.

O APK ficará em `app/build/outputs/apk/debug/app-debug.apk`.

## Uso
- Toque **Gravar** para iniciar o MP4 e o log SRT/CSV.
- Toque **Parar** para finalizar.
- Arquivos ficam em `Android/data/com.rodovia.recorder/files/` no dispositivo.
  - Vídeo: `trip_YYYYMMDD_HHMMSS.mp4`
  - Logs: `logs/trip_YYYYMMDD_HHMMSS.(srt|csv)`

## Observações importantes
- O cálculo de `km+metros` usa **map-matching simples** na polilinha de exemplo. Em produção, substitua por geometria oficial do trecho e implemente **calibração por marco** (offset) e histerese.
- Se quiser queimar a legenda no vídeo, use **FFmpeg** no desktop/servidor com o SRT gerado.

## Próximos passos
- Importar geometria BR (GeoJSON/MBTiles) por rodovia/trecho.
- Ajuste de **km0** e sentido por trecho.
- Marcação de eventos (botões "lâmpada apagada", etc.) no CSV.
- Blur de placas/rostos (ML Kit) opcional.
