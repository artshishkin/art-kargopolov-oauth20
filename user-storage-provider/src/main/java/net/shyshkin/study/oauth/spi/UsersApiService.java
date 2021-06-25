package net.shyshkin.study.oauth.spi;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
public interface UsersApiService {

    @GET
    @Path("/{userName}")
    User getUserDetails(@PathParam("userName") String userName);

    @POST
    @Path("/{userName}/verify-password")
    @Produces(MediaType.APPLICATION_JSON)
    VerifyPasswordResponse verifyUserPassword(
            @PathParam("userName") String userName,
            String password);
}
