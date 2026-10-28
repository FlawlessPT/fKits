CREATE TABLE IF NOT EXISTS fkits_cooldowns (
    player_uuid CHAR(36) CHARACTER SET ascii NOT NULL,
    kit         VARCHAR(16) NOT NULL,
    last_claim  BIGINT NOT NULL,
    PRIMARY KEY (player_uuid, kit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
