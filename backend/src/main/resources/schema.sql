drop table if exists game_records;
drop table if exists bots;
drop table if exists users;

create table users (
    id int auto_increment primary key,
    username varchar(100) not null unique,
    password varchar(100) not null,
    photo varchar(500) not null,
    rating int not null
);

create table bots (
    id int auto_increment primary key,
    user_id int not null,
    title varchar(100) not null,
    description varchar(300),
    code clob not null,
    created_at timestamp not null,
    constraint fk_bots_user foreign key (user_id) references users(id)
);

create table game_records (
    id int auto_increment primary key,
    player_name varchar(100) not null,
    opponent_name varchar(100) not null,
    result varchar(20) not null,
    rating_delta int not null,
    map_json clob not null,
    created_at timestamp not null
);
