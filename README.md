# BG AutoDiag — sorgenti prototipo Android

**Versione:** 0.1.0 — prototipo iniziale BLE  
**Application ID:** `it.bgautodiag.app`

## Cosa fa questa versione
- Mostra una schermata Android chiamata BG AutoDiag.
- Richiede i permessi Bluetooth necessari in base alla versione Android.
- Cerca dispositivi Bluetooth Low Energy (BLE) per 12 secondi.
- Mostra nome, indirizzo Bluetooth e intensità del segnale dei dispositivi rilevati.

## Cosa NON fa ancora
- Non apre una connessione GATT con l'adattatore.
- Non invia comandi ELM327/OBD-II.
- Non legge o cancella DTC/codici guasto.
- Non diagnostica centraline specifiche del costruttore.
- Non è un APK già compilato e non è stato provato su un'auto reale.

Il KONNWEI KW903 può essere pubblicizzato come BLE o con un comportamento specifico del produttore: questa app deve prima rilevarlo. Se il dispositivo usa Bluetooth Classic invece di BLE, questa versione potrebbe non mostrarlo.

## Struttura
- `settings.gradle.kts` — configurazione del progetto Gradle
- `build.gradle.kts` — versioni dei plugin Android e Kotlin
- `app/build.gradle.kts` — configurazione dell'app
- `app/src/main/AndroidManifest.xml` — activity e permessi
- `app/src/main/java/it/bgautodiag/app/MainActivity.kt` — interfaccia e scansione BLE

## Aprire il progetto
Importare la cartella `BGAutoDiag` in Android Studio su un computer con Android SDK e JDK compatibili. La prima compilazione richiede una connessione Internet per scaricare Gradle e i plugin. Questo archivio non include un APK né il Gradle Wrapper.

## Prossimo passo tecnico
1. Verificare se il KW903 appare nella scansione BLE.
2. Identificare il protocollo e il servizio/caratteristiche GATT dell'adattatore.
3. Implementare connessione e lettura/scrittura delle caratteristiche.
4. Solo dopo, inviare comandi OBD-II supportati (ad esempio inizializzazione ELM327 e richiesta codici memorizzati), gestendo risposte incomplete e timeout.

## Sicurezza e diagnosi
Questa app è un prototipo di test. Non usarla per prendere decisioni sulla sicurezza del veicolo. Non cancellare codici guasto né intervenire su sistemi di sicurezza senza procedure e strumenti appropriati.
