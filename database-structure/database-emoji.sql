create table guilds
(
  guildid   bigint not null,
  guildname varchar(21845),
  constraint guilds_guildid_pk
  primary key (guildid)
);

create table groups
(
  guildid   bigint,
  groupid   bigserial             not null,
  roleid    bigint,
  type      varchar(10),
  groupname varchar(20),
  enabled   boolean default false not null,
  constraint groups_groupid_pk
  primary key (groupid),
  constraint groups_guilds_guildid_fk
  foreign key (guildid) references guilds
);

create table grouproles
(
  groupid  bigint not null,
  roleid   bigint not null,
  rolename varchar(21845),
  constraint grouproles_groupid_roleid_pk
  primary key (groupid, roleid),
  constraint grouproles_groups_groupid_fk
  foreign key (groupid) references groups
);

create unique index groups_groupid_uindex
  on groups (groupid);

create table roles
(
  guildid  bigint not null,
  roleid   bigint not null,
  rolename varchar(21845),
  constraint roles_guildid_roleid_pk
  primary key (guildid, roleid),
  constraint roles_guilds_guildid_fk
  foreign key (guildid) references guilds
);
