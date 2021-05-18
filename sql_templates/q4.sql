-- Q4. Plane Capacity Histogram

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q4 CASCADE;

CREATE TABLE q4 (
	airline CHAR(2),
	tail_number CHAR(5),
	very_low INT,
	low INT,
	fair INT,
	normal INT,
	high INT
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;


-- Define views for your intermediate steps here:

DROP VIEW IF EXISTS bookedSeat CASCADE;
create view bookedSeat as
select flight_id, count(row) as bookedSeat
from booking
group by flight_id
;

DROP VIEW IF EXISTS bookedAndDeparture CASCADE;
create view bookedAndDeparture as
select departure.flight_id, bookedSeat
from departure join bookedSeat on departure.flight_id = bookedSeat.flight_id
;

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q4

select plane.airline, plane.tail_number, coalesce(sum(very_low), 0) as very_low, coalesce(sum(low), 0) as low, coalesce(sum(fair), 0) as fair, coalesce(sum(normal), 0) as normal, coalesce(sum(high), 0) as high
from plane left join flight on (plane.airline = flight.airline and plane.tail_number = flight.plane) left join (
select flight.id, 
case
	when (cast(sum(bookedSeat) as float)/(sum(capacity_economy) + sum(capacity_business) + sum((capacity_first))) < 0.2) then count(flight.id)
end as very_low,
case
	when (0.2 <= (cast(sum(bookedSeat) as float)/(sum(capacity_economy) + sum(capacity_business) + sum(capacity_first))) and (cast(sum(bookedSeat) as float)/(sum(capacity_economy) + sum(capacity_business) + sum(capacity_first))) < 0.4) then count(flight.id)
end as low,
case
	when (0.4 <= (cast(sum(bookedSeat) as float)/(sum(capacity_economy) + sum(capacity_business) + sum(capacity_first))) and (cast(sum(bookedSeat) as float)/(sum(capacity_economy) + sum(capacity_business) + sum(capacity_first))) < 0.6) then count(flight.id)
end as fair,
case
	when (0.6 <= (cast(sum(bookedSeat) as float)/(sum(capacity_economy) + sum(capacity_business) + sum(capacity_first))) and (cast(sum(bookedSeat) as float)/(sum(capacity_economy) + sum(capacity_business) + sum(capacity_first))) < 0.8) then count(flight.id)
end as normal,
case
	when (cast(sum(bookedSeat) as float)/(sum(capacity_economy) + sum(capacity_business) + sum(capacity_first)) >= 0.8) then count(flight.id)
end as high
from plane left join flight on (plane.airline = flight.airline and plane.tail_number = flight.plane) left join bookedAndDeparture on flight.id = bookedAndDeparture.flight_id
group by flight.id) numberOfFlight on numberOfFlight.id = flight.id
group by plane.airline, plane.tail_number
;