create table followedAccounts(
    id serial primary key,
    user_id int,
    twitter_account_id varchar(255),
    name varchar(255),
    username varchar(255),
    foreign key (user_id) references twitterLogs(id)
);