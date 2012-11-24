package edu.sjsu.videolibrary.db;

import java.sql.SQLException;
import java.util.ArrayList;

import edu.sjsu.videolibrary.exception.InternalServerException;
import edu.sjsu.videolibrary.exception.NoCategoryFoundException;
import edu.sjsu.videolibrary.exception.NoMovieFoundException;
import edu.sjsu.videolibrary.exception.NoMovieInCategoryException;
import edu.sjsu.videolibrary.model.Movie;

public class StoredProcMovieDAO extends BaseMovieDAO {

	@Override
	public String createNewMovie(String movieName, String movieBanner,
			String releaseDate, int availableCopies, int categoryId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String deleteMovie(String movieId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String returnMovie(int membershipId, String movieId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] listCategories() throws NoCategoryFoundException,
			InternalServerException {
		ArrayList<String> list= new ArrayList<String>();
		try{
			String query = "call videolibrary.get_categories()";
			stmt.execute(query);
			rs=stmt.getResultSet();
			if(!rs.isBeforeFirst())
				throw new NoCategoryFoundException("There are no categories to be displayed");
			while(rs.next()){
				list.add(rs.getString(1));
			}
			rs.close();
			stmt.close();
		}catch(SQLException e){
			throw new InternalServerException("Internal error has occurred.");
		}
		String[] categoryName = list.toArray(new String[list.size()]);
		return categoryName;
	}

	@Override
	public Movie[] listMoviesByCategory(String categoryName)
			throws NoMovieInCategoryException, InternalServerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Movie[] listAllMovies() throws NoMovieFoundException,
			InternalServerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Movie[] searchByName(String userInput) throws NoMovieFoundException,
			InternalServerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Movie[] searchByMovieBanner(String userInput)
			throws NoMovieFoundException, InternalServerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Movie[] searchByReleaseDate(String userInput)
			throws NoMovieFoundException, InternalServerException {
		// TODO Auto-generated method stub
		return null;
	}

}
