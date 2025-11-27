### Flyt av events mellom dp-behandling og dp-saksbehandling


### 1. Oppgave opprettes eller si ifra at behandling må avbrytes
```mermaid
graph RL

  A["dp-behandling"] --> |"behandling_opprettet"| B["dp-saksbehandling"]
  B["dp-saksbehandling"] --> |"Beskyttet person -> avbryt_behandling"| A["dp-behandling"]
```

### 2. Oppgave gjøres klar til behandling
```mermaid
graph RL
  A["dp-behandling"] --> |"forslag_til_behandlingsresultat"| B["dp-saksbehandling"]
```

### 3. Oppgave ferdigstilles eller avbrytes
```mermaid
graph RL
  A["dp-behandling"] --> |"behandlingsresultat"| B["dp-saksbehandling"]
  A["dp-behandling"] --> |"behandling_avbrutt"| B["dp-saksbehandling"]
```



