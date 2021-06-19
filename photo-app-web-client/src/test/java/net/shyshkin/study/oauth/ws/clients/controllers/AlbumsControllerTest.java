package net.shyshkin.study.oauth.ws.clients.controllers;

import net.shyshkin.study.oauth.ws.clients.dto.AlbumDto;
import net.shyshkin.study.oauth.ws.clients.services.AlbumsService;
import net.shyshkin.study.oauth.ws.clients.services.HardcodedAlbumsService;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

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
}