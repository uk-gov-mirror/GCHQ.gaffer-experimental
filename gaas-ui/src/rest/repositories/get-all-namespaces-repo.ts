import { IApiResponse, RestClient } from "../clients/rest-client";
import { IAllNameSpacesResponse } from "../http-message-interfaces/response-interfaces";

export class GetAllNamespacesRepo {
    public async getAll(): Promise<string[]> {
        const response: IApiResponse<IAllNameSpacesResponse> = await new RestClient().get().namespaces().execute();
        return response.data;
    }
}
