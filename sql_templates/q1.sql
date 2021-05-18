-- Q1. Airlines

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q1 CASCADE;

CREATE TABLE q1 (
    pass_id INT,
    name VARCHAR(100),
    airlines INT
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;


-- Define views for your intermediate steps here:


-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q1

select passenger.id as pass_id, passenger.firstname || ' ' || passenger.surname as name, count(flight.airline) as airlines
from passenger join booking on passenger.id = booking.pass_id left join arrival on booking.flight_id = arrival.flight_id left join flight on flight.id = arrival.flight_id
group by passenger.id;