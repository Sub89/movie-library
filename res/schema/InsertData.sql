INSERT INTO VideoLibrary.Category(categoryName)
Values('Drama');
INSERT INTO VideoLibrary.Category(categoryName)
Values('Romance');
INSERT INTO VideoLibrary.Category(categoryName)
Values('Comedy');
INSERT INTO VideoLibrary.Category(categoryName)
Values('Thriller');
INSERT INTO VideoLibrary.Category(categoryName)
Values('Documentary');


INSERT INTO VideoLibrary.Movie(movieName,movieBanner,releaseDate,availableCopies,categoryId)
VALUES ('Ganesha','Ashtavinayak',NOW(),5,1);

INSERT INTO VideoLibrary.Movie(movieName,movieBanner,releaseDate,availableCopies,categoryId)
VALUES ('Jaws','Spielberg',NOW(),5,4);

INSERT INTO VideoLibrary.Movie(movieName,movieBanner,releaseDate,availableCopies,categoryId)
VALUES ('Titanic','Paramount',NOW(),5,2);

INSERT INTO VideoLibrary.Movie(movieName,movieBanner,releaseDate,availableCopies,categoryId)
VALUES ('Funny','ABC',NOW(),5,3);

INSERT INTO VideoLibrary.Movie(movieName,movieBanner,releaseDate,availableCopies,categoryId)
VALUES ('Space','NASA',NOW(),5,5);

insert into videolibrary.user(userId,password,membershipType,startDate,firstName,lastName,address,city,state,zip)
values('sr@yahoo.com','sr123','simple','2012-11-17','Shri','Ram','Newark Street','SanJose','CA','96587');

select * from videolibrary.user;


