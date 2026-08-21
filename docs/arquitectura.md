# Arquitectura del Sistema - App Radio CDMX (Preparada Multipaís)

Este documento define la arquitectura técnica, los flujos de datos y la especificación de componentes para la aplicación Android de emisoras de radio de la Ciudad de México.

El sistema utiliza **Firebase Firestore** como CMS en la nube y base de datos remota, una **herramienta CLI en Python** ejecutada localmente para scraping, validación y poblado del catálogo (parametrizada dinámicamente por país y ciudad), y una **App Android nativa (Offline-First)** construida con Jetpack Compose, Room DB y `androidx.media3` (ExoPlayer).

> [!NOTE]
> **Preparación Multipaís en Arquitectura y UI:** Aunque la versión inicial estará focalizada exclusivamente en la Ciudad de México (CDMX), la arquitectura de datos y **la interfaz de usuario (UI en Jetpack Compose)** se diseñan desde el primer día para soportar la incorporación futura de emisoras de otros países o ciudades sin requerir reestructuraciones de código.

---

## 1. Visión General del Sistema (Modelo Firebase + Python CLI + Android)

### 1.1 Diagrama de Bloques General

```mermaid
flowchart TD
    API["1. API REST / Firestore (Colección stations por país y ciudad)"]
    ROOM[("2. Base de Datos Local (Room DB)")]
    UI["3. Interfaz de Usuario preparada Multipaís (Jetpack Compose)"]
    MEDIA["4. Motor de Audio (ExoPlayer + Media3)"]
    STREAMS["5. Servidores de Radio (Icecast / Shoutcast / HLS)"]

    API -->|"Descarga catálogo JSON"| ROOM
    ROOM -->|"Provee datos guardados"| UI
    UI -->|"Solicita reproducir emisora"| MEDIA
    MEDIA -->|"Consume transmisión de audio"| STREAMS
```

### 1.2 Flujo del Proceso de Scraping, Validación y Poblado (Python CLI)

El mantenimiento del catálogo lo realiza el desarrollador ejecutando manualmente el script en Python desde su laptop personal, pasando como parámetros el país y la ciudad objetivo.

```mermaid
flowchart TD
    DEV["1. Laptop del Desarrollador (python main.py --pais Mexico --ciudad CDMX)"]
    SCRAPING["2. Extracción Dinámica (Filtra Radio-Browser por --pais y --ciudad)"]
    PING["3. Verificación de Streams (Ping HTTP HEAD / GET)"]
    CLEAN["4. Normalización de Metadatos (Asigna País y Ciudad parametrizados)"]
    SDK["5. Autenticación Firebase (serviceAccountKey.json + firebase-admin)"]
    FS[("6. Base de Datos Firestore (Colección stations)")]

    DEV -->|"Recibe parámetros --pais y --ciudad"| SCRAPING
    SCRAPING -->|"Obtiene lista del país/ciudad"| PING
    PING -->|"Filtra streams activos"| CLEAN
    CLEAN -->|"Prepara objetos JSON con ubicación"| SDK
    SDK -->|"Ejecuta UPSERT"| FS
```

---

## 2. Arquitectura de la Aplicación Android

La aplicación Android sigue los principios de **Clean Architecture** (UI, Domain, Data, Media).

### 2.1 Diagrama de Capas de la App y Diseño de UI Multipaís

```mermaid
flowchart TD
    subgraph UI_Layer ["Capa de Presentación (UI preparada Multipaís)"]
        COMPOSE["Jetpack Compose Screens<br/>(Home, Player, Favoritos)"]
        SELECTOR["CountryCitySelectorBar<br/>(Filtros dinámicos de País y Ciudad)"]
        VM["ViewModels<br/>(FilterState: selectedCountry, selectedCity)"]
        COMPOSE <--> SELECTOR
        COMPOSE <--> VM
    end

    subgraph Domain_Layer ["Capa de Dominio"]
        UC["Use Cases<br/>• GetStationsByLocationUseCase<br/>• PlayStationUseCase<br/>• ToggleFavoriteUseCase"]
    end

    subgraph Data_Layer ["Capa de Datos"]
        REPO["StationRepositoryImpl"]
        ROOM[("Room Database<br/>Indexado por pais y ciudad")]
        FIRESTORE["Firebase Firestore SDK"]
        REPO --> ROOM
        REPO --> FIRESTORE
    end

    subgraph Media_Layer ["Capa de Reproducción"]
        SERVICE["RadioMediaService (MediaSessionService)"]
        EXO["ExoPlayer (Media3)"]
        NOTIF["Notification & Controls"]
        SERVICE --> EXO
        SERVICE --> NOTIF
    end

    VM --> UC
    UC --> REPO
    UC --> SERVICE
```

### 2.2 Estrategia de la UI para Soporte Multipaís Futuro
- **Selector Dinámico de Ubicación (`CountryCitySelectorBar`):** La TopAppBar y los filtros superiores integran un componente modular que por defecto selecciona "México / Ciudad de México". Si en el futuro se descargan estaciones de otros países (ej. España / Madrid), el componente activará automáticamente chips de selección rápida o un menú desplegable de países y ciudades sin modificar el diseño general.
- **Tarjetas de Emisoras con Badge de Ubicación:** Cada tarjeta de radio renderiza dinámicamente un badge sutil con la ciudad y el país (o bandera), garantizando claridad visual al coexistir estaciones de distintas regiones.

---

## 3. Flujos de Sincronización (Offline-First)

El consumo de datos garantiza respuesta inmediata en pantalla y actualización reactiva cuando hay conexión a internet:

### 3.1 Carga Inicial de Datos (Lectura Local)

```mermaid
sequenceDiagram
    autonumber
    participant UI as UI (Compose)
    participant VM as ViewModel
    participant Rep as Repository
    participant Room as Room DB

    UI->>VM: Abrir App (Filtro actual: País/Ciudad)
    VM->>Rep: getStations(country, city)
    Rep->>Room: SELECT * FROM stations WHERE pais = :country AND ciudad = :city
    Room-->>Rep: Retorna estaciones locales
    Rep-->>VM: StateFlow (Lista Local)
    VM-->>UI: Muestra catálogo de inmediato
```

### 3.2 Sincronización Remota desde Firebase Firestore

```mermaid
sequenceDiagram
    autonumber
    participant Rep as Repository
    participant FS as Firebase Firestore
    participant Room as Room DB
    participant UI as UI (Compose)

    Rep->>FS: getDocuments() / snapshotListener
    alt Petición Exitosa / Hay Cambios
        FS-->>Rep: Documentos de la colección 'stations'
        Rep->>Room: UPSERT estaciones locales
        Room-->>UI: Emite lista actualizada vía Flow
    else Sin Conexión a Internet
        FS-->>Rep: Error de Red / Offline
        Note over Rep: Mantiene catálogo en caché de Room DB
    end
```

---

## 4. Flujos del Motor de Audio (`androidx.media3`)

### 4.1 Inicio de Reproducción de Estación

```mermaid
sequenceDiagram
    autonumber
    participant User as Usuario
    participant UI as UI (Compose)
    participant Service as RadioMediaService
    participant Exo as ExoPlayer
    participant Stream as Servidor Icecast/HLS

    User->>UI: Toca la emisora (ej. Reactor 105.7)
    UI->>Service: Iniciar Reproducción (URL Stream)
    Service->>Exo: setMediaItem(streamUrl) & play()
    Service->>Service: Activa Notificación en Primer Plano (Foreground)
    Exo->>Stream: Conexión HTTP Stream
    Stream-->>Exo: Transmisión de Audio (AAC/MP3)
    Exo-->>User: Reproducción Sonora
```

### 4.2 Control de Interrupciones (Audio Focus / Llamadas)

```mermaid
sequenceDiagram
    autonumber
    participant System as Sistema Android
    participant Service as RadioMediaService
    participant Exo as ExoPlayer

    System->>Service: Pérdida Temporal de Foco (Llamada Entrante)
    Service->>Exo: pause()
    Note over Service: Audio Pausado temporalmente

    System->>Service: Recuperación de Foco (Llamada Finalizada)
    Service->>Exo: play()
    Note over Service: Reanuda transmisión de audio
```

---

## 5. Modelo de Datos

### 5.1 Estructura del Documento Firestore (Colección `stations`)

```json
{
  "id": "cdmx-reactor-1057",
  "nombre": "Reactor 105.7 FM",
  "frecuencia": "105.7 FM",
  "banda": "FM",
  "grupo": "IMER",
  "stream_url": "https://stream.imer.link/reactor1057.mp3",
  "stream_type": "ICECAST",
  "logo_url": "https://firebasestorage.googleapis.com/.../reactor1057.png",
  "categoria": "Rock / Alternativo",
  "pais": "México",
  "ciudad": "Ciudad de México",
  "activo": true,
  "orden": 1,
  "tags": ["rock", "indie", "imer"],
  "last_checked": "2026-08-12T18:00:00Z",
  "status_code": 200,
  "latency_ms": 145
}
```

### 5.2 Modelo Entidad-Relación de Base de Datos Room

```mermaid
erDiagram
    STATION {
        string id PK
        string nombre
        string frecuencia
        string banda
        string grupo
        string stream_url
        string stream_type
        string logo_url
        string categoria
        string pais
        string ciudad
        boolean activo
        int orden
        boolean is_favorite
        string last_checked
    }

    FAVORITE {
        string station_id PK
        string added_at
    }

    STATION ||--o| FAVORITE : "es marcada en"
```
