package net.shyshkin.study.oauth.ws.clients.controllers;

import net.shyshkin.study.oauth.ws.clients.dto.AlbumDto;
import net.shyshkin.study.oauth.ws.clients.services.AlbumsService;
import net.shyshkin.study.oauth.ws.clients.services.HardcodedAlbumsService;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlbumsController.class)
class AlbumsControllerTest {

    @SpyBean(HardcodedAlbumsService.class)
    AlbumsService albumsService;

    @Autowired
    MockMvc mockMvc;

    @Test
    @Disabled("Test broken because of included security")
    void getAllAlbums() throws Exception {

        //when
        mockMvc
                .perform(get("/albums"))

                //then
                .andExpect(status().isOk())
                .andExpect(view().name("albums"))
                .andExpect(model().attributeExists("albums"))
                .andExpect(
                        model().attribute("albums",
                                allOf(
                                        instanceOf(List.class),
                                        IsCollectionWithSize.hasSize(2),
                                        everyItem(instanceOf(AlbumDto.class)),
                                        everyItem(HasPropertyWithValue.hasProperty("description", startsWith("WebAlbumDescription")))
                                )
                        )
                )
        ;

    }

    @Test
    void getAllAlbums_redirect() throws Exception {

        //given
        String redirectUrl = "http://localhost:8080/auth/realms/katarinazart/protocol/openid-connect/auth?response_type=code&client_id=photo-app-webclient&scope=openid%20profile%20roles&state=760bKRabxF2PV02_q5dxMPIwiP4fIq2cZ9Cbsf0txHU%3D&redirect_uri=http://localhost:8050/login/oauth2/code/photo-app-webclient&nonce=zaPtbH-AlgMzvbUHtnK_18J8Jx88FrS_4bLVzV7dTUg";

        //when
        mockMvc
                .perform(get("/albums"))

                //then
                .andExpect(status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrlPattern("http://*/oauth2/authorization/photo-app-web-client"));
        ;

    }
}