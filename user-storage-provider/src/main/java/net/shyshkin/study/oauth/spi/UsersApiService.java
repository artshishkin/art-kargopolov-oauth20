package net.shyshkin.study.oauth.spi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
//@Produces(MediaType.APPLICATION_JSON)
public interface UsersApiService {

    @GET
    @Path("/{userName}")
    User getUserDetails(@PathParam("userName") String userName);

}
