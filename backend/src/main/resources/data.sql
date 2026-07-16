insert into users (username, password, photo, rating)
values ('alice', '123456', 'https://api.dicebear.com/8.x/thumbs/svg?seed=alice', 1500);

insert into bots (user_id, title, description, code, created_at)
values
    (1, 'Straight Bot', 'A tiny example bot.', 'return 1;', current_timestamp),
    (1, 'Random Bot', 'A placeholder for your strategy.', 'return randomDirection();', current_timestamp);
