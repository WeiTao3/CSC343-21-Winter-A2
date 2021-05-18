-- Q2. Refunds!

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q2 CASCADE;

CREATE TABLE q2 (
    airline CHAR(2),
    name VARCHAR(50),
    year CHAR(4),
    seat_class seat_class,
    refund REAL
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.


-- Define views for your intermediate steps here:
DROP VIEW IF EXISTS refundID CASCADE;
create view refundID as
select flight.id, 
case
	when ((a1.country != a2.country) and (departure.datetime - flight.s_dep) >= '08:00:00' and (departure.datetime - flight.s_dep) < '12:00:00') then 0.35
	when ((a1.country != a2.country) and (departure.datetime - flight.s_dep) >= '12:00:00') then 0.50
	when ((a1.country = a2.country) and (departure.datetime - flight.s_dep) >= '05:00:00' and (departure.datetime - flight.s_dep) < '10:00:00') then 0.35
	when ((a1.country = a2.country) and (departure.datetime - flight.s_dep) >= '10:00:00') then 0.50
end as refund_percent
from flight left join airport a1 on flight.outbound = a1.code left join airport a2 on flight.inbound = a2.code, departure, arrival
where flight.id = departure.flight_id and flight.id = arrival.flight_id
and not (departure.datetime - flight.s_dep) / 2 >= (arrival.datetime - flight.s_arv)
and not (departure.datetime - flight.s_dep) < '05:00:00'
group by flight.id, a1.country, a2.country, departure.datetime, flight.s_dep
;

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q2

select airline.code as airline, airline.name, extract(year from s_dep) as year, seat_class, sum(price * refundID.refund_percent) as refund
from flight join booking on flight.id = booking.flight_id join airline on flight.airline = airline.code join refundID on refundID.id = flight.id
group by flight.id, seat_class, airline.code
;
