package com.czdxwx.aiinterviewcoachbackend;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.utility.request.FlushReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MilvusServiceTester {



    private static String ZILLIZ_HOST="in03-3a34be53b23c3be.serverless.gcp-us-west1.cloud.zilliz.com";

    private static final String ZILLIZ_TOKEN="992b27b22b7c2360230aef06a89e647730868eac17f941fdc4af5874da35f9e2e10ea7581198c5e17b057b3e3fc7511906419700";
    private static final String QUESTIONS_COLLECTION = "interview_questions";
    private static final String TAGS_COLLECTION = "interview_tags";
    private static final int VECTOR_DIMENSION = 2560;
    // ▲▲▲▲▲▲▲▲ 请在这里填入您的 Zilliz Cloud 配置信息 ▲▲▲▲▲▲▲▲

    public static void main(String[] args) {
        System.out.println("--- Starting Standalone Milvus Service Test ---");
        MilvusClientV2 milvusClient = null;
        try {
            String uri = "https://" + ZILLIZ_HOST;
            ConnectConfig connectConfig = ConnectConfig.builder().uri(uri).token(ZILLIZ_TOKEN).build();
            milvusClient = new MilvusClientV2(connectConfig);
            System.out.println("✅ Step 1: Successfully created Milvus client and connected to " + uri);

            runTestForCollection(milvusClient, QUESTIONS_COLLECTION, "question_id", "question_vector", 99999L);
            // 您也可以取消注释下面这行来同时测试 tags 集合
            // runTestForCollection(milvusClient, TAGS_COLLECTION, "tag_id", "tag_vector", 88888L);

            System.out.println("\n🎉🎉🎉 --- ALL TESTS SUCCEEDED! --- 🎉🎉🎉");

        } catch (Exception e) {
            System.err.println("\n🔥🔥🔥 --- TEST FAILED --- 🔥🔥🔥");
            e.printStackTrace();
        } finally {
            if (milvusClient != null) {
                milvusClient.close();
            }
        }
    }

    private static void runTestForCollection(MilvusClientV2 client, String collectionName, String pkFieldName, String vectorFieldName, long testId) throws InterruptedException {
        System.out.println("\n--------------------------------------------------");
        System.out.printf("--- Testing Collection: %s ---\n", collectionName);

        ensureCollectionExists(client, collectionName, pkFieldName, vectorFieldName);
        System.out.printf("✅ Collection '%s' is ready.\n", collectionName);

        List<Float> testVector = generateRandomVector();
        insertVector(client, collectionName, pkFieldName, vectorFieldName, testId, testVector);
        System.out.printf("✅ Inserted test vector with ID: %d into '%s'.\n", testId, collectionName);

        Thread.sleep(2000);

        System.out.println("Searching for the vector we just inserted...");
        // 【核心修正】调用 search 时，确保使用 COSINE
        List<SearchResp.SearchResult> searchResults = search(client, collectionName, pkFieldName, vectorFieldName, testVector, 1);
        System.out.println("✅ Search completed.");

        System.out.println("Verifying search results...");
        if (searchResults.isEmpty()) {
            throw new RuntimeException("Verification failed: Search returned no results.");
        }

        SearchResp.SearchResult topResult = searchResults.get(0);
        long foundId = Long.parseLong(topResult.getId().toString());
        float score = topResult.getScore();

        System.out.println("Top search result: ID=" + foundId + ", Score (Distance)=" + score);

        if (foundId == testId && score == 0.0f) {
            System.out.printf("✅ Verification successful for collection '%s'!\n", collectionName);
        } else {
            throw new RuntimeException(String.format("Verification FAILED for collection '%s'. Expected ID: %d, Got: %d. Expected Score: 0.0, Got: %f", collectionName, testId, foundId, score));
        }
        System.out.println("--------------------------------------------------");
    }


    // --- 辅助方法 ---

    private static void insertVector(MilvusClientV2 client, String collectionName, String pkFieldName, String vectorFieldName, Long entityId, List<Float> vector) {
        JsonObject row = new JsonObject();
        row.addProperty(pkFieldName, entityId);
        row.add(vectorFieldName, new Gson().toJsonTree(vector));
        client.insert(InsertReq.builder().collectionName(collectionName).data(Collections.singletonList(row)).build());
        client.flush(FlushReq.builder().collectionNames(Collections.singletonList(collectionName)).build());
    }

    /**
     * 【核心修正】search 方法现在不再调用 .check()
     */
    private static List<SearchResp.SearchResult> search(MilvusClientV2 client, String collectionName, String pkFieldName, String vectorFieldName, List<Float> vector, int topK) {
        SearchReq searchReq = SearchReq.builder()
                .collectionName(collectionName)
                .data(Collections.singletonList(new FloatVec(vector)))
                .outputFields(Collections.singletonList(pkFieldName))
                .topK(topK)
                .metricType(IndexParam.MetricType.L2)
                .build();

        // 直接调用，如果出错，SDK内部会抛出异常
        SearchResp searchResp = client.search(searchReq);

        // 根据您提供的 SearchResp 源码，其 getSearchResults() 返回 List<List<SearchResult>>
        return searchResp.getSearchResults().get(0);
    }

    private static void ensureCollectionExists(MilvusClientV2 client, String collectionName, String pkFieldName, String vectorFieldName) {
        if (!client.hasCollection(HasCollectionReq.builder().collectionName(collectionName).build())) {
            CreateCollectionReq.FieldSchema idField = CreateCollectionReq.FieldSchema.builder().name(pkFieldName).dataType(DataType.Int64).isPrimaryKey(true).autoID(false).build();
            CreateCollectionReq.FieldSchema vectorField = CreateCollectionReq.FieldSchema.builder().name(vectorFieldName).dataType(DataType.FloatVector).dimension(VECTOR_DIMENSION).build();
            client.createCollection(CreateCollectionReq.builder().collectionName(collectionName).collectionSchema(CreateCollectionReq.CollectionSchema.builder().fieldSchemaList(List.of(idField, vectorField)).build()).build());
            IndexParam indexParam = IndexParam.builder().fieldName(vectorFieldName).indexType(IndexParam.IndexType.AUTOINDEX).metricType(IndexParam.MetricType.L2).build();
            client.createIndex(CreateIndexReq.builder().collectionName(collectionName).indexParams(Collections.singletonList(indexParam)).build());
        }
        client.loadCollection(LoadCollectionReq.builder().collectionName(collectionName).build());
    }

    private static List<Float> generateRandomVector() {
        Random rand = new Random();
        List<Float> vector = new ArrayList<>(VECTOR_DIMENSION);
        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            vector.add(rand.nextFloat());
        }
        return vector;
    }
}