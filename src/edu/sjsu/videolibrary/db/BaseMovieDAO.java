package edu.sjsu.videolibrary.db;

import edu.sjsu.videolibrary.exception.InternalServerException;
import edu.sjsu.videolibrary.exception.MovieAlreadyExistsException;
import edu.sjsu.videolibrary.exception.NoCategoryFoundException;
import edu.sjsu.videolibrary.exception.NoMovieFoundException;
import edu.sjsu.videolibrary.exception.NoMovieInCategoryException;
import edu.sjsu.videolibrary.model.Movie;

public abstract class BaseMovieDAO extends VideoLibraryDAO {

	public BaseMovieDAO() {
		super();
	}

	public BaseMovieDAO(VideoLibraryDAO dao) {
		super(dao);
	}

	public abstract String createNewMovie (String movieName, String movieBanner, String releaseDate, int availableCopies, int categoryId) throws MovieAlreadyExistsException;

	public abstract String deleteMovie (String movieId) ;

	public abstract String returnMovie(int membershipId, String movieId);

	/* Main method for testing 
	public static void main (String [] args) { 
		MovieDAO m = new MovieDAO();
		m.deleteMovie("10");
	}
	 */
	//List categories on home page
	public abstract String[] listCategories() throws NoCategoryFoundException, InternalServerException;
	
	public abstract Movie[] listMoviesByCategory(String categoryName, int start, int stop) throws NoMovieInCategoryException, InternalServerException;

	//Display all Movies
	public Movie[] listAllMovies() throws NoMovieFoundException,InternalServerException{
		return listAllMovies(0,DEFAULT_BATCH_SIZE);
	}
	
	public abstract Movie[] listAllMovies(int start,int stop) throws NoMovieFoundException,InternalServerException;

	
	public abstract String updateCopiesCount(int movieId, int numOfCopies) throws InternalServerException ;
	
	public abstract int getAvailableCopies (int movieId);
	
	public abstract Movie[] searchMovie(String movieName,String movieBanner, String releaseDate, int start, int stop) throws Exception;
}


