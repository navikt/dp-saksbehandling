### Flyt av events mellom dp-behandling og dp-saksbehandling


### 1. Oppgave opprettes
```mermaid
graph RL

  A["dp-behandling"] --> |"behandling_opprettet"| B["dp-saksbehandling"]
  A["dp-behandling"] --> |"Beskyttet person -> avbryt_behandling"| B["dp-saksbehandling"]
```

### 2. Oppgave gjøres klar til behandling
```mermaid
graph RL
  A["dp-behandling"] --> |"forslag_til_vedtak"| B["dp-saksbehandling"]
```

### 3. Oppgave ferdigstilles eller avbrytes
```mermaid
graph RL
  A["dp-behandling"] --> |"vedtak_fattet"| B["dp-saksbehandling"]
  A["dp-behandling"] --> |"behandling_avbrutt"| B["dp-saksbehandling"]
```



