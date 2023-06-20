package com.goodjob.domain.article.entity;

import com.goodjob.domain.BaseEntity;
import com.goodjob.domain.comment.entity.Comment;
import com.goodjob.domain.hashTag.entity.HashTag;
import com.goodjob.domain.likes.entity.Likes;
import com.goodjob.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Article extends BaseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    private Member member;

    @OneToMany(mappedBy = "article", cascade = {CascadeType.ALL})
    private List<Comment> commentList;

    @Setter
    private String title;

    @Setter
    @Column(columnDefinition = "text")
    private String content;

    @Setter
    private Long viewCount;

    @Setter
    private boolean isDeleted;

    @OneToMany(mappedBy = "article", cascade = {CascadeType.ALL})
    @Builder.Default
    private List<Likes> likesList = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = {CascadeType.ALL})
    @Builder.Default
    private List<HashTag> hashTagList = new ArrayList<>();
}
