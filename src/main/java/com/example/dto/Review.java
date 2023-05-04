package com.example.dto;

import java.util.Date;

import lombok.Data;

@Data
public class Review {

	// 신청번호 (PK & FK)
	private long no;
	// 제목
	private String title;
	// 내용
	private String content;
	// 평점(1,2,3,4,5)
	private int rating;
	// 리뷰 등록 일자
	private Date regdate;

}
