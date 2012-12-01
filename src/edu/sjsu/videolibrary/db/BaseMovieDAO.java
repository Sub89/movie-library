package edu.sjsu.videolibrary.db;

import edu.sjsu.videolibrary.exception.InternalServerException;
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

	public abstract String createNewMovie (String movieName, String movieBanner, String releaseDate, int availableCopies, int categoryId);

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

	//List movies by chosen category
	public Movie[] listMoviesByCategory(String categoryName) throws NoMovieInCategoryException, InternalServerException {
		return listMoviesByCategory(categoryName,0, DEFAULT_BATCH_SIZE);
	}
	
	public abstract Movie[] listMoviesByCategory(String categoryName, int start, int stop) throws NoMovieInCategoryException, InternalServerException;

	//Display all Movies
	public abstract Movie[] listAllMovies() throws NoMovieFoundException,InternalServerException;

	
	public abstract void updateCopiesCount(int movieId, int numOfCopies) throws InternalServerException ;
	
	public abstract int getAvailableCopies (int movieId);
	
	public Movie[] searchMovie(String movieName,String movieBanner, String releaseDate) throws Exception {
		return searchMovie(movieName, movieBanner, releaseDate, 0 , DEFAULT_BATCH_SIZE );
	}
	
	public abstract Movie[] searchMovie(String movieName,String movieBanner, String releaseDate, int start, int stop) throws Exception;
}


