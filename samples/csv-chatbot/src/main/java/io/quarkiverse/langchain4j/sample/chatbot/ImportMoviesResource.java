package io.quarkiverse.langchain4j.sample.chatbot;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;

@ApplicationScoped
@Path("/movies")
public class ImportMoviesResource {

    @Inject
    ProducerTemplate producerTemplate;

    @GET
    @Path("now")
    public Response nowPlaying() {
        producerTemplate.sendBody("direct:import-movies-playing-now", null);
        return Response.ok().build();
    }

}
