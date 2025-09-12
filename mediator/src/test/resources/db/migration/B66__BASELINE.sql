create table if not exists person_v1
(
    id                           uuid                                                               not null
        primary key,
    ident                        varchar(11)                                                        not null
        unique,
    registrert_tidspunkt         timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    endret_tidspunkt             timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    skjermes_som_egne_ansatte    boolean   default false                                            not null,
    adressebeskyttelse_gradering text      default 'UGRADERT'::text
);

create index if not exists person_ident_index
    on person_v1 (ident);

create table if not exists utsending_sak_v1
(
    id                   text not null
        constraint sak_v1_pkey
            primary key,
    kontekst             text not null,
    registrert_tidspunkt timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP),
    endret_tidspunkt     timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP)
);

create table if not exists klage_v1
(
    id                   uuid                                                               not null
        primary key,
    tilstand             text                                                               not null,
    opplysninger         jsonb                                                              not null,
    registrert_tidspunkt timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    endret_tidspunkt     timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    journalpost_id       text,
    behandlende_enhet    text
);

create table if not exists klage_tilstand_logg_v1
(
    id            uuid      not null
        primary key,
    klage_id      uuid      not null
        references klage_v1
            on delete cascade,
    tilstand      text      not null,
    hendelse_type text      not null,
    hendelse      jsonb     not null,
    tidspunkt     timestamp not null
);

create index if not exists klage_tilstand_logg_klage_id_index
    on klage_tilstand_logg_v1 (klage_id);

create table if not exists sak_v2
(
    id                   uuid                    not null
        primary key,
    person_id            uuid                    not null
        references person_v1,
    soknad_id            uuid                    not null,
    registrert_tidspunkt timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP),
    endret_tidspunkt     timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP),
    opprettet            timestamp               not null,
    arena_sak_id         text,
    er_dp_sak            boolean   default false not null
);

create table if not exists behandling_v1
(
    id                   uuid                                                               not null
        primary key,
    person_id            uuid                                                               not null
        references person_v1,
    opprettet            timestamp                                                          not null,
    registrert_tidspunkt timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    endret_tidspunkt     timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    behandling_type      text      default 'RETT_TIL_DAGPENGER'::text                       not null,
    sak_id               uuid
        references sak_v2
);

create index if not exists behandling_person_id_index
    on behandling_v1 (person_id);

create table if not exists oppgave_v1
(
    id                      uuid                                                               not null
        primary key,
    behandling_id           uuid                                                               not null
        references behandling_v1
            on delete cascade,
    tilstand                text                                                               not null,
    saksbehandler_ident     text,
    opprettet               timestamp                                                          not null,
    registrert_tidspunkt    timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    endret_tidspunkt        timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    utsatt_til              date,
    melding_om_vedtak_kilde text                                                               not null,
    kontrollert_brev        text                                                               not null
);

create index if not exists oppgave_behandling_id_index
    on oppgave_v1 (behandling_id);

create index if not exists oppgave_saksbehandler_ident_index
    on oppgave_v1 (saksbehandler_ident);

create index if not exists oppgave_opprettet_index
    on oppgave_v1 (opprettet);

create table if not exists emneknagg_v1
(
    id                   bigserial
        primary key,
    oppgave_id           uuid                                                               not null
        references oppgave_v1
            on delete cascade,
    emneknagg            text                                                               not null,
    registrert_tidspunkt timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    endret_tidspunkt     timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP) not null,
    constraint emneknagg_oppgave_unique
        unique (oppgave_id, emneknagg)
);

create index if not exists emneknagg_oppgave_id_index
    on emneknagg_v1 (oppgave_id);

create table if not exists hendelse_v1
(
    behandling_id uuid  not null
        primary key
        references behandling_v1
            on delete cascade,
    hendelse_type text  not null,
    hendelse_data jsonb not null
);

create table if not exists utsending_v1
(
    id                   uuid                                       not null
        primary key,
    tilstand             text                                       not null,
    brev                 text,
    pdf_urn              text,
    journalpost_id       text,
    distribusjon_id      text,
    utsending_sak_id     text
        constraint utsending_v1_sak_id_fkey
            references utsending_sak_v1,
    registrert_tidspunkt timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP),
    endret_tidspunkt     timestamp default timezone('Europe/Oslo'::text, CURRENT_TIMESTAMP),
    type                 text      default 'VEDTAK_DAGPENGER'::text not null,
    behandling_id        uuid
        constraint behandling_id_unique
            unique
        references behandling_v1
);

create index if not exists utsending_sak_id_index
    on utsending_v1 (utsending_sak_id);

create index if not exists utsending_behandling_id_index
    on utsending_v1 (behandling_id);

create table if not exists oppgave_tilstand_logg_v1
(
    id            uuid      not null
        primary key,
    oppgave_id    uuid      not null
        references oppgave_v1
            on delete cascade,
    tilstand      text      not null,
    hendelse_type text      not null,
    hendelse      jsonb     not null,
    tidspunkt     timestamp not null
);

create index if not exists oppgave_tilstand_logg_oppgave_id_index
    on oppgave_tilstand_logg_v1 (oppgave_id);

create table if not exists notat_v1
(
    id                       uuid                                not null
        primary key,
    oppgave_tilstand_logg_id uuid                                not null
        unique
        references oppgave_tilstand_logg_v1
            on delete cascade,
    tekst                    text                                not null,
    endret_tidspunkt         timestamp default CURRENT_TIMESTAMP not null,
    skrevet_av               text                                not null
);

create index if not exists notat_oppgave_tilstand_logg_id_index
    on notat_v1 (oppgave_tilstand_logg_id);

create index if not exists sak_person_id_index
    on sak_v2 (person_id);

create index if not exists sak_soknad_id_index
    on sak_v2 (soknad_id);

create function oppdater_endret_tidspunkt() returns trigger
    language plpgsql
as
$$
BEGIN
    NEW.endret_tidspunkt = timezone('Europe/Oslo'::text, current_timestamp);
    RETURN NEW;
END;
$$;

create trigger oppdater_endret_tidspunkt
    before update
    on person_v1
    for each row
execute procedure oppdater_endret_tidspunkt();

create trigger oppdater_endret_tidspunkt
    before update
    on behandling_v1
    for each row
execute procedure oppdater_endret_tidspunkt();

create trigger oppdater_endret_tidspunkt
    before update
    on oppgave_v1
    for each row
execute procedure oppdater_endret_tidspunkt();

create trigger oppdater_endret_tidspunkt
    before update
    on emneknagg_v1
    for each row
execute procedure oppdater_endret_tidspunkt();

create trigger oppdater_endret_tidspunkt
    before update
    on utsending_sak_v1
    for each row
execute procedure oppdater_endret_tidspunkt();

create trigger oppdater_endret_tidspunkt
    before update
    on utsending_v1
    for each row
execute procedure oppdater_endret_tidspunkt();

create trigger oppdater_endret_tidspunkt
    before update
    on notat_v1
    for each row
execute procedure oppdater_endret_tidspunkt();

create trigger oppdater_endret_tidspunkt
    before update
    on klage_v1
    for each row
execute procedure oppdater_endret_tidspunkt();

create trigger oppdater_endret_tidspunkt
    before update
    on sak_v2
    for each row
execute procedure oppdater_endret_tidspunkt();

