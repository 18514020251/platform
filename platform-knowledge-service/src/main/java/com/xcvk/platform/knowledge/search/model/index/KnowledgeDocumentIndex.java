package com.xcvk.platform.knowledge.search.model.index;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档es对象
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24 9:26
 */
@Data
@Accessors(chain = true)
@Document(indexName = "kb_document", writeTypeHint = WriteTypeHint.FALSE)
public class KnowledgeDocumentIndex implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     * */
    @Id
    private Long id;

    /*
    * 文档id
    * */
    @Field(type = FieldType.Long)
    private Long documentId;

    /*
    * 标题
    * */
    @Field(type = FieldType.Text , analyzer = "ik_max_word" , searchAnalyzer = "ik_smart")
    private String title;

    /*
    * 摘要
    * */
    @Field(type = FieldType.Text , analyzer = "ik_max_word" , searchAnalyzer = "ik_smart")
    private String summary;

    /*
     * 内容
     * */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /*
    * 类别id
    * */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /*
    * 类别名称
    * */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String categoryName;

    /*
    * 标签
    * */
    @Field(type = FieldType.Keyword)
    private String tags;

    /*
    * 状态
    * */
    @Field(type = FieldType.Keyword)
    private String status;

    /*
    * 创建人id
    * */
    @Field(type = FieldType.Long)
    private Long creatorId;

    /*
    * 创建人名称
    * */
    @Field(type = FieldType.Keyword)
    private String creatorName;

    /*
    * 创建时间
    * */
    @Field(type = FieldType.Date ,  format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    /*
    * 修改时间
    * */
    @Field(type = FieldType.Date , format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;

    /*
    * 发布时间
    * */
    @Field(type =  FieldType.Date , format = DateFormat.date_hour_minute_second)
    private LocalDateTime publishedAt;


}
