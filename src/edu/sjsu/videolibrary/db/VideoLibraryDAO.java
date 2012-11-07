package edu.sjsu.videolibrary.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class VideoLibraryDAO {
	protected Connection con = null;

	public VideoLibraryDAO(){
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/marketplace","root","1248");
			con.createStatement();
			if (!con.isClosed()) {
				System.out.println("");
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			e.getMessage();
		} catch (SQLException e) {
			e.printStackTrace();
			e.getMessage();
		} catch (InstantiationException e) {
			e.printStackTrace();
			e.getMessage();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
}
