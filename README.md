# SA_Case2_DecisionWorker

## Inhaltsverzeichnis

1. [Übersicht](#1-übersicht)
2. [Architektur](#2-architektur)
3. [Projektstruktur](#3-projektstruktur)
4. [Technologie-Stack](#4-technologie-stack)
5. [Datenmodell & DTOs](#5-datenmodell--dtos)
6. [Ablauf (Schritt für Schritt)](#6-ablauf-schritt-für-schritt)
7. [Fehlerbehandlung & Retry-Strategie](#7-fehlerbehandlung--retry-strategie)
8. [Konfiguration](#8-konfiguration)
9. [Starten der Anwendung](#9-starten-der-anwendung)

---

## 1. Übersicht

`SA_Case2_DecisionWorker` ist ein **Camunda External Task Worker**, der als Brücke zwischen der **Camunda BPM Process Engine** und einem externen **Entscheidungs-/Versand-API-Dienst** fungiert.

### Was macht die Anwendung?

Sobald eine Camunda-Prozessinstanz den Service-Task mit dem Topic `shippingDecision` erreicht, wird dieser Worker aktiv:

1. Er **pollt** kontinuierlich die Camunda Engine auf offene Tasks mit dem Topic `shippingDecision`.
2. Er liest die **Prozessvariablen** `weight` (Gewicht des Pakets) und `country` (Zielland) aus dem BPMN-Kontext.
3. Er sendet diese Daten per **HTTP POST** an einen externen Entscheidungsdienst (`/decision/make`).
4. Der externe Dienst liefert eine **Versandentscheidung** zurück (Versandmethode, Spediteur, Entscheidungstyp, Regel-ID).
5. Die Ergebnisse werden als **Prozessvariablen** zurück in die Camunda-Engine geschrieben, damit der Prozess weiterläuft.

---

## 2. Architektur

```
┌─────────────────────────────────────────────────────┐
│                  Camunda BPM Engine                 │
│         (http://192.168.111.3:8080/engine-rest)     │
│                                                     │
│   BPMN-Prozess → Service-Task (Topic:              │
│                  "shippingDecision")                │
│   Prozessvariablen: weight (Long), country (Enum)  │
└───────────────────────┬─────────────────────────────┘
                        │  Long Polling (HTTP)
                        ▼
┌─────────────────────────────────────────────────────┐
│            SA_Case2_DecisionWorker                  │
│                                                     │
│  ┌──────────────┐   ┌──────────────────────────┐   │
│  │DecisionWorker│──▶│DecisionExternalTaskHandler│   │
│  │  (Einstieg)  │   │      (ExternalTaskHandler)│   │
│  └──────────────┘   └────────────┬─────────────┘   │
│                                  │                  │
│                     ┌────────────▼─────────────┐   │
│                     │     DecisionService       │   │
│                     │  (Validierung + Mapping)  │   │
│                     └────────────┬─────────────┘   │
│                                  │                  │
│                     ┌────────────▼─────────────┐   │
│                     │    DecisionApiClient      │   │
│                     │  (JAX-RS / Jersey REST)   │   │
│                     └────────────┬─────────────┘   │
└──────────────────────────────────┼─────────────────┘
                                   │  HTTP POST (JSON)
                                   ▼
┌─────────────────────────────────────────────────────┐
│         Externer Entscheidungsdienst                │
│         (http://localhost:8081/decision/make)       │
│                                                     │
│  Request:  { country, weight }                      │
│  Response: { decisionType, shippingMethod,          │
│              carrier, ruleId }                      │
└─────────────────────────────────────────────────────┘
```

---

## 3. Projektstruktur

```
src/main/java/com/fhnw/sa_case2_decisionworker/
│
├── SaCase2DecisionWorkerApplication.java   # Spring Boot Einstiegspunkt
│
├── Worker/
│   ├── DecisionWorker.java                 # Camunda Client Setup & Subscription
│   └── DecisionExternalTaskHandler.java    # Task-Handler (Logik, Fehlerbehandlung)
│
├── Service/
│   └── DecisionService.java                # Fachliche Validierung & DTO-Mapping
│
├── RestClient/
│   └── DecisionApiClient.java              # HTTP-Client für externen Dienst
│
└── DTO/
    ├── ShippingDecisionArgs.java            # Request-DTO → an externen Dienst
    ├── DecisionMade.java                   # Response-DTO ← vom externen Dienst
    └── ShippingResult.java                 # Internes Ergebnis-DTO → an Camunda
```

---

## 4. Technologie-Stack

| Technologie                         | Version  | Verwendungszweck                              |
|--------------------------------------|----------|-----------------------------------------------|
| **Java**                             | 21       | Programmiersprache                            |
| **Spring Boot**                      | 4.0.3    | Anwendungsrahmen, Dependency Injection        |
| **Camunda External Task Client**     | 1.3.1    | Polling & Kommunikation mit der BPM-Engine   |
| **Jersey (JAX-RS Client)**           | 4.0.2    | HTTP-REST-Client für externen API-Aufruf     |
| **Jackson Databind**                 | (managed)| JSON-Serialisierung / -Deserialisierung       |
| **SLF4J Simple**                     | 1.6.1    | Logging                                       |
| **JAXB API**                         | 2.3.1    | XML-Binding (Camunda-Abhängigkeit)           |
| **Maven**                            | (Wrapper)| Build-Tool                                    |

---

## 5. Datenmodell & DTOs

### 5.1 `ShippingDecisionArgs` – Request an externen Dienst

Wird vom `DecisionService` befüllt und per REST an `POST /decision/make` gesendet.

| Feld      | Typ                        | Beschreibung                          |
|-----------|----------------------------|---------------------------------------|
| `country` | `DecisionMade.DestinationCountry` | Zielland des Pakets           |
| `weight`  | `Integer`                  | Gewicht des Pakets in Gramm/kg        |

### 5.2 `DecisionMade` – Response vom externen Dienst

Enthält die Versandentscheidung des externen Systems.

| Feld             | Typ              | Beschreibung                                        |
|------------------|------------------|-----------------------------------------------------|
| `decisionType`   | `DecisionType`   | `AUTOMATIC` oder `MANUAL`                          |
| `shippingMethod` | `ShippingMethod` | `SPECIAL`, `NORMAL` oder `AIR`                     |
| `carrier`        | `String`         | Name des beauftragten Spediteurs                   |
| `ruleId`         | `Long`           | ID der angewendeten Entscheidungsregel             |

### 5.3 `ShippingResult` – Internes Ergebnis-DTO

Wird intern vom `DecisionService` zurückgegeben und enthält dieselben Felder wie `DecisionMade`. Dient der sauberen Schichttrennung.

### 5.4 Enumerationen

**`DestinationCountry`** (Unterstützte Zielländer):

| Wert  | Land        |
|-------|-------------|
| `DE`  | Deutschland |
| `CH`  | Schweiz     |
| `ARG` | Argentinien |
| `JAP` | Japan       |
| `RUS` | Russland    |

**`ShippingMethod`** (Versandmethoden):

| Wert      | Beschreibung         |
|-----------|----------------------|
| `NORMAL`  | Standardversand      |
| `SPECIAL` | Sonderversand        |
| `AIR`     | Luftfracht           |

**`DecisionType`** (Entscheidungsart):

| Wert        | Beschreibung                            |
|-------------|------------------------------------------|
| `AUTOMATIC` | Versand wurde automatisch entschieden   |
| `MANUAL`    | Manueller Eingriff erforderlich         |

---

## 6. Ablauf (Schritt für Schritt)

### Schritt 1 – Anwendungsstart

`SaCase2DecisionWorkerApplication` startet den Spring Boot Kontext. `DecisionWorker.main()` wird separat (oder über Spring) aufgerufen und baut den Camunda-Client auf:

```
Camunda Engine URL : http://group6:p5TuHbjEadLeT6L@192.168.111.3:8080/engine-rest
Async Timeout      : 1000 ms
```

### Schritt 2 – Subscription auf Topic

Der Worker abonniert das Camunda-Topic **`shippingDecision`** mit einer Lock-Dauer von **1000 ms**:

```
client.subscribe("shippingDecision")
      .lockDuration(1000)
      .handler(new DecisionExternalTaskHandler(...))
      .open();
```

Solange kein passender Task vorhanden ist, wartet der Client per **Long Polling** auf neue Tasks.

### Schritt 3 – Task-Empfang & Variablen lesen

Sobald die Camunda Engine einen Task mit dem Topic `shippingDecision` erzeugt, ruft der `DecisionExternalTaskHandler` die `execute()`-Methode auf und liest die BPMN-Prozessvariablen:

```
weight  → Long    (Gewicht des Pakets, z. B. 5000)
country → String  (Zielland als Enum-Name, z. B. "DE")
```

Das `country`-String wird mit `DecisionMade.DestinationCountry.valueOf(...)` in einen Enum-Wert konvertiert.

### Schritt 4 – Fachliche Validierung (DecisionService)

`DecisionService.sendShippingOrder()` prüft die Eingaben:

- `country` darf nicht `null` oder leer sein → sonst `IllegalArgumentException`
- `weight` muss größer als `0` sein → sonst `IllegalArgumentException`

### Schritt 5 – Mapping und REST-Aufruf (DecisionApiClient)

Der `DecisionService` befüllt das `ShippingDecisionArgs`-Objekt und übergibt es an `DecisionApiClient.requestConsignment()`.

Der `DecisionApiClient` sendet einen **HTTP POST** an:
```
POST http://localhost:8081/decision/make
Content-Type: application/json

{
  "country": "DE",
  "weight": 5000
}
```

### Schritt 6 – Antwort verarbeiten

Der externe Dienst antwortet mit einem `DecisionMade`-JSON-Objekt, z. B.:

```json
{
  "decisionType": "AUTOMATIC",
  "shippingMethod": "NORMAL",
  "carrier": "DHL",
  "ruleId": 42
}
```

Der `DecisionService` mappt dieses Ergebnis in ein `ShippingResult`-Objekt und gibt es zurück.

### Schritt 7 – Prozessvariablen setzen & Task abschließen

Der `DecisionExternalTaskHandler` schreibt die Ergebnisse in eine Variable-Map und **schließt den External Task** ab:

```
decisionType   → z. B. "AUTOMATIC"
shippingMethod → z. B. "NORMAL"
carrier        → z. B. "DHL"
ruleId         → z. B. 42
```

Diese Variablen stehen dem weiteren BPMN-Prozess (z. B. User-Task „A38-Formular ergänzen") zur Verfügung.

---

## 7. Fehlerbehandlung & Retry-Strategie

| Fehlertyp                | Klasse                                           | Verhalten                                                  |
|--------------------------|--------------------------------------------------|------------------------------------------------------------|
| **Fachlicher Fehler**    | `IllegalArgumentException`                       | `handleFailure` mit **0 Retries** – kein Wiederholungsversuch |
| **HTTP-Fehler**          | `WebApplicationException` (z. B. 5xx, 4xx)      | `handleFailure` mit **3 Retries**, 60 Sekunden Wartezeit   |
| **Verbindungsfehler**    | `ProcessingException` (Timeout, DNS, etc.)       | `handleFailure` mit **3 Retries**, 60 Sekunden Wartezeit   |

### Retry-Logik im Detail

```
Erster Fehler  → retries = null → verbleibende Versuche = 3
Zweiter Fehler → retries = 3   → verbleibende Versuche = 2
Dritter Fehler → retries = 2   → verbleibende Versuche = 1
Vierter Fehler → retries = 1   → verbleibende Versuche = 0 → Task gilt als fehlgeschlagen
```

---

## 8. Konfiguration

### `application.properties`

```properties
spring.application.name=SA_Case2_DecisionWorker
```

### Endpunkte (hardcodiert in `DecisionWorker.java`)

| Eigenschaft              | Wert                                                                   |
|--------------------------|------------------------------------------------------------------------|
| Camunda Engine URL       | `http://group6:p5TuHbjEadLeT6L@192.168.111.3:8080/engine-rest`        |
| Async Response Timeout   | `1000 ms`                                                              |
| Lock Duration            | `1000 ms`                                                              |
| Topic                    | `shippingDecision`                                                     |
| Entscheidungs-API URL    | `http://localhost:8081/decision/make`                                  |

---

## 9. Starten der Anwendung

### Voraussetzungen

- Java 21 installiert
- Maven (oder Maven Wrapper `mvnw` verwenden)
- Camunda BPM Engine unter `http://192.168.111.3:8080` erreichbar
- Externer Entscheidungsdienst unter `http://localhost:8081/decision/make` erreichbar

### Build

```bash
./mvnw clean package
```

### Starten

```bash
./mvnw spring-boot:run
```

oder als JAR:

```bash
java -jar target/SA_Case2_DecisionWorker-0.0.1-SNAPSHOT.jar
```

> **Wichtig:** Da der eigentliche Worker-Einstiegspunkt in `DecisionWorker.main()` liegt, muss sichergestellt sein, dass diese Klasse beim Start aufgerufen wird (entweder direkt oder durch Spring Boot Integration).

---

