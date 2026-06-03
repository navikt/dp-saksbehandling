-- Flyway test baseline ved V118
-- Generert fra B66__BASELINE.sql + DDL-endringer V67–V118
-- Data-migrasjoner er utelatt da baseline brukes for tomme testdatabaser

create or replace function oppdater_endret_tidspunkt() returns trigger
    language plpgsql
as
$$
BEGIN
    NEW.endret_tidspunkt = timezone('Europe/Oslo'::text, current_timestamp);
    RETURN NEW;
END;
$$;

create table if not exists person_v1
(
    id                           uuid                                                                        not null
        primary key,
    ident                        varchar(11)                                                                 not null
        unique,
    registrert_tidspunkt         timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    endret_tidspunkt             timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    skjermes_som_egne_ansatte    boolean                     default false                                   not null,
    adressebeskyttelse_gradering text                        default 'UGRADERT'::text
);

create index if not exists person_ident_index
    on person_v1 (ident);

create trigger oppdater_endret_tidspunkt
    before update
    on person_v1
    for each row
execute function oppdater_endret_tidspunkt();

create table if not exists utsending_sak_v1
(
    id                   text not null
        constraint sak_v1_pkey
            primary key,
    kontekst             text not null,
    registrert_tidspunkt timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp)
);

create trigger oppdater_endret_tidspunkt
    before update
    on utsending_sak_v1
    for each row
execute function oppdater_endret_tidspunkt();

create table if not exists klage_v1
(
    id                   uuid                                                                        not null
        primary key,
    tilstand             text                                                                        not null,
    opplysninger         jsonb                                                                       not null,
    registrert_tidspunkt timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    endret_tidspunkt     timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    journalpost_id       text,
    behandlende_enhet    text
);

create trigger oppdater_endret_tidspunkt
    before update
    on klage_v1
    for each row
execute function oppdater_endret_tidspunkt();

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

-- sak_v2: soknad_id-kolonnen er fjernet (V113)
create table if not exists sak_v2
(
    id                   uuid                    not null
        primary key,
    person_id            uuid                    not null
        references person_v1,
    registrert_tidspunkt timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp),
    opprettet            timestamp               not null,
    arena_sak_id         text,
    er_dp_sak            boolean                 default false not null
);

create index if not exists sak_person_id_index
    on sak_v2 (person_id);

create trigger oppdater_endret_tidspunkt
    before update
    on sak_v2
    for each row
execute function oppdater_endret_tidspunkt();

-- behandling_v1: behandling_type omdøpt til utlost_av (V68)
create table if not exists behandling_v1
(
    id                   uuid                                                                        not null
        primary key,
    person_id            uuid                                                                        not null
        references person_v1,
    opprettet            timestamp without time zone                                                 not null,
    registrert_tidspunkt timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    endret_tidspunkt     timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    utlost_av            text                                                                        not null,
    sak_id               uuid
        references sak_v2
);

create index if not exists behandling_person_id_index
    on behandling_v1 (person_id);

create trigger oppdater_endret_tidspunkt
    before update
    on behandling_v1
    for each row
execute function oppdater_endret_tidspunkt();

create table if not exists oppgave_v1
(
    id                      uuid                                                                        not null
        primary key,
    behandling_id           uuid                                                                        not null
        references behandling_v1
            on delete cascade,
    tilstand                text                                                                        not null,
    saksbehandler_ident     text,
    opprettet               timestamp without time zone                                                 not null,
    registrert_tidspunkt    timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    endret_tidspunkt        timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    utsatt_til              date,
    melding_om_vedtak_kilde text                                                                        not null,
    kontrollert_brev        text                                                                        not null
);

create index if not exists oppgave_behandling_id_index
    on oppgave_v1 (behandling_id);

create index if not exists oppgave_saksbehandler_ident_index
    on oppgave_v1 (saksbehandler_ident);

create index if not exists oppgave_opprettet_index
    on oppgave_v1 (opprettet);

create trigger oppdater_endret_tidspunkt
    before update
    on oppgave_v1
    for each row
execute function oppdater_endret_tidspunkt();

create table if not exists emneknagg_v1
(
    id                   bigserial
        primary key,
    oppgave_id           uuid                                                                        not null
        references oppgave_v1
            on delete cascade,
    emneknagg            text                                                                        not null,
    registrert_tidspunkt timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    endret_tidspunkt     timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp) not null,
    constraint emneknagg_oppgave_unique
        unique (oppgave_id, emneknagg)
);

create index if not exists emneknagg_oppgave_id_index
    on emneknagg_v1 (oppgave_id);

create trigger oppdater_endret_tidspunkt
    before update
    on emneknagg_v1
    for each row
execute function oppdater_endret_tidspunkt();

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
    id                   uuid                                                not null
        primary key,
    tilstand             text                                                not null,
    brev                 text,
    pdf_urn              text,
    journalpost_id       text,
    distribusjon_id      text,
    utsending_sak_id     text
        constraint utsending_v1_sak_id_fkey
            references utsending_sak_v1,
    registrert_tidspunkt timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp),
    type                 text                        default 'VEDTAK_DAGPENGER'::text not null,
    behandling_id        uuid
        constraint behandling_id_unique
            unique
        references behandling_v1
);

create index if not exists utsending_sak_id_index
    on utsending_v1 (utsending_sak_id);

create index if not exists utsending_behandling_id_index
    on utsending_v1 (behandling_id);

create trigger oppdater_endret_tidspunkt
    before update
    on utsending_v1
    for each row
execute function oppdater_endret_tidspunkt();

create table if not exists oppgave_tilstand_logg_v1
(
    id            uuid                        not null
        primary key,
    oppgave_id    uuid                        not null
        references oppgave_v1
            on delete cascade,
    tilstand      text                        not null,
    hendelse_type text                        not null,
    hendelse      jsonb                       not null,
    tidspunkt     timestamp without time zone not null
);

create index if not exists oppgave_tilstand_logg_oppgave_id_index
    on oppgave_tilstand_logg_v1 (oppgave_id);

-- V79: erstattet oppgave_endret_tidspunkt_index
create index if not exists oppgave_tilstand_logg_tidspunkt_index
    on oppgave_tilstand_logg_v1 (tidspunkt);

-- V115: unik partiell indeks for å hindre duplikate avsluttende tilstander
create unique index if not exists idx_1_avsluttende_tilstand_per_oppgave
    on oppgave_tilstand_logg_v1 (oppgave_id, tilstand)
    where tilstand in ('FERDIG_BEHANDLET', 'AVBRUTT', 'AVBRUTT_MASKINELT');

create table if not exists notat_v1
(
    id                       uuid                                not null
        primary key,
    oppgave_tilstand_logg_id uuid                                not null
        unique
        references oppgave_tilstand_logg_v1
            on delete cascade,
    tekst                    text                                not null,
    endret_tidspunkt         timestamp without time zone default current_timestamp not null,
    skrevet_av               text                                not null
);

create index if not exists notat_oppgave_tilstand_logg_id_index
    on notat_v1 (oppgave_tilstand_logg_id);

-- V76: klageinstans-vedtak koblet til klage
create table if not exists klageinstans_vedtak_v1
(
    id                   uuid                        not null
        primary key,
    type                 text                        not null,
    klage_id             uuid                        not null
        references klage_v1 (id),
    utfall               text                        not null,
    avsluttet            timestamp without time zone not null,
    journalpost_ider     text[]                      not null,
    registrert_tidspunkt timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp)
);

-- V70+V73: innsending (endelig skjema etter V73-endringer)
create table if not exists innsending_v1
(
    id                     uuid                        not null
        primary key,
    person_id              uuid                        not null
        references person_v1 (id),
    journalpost_id         text                        not null,
    mottatt                timestamp without time zone not null,
    skjema_kode            text                        not null,
    kategori               text                        not null,
    tilstand               text                        not null default 'BEHANDLES',
    soknad_id              uuid                                 default null,
    vurdering              text                                 default null,
    resultat_behandling_id uuid                                 default null,
    resultat_type          text                                 default null,
    valgt_sak_id           uuid                                 default null
        references sak_v2 (id),
    registrert_tidspunkt   timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt       timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp)
);

create index if not exists innsending_person_id_index
    on innsending_v1 (person_id);

create index if not exists innsending_v1_sak_v2_index
    on innsending_v1 (valgt_sak_id);

create or replace trigger oppdater_endret_tidspunkt
    before update
    on innsending_v1
    for each row
execute function oppdater_endret_tidspunkt();

-- V83+V84+V89+V95+V96+V100: saksbehandlingsstatistikk (endelig skjema)
create table if not exists saksbehandling_statistikk_v1
(
    sekvensnummer           bigserial                   not null
        primary key,
    tilstand_id             uuid                        not null,
    tilstand                text                        not null,
    tilstand_tidspunkt      timestamp without time zone not null,
    oppgave_id              uuid                        not null,
    mottatt                 timestamp without time zone not null,
    sak_id                  uuid                        not null,
    behandling_id           uuid                        not null,
    person_ident            text                        not null,
    saksbehandler_ident     text,
    beslutter_ident         text,
    utlost_av               text                        not null,
    overfort_til_statistikk boolean                     not null default false,
    behandling_resultat     text,
    fagsystem               text                        not null,
    behandling_aarsak       text,
    arena_sak_id            text,
    resultat_begrunnelse    text
);

-- V109+V110: oppfølging (omdøpt fra generell_oppgave)
create table if not exists oppfolging_v1
(
    id                     uuid                        not null
        primary key,
    person_id              uuid                        not null
        references person_v1 (id),
    tittel                 text                        not null,
    beskrivelse            text,
    strukturert_data       jsonb,
    frist                  date,
    opprettet              timestamp without time zone not null,
    tilstand               text                        not null default 'BEHANDLES',
    vurdering              text,
    resultat_type          text,
    resultat_behandling_id uuid,
    valgt_sak_id           uuid,
    registrert_tidspunkt   timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt       timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp)
);

comment on column oppfolging_v1.frist is 'Frist for oppgaven. Oppgave settes til PåVent med utsattTil = frist';

create index if not exists idx_oppfolging_person_id
    on oppfolging_v1 (person_id);

create index if not exists idx_oppfolging_tilstand
    on oppfolging_v1 (tilstand);

create or replace trigger oppdater_endret_tidspunkt
    before update
    on oppfolging_v1
    for each row
execute function oppdater_endret_tidspunkt();

-- V117: brukere unntatt nødbremse
create table if not exists nodbremset_person_v1
(
    person_id            uuid not null
        primary key
        references person_v1 (id),
    registrert_tidspunkt timestamp without time zone default timezone('Europe/Oslo'::text, current_timestamp)
);

-- Grants til cloudsqliamuser (V77, V83, V112, V117)
do
$$
    begin
        if exists (select 1 from pg_roles where rolname = 'cloudsqliamuser') then
            grant select on table klageinstans_vedtak_v1 to cloudsqliamuser;
            grant select on table saksbehandling_statistikk_v1 to cloudsqliamuser;
            grant select on table oppfolging_v1 to cloudsqliamuser;
            grant select on table nodbremset_person_v1 to cloudsqliamuser;
        end if;
    end
$$;
