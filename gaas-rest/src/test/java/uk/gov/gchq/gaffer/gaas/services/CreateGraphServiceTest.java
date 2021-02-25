/*
 * Copyright 2020 Crown Copyright
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
package uk.gov.gchq.gaffer.gaas.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.gchq.gaffer.gaas.exception.GaaSRestApiException;
import uk.gov.gchq.gaffer.gaas.model.CRDClient;
import uk.gov.gchq.gaffer.gaas.model.Graph;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class CreateGraphServiceTest {

    @Autowired
    private CreateGraphService createGraphService;

    @MockBean
    private CRDClient crdClient;

    @Test
    public void createGraph_shouldCall() throws GaaSRestApiException {
        createGraphService.createGraph(new Graph("myGraph", "Another description"));

        final String response = "{\"apiVersion\":\"gchq.gov.uk/v1\",\"kind\":\"Gaffer\",\"metadata\":{\"name\":\"myGraph\"},\"spec\":{\"graph\":{\"config\":{\"graphId\":\"myGraph\",\"description\":\"Another description\"}}}}";
        final JsonObject jsonRequestBody = new JsonParser().parse(response).getAsJsonObject();
        verify(crdClient, times(1)).createCRD(jsonRequestBody);
    }
}
