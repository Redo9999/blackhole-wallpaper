# 🕳️ Personal Black Hole — Live Wallpaper

Um live wallpaper nativo pra tela inicial do Android: um pequeno buraco
negro que **cresce quanto mais tempo você passa sem interagir com o
celular** (ou seja, quanto mais tempo "trabalhando"), com sombra,
anel de fóton e disco de acreção com efeito de Doppler beaming — o lado
que "se aproxima" fica mais brilhante que o lado que "se afasta", como
num buraco negro girando de verdade.

Toque na tela e o buraco encolhe de volta — é o convite pra você dar
uma pausa.

> Inspirado numa ideia original de [@s13k_](https://x.com/s13k_), que
> fez um shader parecido pro terminal [Ghostty](https://ghostty.org/).
> Essa é uma implementação independente, escrita do zero em Kotlin
> nativo (Canvas + `WallpaperService`), como live wallpaper Android.

## Preview

O buraco tem três camadas visuais:
- **Sombra** (horizonte de eventos) — círculo preto sólido
- **Anel de fóton** — linha fina e brilhante colada na borda
- **Disco de acreção** — desenhado em duas metades (atrás/cima e
  frente/baixo da sombra), simulando a luz curvando ao redor do buraco

## Como compilar

1. Abra a pasta do projeto no **Android Studio** (Hedgehog ou mais
   recente) e deixe o Gradle sincronizar.
2. `Run > app` num dispositivo/emulador — isso só instala o app.
3. No celular: segure a tela inicial → **Papéis de parede** →
   **Personal Black Hole** (na lista de wallpapers ao vivo) → aplicar.

Ou via linha de comando, num ambiente com Android SDK configurado:

```bash
./gradlew assembleDebug
# APK em: app/build/outputs/apk/debug/app-debug.apk
```

## Ajustar o ritmo do ciclo

Em `BlackHoleWallpaperService.kt`:

```kotlin
private const val WORK_PERIOD_SEC = 55 * 60f   // tempo pra crescer até o máximo
private const val BREAK_SEC = 5 * 60f          // tempo de encolhimento no fim
```

## Limitações conhecidas

- **Home screen**: funciona normalmente.
- **Tela de bloqueio**: Android puro não permite apps de terceiros
  animarem a lock screen (é reservado ao sistema). Alguns fabricantes
  (ex: One UI da Samsung) têm extensões próprias pra isso, fora do
  escopo desse projeto.
- **iOS**: não é possível — wallpaper animado é exclusivo do sistema.

## Estrutura

```
BlackHoleWallpaper/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/wenson/blackholewallpaper/
│       │   └── BlackHoleWallpaperService.kt   ← lógica principal
│       └── res/
├── build.gradle.kts
└── settings.gradle.kts
```

## Licença

MIT — veja [LICENSE](LICENSE).

