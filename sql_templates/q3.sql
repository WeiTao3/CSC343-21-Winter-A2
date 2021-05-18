-- Q3. North and South Connections

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q3 CASCADE;

CREATE TABLE q3 (
    outbound VARCHAR(30),
    inbound VARCHAR(30),
    direct INT,
    one_con INT,
    two_con INT,
    earliest timestamp
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.


-- Define views for your intermediate steps here:

DROP VIEW IF EXISTS April30 CASCADE;
create view April30 as
select id, s_dep, s_arv, a1.city as dep_city, a1.country as dep_country, a2.city as arv_city, a2.country as arv_country
from flight left join airport a1 on flight.outbound = a1.code left join airport a2 on flight.inbound = a2.code
where cast(s_dep as date) = '2021-04-30' and cast(s_arv as date) = '2021-04-30'
;

DROP VIEW IF EXISTS direct CASCADE;
create view direct as
select dep_city as outbound, arv_city as inbound, count(*) as direct, min(s_arv) as earliest
from April30
where (dep_country = 'Canada' and arv_country = 'USA') or (dep_country = 'USA' and arv_country = 'Canada')
group by dep_city, arv_city
;

DROP VIEW IF EXISTS one_con CASCADE;
create view one_con as 
select a1.dep_city as outbound, a2.arv_city as inbound, count(*) as one_con, min(a2.s_arv) as earliest
from April30 a1 join April30 a2 on a1.id != a2.id
where ((a1.dep_country = 'Canada' and a2.arv_country = 'USA') or (a1.dep_country = 'USA' and a2.arv_country = 'Canada'))
and (a1.arv_city = a2.dep_city) and (a1.s_arv + '00:30' <= a2.s_dep)
group by a1.dep_city, a2.arv_city
;

DROP VIEW IF EXISTS two_con CASCADE;
create view two_con as
select a1.dep_city as outbound, a3.arv_city as inbound, count(*) as two_con, min(a3.s_arv) as earliest
from April30 a1 join April30 a2 on a1.id != a2.id join April30 a3 on a1.id != a3.id and a2.id != a3.id
where ((a1.dep_country = 'Canada' and a3.arv_country = 'USA') or (a1.dep_country = 'USA' and a3.arv_country = 'Canada'))
and (a1.arv_city = a2.dep_city) and (a2.arv_city = a3.dep_city) and (a1.s_arv + '00:30' <= a2.s_dep) and (a2.s_arv + '00:30' <= a3.s_dep)
group by a1.dep_city, a3.arv_city
;

DROP VIEW IF EXISTS allCity CASCADE;
create view allCity as 
(select distinct dep_city as city, dep_country as country 
from April30) 
union 
(select arv_city as city, arv_country as country from April30)
;

DROP VIEW IF EXISTS allPairs CASCADE;
create view allPairs as 
select c1.city as inbound, c2.city as outbound, null as direct, null as one_con, null as two_con, null as earliest
from allCity c1 cross join allCity c2 
where c1.country != c2.country
;

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q3

select allPairs.outbound, allPairs.inbound, haveRoutes.direct, haveRoutes.one_con, haveRoutes.two_con, haveRoutes.earliest from
(select outbound, inbound, coalesce(sum(direct), 0) as direct, coalesce(sum(one_con), 0) as one_con, coalesce(sum(two_con), 0) as two_con, min(earliest) as earliest
from direct natural full join one_con natural full join two_con
group by outbound, inbound, direct, one_con, two_con) haveRoutes
right join allPairs on haveRoutes.outbound = allPairs.outbound and haveRoutes.inbound = allPairs.inbound
;