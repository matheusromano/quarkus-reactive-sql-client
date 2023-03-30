package org.javatheus;

import java.net.URI;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

@Path("movies")
public class MovieResource {
    
    @Inject
    PgPool client;

    @PostConstruct
    void config() {
        initdb();
    }

    @GET
    public Multi<Movie> get() {
        return Movie.findAll(client);
    }

    @GET
    @Path("{id}")
    public Uni<Response> get(@PathParam("id") Long id)  {
        return Movie.findById(client, id)
        .onItem()
        .transform(movie -> movie != null ? Response.ok(movie) : Response.status(Response.Status.NOT_FOUND))
        .onItem()
        .transform(Response.ResponseBuilder::build);
    }

    @POST
    public Uni<Response> create(Movie movie) {
        return movie.save(client, movie.getTitle())
        .onItem()
        .transform(id -> URI.create("/movies/" + id))
        .onItem()
        .transform(uri -> Response.created(uri).build());
    }

    @DELETE
    @Path("{id}")
    public Uni<Response> delete(@PathParam("id") Long id) {
        return Movie.delete(client, id)
        .onItem()
        .transform(deleted -> deleted ? Response.noContent() : Response.status(Response.Status.NOT_FOUND))
        .onItem()
        .transform(Response.ResponseBuilder::build);
    }

      


    private void initdb() {
        client.query("DROP TABLE IF EXISTS movies").execute()
        .flatMap(m-> client.query("CREATE TABLE movies (id SERIAL PRIMARY KEY, title TEXT NOT NULL)").execute())
        .flatMap(m -> client.query("INSERT INTO movies (title) VALUES ('The Matrix')").execute())
        .flatMap(m -> client.query("INSERT INTO movies (title) VALUES ('The Matrix Reloaded')").execute())
        .flatMap(m -> client.query("INSERT INTO movies (title) VALUES ('The Matrix Revolutions')").execute())
        .await().indefinitely();
    }
}
