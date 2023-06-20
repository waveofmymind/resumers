package com.goodjob.domain.article.service;

import com.goodjob.domain.article.dto.request.ArticleRequestDto;
import com.goodjob.domain.article.dto.response.ArticleResponseDto;
import com.goodjob.domain.article.entity.Article;
import com.goodjob.domain.article.mapper.ArticleMapper;
import com.goodjob.domain.article.repository.ArticleRepository;
import com.goodjob.domain.comment.dto.response.CommentResponseDto;
import com.goodjob.domain.comment.entity.Comment;
import com.goodjob.domain.file.service.FileService;
import com.goodjob.domain.hashTag.service.HashTagService;
import com.goodjob.domain.likes.mapper.LikesMapper;
import com.goodjob.domain.member.entity.Member;
import com.goodjob.domain.subComment.dto.response.SubCommentResponseDto;
import com.goodjob.domain.subComment.entity.SubComment;
import com.goodjob.global.base.rsData.RsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final LikesMapper likesMapper;
    private final HashTagService hashTagService;
    private final FileService fileService;


    @Transactional(readOnly = true)
    public Page<ArticleResponseDto> findAll(int page, int sortCode, String category, String query) {
        Pageable pageable = PageRequest.of(page, 10);

        List<Article> articles = articleRepository.findQslBySortCode(sortCode, category, query);

        List<ArticleResponseDto> articleResponseDtos = articles
                .stream()
                .map(article -> {
                    ArticleResponseDto articleResponseDto = articleMapper.toDto(article);
                    countCommentsAndSubComments(articleResponseDto);
                    return articleResponseDto;
                })
                .collect(Collectors.toList());

        return convertToPage(articleResponseDtos, pageable);
    }

    private Page<ArticleResponseDto> convertToPage(List<ArticleResponseDto> articles, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), articles.size());

        List<ArticleResponseDto> content = articles.subList(start, end);
        return new PageImpl<>(content, pageable, articles.size());
    }

    private void countCommentsAndSubComments(ArticleResponseDto articleResponseDto) {
        List<CommentResponseDto> commentList = articleResponseDto.getCommentList();
        Long sum = 0L;

        for(CommentResponseDto commentResponseDto : commentList) {
            if (!commentResponseDto.isDeleted()) {
                sum++;
                List<SubCommentResponseDto> subCommentList = commentResponseDto.getSubCommentList();
                for(SubCommentResponseDto subComment : subCommentList) {
                    if(!subComment.isDeleted()) {
                        sum++;
                    }
                }
            }
        }

        articleResponseDto.setCommentsCount(sum);
    }

    @Transactional
    public RsData getArticleResponseDto(Long id) {
        RsData<Article> articleRsData = getArticle(id);

        if(articleRsData.isFail()) {
            return articleRsData;
        }

        Article article = articleRsData.getData();

        ArticleResponseDto articleResponseDto = increaseViewCount(article);
        countCommentsAndSubComments(articleResponseDto);

        return RsData.of("S-1", "게시글에 대한 정보를 가져옵니다.", articleResponseDto);
    }

    private ArticleResponseDto increaseViewCount(Article article) {
        Long viewCount = article.getViewCount();
        article.setViewCount(viewCount + 1);
        ArticleResponseDto articleResponseDto = articleMapper.toDto(article);
        return articleResponseDto;
    }

    public RsData getArticle(Long id) {
        Optional<Article> articleOp = articleRepository.findQslById(id);

        if(articleOp.isEmpty()) {
            return RsData.of("F-1", "해당 게시글이 존재하지 않습니다.");
        }

        Article article = articleOp.get();

        if(article.isDeleted()) {
            return RsData.of("F-2", "해당 게시글은 이미 삭제되었습니다.");
        }


        return RsData.of("S-1", "게시글에 대한 정보를 가져옵니다.", article);
    }

    public RsData createArticle(Member author, ArticleRequestDto articleRequestDto) {
        if(articleRequestDto.getTitle().trim().equals("")) {
            return RsData.of("F-1", "제목을 입력해야 합니다.");
        }

        if(articleRequestDto.getContent().trim().equals("")) {
            return RsData.of("F-2", "내용을 입력해야 합니다.");
        }

        if(articleRequestDto.getTitle().trim().length() > 30) {
            return RsData.of("F-3", "제목은 30자 이내로 작성해야 합니다.");
        }

        Article article = Article
                .builder()
                .member(author)
                .commentList(null)
                .title(articleRequestDto.getTitle())
                .content(articleRequestDto.getContent())
                .viewCount(0L)
                .isDeleted(false)
                .likesList(null)
                .build();

        articleRepository.save(article);

        hashTagService.applyHashTags(article, articleRequestDto.getHashTagStr());

        return RsData.of("S-1", "게시글을 성공적으로 생성하였습니다.", article);
    }

    @Transactional
    public RsData updateArticle(Member author, Long id, ArticleRequestDto articleRequestDto) {
        RsData<Article> articleRsData = getArticle(id);

        if(articleRsData.isFail()) {
            return articleRsData;
        }



        Article article = articleRsData.getData();

        if(article.getMember().getId() != author.getId()) {
            return RsData.of("F-3", "수정 권한이 없습니다.");
        }

        if(articleRequestDto.getTitle().trim().equals("")) {
            return RsData.of("F-4", "제목을 입력해야 합니다.");
        }

        if(articleRequestDto.getContent().trim().equals("")) {
            return RsData.of("F-5", "내용을 입력해야 합니다.");
        }

        if(articleRequestDto.getTitle().trim().length() > 30) {
            return RsData.of("F-6", "제목은 30자 이내로 작성해야 합니다.");
        }

        article.setTitle(articleRequestDto.getTitle());
        article.setContent(articleRequestDto.getContent());
        hashTagService.applyHashTags(article, articleRequestDto.getHashTagStr());

        return RsData.of("S-1", "게시글이 수정되었습니다.", article);
    }

    @Transactional
    public RsData deleteArticle(Member author, Long id) {
        RsData<Article> articleRsData = getArticle(id);

        if(articleRsData.isFail()) {
            return articleRsData;
        }

        Article article = articleRsData.getData();

        if(article.getMember().getId() != author.getId()) {
            return RsData.of("F-3", "삭제 권한이 없습니다.");
        }

        List<Comment> commentList = article.getCommentList();

        for(Comment comment : commentList) {
            comment.setDeleted(true);

            List<SubComment> subCommentList = comment.getSubCommentList();

            for(SubComment subComment : subCommentList) {
                subComment.setDeleted(true);
            }
        }

        article.setDeleted(true);

        return RsData.of("S-1", "게시글이 삭제되었습니다.", article);
    }

}
