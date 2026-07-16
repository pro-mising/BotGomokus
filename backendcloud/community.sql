create table if not exists community_post (
    id int primary key auto_increment,
    user_id int not null,
    title varchar(100) not null,
    content text not null,
    tag varchar(30) default 'General',
    likes int default 0,
    createtime datetime default current_timestamp
);

create table if not exists community_comment (
    id int primary key auto_increment,
    post_id int not null,
    user_id int not null,
    content text not null,
    createtime datetime default current_timestamp
);

create table if not exists community_post_like (
    id int primary key auto_increment,
    post_id int not null,
    user_id int not null,
    createtime datetime default current_timestamp,
    unique key unique_community_post_like (post_id, user_id)
);
