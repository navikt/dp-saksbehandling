### Tilstander i Oppgave

```mermaid
stateDiagram-v2
    direction LR

    Opprettet --> KlarTilBehandling : oppgaveKlarTilBehandling()
    KlarTilBehandling --> UnderBehandling : tildel()
    UnderBehandling --> KlarTilBehandling : fjernAnsvar()
    UnderBehandling --> KlarTilKontroll : gjørKlarTilKontroll()
    UnderBehandling --> PåVent : utsett()
    UnderBehandling --> FerdigBehandlet : ferdigstill()
    KlarTilKontroll --> UnderKontroll : tildelTotrinnskontroll()
    UnderKontroll --> FerdigBehandlet : ferdigstill()
    UnderKontroll --> UnderBehandling : sendTilbakeTilUnderBehandling()
    UnderKontroll --> KlarTilKontroll : sendTilbakeTilKlarTilKontroll()
    PåVent --> UnderBehandling : tildel()
    PåVent --> KlarTilBehandling : fjernAnsvar()
```