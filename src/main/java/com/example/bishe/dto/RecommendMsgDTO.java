package com.example.bishe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendMsgDTO {
    private Long userId;
    private Integer score;

    public RecommendMsgDTO(Long userId){
        this.userId = userId;
    }
}
