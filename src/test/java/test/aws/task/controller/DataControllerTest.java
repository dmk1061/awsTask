package test.aws.task.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import test.aws.task.service.DataService;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(SpringExtension.class)
@WebMvcTest(DataController.class)
@AutoConfigureMockMvc
public class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    DataService dataService;
    @Test
    public void testSetUserId() throws Exception {
        String userId = "user123";
        String id = "123";
        String data = "some data";

        ResultActions resultActions = mockMvc.perform(post("/set/{userId}/{id}", userId, id)
                .contentType("application/json")
                .content(data));

        resultActions.andExpect(status().isOk());
    }
    @Test
    public void testGetUserId() throws Exception {
        String userId = "user123";
        String id = "123";

        ResultActions resultActions = mockMvc.perform(get("/get/"+ userId+"/" + id));

        resultActions.andExpect(status().isOk());
    }

}