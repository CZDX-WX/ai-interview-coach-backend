package com.czdxwx.aiinterviewcoachbackend.service;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.param.R;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.exception.MilvusClientException;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.utility.request.FlushReq;
import io.milvus.v2.service.vector.request.GetReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.GetResp;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class MilvusService {

    private static final Logger logger = LoggerFactory.getLogger(MilvusService.class);
    private MilvusClientV2 milvusClient;

    @Value("${milvus.host}")
    private String host;
    @Value("${milvus.token}")
    private String token;
    @Value("${milvus.collection.questions}")
    private String questionsCollectionName;
    @Value("${milvus.collection.tags}")
    private String tagsCollectionName;
    @Value("${milvus.collection.dimension}")
    private Integer dimension;

    private final Gson gson = new Gson();

    @PostConstruct
    private void init() {
        String uri = "https://" + host;
        final ConnectConfig connectConfig = ConnectConfig.builder().uri(uri).token(token).build();
        this.milvusClient = new MilvusClientV2(connectConfig);
        ensureCollectionExists(questionsCollectionName, "question_id", "question_vector");
        ensureCollectionExists(tagsCollectionName, "tag_id", "tag_vector");
    }

    // --- 公开业务方法 ---
    public void insertQuestionVector(Long questionId, List<Float> vector) {
        insert(questionsCollectionName, "question_id", "question_vector", questionId, vector);
    }

    public List<SearchResp.SearchResult> searchQuestions(List<Float> vector, int topK) {
        return search(questionsCollectionName, "question_id", vector, topK);
    }

    public void insertTagVector(Long tagId, List<Float> vector) {
        insert(tagsCollectionName, "tag_id", "tag_vector", tagId, vector);
    }

    public List<SearchResp.SearchResult> searchTags(List<Float> vector, int topK) {
        return search(tagsCollectionName, "tag_id", vector, topK);
    }

    /**
     * 【基于源码的最终修正版】根据主键ID列表，批量获取向量。
     */
    public List<List<Float>> getVectorsByIds(String collectionName, String pkFieldName, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        GetReq getReq = GetReq.builder()
                .collectionName(collectionName)
                .ids(new ArrayList<>(ids)) // 转换为 List<Object>
                .outputFields(List.of("vector"))
                .build();

        // 【修正】直接接收 GetResp，不再有R<>包装器，也不再需要 check() 或 RpcUtils
        GetResp getResp = milvusClient.get(getReq);

        List<List<Float>> vectors = new ArrayList<>();

        if (getResp != null && getResp.getGetResults() != null) {
            // 【修正】根据您提供的 QueryResult 源码，使用 .getEntity() 获取数据
            for (QueryResp.QueryResult queryResult : getResp.getGetResults()) {
                Map<String, Object> entityMap = queryResult.getEntity();
                if (entityMap != null && entityMap.containsKey("vector")) {
                    Object vectorData = entityMap.get("vector");
                    if (vectorData instanceof List) {
                        vectors.add((List<Float>) vectorData);
                    }
                }
            }
        }
        return vectors;
    }

    // --- 私有辅助方法 ---

    private List<SearchResp.SearchResult> search(String collectionName, String pkFieldName, List<Float> vector, int topK) {
        milvusClient.loadCollection(LoadCollectionReq.builder().collectionName(collectionName).build());
        List<BaseVector> searchVectors = Collections.singletonList(new FloatVec(vector));

        SearchReq searchReq = SearchReq.builder()
                .collectionName(collectionName)
                .data(Collections.singletonList(new FloatVec(vector)))
                .outputFields(Collections.singletonList(pkFieldName))
                .topK(topK)
                .metricType(IndexParam.MetricType.COSINE) // <-- 核心修正
                .build();

        // 【修正】直接接收 SearchResp，不再需要 R<> 包装器
        SearchResp searchResp = milvusClient.search(searchReq);

        // 根据您提供的 SearchResp 源码，其 getSearchResults() 返回 List<List<SearchResult>>
        return searchResp.getSearchResults().get(0);
    }

    // insert 和 ensureCollectionExists 方法无需改动，但为了完整性一并提供
    private void insert(String collectionName, String pkFieldName, String vectorFieldName, Long entityId, List<Float> vector) {
        JsonObject row = new JsonObject();
        row.addProperty(pkFieldName, entityId);
        row.add(vectorFieldName, gson.toJsonTree(vector));
        List<JsonObject> rows = Collections.singletonList(row);
        milvusClient.insert(InsertReq.builder().collectionName(collectionName).data(rows).build());
        milvusClient.flush(FlushReq.builder().collectionNames(Collections.singletonList(collectionName)).build());
    }

    private void ensureCollectionExists(String collectionName, String pkFieldName, String vectorFieldName) {
        if (!milvusClient.hasCollection(HasCollectionReq.builder().collectionName(collectionName).build())) {
            logger.info("Milvus collection '{}' not found, creating...", collectionName);
            CreateCollectionReq.FieldSchema idField = CreateCollectionReq.FieldSchema.builder().name(pkFieldName).dataType(DataType.Int64).isPrimaryKey(true).autoID(false).build();
            CreateCollectionReq.FieldSchema vectorField = CreateCollectionReq.FieldSchema.builder().name(vectorFieldName).dataType(DataType.FloatVector).dimension(dimension).build();
            milvusClient.createCollection(CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(CreateCollectionReq.CollectionSchema.builder().fieldSchemaList(List.of(idField, vectorField)).build())
                    .build());
            IndexParam indexParam = IndexParam.builder()
                    .fieldName(vectorFieldName)
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .metricType(IndexParam.MetricType.COSINE) // <-- 核心修正
                    .build();
            milvusClient.createIndex(CreateIndexReq.builder().collectionName(collectionName).indexParams(Collections.singletonList(indexParam)).build());
        }
        milvusClient.loadCollection(LoadCollectionReq.builder().collectionName(collectionName).build());
    }
}