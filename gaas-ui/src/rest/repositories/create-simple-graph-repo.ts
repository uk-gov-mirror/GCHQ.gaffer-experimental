import { StoreType } from '../../domain/store-type';
import { IApiResponse, RestClient } from '../clients/rest-client';
import { ICreateSimpleGraphRequestBody } from '../http-message-interfaces/request-interfaces';

export class CreateSimpleGraphRepo {
    public async create(graphId: string, description: string, storeType: StoreType): Promise<void> {
        const httpRequestBody: ICreateSimpleGraphRequestBody = {
            graphId: graphId,
            description: description,
            storeType: storeType,
        };

        const response: IApiResponse<undefined> = await new RestClient()
            .post()
            .graphs()
            .requestBody(httpRequestBody)
            .execute();
    }
}
