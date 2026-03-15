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
│   Prozessvariablen: weight (Long), country (String)│
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
│    (SA_Case2_DecisionApplication)                   │
│         (http://localhost:8081/decision/make)       │
│                                                     │
│  Request:  { destinationCountry, weight }           │
│  Response: { decisionType, shippingType,            │
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
    ├── DecisionMade.java                   # Response-DTO ← vom externen Dienst (inkl. Enums)
    └── ShippingResult.java                 # Internes Ergebnis-DTO → an Camunda
```

---

## 4. Technologie-Stack

| Technologie                         | Version  | Verwendungszweck                              |
|--------------------------------------|----------|-----------------------------------------------|
| **Java**                             | 25       | Programmiersprache                            |
| **Spring Boot**                      | 4.0.3    | Anwendungsrahmen, Dependency Injection        |
| **Camunda External Task Client**     | 1.3.1    | Polling & Kommunikation mit der BPM-Engine   |
| **Jersey (JAX-RS Client)**           | 4.0.2    | HTTP-REST-Client für externen API-Aufruf     |
| **Jackson Databind**                 | 2.20.2   | JSON-Serialisierung / -Deserialisierung       |
| **SLF4J Simple**                     | 1.6.1    | Logging                                       |
| **JAXB API**                         | 2.3.1    | XML-Binding (Camunda-Abhängigkeit)           |
| **Maven**                            | (Wrapper)| Build-Tool                                    |

---

## 5. Datenmodell & DTOs

### 5.1 `ShippingDecisionArgs` – Request an externen Dienst

Wird vom `DecisionService` befüllt und per REST an `POST /decision/make` gesendet.

| Feld                 | Typ                               | Beschreibung                   |
|----------------------|-----------------------------------|--------------------------------|
| `destinationCountry` | `DecisionMade.DestinationCountry` | Zielland des Pakets            |
| `weight`             | `Integer`                         | Gewicht des Pakets in Gramm/kg |


### 5.2 `DecisionMade` – Response vom externen Dienst

Enthält die Versandentscheidung des externen Systems. Diese Klasse dient als **reines Transport-DTO** für die JSON-Deserialisierung der HTTP-Antwort.
Sie hält ausserdem alle gemeinsam genutzten **Enum-Definitionen** (`DecisionType`, `ShippingType`, `DestinationCountry`).

| Feld             | Typ            | Beschreibung                                                                |
|------------------|----------------|-----------------------------------------------------------------------------|
| `decisionType`   | `DecisionType` | `AUTOMATIC` oder `MANUAL`                                                  |
| `shippingType`   | `ShippingType` | `SPECIAL`, `NORMAL` oder `AIR` – **kann `null` sein bei `MANUAL`**         |
| `carrier`        | `String`       | Name des beauftragten Spediteurs – **kann `null` sein bei `MANUAL`**       |
| `ruleId`         | `Long`         | ID der angewendeten Entscheidungsregel – **kann `null` sein bei `MANUAL`** |

> **Wichtig:** Ist `decisionType == MANUAL`, liefert der externe Dienst `shippingType`, `carrier` und `ruleId` als `null`. Diese Felder werden dann im nachgelagerten User-Task manuell befüllt.

### 5.3 `ShippingResult` – Internes Ergebnis-DTO

Wird intern vom `DecisionService` zurückgegeben und enthält dieselben Felder wie `DecisionMade`.

**Warum ein separates DTO?**
Auch wenn `ShippingResult` und `DecisionMade` strukturell gleich aussehen, erfüllen sie bewusst unterschiedliche Rollen im System:

- `DecisionMade` repräsentiert die **externe HTTP-Antwort** des Entscheidungsdienstes. Es ist eng an die JSON-Struktur der API gekoppelt und darf sich ändern, wenn sich die externe Schnittstelle ändert.
- `ShippingResult` ist das **interne Fachmodell**, das der `DecisionService` an den `DecisionExternalTaskHandler` zurückgibt. Es entkoppelt die interne Logik von der externen API – sollte sich die externe Schnittstelle ändern (z. B. neue Felder, umbenannte Properties), muss nur das Mapping im `DecisionService` angepasst werden, ohne den restlichen Code zu berühren.

Diese Trennung folgt dem Prinzip der **Schichtenarchitektur** und verhindert, dass externe Datenstrukturen direkt durch alle Schichten propagiert werden.

### 5.4 Enumerationen (definiert in `DecisionMade`)

**`DestinationCountry`** (Unterstützte Zielländer):

| Wert  | Land         |
|-------|--------------|
| `ARG` | Argentinien  |
| `JAP` | Japan        |
| `DE`  | Deutschland  |
| `CH`  | Schweiz      |
| `RUS` | Russland     |

**`ShippingType`** (Versandmethoden):

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

`DecisionWorker.main()` wird direkt aufgerufen (unabhängig vom Spring Boot Kontext) und baut den Camunda External Task Client auf:

```java
ExternalTaskClient client = ExternalTaskClient.create()
    .baseUrl("http://xxx@192.168.111.3:8080/engine-rest")
    .asyncResponseTimeout(1000)
    .build();
```

### Schritt 2 – Subscription auf Topic

Der Worker abonniert das Camunda-Topic **`shippingDecision`** mit einer Lock-Dauer von **1000 ms**:

```java
client.subscribe("shippingDecision")
      .lockDuration(1000)
      .handler(new DecisionExternalTaskHandler(...))
      .open();
```

Solange kein passender Task vorhanden ist, wartet der Client per **Long Polling** auf neue Tasks.

### Schritt 3 – Task-Empfang & Variablen lesen

Sobald die Camunda Engine einen Task mit dem Topic `shippingDecision` erzeugt, ruft der `DecisionExternalTaskHandler` die `execute()`-Methode auf und liest die BPMN-Prozessvariablen:

```
weight  → Long    (Gewicht des Pakets, z. B. 100)
country → String  (Zielland als Enum-Kürzel, z. B. "ARG", "RUS", "CH")
```

Das `country`-String wird direkt per `DecisionMade.DestinationCountry.valueOf(...)` in einen Enum-Wert konvertiert.
Bei einem unbekannten Wert wirft `valueOf()` eine `IllegalArgumentException` (→ kein Retry, Fehler in Camunda gemeldet).

### Schritt 4 – Fachliche Validierung (DecisionService)

`DecisionService.sendDecisionOrder()` prüft die Eingaben:

- `country` darf nicht `null` sein → sonst `IllegalArgumentException`
- `weight` muss grösser als `0` sein → sonst `IllegalArgumentException`

### Schritt 5 – Mapping und REST-Aufruf (DecisionApiClient)

Der `DecisionService` befüllt das `ShippingDecisionArgs`-Objekt und übergibt es an `DecisionApiClient.requestConsignment()`.

Der `DecisionApiClient` sendet einen **HTTP POST** an:
```
POST http://localhost:8081/decision/make
Content-Type: application/json

{
  "destinationCountry": "ARG",
  "weight": 5000
}
```

### Schritt 6 – Antwort verarbeiten

Der externe Dienst antwortet mit einem `DecisionMade`-JSON-Objekt.
Der `DecisionService` loggt die erhaltene Antwort und mappt sie in ein `ShippingResult`-Objekt.

**Beispiel AUTOMATIC:**
```json
{
  "decisionType": "AUTOMATIC",
  "shippingType": "SPECIAL",
  "carrier": "SpecialCarrier",
  "ruleId": 1
}
```

**Beispiel MANUAL** (z. B. bei Russland):
```json
{
  "decisionType": "MANUAL",
  "shippingType": null,
  "carrier": null,
  "ruleId": null
}
```

### Schritt 7 – Prozessvariablen setzen & Task abschliessen

Der `DecisionExternalTaskHandler` schreibt die Ergebnisse als **plain Strings** in eine Variable-Map und **schliesst den External Task** ab:

```
decisionType   → z. B. "AUTOMATIC" oder "MANUAL"
shippingType   → z. B. "SPECIAL" oder null (bei MANUAL)
carrier        → z. B. "SpecialCarrier" oder null (bei MANUAL)
ruleId         → z. B. 1 oder null (bei MANUAL)
```

> **Wichtig:** Enum-Werte werden als `String` (`.name()`) gespeichert – **nicht** als Java-Objekt.
> Nur so kann die Camunda Engine den BPMN-Ausdruck `${decisionType == 'MANUAL'}` korrekt auswerten.
> Null-Werte (bei `MANUAL`) werden sicher übergeben, da die Konvertierung null-prüft (`!= null ? .name() : null`).
> Dadurch wird der `NullPointerException`, die bei direktem `.toString()` auf `null`-Enums auftreten würde, zuverlässig verhindert.

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

| Eigenschaft              | Wert                                        |
|--------------------------|---------------------------------------------|
| Camunda Engine URL       | `http://xxx@192.168.111.3:8080/engine-rest` |
| Async Response Timeout   | `1000 ms`                                   |
| Lock Duration            | `1000 ms`                                   |
| Topic                    | `shippingDecision`                          |
| Entscheidungs-API URL    | `http://localhost:8081/decision/make`       |

---

## 9. Starten der Anwendung

### Voraussetzungen

- Java 25 installiert
- Maven (oder Maven Wrapper `mvnw` verwenden)
- Camunda BPM Engine erreichbar
- Externer Entscheidungsdienst (`SA_Case2_DecisionApplication`) unter `http://localhost:8081/decision/make` erreichbar und gestartet

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

> **Wichtig:** Der eigentliche Worker-Einstiegspunkt liegt in `DecisionWorker.main()`. Diese Klasse wird direkt über die IntelliJ Run-Konfiguration gestartet – **nicht** über den Spring Boot Application-Einstiegspunkt. Spring Boot wird lediglich für den Anwendungskontext (Dependency Injection, Konfiguration) verwendet.

---

