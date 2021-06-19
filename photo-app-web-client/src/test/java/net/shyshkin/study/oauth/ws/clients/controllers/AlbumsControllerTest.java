package net.shyshkin.study.oauth.ws.clients.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AlbumsController.class)
class AlbumsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void getAllAlbums() throws Exception {

        //when
        mockMvc
                .perform(get("/albums"))

                //then
                .andExpect(status().isOk())
                .andExpect(view().name("albums"));

    }
}