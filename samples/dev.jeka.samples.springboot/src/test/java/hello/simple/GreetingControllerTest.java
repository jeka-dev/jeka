package hello.simple;

import hello.GreetingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(GreetingController.class)
public class GreetingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GreetingController service;

    @Test
    public void returnHello() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/greeting"))
                .andExpect(MockMvcResultMatchers.status().is(200));
    }

}
