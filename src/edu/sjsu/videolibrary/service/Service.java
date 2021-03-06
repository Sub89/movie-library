package edu.sjsu.videolibrary.service;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.jws.WebService;
import edu.sjsu.videolibrary.model.Admin;
import edu.sjsu.videolibrary.model.Movie;
import edu.sjsu.videolibrary.model.PaymentForPremiumMemInfo;
import edu.sjsu.videolibrary.model.StatementInfo;
import edu.sjsu.videolibrary.model.States;
import edu.sjsu.videolibrary.model.Transaction;
import edu.sjsu.videolibrary.model.User;
import edu.sjsu.videolibrary.model.ItemOnCart;
import edu.sjsu.videolibrary.util.Utils;
import edu.sjsu.videolibrary.db.BaseAdminDAO;
import edu.sjsu.videolibrary.db.BaseCartDAO;
import edu.sjsu.videolibrary.db.BaseMovieDAO;
import edu.sjsu.videolibrary.db.BaseUserDAO;
import edu.sjsu.videolibrary.db.Cache;
import edu.sjsu.videolibrary.db.DAOFactory;
import edu.sjsu.videolibrary.db.VideoLibraryDAO;
import edu.sjsu.videolibrary.exception.InternalServerException;
import edu.sjsu.videolibrary.exception.InvalidCreditCardException;
import edu.sjsu.videolibrary.exception.InvalidUserInputException;
import edu.sjsu.videolibrary.exception.ItemAlreadyInCartException;
import edu.sjsu.videolibrary.exception.MovieAlreadyExistsException;
import edu.sjsu.videolibrary.exception.NoCategoryFoundException;
import edu.sjsu.videolibrary.exception.NoMovieFoundException;
import edu.sjsu.videolibrary.exception.NoMovieInCategoryException;
import edu.sjsu.videolibrary.exception.NoUserFoundException;
import edu.sjsu.videolibrary.exception.UserAlreadyExistsException;
@WebService

public class Service {
	private static final int MOVIE_LIMIT_FOR_SIMPLE_MEMBERS = 2;
	private static final int MOVIE_LIMIT_FOR_PREMIUM_MEMBERS = 8;
	
	Cache cache = Cache.getInstance();

	// Add movies to shopping cart	
	public boolean addItemsToCart(int membershipId, int movieId) {
		boolean isAddedToCart = false;

		BaseCartDAO cartDAO = DAOFactory.getCartDAO();
		try {
			cartDAO.startTransaction();
			cartDAO.addToCart(movieId, membershipId);
			System.out.println("Added to cart successfully");
			cartDAO.commitTransaction();

			isAddedToCart = true;
			cache.invalidate("viewCart"+membershipId);
		} catch (ItemAlreadyInCartException e) {
			System.out.println(e.getMessage());
			try {
				cartDAO.commitTransaction();
			} catch (InternalServerException e1) {
				e.getMessage();
			}
		} catch (InternalServerException e) {
			e.getMessage();
			try {
				cartDAO.rollbackTransaction();
			} catch (InternalServerException e1) {
				e.getMessage();
			}
		}
		finally{
			cartDAO.release();
		}

		return isAddedToCart;
	}

	// Delete item from cart
	public boolean deleteMovieFromCart (int movieId, int membershipId) {
		boolean isDeletedFromCart = false;

		BaseCartDAO cartDAO = DAOFactory.getCartDAO();
		try {
			cartDAO.startTransaction();
			cartDAO.deleteFromCart(movieId, membershipId);
			cartDAO.commitTransaction();

			isDeletedFromCart = true;
		} catch (InternalServerException e) {
			e.getMessage();
			try {
				cartDAO.rollbackTransaction();
			} catch (InternalServerException e1) {
				e.getMessage();
			}
		}
		finally{
			cartDAO.release();
		}

		return isDeletedFromCart;
	}

	/*
	private ItemOnCart[] viewCartInternal(int membershipId, BaseCartDAO cartDAO){
		List<ItemOnCart> cartItemsList;
		ItemOnCart[] cartItems = null;

		cartItems = (ItemOnCart[])cache.get("viewCart" + membershipId);
		if(cartItems == null){
			try {
				cartItemsList	= cartDAO.listCartItems(membershipId);
				cartItems = new ItemOnCart[cartItemsList.size()];
				for (int i = 0; i < cartItemsList.size(); i++) {
					cartItems[i] = cartItemsList.get(i);
				}	
				cache.put("viewCart" + membershipId, cartItems);
			} catch (InternalServerException e) {
				e.getMessage();
			}
		}
		return cartItems;
	}*/

	// View Cart 
	public ItemOnCart[] viewCart(int membershipId) {
		BaseCartDAO cartDAO = DAOFactory.getCartDAO();
		try {
			return viewCartInternal(membershipId, cartDAO);
		} finally {
			cartDAO.release();
		}
	}
	/* OLD CODE FOR VIEW CART (With rent amount for membershiptype handled)*/
	 public ItemOnCart[] viewCartInternal(int membershipId, BaseCartDAO cartDAO) {
		List<ItemOnCart> cartItemsList;
		ItemOnCart[] cartItems = null;
		double rentAmount = 0;
		cartItems = (ItemOnCart[])cache.get("viewCart" + membershipId);
		if(cartItems == null){
			try {
				// cartDAO.startTransaction(); // This is a view function so no transaction required
				cartItemsList	= cartDAO.listCartItems(membershipId);
				cartItems = new ItemOnCart[cartItemsList.size()];
				System.out.println("I've got " + cartItems.length + " items with me!");
				BaseUserDAO userDAO = DAOFactory.getUserDAO(cartDAO);
				User user =	userDAO.queryMembershipType(membershipId);
				for (int i = 0; i < cartItemsList.size(); i++) {
					cartItems[i] = cartItemsList.get(i);
					if (user.getMembershipType().equals("Simple")) {
						rentAmount = cartItems[i].getRentAmount();
						System.out.println("rentAmount = " + rentAmount);
					} 
					cartItems[i].setRentAmount(rentAmount);
					System.out.println(cartItems[i].getMovieName());
				}	
				cache.put("viewCart" + membershipId, cartItems);
			} catch (InternalServerException e) {
				e.getMessage();
			} /*finally {
				// This is a view function, so no commit required
				try {
					cartDAO.commitTransaction();
				} catch (InternalServerException e) {
					e.getMessage();
				}
			}*/
		}
		System.out.println("I'm done");
		return cartItems;
	}

	private boolean validateCreditCard (String cc) {
		boolean isValid = false;
		if (cc != null && cc.length() == 16) {
			isValid = true;
		}
		return isValid;
	}
	// Check out cart
	public boolean checkOutMovieCart(int membershipId, String creditCardNumber) {

		double totalAmount = 0;
		boolean processComplete = false;
		int limitOfMovies;
		int availableCopiesOfMovie = 0;
		int userRentedMovies = 0;
		String cardNumber = "";
		BaseCartDAO cartDAO = DAOFactory.getCartDAO();
		BaseMovieDAO movieDAO = DAOFactory.getMovieDAO(cartDAO);
		BaseUserDAO userDAO = DAOFactory.getUserDAO(cartDAO);
		try {
			cartDAO.startTransaction();

			User user = userDAO.queryMembershipType(membershipId);
			String membershipType = user.getMembershipType();

			// Checking movie Limit with membership type

			cardNumber = creditCardNumber;
			if (membershipType.equalsIgnoreCase("Simple")) {
				limitOfMovies = MOVIE_LIMIT_FOR_SIMPLE_MEMBERS;

				cardNumber = creditCardNumber;
				if (cardNumber == null) {
					System.out.println("Simple User must enter cc number");
				}
			} else {
				limitOfMovies = MOVIE_LIMIT_FOR_PREMIUM_MEMBERS;
				cardNumber = userDAO.queryCreditCardNumber(membershipId);
				System.out.println("Credit card number for this premium user = " + cardNumber);
			}

			// Check credit card

			boolean isCardValid = false;			
			isCardValid = validateCreditCard(cardNumber);
			if (isCardValid) {
				ItemOnCart[] cartItems = viewCart(membershipId); 
				// Get membership type and check the limit for movies. 				
				userRentedMovies = user.getRentedMovies();
				if (cartItems.length > limitOfMovies && userRentedMovies >= limitOfMovies) {
					System.out.println("Exceeded limit of movies. Please delete some items from cart.");
				} else {
					int[] movieId = new int[cartItems.length];
					double[] rentAmount = new double[cartItems.length];
					for (int i = 0; i< cartItems.length; i++) {
						if (cartItems[i] != null) {
							movieId[i] = cartItems[i].getMovieId();
							availableCopiesOfMovie = movieDAO.getAvailableCopies(movieId[i]);
							if (membershipType.equalsIgnoreCase("Simple")) {
								rentAmount[i] = cartItems[i].getRentAmount();
								totalAmount = totalAmount + rentAmount[i];
							} else {
								totalAmount = 25;
							}
							System.out.println("Movie Id [" + i + "] = " + movieId[i] );
						}
					}
					if (availableCopiesOfMovie != 0) {
						int transactionId = cartDAO.recordPaymentTransaction(totalAmount, membershipId);
						for (int i = 0; i < movieId.length; i++) {
							cartDAO.recordMovieTransaction(movieId[i], transactionId, membershipId);
							movieDAO.updateCopiesCount(movieId[i],availableCopiesOfMovie - 1 );
							userRentedMovies = userRentedMovies + 1;
							userDAO.updateRentedMoviesForUser(membershipId, userRentedMovies);
						}
						cartDAO.deleteCart(membershipId);
					} else {
						System.out.println("Currently stock unavailable. Please try later or add some other movie");
					}
				}
			}
			cartDAO.commitTransaction();
			cache.invalidate("viewCart"+membershipId);
			processComplete = true;
		} catch (InternalServerException e) {
			e.getMessage();
			try {
				cartDAO.rollbackTransaction();
			} catch (InternalServerException e1) {
				e.getMessage();
			}
		} catch (InvalidCreditCardException e) {
			e.getMessage();
			System.out.println("Invalid credit card number");
		}
		finally{
			cartDAO.release();
			if( movieDAO != null ) {
				movieDAO.release();
			}
			cache.invalidate("listAllMovies");
			cache.invalidatePrefix("searchMovie");
			cache.invalidatePrefix("listMoviesByCategory");
		}
		return processComplete;
	}

	// Return Movie

	public boolean returnMovie (int membershipId, int movieId) {
		int numOfCopies = 0;
		int rentedMovies = 0;
		boolean isMovieReturned = false;
		BaseCartDAO cartDAO = DAOFactory.getCartDAO();
		try {
			cartDAO.startTransaction();
			BaseMovieDAO movieDAO = DAOFactory.getMovieDAO(cartDAO);
			BaseUserDAO userDAO = DAOFactory.getUserDAO(cartDAO);
			cartDAO.updateReturnDate(movieId, membershipId);
			numOfCopies = movieDAO.getAvailableCopies(movieId) + 1;
			movieDAO.updateCopiesCount(movieId, numOfCopies);
			User user = userDAO.queryMembershipType(membershipId);
			rentedMovies = user.getRentedMovies() - 1;
			userDAO.updateRentedMoviesForUser(membershipId, rentedMovies);
			isMovieReturned = true;
			cartDAO.commitTransaction();
		} catch (InternalServerException e) {
			e.getMessage();
			try {
				cartDAO.rollbackTransaction();
			} catch (InternalServerException e1) {
				e.getMessage();
			}
		} finally {
			cache.invalidate("listAllMovies");
			cache.invalidatePrefix("searchMovie");
			cache.invalidatePrefix("listMoviesByCategory");
		}
		return isMovieReturned;
	}

	public String signUpUser (String userId, String password, String memType,String firstName, String lastName, 
			String address, String city,String state, String zipCode,String ccNumber){
		String result = null;
		BaseUserDAO userDAO  = DAOFactory.getUserDAO();
		boolean isValidEmail = Utils.isValidEMailAddress(userId);
		try{
			//if (isValidEmail) {
				result = userDAO.signUpUser(userId, password, memType, firstName, lastName, address, city, state, zipCode, ccNumber);
			//} else {
			//	throw new InvalidUserInputException("Invalid Email input");
			//}
			
		}
		catch(UserAlreadyExistsException e){
			System.out.println(e.getLocalizedMessage());
			return "duplicate";
		} /*catch(InvalidUserInputException e) {
			System.out.println(e.getLocalizedMessage());
		}*/
		catch(SQLException e){
			System.out.println(e.getMessage());
			result = "false";
		} 
		finally{
			userDAO.release();
		}
		return result;
	}

	public String signUpAdmin (String userId, String password, String firstName, String lastName) throws SQLException
	{
		BaseUserDAO userDAO  = DAOFactory.getUserDAO();
		String result = null;
		//boolean isValidEmail = Utils.isValidEMailAddress(userId);
		try {
			//if (isValidEmail) {
				result = userDAO.signUpAdmin(userId, password, firstName, lastName);
			//} else {
			//	throw new InvalidUserInputException("Invaild email id");
			//}
		} catch(UserAlreadyExistsException e){
			System.out.println(e.getLocalizedMessage());		
		} catch(SQLException e){
			System.out.println(e.getMessage());
			result = "false";
		} /*catch (InvalidUserInputException e) {
			System.out.println(e.getLocalizedMessage());
		} */ finally {
			userDAO.release();
		}
		return result;
	}

	public User signInUser(String userId, String password) throws SQLException
	{
		User user = new User();
		BaseUserDAO userDAO  = DAOFactory.getUserDAO();
		try{
			user = (User)cache.get("signInUser"+userId+password);
			if(user == null){		
				user = userDAO.signInUser(userId, password);
				if(user != null){
					cache.put("signInUser"+userId+password, user);
				}
			}
		} catch (NoUserFoundException e) {
			System.out.println(e.getMessage());
		}
		finally{
			userDAO.release();
		}
		return user;
	}
	public Admin signInAdmin (String userId, String password) throws SQLException
	{
		BaseAdminDAO adminDAO  = DAOFactory.getAdminDAO();
		Admin admin = null;
		try{
			admin =  adminDAO.signInAdminObject(userId, password);
		} catch (NoUserFoundException e) {
			System.out.println(e.getMessage());
		}
		finally{
			adminDAO.release();
		}
		return admin;
	}

	//List members
	public User [] viewMembers (String type) {	
		return viewMembersByPage (type, 0, VideoLibraryDAO.DEFAULT_BATCH_SIZE);

	}
	public User[] viewMembersByPage (String type, int offset, int count) {
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		List <User> memberList = adminDAO.listMembers(type, offset, count);
		User [] members = memberList.toArray(new User[0]);
		System.out.println(memberList.size() + " members listed");
		adminDAO.release();
		return members;
	}	
	public Admin [] viewAdmins () {	
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		List <Admin> memberList = adminDAO.listAdmins();
		Admin [] admins = memberList.toArray(new Admin[0]);
		adminDAO.release();
		return admins;
	}


	//Delete an user and admin account
	public String deleteUserAccount (String userId) {
		String isDeleted = "false"; 
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		try {
			isDeleted = adminDAO.deleteUser(userId);
		} catch (Exception e) { 
			System.out.println(e.getMessage());
			e.getMessage();			
		}
		finally{
			adminDAO.release();
		}
		return isDeleted; 
	}

	public String deleteAdminAccount (String userId) {
		String isDeleted = "false"; 
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		try {
			isDeleted = adminDAO.deleteAdmin(userId);
		} catch (Exception e) { 
			System.out.println(e.getMessage());
			e.getMessage();			
		}
		finally{
			adminDAO.release();
		}
		return isDeleted; 
	}	


	//Create and delete movie

	public String createNewMovie (String movieName, String movieBanner, String releaseDate, int availableCopies, int categoryId)  { 
		String isCreated = "false";
		cache.invalidate("listAllMovies");
		cache.invalidatePrefix("searchMovie");
		cache.invalidatePrefix("listMoviesByCategory");
		BaseMovieDAO movieDAO = DAOFactory.getMovieDAO();
		try {
			if (availableCopies < 0 || categoryId < 1) {
				throw new InvalidUserInputException ("Invalid input from user");
			} 
			isCreated = movieDAO.createNewMovie(movieName, movieBanner, releaseDate, availableCopies, categoryId);
		} catch (MovieAlreadyExistsException e) {
			System.out.println(e.getLocalizedMessage());
		}catch (InvalidUserInputException e) { 
			System.out.println(e.getLocalizedMessage());
		}	
		finally{
			movieDAO.release();
		}
		return isCreated;
	}

	public String deleteMovie (String movieId) {
		String isDeleted = "false"; 
		BaseMovieDAO movieDAO = DAOFactory.getMovieDAO();
		try {
			isDeleted = movieDAO.deleteMovie(movieId);
		} catch (Exception e) { 
			System.out.println(e.getMessage());
			e.printStackTrace();			
		}		
		finally{
			cache.invalidate("listAllMovies");
			cache.invalidatePrefix("searchMovie");
			cache.invalidatePrefix("listMoviesByCategory");
			movieDAO.release();
		}
		return isDeleted; 
	}

	public User displayUserInformation (int membershipId){
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		User user = adminDAO.displayUserInformation(membershipId);
		adminDAO.release();
		return user;
	}

	public Movie displayMovieInformation (int movieId){
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		Movie movie = adminDAO.displayMovieInformation(movieId);
		adminDAO.release();
		return movie;
	}

	public Transaction[] viewAccountTransactions(int membershipId){
		BaseUserDAO userDAO  = DAOFactory.getUserDAO();
		LinkedList<Transaction> ac = null;
		try {
			ac = userDAO.viewAccountTransactions(membershipId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.getMessage();
		}
		finally{
			userDAO.release();
		}
		Transaction[] trans = ac.toArray(new Transaction[0]);


		return trans;
	}
	
	public Transaction[] moviesToReturn(int membershipId) {
		BaseUserDAO userDAO  = DAOFactory.getUserDAO();
		LinkedList<Transaction> ac = null;
		try {
			ac = userDAO.viewMoviesToReturn(membershipId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.getMessage();
		}
		finally{
			userDAO.release();
		}
		Transaction[] trans = ac.toArray(new Transaction[0]);

		return trans;
	}

	public String makeMonthlyPayment(int membershipId){
		BaseUserDAO userDAO  = DAOFactory.getUserDAO();
		String result = null;
		try {
			result = userDAO.makeMonthlyPayment(membershipId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			userDAO.release();
		}
		return result;
	}

	public String updateUserInfo(int membershipId,String userId,String firstName, String lastName, String address, String city, String state, String zipCode, String membershipType,String creditCardNumber){
		BaseUserDAO userDAO  = DAOFactory.getUserDAO();
		String result =  null;
		if (userId == null || firstName == null || lastName == null || address == null || city == null || zipCode == null || creditCardNumber == null) {
			System.out.println("Incomplete user information. Missing details");
			return "Incomplete form";
		}
		try {
			result = userDAO.updateUserInfo(membershipId, userId, firstName, lastName, address, city, state, zipCode, membershipType, creditCardNumber);
		} catch (SQLException e) {
			if( e.getMessage().contains("Duplicate entry")) {
				return "duplicate";
			}
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		finally{
			cache.invalidatePrefix("signInUser"+userId);
			cache.invalidatePrefix("searchUser");
			userDAO.release();
		}
		return result;
	}

	public String updatePassword(int membershipId,String oldPassword,String newPassword){
		if (oldPassword == null || newPassword == null) {
			System.out.println("Incomplete user information. Missing details");
			return "Incomplete form";
		}
		BaseUserDAO userDAO  = DAOFactory.getUserDAO();
		String result = null;
		try {
			result = userDAO.updatePassword(membershipId, oldPassword, newPassword);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			cache.invalidatePrefix("signInUser"); //invalidates caching for all users
			userDAO.release();
		}

		return result;
	}

	public String updateMovieInfo(int movieId,String movieName, String movieBanner, String releaseDate, int availableCopies, int categoryId){
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		try {
			if (availableCopies < 0 || categoryId < 0) {
				throw new InvalidUserInputException ("Invalid user Input");
			} 
			String result = adminDAO.updateMovieInfo(movieId, movieName, movieBanner, releaseDate, availableCopies, categoryId);
			adminDAO.release();
			return result;
		} 
		
		catch (InvalidUserInputException e) {
			System.out.println(e.getLocalizedMessage());
			return e.getMessage();
		} finally {
			cache.invalidate("listAllMovies");
			cache.invalidatePrefix("searchMovie");
			cache.invalidatePrefix("listMoviesByCategory");
		}
	}

//	public String generateMonthlyStatement(int membershipId,int month,int year){
//		String result = null;
//		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
//		try {
//			result = adminDAO.generateMonthlyStatement(membershipId, -1, month, year);
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		finally{
//			adminDAO.release();
//		}
//		return result;
//	}

	public StatementInfo[] viewStatement(int membershipId,int month,int year){
		BaseUserDAO userDAO  = DAOFactory.getUserDAO();
		StatementInfo[] stmnt = null;
		try {
			stmnt = userDAO.viewStatement(membershipId, month, year).toArray(new StatementInfo[0]);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			userDAO.release();
		}
		return stmnt;
	}

	public PaymentForPremiumMemInfo generateMonthlyBillForPremiumMember(int membershipId,int month,int year){
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		PaymentForPremiumMemInfo pymnt = adminDAO.generateMonthlyBillForPremiumMember(membershipId, month, year);
		adminDAO.release();
		return pymnt;
	}

	public double getRentAmountforMovie(){
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		double movieRentAmount = adminDAO.getRentAmountforMovie();
		adminDAO.release();
		return movieRentAmount;
	}

	public double getMonthlyFeesForPremiumMember(){
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		double monthlyFees = adminDAO.getMonthlyFeesForPremiumMember();
		adminDAO.release();
		return monthlyFees;
	}

	//List categories on home page
	public String[] listCategories(){
		String[] categoryName = null;
		categoryName = (String[])cache.get("listCategories");
		if( categoryName == null ) {
			BaseMovieDAO movieDAO = DAOFactory.getMovieDAO();
			try {
				categoryName = movieDAO.listCategories();
				cache.put("listCategories", categoryName);
			} catch (NoCategoryFoundException e) {
				e.getLocalizedMessage();
				e.printStackTrace();
			} catch (InternalServerException e) {
				e.getLocalizedMessage();
				e.printStackTrace();
			}
			finally{
				movieDAO.release();
			}
		}
		return categoryName;
	}

	//List movies by chosen category
	public Movie[] listMoviesByCategory(String categoryName) {
		return listMoviesByCategoryByPage(categoryName, 0, VideoLibraryDAO.DEFAULT_BATCH_SIZE);
	}
	
	//List movies by chosen category
	public Movie[] listMoviesByCategoryByPage(String categoryName,int start,int stop) {
		Movie[] array = null;
		array = (Movie[]) cache.get("listMoviesByCategory" + categoryName + start + stop);
		if( array == null ) {
			BaseMovieDAO movieDAO = DAOFactory.getMovieDAO();
			try {
				array = movieDAO.listMoviesByCategory(categoryName,start,stop);
				cache.put("listMoviesByCategory" + categoryName + start + stop, array);
			} catch (NoMovieInCategoryException e) {
				e.getLocalizedMessage();
				e.printStackTrace();
			} catch (InternalServerException e) {
				e.getLocalizedMessage();
				e.printStackTrace();
			}
			finally{
				movieDAO.release();
			}
		}
		return array;
	}

	//Display all Movies
	public Movie[] listAllMovies(){
		return listAllMoviesByPage(0, VideoLibraryDAO.DEFAULT_BATCH_SIZE);
	}
	
	public Movie[] listAllMoviesByPage(int start, int stop){
		Movie[] array=null;
		array = (Movie[]) cache.get("listAllMovies" +start + stop);
		if( array == null ) {
			BaseMovieDAO movieDAO = DAOFactory.getMovieDAO();
			try {
				array = movieDAO.listAllMovies(start,stop);
				cache.put("listAllMovies" +start + stop, array);
			} catch (NoMovieFoundException e) {
				e.getLocalizedMessage();
				e.printStackTrace();
			} catch (InternalServerException e) {
				e.getLocalizedMessage();
				e.printStackTrace();
			}
			finally{
				movieDAO.release();
			}

		}
		return array;
	}


	//search movies by name


	// Search User by any attribute 

	public User[] searchUser (String membershipId, String userId,
			String membershipType, String startDate, String firstName,
			String lastName, String address, String city, String state,
			String zipCode) {

		return searchUserByPage ( membershipId,  userId,
				 membershipType,  startDate,  firstName,
				 lastName, address,  city,  state,
				 zipCode, 0, VideoLibraryDAO.DEFAULT_BATCH_SIZE);
		
	}
	
	public User[] searchUserByPage (String membershipId, String userId,
			String membershipType, String startDate, String firstName,
			String lastName, String address, String city, String state,
			String zipCode, int offset, int count) {
		User[] users = null;
		users = (User[])cache.get("searchUser"+membershipId+ userId+ membershipType+startDate+ firstName+ lastName+ address+ city+ state+ zipCode);
		if(users == null){
			BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
			try {
				users = adminDAO.searchUser(membershipId, userId, membershipType, startDate, firstName, lastName, address, city, state, zipCode, offset, count);
				cache.put("searchUser"+membershipId+ userId+ membershipType+startDate+ firstName+ lastName+ address+ city+ state+ zipCode, users);
			} catch (NoUserFoundException e) {
				System.out.println(e.getLocalizedMessage());
			} finally {
				adminDAO.release();
			}
		}
		return users;
		
	}

	public Admin displayAdminInformation (String adminId) {
		BaseAdminDAO adminDAO = DAOFactory.getAdminDAO();
		Admin admin = null;
		try {
			admin = adminDAO.displayAdminInformation(adminId);
		} catch (NoUserFoundException e) {
			System.out.println(e.getLocalizedMessage());
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		finally{
			adminDAO.release();
		}
		return admin; 
	}

	public String updateAdminInfo (String adminId,String firstName, String lastName, String password){
		BaseAdminDAO adminDAO  = DAOFactory.getAdminDAO();
		String result = "false";
		try {
			result = adminDAO.updateAdminInfo(adminId, firstName, lastName, password);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			adminDAO.release();
		}
		return result;
	}


	public String updateUserPassword (int membershipId, String newPassword) {
		BaseAdminDAO adminDAO  = DAOFactory.getAdminDAO();
		String result = null;
		try {
			result = adminDAO.updateUserPassword(membershipId, newPassword);
			cache.invalidatePrefix("signInUser"); //invalidate caching for all users
		} catch (SQLException e) {

			e.printStackTrace();
		}
		finally{
			adminDAO.release();
		}
		return result;		
	}

	public String [] getStates () { 
		States states = new States();
		return states.getStates();
	}
	
	public Movie[] searchMovieByPage(String movieName, String movieBanner, String releaseDate, int start, int stop){
		Movie[] array = null;
		array = (Movie[])cache.get("searchMovie" + movieName + movieBanner + releaseDate + start + stop);
		if( array == null ) {
			BaseMovieDAO movieDAO = DAOFactory.getMovieDAO();
			try {
				array = movieDAO.searchMovie(movieName, movieBanner, releaseDate, start, stop);
				cache.put("searchMovie" + movieName + movieBanner + releaseDate, array);
			} catch (Exception e) {			
				e.printStackTrace();
			}
			finally{
				movieDAO.release();
			}
		}
		return array;
	}

	public Movie[] searchMovie(String movieName, String movieBanner, String releaseDate){
		return searchMovieByPage(movieName, movieBanner, releaseDate,0 , VideoLibraryDAO.DEFAULT_BATCH_SIZE);
	}

}

