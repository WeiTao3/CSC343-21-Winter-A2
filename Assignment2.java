/* 
 * This code is provided solely for the personal and private use of students 
 * taking the CSC343H course at the University of Toronto. Copying for purposes 
 * other than this use is expressly prohibited. All forms of distribution of 
 * this code, including but not limited to public repositories on GitHub, 
 * GitLab, Bitbucket, or any other online platform, whether as given or with 
 * any changes, are expressly prohibited. 
*/ 

import java.sql.*;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.lang.*;

public class Assignment2 {
   /////////
   // DO NOT MODIFY THE VARIABLE NAMES BELOW.
   
   // A connection to the database
   Connection connection;

   // Can use if you wish: seat letters
   List<String> seatLetters = Arrays.asList("A", "B", "C", "D", "E", "F");

   Assignment2() throws SQLException {
      try {
         Class.forName("org.postgresql.Driver");
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      }
   }

  /**
   * Connects and sets the search path.
   *
   * Establishes a connection to be used for this session, assigning it to
   * the instance variable 'connection'.  In addition, sets the search
   * path to 'air_travel, public'.
   *
   * @param  url       the url for the database
   * @param  username  the username to connect to the database
   * @param  password  the password to connect to the database
   * @return           true if connecting is successful, false otherwise
   */
   public boolean connectDB(String URL, String username, String password) {
      // Implement this method!
      try {
        connection = DriverManager.getConnection(URL, username, password);
        String queryString = "set search_path to air_travel, public;";
        PreparedStatement pStatement = connection.prepareStatement(queryString);
        int rs = pStatement.executeUpdate();
      } catch (SQLException se) {
        return false;
      }
      return true;
   }

  /**
   * Closes the database connection.
   *
   * @return true if the closing was successful, false otherwise
   */
   public boolean disconnectDB() {
      try {
        connection.close();
      } catch (SQLException se) {
        return false;
      }
      return true;
   }
   
   /* ======================= Airline-related methods ======================= */

   /**
    * Attempts to book a flight for a passenger in a particular seat class. 
    * Does so by inserting a row into the Booking table.
    *
    * Read handout for information on how seats are booked.
    * Returns false if seat can't be booked, or if passenger or flight cannot be found.
    *
    * 
    * @param  passID     id of the passenger
    * @param  flightID   id of the flight
    * @param  seatClass  the class of the seat (economy, business, or first) 
    * @return            true if the booking was successful, false otherwise. 
    */
   public boolean bookSeat(int passID, int flightID, String seatClass) {
      // Implement this method!
      try {
        String queryString;
        PreparedStatement pStatement;

        ///// get price of the booking from price relation according to the flightID and seatClass
        queryString = String.format("select %1$s from price where flight_id = %2$d;", seatClass, flightID);
        pStatement = connection.prepareStatement(queryString);
        ResultSet rs = pStatement.executeQuery();
        int price = 0;
        while(rs.next()) {
          price = rs.getInt(seatClass);
        }
        // System.out.println("price: " + price);

        ///// get the new id of booking relation
        queryString = "select id from booking order by id;";
        pStatement = connection.prepareStatement(queryString);
        ResultSet countAll = pStatement.executeQuery();
        int newID = 0;
        while(countAll.next()){
          newID = countAll.getInt("id");
        }
        newID = newID + 1;
        // System.out.println("newID: " + newID);

        ///// get the current booking time
        Timestamp currentTime = getCurrentTimeStamp();

        /////// check the number of booked seats based on flight id and seat class
        queryString = String.format("select count(*) from booking where " + 
          "flight_id = %1$d and seat_class = '%2$s';", flightID, seatClass);
        pStatement = connection.prepareStatement(queryString);
        ResultSet countBooked = pStatement.executeQuery();
        int totalBooked = 0;
        while(countBooked.next()){
          totalBooked = countBooked.getInt("count");
        } 
        // System.out.println("totalBooked: " + totalBooked);

        /////// check the capacity each seat class (economy, business, first) based on flight id
        queryString = String.format("select capacity_economy,capacity_business," + 
          "capacity_first from flight join plane on plane.airline = " + 
          "flight.airline and plane.tail_number = flight.plane " + 
          "where flight.id = %d group by plane.tail_number;", flightID);
        pStatement = connection.prepareStatement(queryString);
        ResultSet capacity = pStatement.executeQuery();
        int capacityEconomy = 0;
        int capacityBusiness = 0;
        int capacityFirst = 0;
        while(capacity.next()){
          capacityEconomy = capacity.getInt("capacity_economy");
          capacityBusiness = capacity.getInt("capacity_business");
          capacityFirst = capacity.getInt("capacity_first");
        }
        int capacitySeatClass; 
        if(seatClass.equals("economy")) {
          capacitySeatClass = capacityEconomy;
        } else if (seatClass.equals("business")) {
          capacitySeatClass = capacityBusiness;
        } else {
          capacitySeatClass = capacityFirst;
        }
        // System.out.println("capacitySeatClass: " + capacitySeatClass);

        /////// Set new seat row and letter based on totalBooked, and capacity of each seat class
        if(totalBooked == 0) {
          int newRow;
          if(seatClass.equals("economy")) {
            newRow = (int) (Math.ceil(capacityFirst/6.0) + Math.ceil(capacityBusiness/6.0)) + 1;
          } else if (seatClass.equals("business")) {
            newRow = (int) (Math.ceil(capacityFirst/6.0)) + 1;
          } else {
            newRow = 1;
          }
          // System.out.println("if newRow: " + newRow);
          queryString = String.format("insert into booking values " + 
            "(%1$d, %2$d, %3$d, '%4$s', %5$d, '%6$s', '%7$s', 'A');",
             newID, passID, flightID, currentTime, price, seatClass, newRow);
          pStatement = connection.prepareStatement(queryString);
          int row = pStatement.executeUpdate();
        } else if (totalBooked >= capacitySeatClass) {
          if (!seatClass.equals("economy")) {
            return false;
          } else {
            if (totalBooked == (capacitySeatClass + 10)) {
              return false;
            } else {
              // System.out.println("No new row and letter");
              queryString = String.format("insert into booking values " + 
                "(%1$d, %2$d, %3$d, '%4$s', %5$d, '%6$s', null, null);", 
                newID, passID, flightID, currentTime, price, seatClass);
              pStatement = connection.prepareStatement(queryString);
              int row = pStatement.executeUpdate();
            }
          }
        } else {
          /////// get last row number amd letter 
          queryString = String.format("select row, letter from booking " + 
            "where flight_id = %1$d and seat_class = '%2$s' and row is not " + 
            "null order by row, letter;", flightID, seatClass);
          pStatement = connection.prepareStatement(queryString);
          ResultSet rowLetter = pStatement.executeQuery();
          int lastRow = 1;
          String lastLetter = "";
          while(rowLetter.next()) {
            lastRow = rowLetter.getInt("row");
            lastLetter = rowLetter.getString("letter");
          }
          /////// update new row number and letter based on last row number and letter
          int newRow;
          String newLetter;
          if(!lastLetter.equals("F")) {
            newRow = lastRow;
            newLetter = seatLetters.get(seatLetters.indexOf(lastLetter) + 1);
          } else {
            newRow = lastRow + 1;
            newLetter = "A";
          }
          // System.out.println("else newRow newLetter: " + newRow + newLetter);
          queryString = String.format("insert into booking values " + 
            "(%1$d, %2$d, %3$d, '%4$s', %5$d, '%6$s', %7$d, '%8$s');", 
            newID, passID, flightID, currentTime, price, seatClass, newRow, newLetter);
          pStatement = connection.prepareStatement(queryString);
          int row = pStatement.executeUpdate();

        }
        // String testString = String.format("select * from booking where flight_id = %d;", flightID);
        // PreparedStatement testStatement = connection.prepareStatement(testString);
        // ResultSet resultSet = testStatement.executeQuery();
        // while(resultSet.next()) {
        //   System.out.println("id: " + resultSet.getInt("id") + " " + "pass_id: " + resultSet.getInt("pass_id") + " " + "flight_id: " + resultSet.getInt("flight_id") + " " + "datetime: " + resultSet.getString("datetime") + " " + "price: " + resultSet.getInt("price") + " " + "seat_class: " + resultSet.getString("seat_class") + " " + "row: " + resultSet.getInt("row") + " " + "letter: " + resultSet.getString("letter"));
        // }
        return true;
      } catch (SQLException se) {
        // System.out.println("error in bookSeat");
        return false;
      }
   }

   /**
    * Attempts to upgrade overbooked economy passengers to business class
    * or first class (in that order until each seat class is filled).
    * Does so by altering the database records for the bookings such that the
    * seat and seat_class are updated if an upgrade can be processed.
    *
    * Upgrades should happen in order of earliest booking timestamp first.
    *
    * If economy passengers are left over without a seat (i.e. more than 10 overbooked passengers or not enough higher class seats), 
    * remove their bookings from the database.
    * 
    * @param  flightID  The flight to upgrade passengers in.
    * @return           the number of passengers upgraded, or -1 if an error occured.
    */
   public int upgrade(int flightID) {
      // Implement this method!
      String queryString;
      PreparedStatement pStatement;
      try {
        ////// Get all overbooked booking id and datetime
        queryString = String.format("select id, flight_id, datetime from " + 
          "booking where flight_id = %s and seat_class = 'economy' and row " + 
          "is null order by datetime;", flightID);
        pStatement = connection.prepareStatement(queryString);
        ResultSet overbooked = pStatement.executeQuery();

        /////// check the capacity of (business, first) seat class based on flight id
        queryString = String.format("select capacity_economy,capacity_business," + 
          "capacity_first from flight join plane on plane.airline = " + 
          "flight.airline and plane.tail_number = flight.plane where " + 
          "flight.id = %d group by plane.tail_number;", flightID);
        pStatement = connection.prepareStatement(queryString);
        ResultSet capacity = pStatement.executeQuery();
        int capacityBusiness = 0;
        int capacityFirst = 0;
        while(capacity.next()){
          capacityBusiness = capacity.getInt("capacity_business");
          capacityFirst = capacity.getInt("capacity_first");
        }
        // System.out.println("capacityBusiness: " + capacityBusiness + " capacityFirst: " + capacityFirst);
        int numberUpgraded = 0;
        while(overbooked.next()) {
          ////// get the number of booked seats in business seat class
          String businessString = String.format("select count(*) from booking " + 
            "where seat_class = 'business' and flight_id = %d;", flightID);
          PreparedStatement pStatementBusiness = connection.prepareStatement(businessString);
          ResultSet rs1 = pStatementBusiness.executeQuery();
          int bookedBusiness = 0;
          while(rs1.next()) {
            bookedBusiness = rs1.getInt("count");
          }
          // System.out.println("bookedBusiness: " + bookedBusiness);

          /////// get the number of booked seats in first seat class
          String firstString = String.format("select count(*) from booking " + 
            "where seat_class = 'first' and flight_id = %d;", flightID);
          PreparedStatement pStatementFirst = connection.prepareStatement(firstString);
          ResultSet rs2 = pStatementFirst.executeQuery();
          int bookedFirst = 0;
          while(rs2.next()) {
            bookedFirst = rs2.getInt("count");
          }
          // System.out.println("bookedFirst: " + bookedFirst);

          String newSeatClass = null;
          ResultSet rowLetter;
          if(bookedBusiness < capacityBusiness) {
            /////// get last row number amd letter of business seat class
            queryString = String.format("select row, letter from booking " + 
              "where flight_id = %1$d and seat_class = 'business' and row " + 
              "is not null order by row, letter;", flightID);
            pStatement = connection.prepareStatement(queryString);
            rowLetter = pStatement.executeQuery();
            newSeatClass = "business";
          } else {
            if(bookedFirst < capacityFirst) {
              /////// get last row number amd letter of first seat class
              queryString = String.format("select row, letter from booking " + 
                "where flight_id = %1$d and seat_class = 'first' and " + 
                "row is not null order by row, letter;", flightID);
              pStatement = connection.prepareStatement(queryString);
              rowLetter = pStatement.executeQuery();
              newSeatClass = "first";
            } else {
              String deleteString = String.format("delete from booking " + 
                "where id = %d and seat_class = 'economy';", overbooked.getInt("id"));
              PreparedStatement deleteStatement = connection.prepareStatement(deleteString);
              int deleteInt = deleteStatement.executeUpdate();
              
              // String testAfterDelete = String.format("select * from booking where flight_id = %d;", flightID);
              // PreparedStatement testStatementAfterDelete = connection.prepareStatement(testAfterDelete);
              // ResultSet resultAfterDelete = testStatementAfterDelete.executeQuery();
              // while(resultAfterDelete.next()) {
              //   System.out.println("id: " + resultAfterDelete.getInt("id") + " " 
              //     + "pass_id: " + resultAfterDelete.getInt("pass_id") + " " 
              //     + "flight_id: " + resultAfterDelete.getInt("flight_id") + " " 
              //     + "datetime: " + resultAfterDelete.getString("datetime") + " " 
              //     + "price: " + resultAfterDelete.getInt("price") + " " 
              //     + "seat_class: " + resultAfterDelete.getString("seat_class") + " " 
              //     + "row: " + resultAfterDelete.getInt("row") + " " 
              //     + "letter: " + resultAfterDelete.getString("letter"));
              // }
              continue;
            }
          }
          // System.out.println("newSeatClass: " + newSeatClass);
          int lastRow;
          if (newSeatClass.equals("business")) {
            lastRow = (int) (Math.ceil(capacityFirst/6.0)) + 1;
          } else {
            lastRow = 1;
          }
          String lastLetter = "";
          while(rowLetter.next()) {
            lastRow = rowLetter.getInt("row");
            lastLetter = rowLetter.getString("letter");
          }
          // System.out.println("lastRow: " + lastRow + " " + "lastLetter: " + lastLetter);

          /////// update new row number and letter based on last row number and letter
          int newRow;
          String newLetter;
          if(!lastLetter.equals("F")) {
            newRow = lastRow;
            newLetter = seatLetters.get(seatLetters.indexOf(lastLetter) + 1);
          } else {
            newRow = lastRow + 1;
            newLetter = "A";
          }
          // System.out.println("newRow: " + newRow + " " + "newLetter: " + newLetter);
          queryString = String.format("update booking set seat_class " + 
            "= '%1$s', row = %2$d, letter = '%3$s' where id = %4$d;", 
            newSeatClass, newRow, newLetter, overbooked.getInt("id"));
          pStatement = connection.prepareStatement(queryString);
          int row = pStatement.executeUpdate();
          numberUpgraded = numberUpgraded + 1;

          // String testString = String.format("select * from booking where flight_id = %d;", flightID);
          // PreparedStatement testStatement = connection.prepareStatement(testString);
          // ResultSet resultSet = testStatement.executeQuery();
          // while(resultSet.next()) {
          //   System.out.println("id: " + resultSet.getInt("id") + " " 
          //     + "pass_id: " + resultSet.getInt("pass_id") + " " 
          //     + "flight_id: " + resultSet.getInt("flight_id") + " " 
          //     + "datetime: " + resultSet.getString("datetime") + " " 
          //     + "price: " + resultSet.getInt("price") + " " 
          //     + "seat_class: " + resultSet.getString("seat_class") + " " 
          //     + "row: " + resultSet.getInt("row") + " " 
          //     + "letter: " + resultSet.getString("letter"));
          // }
        }
        return numberUpgraded;
      } catch (SQLException se) {
        return -1;
      }
   }


   /* ----------------------- Helper functions below  ------------------------- */

    // A helpful function for adding a timestamp to new bookings.
    // Example of setting a timestamp in a PreparedStatement:
    // ps.setTimestamp(1, getCurrentTimeStamp());

    /**
    * Returns a SQL Timestamp object of the current time.
    * 
    * @return           Timestamp of current time.
    */
   private java.sql.Timestamp getCurrentTimeStamp() {
      java.util.Date now = new java.util.Date();
      return new java.sql.Timestamp(now.getTime());
   }

   // Add more helper functions below if desired.


  
  /* ----------------------- Main method below  ------------------------- */

   public static void main(String[] args) {
      // You can put testing code in here. It will not affect our autotester.
      System.out.println("Running the code!");
      // try {
      //   Assignment2 assignment2 = new Assignment2();
      //   assignment2.connectDB("jdbc:postgresql://localhost:5432/csc343h-taowei3", "taowei3", "");

      //   String test = "delete from booking where id = 11 and seat_class = 'business';";
      //   PreparedStatement testStatement = assignment2.connection.prepareStatement(test);
      //   int testInt = testStatement.executeUpdate();

      //   boolean insert1 = assignment2.bookSeat(1, 10, "economy");
      //   boolean insert2 = assignment2.bookSeat(2, 10, "economy");
      //   System.out.println(insert1);
      //   System.out.println(insert2);
      //   int result = assignment2.upgrade(10);
      //   System.out.println(result);
  
      //   assignment2.disconnectDB();
      // } catch(SQLException se) {
      //   System.out.println("error!!!");
      // }
   }

}
