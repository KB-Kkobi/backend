DROP TABLE if exists tbl_member;

create table tbl_member(
                           username varchar(50) primary key,
                           password varchar(128) not null ,
                           email varchar(50) not null ,
                           reg_date datetime default now(),
                           update_date datetime default now()
);

drop table if exists tbl_membera_auth;

create table tbl_member_auth(
                                username varchar(50) not null ,
                                auth varchar(50) not null ,
                                primary key (username, auth),
                                constraint fk_authorities_users foreign key (username) references tbl_member(username)
);

insert into tbl_member(username, password, email)
values
    ('admin', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', 'admin@galapgos.org'),
    ('user0', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', 'user0@galapgos.org'),
    ('user1', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', 'user1@galapgos.org'),
    ('user2', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', 'user2@galapgos.org'),
    ('user3', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', 'user3@galapgos.org'),
    ('user4', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', 'user4@galapgos.org');

select * from tbl_member;


insert into tbl_member_auth(username, auth)
values
    ('admin','ROLE_ADMIN'),
    ('admin','ROLE_MANAGER'),
    ('admin','ROLE_MEMBER'),
    ('user0','ROLE_MANAGER'),
    ('user0','ROLE_MEMBER'),
    ('user1','ROLE_MEMBER'),
    ('user2','ROLE_MEMBER'),
    ('user3','ROLE_MEMBER'),
    ('user4','ROLE_MEMBER');
select * from tbl_member_auth order by auth;