INSERT INTO albums(id, title, expire_date) VALUES (2, 'Korea and Japan', '2024-12-31');
INSERT INTO albums(id, title, expire_date) VALUES (3, 'Korea and China', '2024-12-31');

INSERT INTO cards(album_id, player_id) SELECT 2 AS album_id, id as player_id FROM players where team_id in (1885010, 1883723);
INSERT INTO cards(album_id, player_id) SELECT 3 AS album_id, id as player_id FROM players where team_id in (1885010, 1882892);
