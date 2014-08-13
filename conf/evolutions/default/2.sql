# --- !Ups
create table client (
  id bigint primary key auto_increment(1),
  company_id bigint not null,
  name varchar(256) not null,
  password varchar(256) not null,
  email text null,
  address1 text null,
  address2 text null);
  
# --- !Downs
drop table client;