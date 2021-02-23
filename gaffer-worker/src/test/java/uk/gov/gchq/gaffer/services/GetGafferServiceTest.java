package uk.gov.gchq.gaffer.services;

import com.google.gson.Gson;
import io.kubernetes.client.openapi.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.gchq.gaffer.AbstractTest;
import uk.gov.gchq.gaffer.model.Graph;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/*
 * Copyright 2021 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@SpringBootTest
class GetGafferServiceTest extends AbstractTest {

    @MockBean
    private CustomObjectsApiService customObjectsApiService;

    @Autowired
    private GetGafferService getGafferService;

    @Autowired
    private DeleteGraphService deleteGraphService;

    @Test
    void testGetGraphs() throws ApiException {

        final String graphRequest = "{\"graphId\":\"" + TEST_GRAPH_ID + "\",\"description\":\"" + TEST_GRAPH_DESCRIPTION + "\"}";
        Gson g = new Gson();
        Graph graph = g.fromJson(graphRequest, Graph.class);
        List<Graph> graphList = new ArrayList<>();
        graphList.add(graph);
        when(customObjectsApiService.getAllGraphs()).thenReturn(graphList);
        List<Graph> graphs = getGafferService.getGraphs();

        assertEquals(TEST_GRAPH_ID, graphs.get(0).getGraphId());
        assertEquals(TEST_GRAPH_DESCRIPTION, graphs.get(0).getDescription());
        assertArrayEquals(graphList.toArray(), graphs.toArray());

    }
}