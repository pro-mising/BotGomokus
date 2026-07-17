create table if not exists record_analysis (
    id int primary key auto_increment,
    record_id int not null,
    total_steps int default 0,
    winner varchar(20),
    win_direction varchar(20),
    key_step int default 0,
    key_moment varchar(255),
    summary text,
    highlight_score int default 0,
    createtime datetime default current_timestamp,
    updatetime datetime default current_timestamp on update current_timestamp,
    unique key unique_record_analysis (record_id)
);

create table if not exists record_favorite (
    id int primary key auto_increment,
    record_id int not null,
    user_id int not null,
    createtime datetime default current_timestamp,
    unique key unique_record_favorite (record_id, user_id)
);
