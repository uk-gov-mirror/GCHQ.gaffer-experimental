import { Graph } from "../../domain/graph";
import { IApiResponse, RestClient } from "../clients/rest-client";
import { IGraphByIdResponse } from "../http-message-interfaces/response-interfaces";

export class GetGraphRepo {
    public async get(graphId: string): Promise<Graph> {
        const response: IApiResponse<IGraphByIdResponse> = await new RestClient().get().graphs(graphId).execute();

        return new Graph(response.data.graphId, response.data.description);
    }
}
