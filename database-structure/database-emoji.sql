CREATE TABLE guilds
(
  guildid      BIGINT                                       NOT NULL,
  prefix       VARCHAR(10) DEFAULT 'tb!' :: CHARACTER VARYING,
  guildname    VARCHAR(21845),
  emoji_prefix VARCHAR(10) DEFAULT ';' :: CHARACTER VARYING NOT NULL,
  max_emoji    INTEGER DEFAULT 4                            NOT NULL,
  CONSTRAINT guilds_guildid_pk
  PRIMARY KEY (guildid)
);

CREATE TABLE roles
(
  guildid  BIGINT NOT NULL,
  roleid   BIGINT NOT NULL,
  rolename VARCHAR(21845),
  CONSTRAINT roles_guildid_roleid_pk
  PRIMARY KEY (guildid, roleid),
  CONSTRAINT roles_guilds_guildid_fk
  FOREIGN KEY (guildid) REFERENCES guilds
);

CREATE TABLE groups
(
  guildid   BIGINT,
  groupid   BIGSERIAL             NOT NULL,
  roleid    BIGINT,
  type      VARCHAR(10),
  groupname VARCHAR(20),
  enabled   BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT groups_groupid_pk
  PRIMARY KEY (groupid),
  CONSTRAINT groups_guilds_guildid_fk
  FOREIGN KEY (guildid) REFERENCES guilds
);

CREATE UNIQUE INDEX groups_groupid_uindex
  ON groups (groupid);

CREATE TABLE grouproles
(
  groupid  BIGINT NOT NULL,
  roleid   BIGINT NOT NULL,
  rolename VARCHAR(21845),
  CONSTRAINT grouproles_groupid_roleid_pk
  PRIMARY KEY (groupid, roleid),
  CONSTRAINT grouproles_groups_groupid_fk
  FOREIGN KEY (groupid) REFERENCES groups
);

CREATE TABLE registered_emoji_server
(
  guildid BIGINT      NOT NULL,
  title   VARCHAR(10) NOT NULL,
  CONSTRAINT registered_emoji_server_pkey
  PRIMARY KEY (guildid),
  CONSTRAINT registered_emoji_server_guilds_guildid_fk
  FOREIGN KEY (guildid) REFERENCES guilds
);

CREATE UNIQUE INDEX registered_emoji_server_guildid_uindex
  ON registered_emoji_server (guildid);

CREATE UNIQUE INDEX registered_emoji_server_title_uindex
  ON registered_emoji_server (title);

CREATE TABLE active_emoji_guilds
(
  guildid       BIGINT NOT NULL,
  emoji_guildid BIGINT NOT NULL,
  CONSTRAINT active_emoji_guilds_guildid_emoji_guildid_pk
  PRIMARY KEY (guildid, emoji_guildid),
  CONSTRAINT active_emoji_guilds_guilds_guildid_guildid_fk
  FOREIGN KEY (guildid) REFERENCES guilds,
  CONSTRAINT active_emoji_guilds_registered_emoji_server_guildid_fk
  FOREIGN KEY (emoji_guildid) REFERENCES registered_emoji_server
);


