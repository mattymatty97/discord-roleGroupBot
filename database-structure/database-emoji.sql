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



