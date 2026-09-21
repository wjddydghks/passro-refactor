package com.passro.passrobackend.global.configuration;

public final class SwaggerSuccessExamples {

    private SwaggerSuccessExamples() {
    }

    public static final String ACCOUNT_OK =
            "{\"success\":true,\"code\":\"ACCOUNT200_1\",\"message\":\"요청 성공.\",\"result\":null}";
    public static final String ACCOUNT_LOGIN_SUCCESS = """
    {
      "code": "ACCOUNT200_1",
      "message": "요청 성공",
      "result": {
        "accessToken": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzg1MDcwMjk0LCJleHAiOjE3ODUwNzM4OTQsInJvbGUiOiJVU0VSIn0...",
        "refreshToken": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzg1MDcwMjk0LCJleHAiOjE3ODYyNzk4OTR9..."
      },
      "success": true
    }
    """;
    public static final String ACCOUNT_REISSUE_SUCCESS = ACCOUNT_LOGIN_SUCCESS;  // 구조 동일하니 재사용 가능
    public static final String FILE_URL =
            "{\"success\":true,\"code\":\"FILE200_1\",\"message\":\"요청 성공.\",\"result\":\"https://s3.example.com/file?signature=...\"}";
    public static final String INQUIRY_CREATED =
            "{\"success\":true,\"code\":\"INQUIRY201_1\",\"message\":\"문의 등록 성공.\",\"result\":{\"inquiryId\":1,\"deliveryId\":1,\"category\":\"DELAY\",\"title\":\"배송 문의\",\"content\":\"배송 진행 상황을 알고 싶습니다.\",\"writerNickname\":\"passro\",\"createdAt\":\"2026-07-24T00:00:00\"}}";
    public static final String INQUIRY_LIST =
            "{\"success\":true,\"code\":\"INQUIRY200_1\",\"message\":\"문의 조회 성공.\",\"result\":[{\"inquiryId\":1,\"deliveryId\":1,\"category\":\"DELAY\",\"title\":\"배송 문의\",\"content\":\"배송 진행 상황을 알고 싶습니다.\",\"writerNickname\":\"passro\",\"createdAt\":\"2026-07-24T00:00:00\"}]}";
    public static final String REVIEW_CREATED =
            "{\"success\":true,\"code\":\"REVIEW201_1\",\"message\":\"리뷰 작성 성공\",\"result\":\"리뷰 작성이 완료되었습니다.\"}";
    public static final String REVIEW_AVERAGE =
            "{\"success\":true,\"code\":\"REVIEW200_1\",\"message\":\"평균 평점 조회 성공\",\"result\":{\"averageRating\":4.5}}";
    public static final String SENDER_LIST =
            "{\"success\":true,\"code\":\"SENDER200_1\",\"message\":\"요청 성공.\",\"result\":[{\"deliveryId\":1,\"name\":\"노트북\",\"originPlace\":{\"id\":1,\"subwayRouteName\":\"2호선\",\"subwayStationName\":\"강남\"},\"destPlace\":{\"id\":2,\"subwayRouteName\":\"2호선\",\"subwayStationName\":\"홍대입구\"},\"status\":\"WAIT\"}]}";
    public static final String SENDER_DETAIL =
            "{\"success\":true,\"code\":\"SENDER200_1\",\"message\":\"요청 성공.\",\"result\":{\"id\":1,\"status\":\"DELIVERING\",\"shipperInfo\":{\"name\":\"홍길동\",\"picture\":\"https://example.com/profile.png\",\"place\":{\"id\":1,\"address\":\"서울시 강남구\"}},\"deliveryTimeLine\":[]}}";
    public static final String SENDER_PAYMENT =
            "{\"success\":true,\"code\":\"SENDER200_1\",\"message\":\"요청 성공.\",\"result\":{\"id\":1,\"basePoint\":2000,\"distancePoint\":200,\"weightPoint\":500,\"totalPoint\":2700}}";
    public static final String SENDER_CREATED =
            "{\"success\":true,\"code\":\"SENDER201_1\",\"message\":\"생성 성공.\",\"result\":\"1\"}";
    public static final String SENDER_OK =
            "{\"success\":true,\"code\":\"SENDER200_1\",\"message\":\"요청 성공.\",\"result\":null}";
    public static final String SHIPPER_LIST =
            "{\"success\":true,\"code\":\"SHIPPER200_1\",\"message\":\"요청 성공.\",\"result\":[{\"id\":1,\"senderInfo\":{\"name\":\"김발송\",\"picture\":\"https://example.com/profile.png\",\"place\":{\"id\":1,\"address\":\"서울시 강남구\"}},\"shipperInfo\":null,\"originPlace\":{\"id\":2,\"address\":\"서울시 강남구\"},\"destPlace\":{\"id\":3,\"address\":\"서울시 마포구\"},\"deliveryState\":\"WAIT\",\"memo\":\"안전 배송 부탁드립니다.\"}]}";
    public static final String SHIPPER_DETAIL =
            "{\"success\":true,\"code\":\"SHIPPER200_1\",\"message\":\"요청 성공.\",\"result\":{\"id\":1,\"senderInfo\":{\"name\":\"김발송\",\"picture\":\"https://example.com/profile.png\",\"place\":{\"id\":1,\"address\":\"서울시 강남구\"}},\"shipperInfo\":{\"name\":\"이배송\",\"picture\":\"https://example.com/shipper.png\",\"place\":{\"id\":4,\"address\":\"서울시 송파구\"}},\"originPlace\":{\"id\":2,\"address\":\"서울시 강남구\"},\"destPlace\":{\"id\":3,\"address\":\"서울시 마포구\"},\"deliveryState\":\"MATCHED\",\"memo\":\"안전 배송 부탁드립니다.\"}}";
    public static final String SHIPPER_OK =
            "{\"success\":true,\"code\":\"SHIPPER200_1\",\"message\":\"요청 성공.\",\"result\":null}";
    public static final String SUBWAY_SHORTEST_ROUTE =
            "{\"success\":true,\"code\":\"SUBWAY200_1\",\"message\":\"최단 경로 탐색 성공.\",\"result\":{\"shortestDistance\":5,\"transferCount\":1,\"stations\":[{\"id\":101,\"region\":\"수도권\",\"routeName\":\"수인분당\",\"stationName\":\"미금(분당서울대병원)\"},{\"id\":102,\"region\":\"수도권\",\"routeName\":\"수인분당\",\"stationName\":\"정자\"},{\"id\":201,\"region\":\"수도권\",\"routeName\":\"신분당\",\"stationName\":\"정자\"},{\"id\":202,\"region\":\"수도권\",\"routeName\":\"신분당\",\"stationName\":\"판교(판교테크노밸리)\"}]}}";
    public static final String SUBWAY_STATION_LIST =
            "{\"success\":true,\"code\":\"SUBWAY200_2\",\"message\":\"지하철역 검색 성공.\",\"result\":[{\"id\":101,\"routeName\":\"2호선\",\"stationName\":\"강남\"}]}";
}
