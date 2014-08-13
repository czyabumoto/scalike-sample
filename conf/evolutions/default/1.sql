# --- !Ups
create table company (
  id bigint primary key auto_increment(1),
  name1 varchar(256) not null,
  name2 varchar(256) not null,
  email text null,
  address1 text null,
  address2 text null);
  
# --- !Downs
drop table company;