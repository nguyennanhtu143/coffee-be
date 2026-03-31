package org.example.coffee.mapper;

import org.example.coffee.dto.comment.CommentInput;
import org.example.coffee.entity.CommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface CommentMapper {
    CommentEntity getEntityFromInput(CommentInput commentInput);

    CommentEntity updateEntityFromInput(@MappingTarget CommentEntity commentEntity, CommentInput commentInput);
}
